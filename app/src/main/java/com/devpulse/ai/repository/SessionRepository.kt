package com.devpulse.ai.repository

import com.devpulse.ai.DevPulseApp
import com.devpulse.ai.data.local.DevPulseDatabase
import com.devpulse.ai.data.local.entity.DevSessionEntity
import com.devpulse.ai.data.local.entity.HealthEventEntity
import com.devpulse.ai.data.local.entity.OnePercentImprovementEntity
import com.devpulse.ai.data.local.entity.PreSessionChecklistItemEntity
import com.devpulse.ai.data.local.entity.RecoveryActivityEntity
import com.devpulse.ai.data.local.entity.SessionHandoffEntity
import com.devpulse.ai.domain.session.DeveloperState
import com.devpulse.ai.domain.session.ImprovementCategory
import com.devpulse.ai.domain.session.SessionActivityType
import com.devpulse.ai.domain.session.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

class SessionRepository(
    private val database: DevPulseDatabase = DevPulseApp.instance.database
) {
    private val sessionDao = database.devSessionDao()
    private val blockDao = database.sessionBlockDao()
    private val handoffDao = database.sessionHandoffDao()
    private val improvementDao = database.onePercentImprovementDao()
    private val checklistDao = database.preSessionChecklistDao()
    private val recoveryDao = database.recoveryActivityDao()
    private val healthEventDao = database.healthEventDao()

    fun observeActiveSession(): Flow<DevSessionEntity?> = sessionDao.observeActiveSession()

    fun observeLatestUnfinishedHandoff(): Flow<SessionHandoffEntity?> =
        handoffDao.observeLatestUnfinishedHandoff()

    suspend fun getLatestUnfinishedHandoff(): SessionHandoffEntity? = withContext(Dispatchers.IO) {
        handoffDao.getLatestUnfinishedHandoff()
    }

    fun observeAllSessions(): Flow<List<DevSessionEntity>> = sessionDao.observeAllSessions()

    fun observeTodaySessions(): Flow<List<DevSessionEntity>> {
        val (start, end) = getTodayTimeRange()
        return sessionDao.observeSessionsBetween(start, end)
    }

    fun observeAllImprovements(): Flow<List<OnePercentImprovementEntity>> =
        improvementDao.observeAllImprovements()

    fun observeRecentImprovements(limit: Int = 10): Flow<List<OnePercentImprovementEntity>> =
        improvementDao.observeRecentImprovements(limit)

    fun observeTotalSessionsCount(): Flow<Int> = sessionDao.observeTotalSessionsCount()

    fun observeTotalImprovementsCount(): Flow<Int> = improvementDao.observeTotalImprovementsCount()

    fun observeTodaySessionsCount(): Flow<Int> {
        val (start, end) = getTodayTimeRange()
        return sessionDao.observeSessionsCountBetween(start, end)
    }

    fun observeTodayImprovementsCount(): Flow<Int> {
        val (start, end) = getTodayTimeRange()
        return improvementDao.observeImprovementsCountBetween(start, end)
    }

    fun observeChecklist(): Flow<List<PreSessionChecklistItemEntity>> =
        checklistDao.observeChecklist()

    fun observeRecoveryActivities(): Flow<List<RecoveryActivityEntity>> =
        recoveryDao.observeActivities()

    fun observeTodayHealthEvents(): Flow<List<HealthEventEntity>> {
        val (start, end) = getTodayTimeRange()
        return healthEventDao.observeHealthEventsBetween(start, end)
    }

    fun observeTodayHealthCount(type: String): Flow<Int> {
        val (start, end) = getTodayTimeRange()
        return healthEventDao.observeCountByTypeBetween(type, start, end)
    }

    fun observeAllHealthEvents(): Flow<List<HealthEventEntity>> =
        healthEventDao.observeAllHealthEvents()

    fun observeRecentHealthEvents(limit: Int = 20): Flow<List<HealthEventEntity>> =
        healthEventDao.observeRecentHealthEvents(limit)

    fun observeHealthEventsForSession(sessionId: String): Flow<List<HealthEventEntity>> =
        healthEventDao.observeHealthEventsForSession(sessionId)

    suspend fun getHealthEventsForSession(sessionId: String): List<HealthEventEntity> = withContext(Dispatchers.IO) {
        healthEventDao.getHealthEventsForSession(sessionId)
    }

    suspend fun recordHealthEvent(
        sessionId: String?,
        type: String,
        source: String = "SESSION_NUDGE",
        completed: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        val eventId = UUID.randomUUID().toString()
        val event = HealthEventEntity(
            id = eventId,
            sessionId = sessionId,
            type = type,
            timestamp = System.currentTimeMillis(),
            completed = completed,
            source = source
        )
        healthEventDao.upsertHealthEvent(event)
        eventId
    }

    suspend fun ensureDefaultsSeeded() = withContext(Dispatchers.IO) {
        if (checklistDao.getChecklistCount() == 0) {
            val defaults = listOf(
                PreSessionChecklistItemEntity(
                    id = "check_notifications",
                    title = "Turn off distracting notifications",
                    isDefault = true,
                    isEnabled = true,
                    sortOrder = 0
                ),
                PreSessionChecklistItemEntity(
                    id = "check_water",
                    title = "Keep water nearby",
                    isDefault = true,
                    isEnabled = true,
                    sortOrder = 1
                ),
                PreSessionChecklistItemEntity(
                    id = "check_goal",
                    title = "Define ONE specific problem to solve",
                    isDefault = true,
                    isEnabled = true,
                    sortOrder = 2
                ),
                PreSessionChecklistItemEntity(
                    id = "check_tools",
                    title = "Open required IDE, tools & docs",
                    isDefault = true,
                    isEnabled = true,
                    sortOrder = 3
                ),
                PreSessionChecklistItemEntity(
                    id = "check_phone",
                    title = "Put phone out of sight / focus mode",
                    isDefault = true,
                    isEnabled = true,
                    sortOrder = 4
                )
            )
            checklistDao.upsertChecklistItems(defaults)
        }

        if (recoveryDao.getActivityCount() == 0) {
            val defaultActivities = listOf(
                RecoveryActivityEntity(
                    id = "rec_water",
                    title = "Drink a full glass of water",
                    category = "HYDRATION",
                    durationMinutes = 2,
                    isDefault = true
                ),
                RecoveryActivityEntity(
                    id = "rec_eye_rest",
                    title = "20-20-20 Eye Rest & Screen Break",
                    category = "MENTAL",
                    durationMinutes = 2,
                    isDefault = true
                ),
                RecoveryActivityEntity(
                    id = "rec_stretch",
                    title = "Neck, shoulder & spine stretch",
                    category = "PHYSICAL",
                    durationMinutes = 5,
                    isDefault = true
                ),
                RecoveryActivityEntity(
                    id = "rec_walk",
                    title = "Short walking break around the room",
                    category = "PHYSICAL",
                    durationMinutes = 5,
                    isDefault = true
                ),
                RecoveryActivityEntity(
                    id = "rec_air",
                    title = "Step outside for fresh air & sunlight",
                    category = "MENTAL",
                    durationMinutes = 5,
                    isDefault = true
                )
            )
            recoveryDao.upsertActivities(defaultActivities)
        }
    }

    suspend fun createSession(
        activityType: SessionActivityType,
        targetDurationMinutes: Int = 25,
        goal: String? = null,
        initialState: DeveloperState? = null
    ): String = withContext(Dispatchers.IO) {
        val sessionId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val session = DevSessionEntity(
            id = sessionId,
            title = activityType.displayName,
            activityType = activityType.name,
            goal = goal?.trim()?.ifEmpty { null },
            targetDurationMinutes = targetDurationMinutes,
            actualDurationMinutes = 0,
            initialState = initialState?.name,
            finalState = null,
            status = SessionStatus.IN_PROGRESS.name,
            startedAt = now,
            completedAt = null
        )
        sessionDao.upsertSession(session)
        sessionId
    }

    suspend fun completeSession(
        sessionId: String,
        actualDurationMinutes: Int,
        finalState: DeveloperState? = null,
        category: ImprovementCategory,
        reflectionText: String
    ): String = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val existingSession = sessionDao.getSessionById(sessionId)
        if (existingSession != null) {
            val updated = existingSession.copy(
                actualDurationMinutes = actualDurationMinutes,
                finalState = finalState?.name,
                status = SessionStatus.COMPLETED.name,
                completedAt = now
            )
            sessionDao.upsertSession(updated)
        }

        val improvementId = UUID.randomUUID().toString()
        val improvement = OnePercentImprovementEntity(
            id = improvementId,
            sessionId = sessionId,
            category = category.name,
            reflectionText = reflectionText.trim(),
            timestamp = now
        )
        improvementDao.upsertImprovement(improvement)
        improvementId
    }

    suspend fun recordStandaloneImprovement(
        category: ImprovementCategory,
        reflectionText: String
    ): String = withContext(Dispatchers.IO) {
        val improvementId = UUID.randomUUID().toString()
        val improvement = OnePercentImprovementEntity(
            id = improvementId,
            sessionId = null,
            category = category.name,
            reflectionText = reflectionText.trim(),
            timestamp = System.currentTimeMillis()
        )
        improvementDao.upsertImprovement(improvement)
        improvementId
    }

    suspend fun toggleChecklistItem(id: String, isEnabled: Boolean) = withContext(Dispatchers.IO) {
        checklistDao.updateItemEnabled(id, isEnabled)
    }

    suspend fun addCustomChecklistItem(title: String) = withContext(Dispatchers.IO) {
        val item = PreSessionChecklistItemEntity(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            isDefault = false,
            isEnabled = true,
            sortOrder = 99
        )
        checklistDao.upsertChecklistItems(listOf(item))
    }

    suspend fun addCustomRecoveryActivity(title: String, durationMinutes: Int = 5) = withContext(Dispatchers.IO) {
        val item = RecoveryActivityEntity(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            category = "CUSTOM",
            durationMinutes = durationMinutes,
            isDefault = false
        )
        recoveryDao.upsertActivities(listOf(item))
    }

    private fun getTodayTimeRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = calendar.timeInMillis - 1
        return Pair(startOfDay, endOfDay)
    }
}
