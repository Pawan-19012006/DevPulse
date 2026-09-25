package com.devpulse.ai.domain.session

import com.devpulse.ai.DevPulseApp
import com.devpulse.ai.data.local.DevPulseDatabase
import com.devpulse.ai.data.local.entity.DevSessionEntity
import com.devpulse.ai.data.local.entity.OnePercentImprovementEntity
import com.devpulse.ai.data.local.entity.SessionBlockEntity
import com.devpulse.ai.data.local.entity.SessionHandoffEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

import com.devpulse.ai.data.local.dao.DevSessionDao
import com.devpulse.ai.data.local.dao.HealthEventDao
import com.devpulse.ai.data.local.dao.OnePercentImprovementDao
import com.devpulse.ai.data.local.dao.SessionBlockDao
import com.devpulse.ai.data.local.dao.SessionHandoffDao
import com.devpulse.ai.data.local.entity.HealthEventEntity

class SessionEngine(
    private val sessionDao: DevSessionDao,
    private val blockDao: SessionBlockDao,
    private val handoffDao: SessionHandoffDao,
    private val improvementDao: OnePercentImprovementDao,
    private val healthEventDao: HealthEventDao? = null,
    private val timeProvider: TimeProvider = SystemTimeProvider,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + Job()),
    private val autoStartTicker: Boolean = true
) {
    constructor(
        database: DevPulseDatabase = DevPulseApp.instance.database,
        timeProvider: TimeProvider = SystemTimeProvider,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Main + Job())
    ) : this(
        sessionDao = database.devSessionDao(),
        blockDao = database.sessionBlockDao(),
        handoffDao = database.sessionHandoffDao(),
        improvementDao = database.onePercentImprovementDao(),
        healthEventDao = database.healthEventDao(),
        timeProvider = timeProvider,
        scope = scope,
        autoStartTicker = true
    )

    private val _state = MutableStateFlow<SessionEngineState>(SessionEngineState.Idle)
    val state: StateFlow<SessionEngineState> = _state.asStateFlow()

    private val _activeNudge = MutableStateFlow<HealthNudge?>(null)
    val activeNudge: StateFlow<HealthNudge?> = _activeNudge.asStateFlow()

    private val triggeredNudgesInBlock = mutableSetOf<HealthEventType>()

    private var tickerJob: Job? = null

    init {
        scope.launch {
            restoreActiveSession()
        }
    }

    suspend fun getLatestUnfinishedHandoff(): SessionHandoffEntity? = withContext(Dispatchers.IO) {
        handoffDao.getLatestUnfinishedHandoff()
    }

    suspend fun startFocusSession(
        activityType: SessionActivityType,
        overallGoal: String,
        initialMindset: DeveloperState?,
        durationMinutes: Int
    ): String = withContext(Dispatchers.IO) {
        stopTicker()
        val sessionId = UUID.randomUUID().toString()
        val now = timeProvider.now()
        val plannedSeconds = durationMinutes * 60L

        val sessionEntity = DevSessionEntity(
            id = sessionId,
            title = activityType.displayName,
            activityType = activityType.name,
            goal = overallGoal,
            targetDurationMinutes = durationMinutes,
            actualDurationMinutes = 0,
            initialState = initialMindset?.name,
            finalState = null,
            status = "IN_PROGRESS",
            startedAt = now,
            completedAt = null,
            sessionMode = SessionMode.FOCUS.name,
            currentObjective = overallGoal,
            workBlockDurationMinutes = durationMinutes,
            recoveryBlockDurationMinutes = 5,
            totalBlocks = 1,
            currentBlockIndex = 0,
            currentMindset = initialMindset?.name
        )
        sessionDao.upsertSession(sessionEntity)

        val blockId = UUID.randomUUID().toString()
        val blockEntity = SessionBlockEntity(
            id = blockId,
            sessionId = sessionId,
            blockIndex = 0,
            blockType = BlockType.WORK.name,
            objective = overallGoal,
            plannedDurationSeconds = plannedSeconds,
            startedAt = now,
            pausedAt = null,
            totalPausedDurationMs = 0L,
            completedAt = null,
            status = BlockStatus.WORKING.name
        )
        blockDao.upsertBlock(blockEntity)

        val sessionDomain = sessionEntity.toDomain()
        val blockDomain = blockEntity.toDomain()

        _state.value = SessionEngineState.Working(
            session = sessionDomain,
            currentBlock = blockDomain,
            elapsedSeconds = 0L,
            remainingSeconds = plannedSeconds,
            progressRatio = 0.0f
        )

        startTicker()
        sessionId
    }

    suspend fun startDeepWorkSession(
        activityType: SessionActivityType,
        overallGoal: String,
        initialObjective: String,
        initialMindset: DeveloperState?,
        totalPlannedHours: Int,
        workDurationMinutes: Int,
        recoveryDurationMinutes: Int
    ): String = withContext(Dispatchers.IO) {
        stopTicker()
        val sessionId = UUID.randomUUID().toString()
        val now = timeProvider.now()
        val totalMinutes = totalPlannedHours * 60
        val blockCycle = (workDurationMinutes + recoveryDurationMinutes).coerceAtLeast(1)
        val calculatedBlocks = (totalMinutes / blockCycle).coerceAtLeast(1)
        val plannedSeconds = workDurationMinutes * 60L

        val sessionEntity = DevSessionEntity(
            id = sessionId,
            title = activityType.displayName,
            activityType = activityType.name,
            goal = overallGoal,
            targetDurationMinutes = totalMinutes,
            actualDurationMinutes = 0,
            initialState = initialMindset?.name,
            finalState = null,
            status = "IN_PROGRESS",
            startedAt = now,
            completedAt = null,
            sessionMode = SessionMode.DEEP_WORK.name,
            currentObjective = initialObjective,
            workBlockDurationMinutes = workDurationMinutes,
            recoveryBlockDurationMinutes = recoveryDurationMinutes,
            totalBlocks = calculatedBlocks,
            currentBlockIndex = 0,
            currentMindset = initialMindset?.name
        )
        sessionDao.upsertSession(sessionEntity)

        val blockId = UUID.randomUUID().toString()
        val blockEntity = SessionBlockEntity(
            id = blockId,
            sessionId = sessionId,
            blockIndex = 0,
            blockType = BlockType.WORK.name,
            objective = initialObjective,
            plannedDurationSeconds = plannedSeconds,
            startedAt = now,
            pausedAt = null,
            totalPausedDurationMs = 0L,
            completedAt = null,
            status = BlockStatus.WORKING.name
        )
        blockDao.upsertBlock(blockEntity)

        val sessionDomain = sessionEntity.toDomain()
        val blockDomain = blockEntity.toDomain()

        _state.value = SessionEngineState.Working(
            session = sessionDomain,
            currentBlock = blockDomain,
            elapsedSeconds = 0L,
            remainingSeconds = plannedSeconds,
            progressRatio = 0.0f
        )

        startTicker()
        sessionId
    }

    fun pause() {
        val current = _state.value
        if (current is SessionEngineState.Working) {
            val now = timeProvider.now()
            val pausedBlock = current.currentBlock.copy(
                pausedAt = now,
                status = BlockStatus.PAUSED
            )
            val pausedSession = current.session.copy(status = SessionStatus.IN_PROGRESS)

            scope.launch(Dispatchers.IO) {
                blockDao.updateBlockState(
                    id = pausedBlock.id,
                    pausedAt = now,
                    totalPausedDurationMs = pausedBlock.totalPausedDurationMs,
                    status = BlockStatus.PAUSED.name,
                    completedAt = null
                )
                sessionDao.updateStatus(
                    sessionId = pausedSession.id,
                    status = "PAUSED",
                    completedAt = null,
                    actualDurationMinutes = (pausedBlock.calculateElapsedSeconds(now) / 60L).toInt()
                )
            }

            _state.value = SessionEngineState.Paused(
                session = pausedSession,
                currentBlock = pausedBlock,
                elapsedSeconds = current.elapsedSeconds,
                remainingSeconds = current.remainingSeconds,
                progressRatio = current.progressRatio
            )
            stopTicker()
        }
    }

    fun resume() {
        val current = _state.value
        if (current is SessionEngineState.Paused) {
            val now = timeProvider.now()
            val pausedAt = current.currentBlock.pausedAt ?: now
            val addedPause = (now - pausedAt).coerceAtLeast(0L)
            val updatedPausedDuration = current.currentBlock.totalPausedDurationMs + addedPause

            val resumedBlock = current.currentBlock.copy(
                pausedAt = null,
                totalPausedDurationMs = updatedPausedDuration,
                status = BlockStatus.WORKING
            )
            val resumedSession = current.session.copy(status = SessionStatus.IN_PROGRESS)

            scope.launch(Dispatchers.IO) {
                blockDao.updateBlockState(
                    id = resumedBlock.id,
                    pausedAt = null,
                    totalPausedDurationMs = updatedPausedDuration,
                    status = BlockStatus.WORKING.name,
                    completedAt = null
                )
                sessionDao.updateStatus(
                    sessionId = resumedSession.id,
                    status = "IN_PROGRESS",
                    completedAt = null,
                    actualDurationMinutes = (resumedBlock.calculateElapsedSeconds(now) / 60L).toInt()
                )
            }

            val elapsed = resumedBlock.calculateElapsedSeconds(now)
            val remaining = resumedBlock.calculateRemainingSeconds(now)
            val progress = resumedBlock.calculateProgressRatio(now)

            _state.value = SessionEngineState.Working(
                session = resumedSession,
                currentBlock = resumedBlock,
                elapsedSeconds = elapsed,
                remainingSeconds = remaining,
                progressRatio = progress
            )
            startTicker()
        }
    }

    fun reportMindset(mindset: DeveloperState) {
        val current = _state.value
        val sessionId = when (current) {
            is SessionEngineState.Working -> current.session.id
            is SessionEngineState.Paused -> current.session.id
            is SessionEngineState.Recovery -> current.session.id
            is SessionEngineState.WorkBlockHandoff -> current.session.id
            is SessionEngineState.ReadyForNextBlock -> current.session.id
            is SessionEngineState.SessionCompleteReflection -> current.session.id
            else -> null
        } ?: return

        scope.launch(Dispatchers.IO) {
            sessionDao.updateMindset(sessionId, mindset.name)
        }
    }

    suspend fun submitWorkBlockHandoff(
        accomplished: String,
        nextObjective: String
    ) = withContext(Dispatchers.IO) {
        val current = _state.value
        if (current !is SessionEngineState.WorkBlockHandoff) return@withContext

        val now = timeProvider.now()
        val handoffId = UUID.randomUUID().toString()
        val handoff = SessionHandoffEntity(
            id = handoffId,
            sessionId = current.session.id,
            blockIndex = current.completedBlock.blockIndex,
            sessionGoal = current.session.overallGoal,
            accomplished = accomplished.trim().ifBlank { "Completed focused work block." },
            nextObjective = nextObjective.trim().ifBlank { "Continue next objective." },
            isUnfinished = true,
            createdAt = now
        )
        handoffDao.upsertHandoff(handoff)

        if (current.isFinalBlock) {
            val totalWorkedMinutes = current.completedBlock.calculateElapsedSeconds(now) / 60
            sessionDao.updateStatus(
                sessionId = current.session.id,
                status = "COMPLETED",
                completedAt = now,
                actualDurationMinutes = totalWorkedMinutes.toInt().coerceAtLeast(1)
            )
            val completedSession = current.session.copy(
                status = SessionStatus.COMPLETED,
                completedAt = now
            )
            _state.value = SessionEngineState.SessionCompleteReflection(
                session = completedSession,
                lastHandoff = handoff
            )
        } else {
            // Start Recovery Block
            val recoveryBlockId = UUID.randomUUID().toString()
            val recoveryPlannedSeconds = current.session.recoveryBlockDurationMinutes * 60L
            val recoveryBlockEntity = SessionBlockEntity(
                id = recoveryBlockId,
                sessionId = current.session.id,
                blockIndex = current.completedBlock.blockIndex,
                blockType = BlockType.RECOVERY.name,
                objective = "Take an intentional recovery break",
                plannedDurationSeconds = recoveryPlannedSeconds,
                startedAt = now,
                pausedAt = null,
                totalPausedDurationMs = 0L,
                completedAt = null,
                status = BlockStatus.WORKING.name
            )
            blockDao.upsertBlock(recoveryBlockEntity)

            _state.value = SessionEngineState.Recovery(
                session = current.session,
                recoveryBlock = recoveryBlockEntity.toDomain(),
                nextObjective = handoff.nextObjective,
                elapsedSeconds = 0L,
                remainingSeconds = recoveryPlannedSeconds,
                progressRatio = 0.0f
            )
            startTicker()
        }
    }

    fun endRecoveryEarly() {
        val current = _state.value
        if (current !is SessionEngineState.Recovery) return

        stopTicker()
        val now = timeProvider.now()
        scope.launch(Dispatchers.IO) {
            blockDao.updateBlockState(
                id = current.recoveryBlock.id,
                pausedAt = null,
                totalPausedDurationMs = current.recoveryBlock.totalPausedDurationMs,
                status = BlockStatus.COMPLETED.name,
                completedAt = now
            )
        }

        _state.value = SessionEngineState.ReadyForNextBlock(
            session = current.session,
            nextBlockIndex = current.session.currentBlockIndex + 1,
            carryoverObjective = current.nextObjective
        )
    }

    suspend fun continueNextBlock(customObjective: String? = null) = withContext(Dispatchers.IO) {
        val current = _state.value
        if (current !is SessionEngineState.ReadyForNextBlock) return@withContext

        val now = timeProvider.now()
        val targetObjective = customObjective?.trim()?.ifBlank { null } ?: current.carryoverObjective
        val nextBlockIndex = current.nextBlockIndex
        val plannedSeconds = current.session.workBlockDurationMinutes * 60L

        sessionDao.updateCurrentBlock(current.session.id, nextBlockIndex, targetObjective)

        val nextBlockId = UUID.randomUUID().toString()
        val nextBlockEntity = SessionBlockEntity(
            id = nextBlockId,
            sessionId = current.session.id,
            blockIndex = nextBlockIndex,
            blockType = BlockType.WORK.name,
            objective = targetObjective,
            plannedDurationSeconds = plannedSeconds,
            startedAt = now,
            pausedAt = null,
            totalPausedDurationMs = 0L,
            completedAt = null,
            status = BlockStatus.WORKING.name
        )
        blockDao.upsertBlock(nextBlockEntity)

        val updatedSession = current.session.copy(
            currentBlockIndex = nextBlockIndex,
            currentObjective = targetObjective
        )

        _state.value = SessionEngineState.Working(
            session = updatedSession,
            currentBlock = nextBlockEntity.toDomain(),
            elapsedSeconds = 0L,
            remainingSeconds = plannedSeconds,
            progressRatio = 0.0f
        )
        startTicker()
    }

    suspend fun completeSessionEarly(
        accomplished: String,
        nextObjective: String
    ) = withContext(Dispatchers.IO) {
        stopTicker()
        val current = _state.value
        val (session, currentBlock) = when (current) {
            is SessionEngineState.Working -> Pair(current.session, current.currentBlock)
            is SessionEngineState.Paused -> Pair(current.session, current.currentBlock)
            is SessionEngineState.Recovery -> Pair(current.session, current.recoveryBlock)
            else -> return@withContext
        }

        val now = timeProvider.now()
        blockDao.updateBlockState(
            id = currentBlock.id,
            pausedAt = null,
            totalPausedDurationMs = currentBlock.totalPausedDurationMs,
            status = BlockStatus.COMPLETED.name,
            completedAt = now
        )

        val handoffId = UUID.randomUUID().toString()
        val handoff = SessionHandoffEntity(
            id = handoffId,
            sessionId = session.id,
            blockIndex = currentBlock.blockIndex,
            sessionGoal = session.overallGoal,
            accomplished = accomplished.trim().ifBlank { "Stopped session early." },
            nextObjective = nextObjective.trim().ifBlank { session.currentObjective },
            isUnfinished = true,
            createdAt = now
        )
        handoffDao.upsertHandoff(handoff)

        val workedMinutes = (currentBlock.calculateElapsedSeconds(now) / 60L).toInt().coerceAtLeast(1)
        sessionDao.updateStatus(
            sessionId = session.id,
            status = "COMPLETED",
            completedAt = now,
            actualDurationMinutes = workedMinutes
        )

        val completedSession = session.copy(
            status = SessionStatus.COMPLETED,
            completedAt = now
        )
        _state.value = SessionEngineState.SessionCompleteReflection(
            session = completedSession,
            lastHandoff = handoff
        )
    }

    suspend fun submitFinalReflection(
        category: ImprovementCategory,
        reflectionText: String
    ) = withContext(Dispatchers.IO) {
        val current = _state.value
        if (current !is SessionEngineState.SessionCompleteReflection) return@withContext

        val now = timeProvider.now()
        val improvementId = UUID.randomUUID().toString()
        val improvement = OnePercentImprovementEntity(
            id = improvementId,
            sessionId = current.session.id,
            category = category.name,
            reflectionText = reflectionText.trim().ifBlank { "Showed up and kept building." },
            timestamp = now
        )
        improvementDao.upsertImprovement(improvement)

        _state.value = SessionEngineState.Finished(
            sessionId = current.session.id,
            improvement = improvement
        )
    }

    fun resetToIdle() {
        stopTicker()
        _activeNudge.value = null
        triggeredNudgesInBlock.clear()
        _state.value = SessionEngineState.Idle
    }

    private fun checkHealthNudges(elapsedSeconds: Long) {
        if (_activeNudge.value != null) return
        when {
            elapsedSeconds in 600L..605L && HealthEventType.HYDRATION !in triggeredNudgesInBlock -> {
                triggeredNudgesInBlock.add(HealthEventType.HYDRATION)
                _activeNudge.value = HealthNudge(
                    type = HealthEventType.HYDRATION,
                    title = "QUICK RESET",
                    message = "You've been focused for a while. Take a drink of water."
                )
            }
            elapsedSeconds in 1200L..1205L && HealthEventType.EYE_RECOVERY !in triggeredNudgesInBlock -> {
                triggeredNudgesInBlock.add(HealthEventType.EYE_RECOVERY)
                _activeNudge.value = HealthNudge(
                    type = HealthEventType.EYE_RECOVERY,
                    title = "REST YOUR EYES",
                    message = "Look away from the screen for 20 seconds to release tension."
                )
            }
            elapsedSeconds in 1800L..1805L && HealthEventType.MOVEMENT !in triggeredNudgesInBlock -> {
                triggeredNudgesInBlock.add(HealthEventType.MOVEMENT)
                _activeNudge.value = HealthNudge(
                    type = HealthEventType.MOVEMENT,
                    title = "POSTURE RESET",
                    message = "Stand up, roll your shoulders, and stretch your spine."
                )
            }
        }
    }

    /**
     * Development / Prototype accelerator to test session health nudges without waiting 10/20 mins.
     */
    fun triggerPrototypeNudge(type: HealthEventType = HealthEventType.HYDRATION) {
        triggeredNudgesInBlock.add(type)
        val message = when (type) {
            HealthEventType.HYDRATION -> "You've been focused for a while. Take a drink of water."
            HealthEventType.EYE_RECOVERY -> "Look away from the screen for 20 seconds to release tension."
            HealthEventType.MOVEMENT -> "Stand up, roll your shoulders, and stretch your spine."
            else -> "Take a brief pause to refresh your mind."
        }
        val title = when (type) {
            HealthEventType.HYDRATION -> "QUICK RESET"
            HealthEventType.EYE_RECOVERY -> "REST YOUR EYES"
            HealthEventType.MOVEMENT -> "POSTURE RESET"
            else -> "RECOVERY CHECK"
        }
        _activeNudge.value = HealthNudge(type = type, title = title, message = message)
    }

    fun completeHealthNudge(type: HealthEventType? = null) {
        val targetType = type ?: _activeNudge.value?.type ?: HealthEventType.HYDRATION
        val currentSessionId = when (val s = _state.value) {
            is SessionEngineState.Working -> s.session.id
            is SessionEngineState.Paused -> s.session.id
            is SessionEngineState.Recovery -> s.session.id
            else -> null
        }
        val now = timeProvider.now()
        scope.launch {
            healthEventDao?.upsertHealthEvent(
                HealthEventEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = currentSessionId,
                    type = targetType.name,
                    timestamp = now,
                    completed = true,
                    source = "SESSION_NUDGE"
                )
            )
        }
        _activeNudge.value = null
    }

    fun dismissHealthNudge() {
        _activeNudge.value = null
    }

    fun recordGuidedRecoveryHealthEvent(type: HealthEventType) {
        val currentSessionId = when (val s = _state.value) {
            is SessionEngineState.Working -> s.session.id
            is SessionEngineState.Paused -> s.session.id
            is SessionEngineState.Recovery -> s.session.id
            is SessionEngineState.ReadyForNextBlock -> s.session.id
            is SessionEngineState.WorkBlockHandoff -> s.session.id
            else -> null
        }
        val now = timeProvider.now()
        scope.launch {
            healthEventDao?.upsertHealthEvent(
                HealthEventEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = currentSessionId,
                    type = type.name,
                    timestamp = now,
                    completed = true,
                    source = "GUIDED_RECOVERY"
                )
            )
        }
    }

    /**
     * Ticker loop calculating timestamps dynamically.
     */
    private fun startTicker() {
        stopTicker()
        if (!autoStartTicker) return
        tickerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                tick()
            }
        }
    }

    /**
     * Fast-forwards the current timed interval (Work block, Paused block, or Recovery)
     * to simulated completion. This is a prototype accelerator used for testing and demonstrating
     * the Deep Work session engine without waiting for 45/15 minutes.
     */
    fun fastForwardCurrentBlock() {
        val now = timeProvider.now()
        when (val current = _state.value) {
            is SessionEngineState.Working -> {
                stopTicker()
                val block = current.currentBlock
                val simulatedCompletion = now
                scope.launch {
                    blockDao.updateBlockState(
                        id = block.id,
                        pausedAt = null,
                        totalPausedDurationMs = block.totalPausedDurationMs,
                        status = BlockStatus.COMPLETED.name,
                        completedAt = simulatedCompletion
                    )
                }
                val isFinal = current.session.sessionMode == SessionMode.FOCUS ||
                        (current.session.currentBlockIndex >= current.session.totalBlocks - 1)

                _state.value = SessionEngineState.WorkBlockHandoff(
                    session = current.session,
                    completedBlock = block.copy(
                        status = BlockStatus.COMPLETED,
                        completedAt = simulatedCompletion
                    ),
                    isFinalBlock = isFinal
                )
            }
            is SessionEngineState.Paused -> {
                stopTicker()
                val block = current.currentBlock
                val activePause = if (block.pausedAt != null) (now - block.pausedAt).coerceAtLeast(0L) else 0L
                val totalPause = block.totalPausedDurationMs + activePause
                scope.launch {
                    blockDao.updateBlockState(
                        id = block.id,
                        pausedAt = null,
                        totalPausedDurationMs = totalPause,
                        status = BlockStatus.COMPLETED.name,
                        completedAt = now
                    )
                }
                val isFinal = current.session.sessionMode == SessionMode.FOCUS ||
                        (current.session.currentBlockIndex >= current.session.totalBlocks - 1)

                _state.value = SessionEngineState.WorkBlockHandoff(
                    session = current.session,
                    completedBlock = block.copy(
                        status = BlockStatus.COMPLETED,
                        totalPausedDurationMs = totalPause,
                        pausedAt = null,
                        completedAt = now
                    ),
                    isFinalBlock = isFinal
                )
            }
            is SessionEngineState.Recovery -> {
                endRecoveryEarly()
            }
            else -> {
                // Not in a timed state
            }
        }
    }

    fun tick() {
        val now = timeProvider.now()
        when (val current = _state.value) {
            is SessionEngineState.Working -> {
                val block = current.currentBlock
                val elapsed = block.calculateElapsedSeconds(now)
                val remaining = block.calculateRemainingSeconds(now)
                val progress = block.calculateProgressRatio(now)

                if (block.isTimeFinished(now)) {
                    stopTicker()
                    scope.launch(Dispatchers.IO) {
                        blockDao.updateBlockState(
                            id = block.id,
                            pausedAt = null,
                            totalPausedDurationMs = block.totalPausedDurationMs,
                            status = BlockStatus.COMPLETED.name,
                            completedAt = now
                        )
                    }
                    val isFinal = current.session.sessionMode == SessionMode.FOCUS ||
                            (current.session.currentBlockIndex >= current.session.totalBlocks - 1)

                    _state.value = SessionEngineState.WorkBlockHandoff(
                        session = current.session,
                        completedBlock = block.copy(
                            status = BlockStatus.COMPLETED,
                            completedAt = now
                        ),
                        isFinalBlock = isFinal
                    )
                } else {
                    _state.value = current.copy(
                        elapsedSeconds = elapsed,
                        remainingSeconds = remaining,
                        progressRatio = progress
                    )
                    checkHealthNudges(elapsed)
                }
            }

            is SessionEngineState.Recovery -> {
                val block = current.recoveryBlock
                val elapsed = block.calculateElapsedSeconds(now)
                val remaining = block.calculateRemainingSeconds(now)
                val progress = block.calculateProgressRatio(now)

                if (block.isTimeFinished(now)) {
                    stopTicker()
                    scope.launch(Dispatchers.IO) {
                        blockDao.updateBlockState(
                            id = block.id,
                            pausedAt = null,
                            totalPausedDurationMs = block.totalPausedDurationMs,
                            status = BlockStatus.COMPLETED.name,
                            completedAt = now
                        )
                    }
                    _state.value = SessionEngineState.ReadyForNextBlock(
                        session = current.session,
                        nextBlockIndex = current.session.currentBlockIndex + 1,
                        carryoverObjective = current.nextObjective
                    )
                } else {
                    _state.value = current.copy(
                        elapsedSeconds = elapsed,
                        remainingSeconds = remaining,
                        progressRatio = progress
                    )
                }
            }

            else -> {
                // Idle, Paused, Handoff, Reflection: no active ticking needed
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    suspend fun restoreActiveSession() = withContext(Dispatchers.IO) {
        val activeSessionEntity = sessionDao.getActiveSession() ?: return@withContext
        val activeBlockEntity = blockDao.getActiveBlock(activeSessionEntity.id)

        val sessionDomain = activeSessionEntity.toDomain()
        val now = timeProvider.now()

        if (activeBlockEntity != null) {
            val blockDomain = activeBlockEntity.toDomain()
            val isPaused = activeBlockEntity.status == BlockStatus.PAUSED.name

            if (blockDomain.blockType == BlockType.WORK) {
                if (blockDomain.isTimeFinished(now)) {
                    val isFinal = sessionDomain.sessionMode == SessionMode.FOCUS ||
                            (sessionDomain.currentBlockIndex >= sessionDomain.totalBlocks - 1)
                    _state.value = SessionEngineState.WorkBlockHandoff(
                        session = sessionDomain,
                        completedBlock = blockDomain.copy(status = BlockStatus.COMPLETED, completedAt = now),
                        isFinalBlock = isFinal
                    )
                } else if (isPaused) {
                    _state.value = SessionEngineState.Paused(
                        session = sessionDomain,
                        currentBlock = blockDomain,
                        elapsedSeconds = blockDomain.calculateElapsedSeconds(now),
                        remainingSeconds = blockDomain.calculateRemainingSeconds(now),
                        progressRatio = blockDomain.calculateProgressRatio(now)
                    )
                } else {
                    _state.value = SessionEngineState.Working(
                        session = sessionDomain,
                        currentBlock = blockDomain,
                        elapsedSeconds = blockDomain.calculateElapsedSeconds(now),
                        remainingSeconds = blockDomain.calculateRemainingSeconds(now),
                        progressRatio = blockDomain.calculateProgressRatio(now)
                    )
                    startTicker()
                }
            } else {
                // Recovery block
                if (blockDomain.isTimeFinished(now)) {
                    _state.value = SessionEngineState.ReadyForNextBlock(
                        session = sessionDomain,
                        nextBlockIndex = sessionDomain.currentBlockIndex + 1,
                        carryoverObjective = sessionDomain.currentObjective
                    )
                } else {
                    _state.value = SessionEngineState.Recovery(
                        session = sessionDomain,
                        recoveryBlock = blockDomain,
                        nextObjective = sessionDomain.currentObjective,
                        elapsedSeconds = blockDomain.calculateElapsedSeconds(now),
                        remainingSeconds = blockDomain.calculateRemainingSeconds(now),
                        progressRatio = blockDomain.calculateProgressRatio(now)
                    )
                    startTicker()
                }
            }
        }
    }

    private fun DevSessionEntity.toDomain(): DevSession {
        return DevSession(
            id = id,
            title = title,
            sessionMode = runCatching { SessionMode.valueOf(sessionMode) }.getOrDefault(SessionMode.FOCUS),
            activityType = runCatching { SessionActivityType.valueOf(activityType) }.getOrDefault(SessionActivityType.CODING),
            overallGoal = goal ?: title,
            currentObjective = currentObjective,
            initialMindset = initialState?.let { runCatching { DeveloperState.valueOf(it) }.getOrNull() },
            currentMindset = currentMindset?.let { runCatching { DeveloperState.valueOf(it) }.getOrNull() },
            totalPlannedDurationMinutes = targetDurationMinutes,
            workBlockDurationMinutes = workBlockDurationMinutes,
            recoveryBlockDurationMinutes = recoveryBlockDurationMinutes,
            totalBlocks = totalBlocks,
            currentBlockIndex = currentBlockIndex,
            status = runCatching { SessionStatus.valueOf(status) }.getOrDefault(SessionStatus.IN_PROGRESS),
            startedAt = startedAt,
            completedAt = completedAt
        )
    }

    private fun SessionBlockEntity.toDomain(): SessionBlock {
        return SessionBlock(
            id = id,
            sessionId = sessionId,
            blockIndex = blockIndex,
            blockType = runCatching { BlockType.valueOf(blockType) }.getOrDefault(BlockType.WORK),
            objective = objective,
            plannedDurationSeconds = plannedDurationSeconds,
            startedAt = startedAt,
            pausedAt = pausedAt,
            totalPausedDurationMs = totalPausedDurationMs,
            completedAt = completedAt,
            status = runCatching { BlockStatus.valueOf(status) }.getOrDefault(BlockStatus.WORKING)
        )
    }
}
