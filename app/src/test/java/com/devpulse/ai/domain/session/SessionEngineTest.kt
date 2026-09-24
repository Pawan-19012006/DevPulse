package com.devpulse.ai.domain.session

import com.devpulse.ai.data.local.dao.DevSessionDao
import com.devpulse.ai.data.local.dao.OnePercentImprovementDao
import com.devpulse.ai.data.local.dao.SessionBlockDao
import com.devpulse.ai.data.local.dao.SessionHandoffDao
import com.devpulse.ai.data.local.entity.DevSessionEntity
import com.devpulse.ai.data.local.entity.OnePercentImprovementEntity
import com.devpulse.ai.data.local.entity.SessionBlockEntity
import com.devpulse.ai.data.local.entity.SessionHandoffEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionEngineTest {

    private lateinit var testTime: TestTimeProvider
    private lateinit var sessionDao: FakeDevSessionDao
    private lateinit var blockDao: FakeSessionBlockDao
    private lateinit var handoffDao: FakeSessionHandoffDao
    private lateinit var improvementDao: FakeOnePercentImprovementDao
    private lateinit var engine: SessionEngine
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        testTime = TestTimeProvider(initialTime = 1_700_000_000_000L)
        sessionDao = FakeDevSessionDao()
        blockDao = FakeSessionBlockDao()
        handoffDao = FakeSessionHandoffDao()
        improvementDao = FakeOnePercentImprovementDao()

        engine = SessionEngine(
            sessionDao = sessionDao,
            blockDao = blockDao,
            handoffDao = handoffDao,
            improvementDao = improvementDao,
            timeProvider = testTime,
            scope = testScope,
            autoStartTicker = false
        )
    }

    // 1. Focus session starts correctly
    @Test
    fun `test 1 - Focus session starts correctly`() = testScope.runTest {
        val sessionId = engine.startFocusSession(
            activityType = SessionActivityType.CODING,
            overallGoal = "Implement Auth",
            initialMindset = DeveloperState.READY,
            durationMinutes = 25
        )
        advanceUntilIdle()

        val state = engine.state.value
        assertTrue("Expected Working state but was $state", state is SessionEngineState.Working)
        val working = state as SessionEngineState.Working

        assertEquals(sessionId, working.session.id)
        assertEquals(SessionMode.FOCUS, working.session.sessionMode)
        assertEquals("Implement Auth", working.session.overallGoal)
        assertEquals(BlockType.WORK, working.block.blockType)
        assertEquals(1500L, working.block.plannedDurationSeconds)
        assertEquals(BlockStatus.WORKING, working.block.status)
        assertEquals(1500L, working.remainingSeconds)
        assertEquals(0L, working.elapsedSeconds)
    }

    // 2. Remaining time is calculated correctly
    @Test
    fun `test 2 - Remaining time is calculated correctly`() = testScope.runTest {
        engine.startFocusSession(
            activityType = SessionActivityType.DEBUGGING,
            overallGoal = "Fix Race Condition",
            initialMindset = DeveloperState.GOOD,
            durationMinutes = 25
        )
        advanceUntilIdle()

        // Advance 300 seconds (5 minutes)
        testTime.advanceSeconds(300)
        engine.tick()

        val state = engine.state.value as SessionEngineState.Working
        assertEquals(1200L, state.remainingSeconds)
        assertEquals(300L, state.elapsedSeconds)
        assertEquals(0.20f, state.progressRatio, 0.01f)
    }

    // 3. Pause stops elapsed work time
    @Test
    fun `test 3 - Pause stops elapsed work time`() = testScope.runTest {
        engine.startFocusSession(
            activityType = SessionActivityType.CODING,
            overallGoal = "Build Cache",
            initialMindset = DeveloperState.FLOWING,
            durationMinutes = 25
        )
        advanceUntilIdle()

        // Work for 500 seconds
        testTime.advanceSeconds(500)
        engine.tick()

        // Pause
        engine.pause()
        advanceUntilIdle()

        val pausedState = engine.state.value
        assertTrue(pausedState is SessionEngineState.Paused)
        val paused = pausedState as SessionEngineState.Paused
        assertEquals(1000L, paused.remainingSeconds)
        assertEquals(500L, paused.elapsedSeconds)

        // Advance time by 600 seconds while paused
        testTime.advanceSeconds(600)
        engine.tick()

        // Verify remaining time does NOT decrement during pause
        val stillPaused = engine.state.value as SessionEngineState.Paused
        assertEquals(1000L, stillPaused.remainingSeconds)
        assertEquals(500L, stillPaused.elapsedSeconds)
    }

    // 4. Resume continues correctly
    @Test
    fun `test 4 - Resume continues correctly`() = testScope.runTest {
        engine.startFocusSession(
            activityType = SessionActivityType.CODING,
            overallGoal = "Build Cache",
            initialMindset = DeveloperState.FLOWING,
            durationMinutes = 25
        )
        advanceUntilIdle()

        testTime.advanceSeconds(500)
        engine.pause()
        advanceUntilIdle()

        // Paused for 600 seconds
        testTime.advanceSeconds(600)

        // Resume
        engine.resume()
        advanceUntilIdle()

        val resumedState = engine.state.value
        assertTrue(resumedState is SessionEngineState.Working)
        val resumed = resumedState as SessionEngineState.Working
        assertEquals(1000L, resumed.remainingSeconds)

        // Work for another 200 seconds
        testTime.advanceSeconds(200)
        engine.tick()

        val stateAfterMoreWork = engine.state.value as SessionEngineState.Working
        assertEquals(800L, stateAfterMoreWork.remainingSeconds)
        assertEquals(700L, stateAfterMoreWork.elapsedSeconds)
    }

    // 5. Work block transitions to handoff
    @Test
    fun `test 5 - Work block transitions to handoff`() = testScope.runTest {
        engine.startFocusSession(
            activityType = SessionActivityType.LEARNING,
            overallGoal = "Learn Room Migrations",
            initialMindset = DeveloperState.READY,
            durationMinutes = 1
        )
        advanceUntilIdle()

        // Advance past duration (60 seconds)
        testTime.advanceSeconds(61)
        engine.tick()
        advanceUntilIdle()

        val state = engine.state.value
        assertTrue("Expected WorkBlockHandoff but was $state", state is SessionEngineState.WorkBlockHandoff)
        val handoff = state as SessionEngineState.WorkBlockHandoff
        assertEquals("Learn Room Migrations", handoff.session.overallGoal)
        assertEquals(1, handoff.completedBlock.blockIndex + 1)
    }

    // 6. Handoff creates next objective
    @Test
    fun `test 6 - Handoff creates next objective`() = testScope.runTest {
        engine.startDeepWorkSession(
            activityType = SessionActivityType.CODING,
            overallGoal = "Build Sync Engine",
            initialObjective = "Implement HTTP client",
            initialMindset = DeveloperState.READY,
            totalPlannedHours = 2,
            workDurationMinutes = 45,
            recoveryDurationMinutes = 15
        )
        advanceUntilIdle()

        // Complete block 1
        testTime.advanceSeconds(45 * 60L + 1)
        engine.tick()
        advanceUntilIdle()

        assertTrue(engine.state.value is SessionEngineState.WorkBlockHandoff)

        // Submit handoff
        engine.submitWorkBlockHandoff(
            accomplished = "Implemented HTTP client with retry logic",
            nextObjective = "Add Room response caching"
        )
        advanceUntilIdle()

        // For Deep Work, transitions to Recovery
        val state = engine.state.value
        assertTrue("Expected Recovery state but was $state", state is SessionEngineState.Recovery)
        val recovery = state as SessionEngineState.Recovery
        assertEquals("Add Room response caching", recovery.nextObjective)

        // Verify handoff was persisted
        val savedHandoff = handoffDao.getLatestUnfinishedHandoff()
        assertNotNull(savedHandoff)
        assertEquals("Implemented HTTP client with retry logic", savedHandoff?.accomplished)
        assertEquals("Add Room response caching", savedHandoff?.nextObjective)
        assertTrue(savedHandoff?.isUnfinished == true)
    }

    // 7. Recovery transitions to next work block
    @Test
    fun `test 7 - Recovery transitions to next work block`() = testScope.runTest {
        engine.startDeepWorkSession(
            activityType = SessionActivityType.CODING,
            overallGoal = "Build Sync Engine",
            initialObjective = "Block 1 Obj",
            initialMindset = DeveloperState.READY,
            totalPlannedHours = 2,
            workDurationMinutes = 45,
            recoveryDurationMinutes = 15
        )
        advanceUntilIdle()

        testTime.advanceSeconds(45 * 60L + 1)
        engine.tick()
        advanceUntilIdle()

        engine.submitWorkBlockHandoff("Accomplished Block 1", "Block 2 Obj")
        advanceUntilIdle()

        assertTrue(engine.state.value is SessionEngineState.Recovery)

        // Recovery finishes or user clicks end early
        engine.endRecoveryEarly()
        advanceUntilIdle()

        val readyState = engine.state.value
        assertTrue("Expected ReadyForNextBlock but was $readyState", readyState is SessionEngineState.ReadyForNextBlock)
        val ready = readyState as SessionEngineState.ReadyForNextBlock
        assertEquals("Block 2 Obj", ready.carryoverObjective)
    }

    // 8. Deep Work advances through multiple blocks
    @Test
    fun `test 8 - Deep Work advances through multiple blocks`() = testScope.runTest {
        engine.startDeepWorkSession(
            activityType = SessionActivityType.PROJECT_WORK,
            overallGoal = "Multi Block Epic",
            initialObjective = "Block 0 Obj",
            initialMindset = DeveloperState.READY,
            totalPlannedHours = 2,
            workDurationMinutes = 45,
            recoveryDurationMinutes = 15
        )
        advanceUntilIdle()

        // Block 0 -> Handoff
        testTime.advanceSeconds(45 * 60L + 1)
        engine.tick()
        advanceUntilIdle()

        // Handoff -> Recovery
        engine.submitWorkBlockHandoff("Done 0", "Start 1")
        advanceUntilIdle()

        // Recovery -> Ready
        engine.endRecoveryEarly()
        advanceUntilIdle()

        // Ready -> Continue Block 1
        engine.continueNextBlock()
        advanceUntilIdle()

        val workingBlock1 = engine.state.value as SessionEngineState.Working
        assertEquals(1, workingBlock1.session.currentBlockIndex)
        assertEquals("Start 1", workingBlock1.block.objective)
        assertEquals(BlockType.WORK, workingBlock1.block.blockType)
    }

    // 9. Final block completes the session
    @Test
    fun `test 9 - Final block completes the session`() = testScope.runTest {
        // Focus session has exactly 1 block
        engine.startFocusSession(
            activityType = SessionActivityType.DSA,
            overallGoal = "Solve Graph Problem",
            initialMindset = DeveloperState.READY,
            durationMinutes = 20
        )
        advanceUntilIdle()

        // Finish work block
        testTime.advanceSeconds(20 * 60L + 1)
        engine.tick()
        advanceUntilIdle()

        assertTrue(engine.state.value is SessionEngineState.WorkBlockHandoff)

        // Submit handoff for final block
        engine.submitWorkBlockHandoff(
            accomplished = "Solved using Dijkstra",
            nextObjective = "Review time complexity tomorrow"
        )
        advanceUntilIdle()

        // Focus session final handoff transitions to SessionCompleteReflection
        val state = engine.state.value
        assertTrue("Expected SessionCompleteReflection but was $state", state is SessionEngineState.SessionCompleteReflection)
        val reflection = state as SessionEngineState.SessionCompleteReflection
        assertEquals("Solved using Dijkstra", reflection.lastHandoff?.accomplished)
        assertEquals("Review time complexity tomorrow", reflection.lastHandoff?.nextObjective)
    }

    // 10. Unfinished objective is persisted for future continuation
    @Test
    fun `test 10 - Unfinished objective is persisted for future continuation`() = testScope.runTest {
        engine.startFocusSession(
            activityType = SessionActivityType.CODING,
            overallGoal = "JWT Authentication",
            initialMindset = DeveloperState.GOOD,
            durationMinutes = 25
        )
        advanceUntilIdle()

        testTime.advanceSeconds(25 * 60L + 1)
        engine.tick()
        advanceUntilIdle()

        engine.submitWorkBlockHandoff(
            accomplished = "Implemented refresh token generation",
            nextObjective = "Handle refresh-token expiration and the 401 case"
        )
        advanceUntilIdle()

        val handoff = handoffDao.getLatestUnfinishedHandoff()
        assertNotNull(handoff)
        assertEquals("JWT Authentication", handoff?.sessionGoal)
        assertEquals("Handle refresh-token expiration and the 401 case", handoff?.nextObjective)
        assertEquals(true, handoff?.isUnfinished)
    }

    // 11. Active session can be restored
    @Test
    fun `test 11 - Active session can be restored`() = testScope.runTest {
        val startedAt = testTime.now()
        val sessionEntity = DevSessionEntity(
            id = "persisted-session-123",
            title = "Coding",
            activityType = SessionActivityType.CODING.name,
            sessionMode = SessionMode.FOCUS.name,
            goal = "Survive Process Death",
            currentObjective = "Survive Process Death",
            targetDurationMinutes = 30,
            actualDurationMinutes = 0,
            startedAt = startedAt,
            status = SessionStatus.IN_PROGRESS.name,
            totalBlocks = 1,
            currentBlockIndex = 0
        )
        sessionDao.upsertSession(sessionEntity)

        val blockEntity = SessionBlockEntity(
            id = "persisted-block-123",
            sessionId = "persisted-session-123",
            blockIndex = 0,
            blockType = BlockType.WORK.name,
            objective = "Survive Process Death",
            plannedDurationSeconds = 1800,
            startedAt = startedAt,
            status = BlockStatus.WORKING.name
        )
        blockDao.upsertBlock(blockEntity)

        // Advance time by 600s before restoring
        testTime.advanceSeconds(600)

        // Create new engine instance representing app process restart
        val restoredEngine = SessionEngine(
            sessionDao = sessionDao,
            blockDao = blockDao,
            handoffDao = handoffDao,
            improvementDao = improvementDao,
            timeProvider = testTime,
            scope = testScope,
            autoStartTicker = false
        )
        restoredEngine.restoreActiveSession()
        advanceUntilIdle()

        val restoredState = restoredEngine.state.value
        assertTrue("Expected restored Working state but was $restoredState", restoredState is SessionEngineState.Working)
        val working = restoredState as SessionEngineState.Working
        assertEquals("persisted-session-123", working.session.id)
        assertEquals(1200L, working.remainingSeconds)
        assertEquals(600L, working.elapsedSeconds)
    }

    // 12. Ending early preserves handoff
    @Test
    fun `test 12 - Ending early preserves handoff`() = testScope.runTest {
        engine.startFocusSession(
            activityType = SessionActivityType.PLANNING,
            overallGoal = "System Architecture",
            initialMindset = DeveloperState.READY,
            durationMinutes = 60
        )
        advanceUntilIdle()

        // Developer works for only 15 minutes and ends early
        testTime.advanceSeconds(900)
        engine.completeSessionEarly(
            accomplished = "Drafted component diagram",
            nextObjective = "Finalize database schema tomorrow"
        )
        advanceUntilIdle()

        // Ensure session status in DB is COMPLETED (not failed)
        val session = sessionDao.getActiveSession()
        assertNull("Session should no longer be active", session)

        val handoff = handoffDao.getLatestUnfinishedHandoff()
        assertNotNull(handoff)
        assertEquals("Finalize database schema tomorrow", handoff?.nextObjective)
        assertEquals("Drafted component diagram", handoff?.accomplished)

        // State moves to reflection so user can log +1% Better
        val state = engine.state.value
        assertTrue(state is SessionEngineState.SessionCompleteReflection)
    }

    // 13. Timer does not depend on Compose recomposition
    @Test
    fun `test 13 - Timer does not depend on Compose recomposition`() = testScope.runTest {
        engine.startFocusSession(
            activityType = SessionActivityType.CODING,
            overallGoal = "No Recomposition Drift",
            initialMindset = DeveloperState.READY,
            durationMinutes = 10
        )
        advanceUntilIdle()

        // Simulate 50 calls to tick() or UI recompositions at the exact same timestamp
        repeat(50) {
            engine.tick()
        }
        var state = engine.state.value as SessionEngineState.Working
        assertEquals("Elapsed must remain 0 when time has not elapsed", 0L, state.elapsedSeconds)
        assertEquals(600L, state.remainingSeconds)

        // Advance exactly 123 seconds
        testTime.advanceSeconds(123)
        // Simulate arbitrary UI reads/recompositions
        repeat(10) {
            engine.tick()
        }
        state = engine.state.value as SessionEngineState.Working
        assertEquals(123L, state.elapsedSeconds)
        assertEquals(477L, state.remainingSeconds)
    }

    // 14. Session state transitions are deterministic
    @Test
    fun `test 14 - Session state transitions are deterministic`() = testScope.runTest {
        // IDLE
        assertTrue(engine.state.value is SessionEngineState.Idle)

        // -> WORKING
        engine.startFocusSession(SessionActivityType.CODING, "Deterministic Test", DeveloperState.READY, 10)
        advanceUntilIdle()
        assertTrue(engine.state.value is SessionEngineState.Working)

        // -> PAUSED
        engine.pause()
        advanceUntilIdle()
        assertTrue(engine.state.value is SessionEngineState.Paused)

        // -> WORKING
        engine.resume()
        advanceUntilIdle()
        assertTrue(engine.state.value is SessionEngineState.Working)

        // -> HANDOFF
        testTime.advanceSeconds(601)
        engine.tick()
        advanceUntilIdle()
        assertTrue(engine.state.value is SessionEngineState.WorkBlockHandoff)

        // -> REFLECTION
        engine.submitWorkBlockHandoff("Done", "Next")
        advanceUntilIdle()
        assertTrue(engine.state.value is SessionEngineState.SessionCompleteReflection)

        // -> FINISHED
        engine.submitFinalReflection(ImprovementCategory.BUILDING, "Learned deterministic state machines")
        advanceUntilIdle()
        assertTrue(engine.state.value is SessionEngineState.Finished)

        // -> IDLE
        engine.resetToIdle()
        assertTrue(engine.state.value is SessionEngineState.Idle)
    }

    // 15. Fast-forwarding a work block transitions to handoff and persists
    @Test
    fun `test 15 - Fast forwarding a work block transitions to handoff and persists`() = testScope.runTest {
        engine.startDeepWorkSession(
            activityType = SessionActivityType.CODING,
            overallGoal = "Finish graph problems",
            initialObjective = "Solve problem A",
            initialMindset = DeveloperState.READY,
            totalPlannedHours = 3,
            workDurationMinutes = 45,
            recoveryDurationMinutes = 15
        )
        advanceUntilIdle()
        assertTrue(engine.state.value is SessionEngineState.Working)

        // Fast forward work block
        engine.fastForwardCurrentBlock()
        advanceUntilIdle()

        val state = engine.state.value
        assertTrue("Expected WorkBlockHandoff but was $state", state is SessionEngineState.WorkBlockHandoff)
        val handoffState = state as SessionEngineState.WorkBlockHandoff
        assertEquals("Solve problem A", handoffState.completedBlock.objective)
        assertEquals(BlockStatus.COMPLETED, handoffState.completedBlock.status)

        // Verify block completion persisted in DAO
        val blockInDb = blockDao.getBlockById(handoffState.completedBlock.id)
        assertNotNull(blockInDb)
        assertEquals(BlockStatus.COMPLETED.name, blockInDb?.status)
    }

    // 16. Fast-forwarding recovery transitions to next block with objective continuity
    @Test
    fun `test 16 - Fast forwarding recovery transitions to next block with objective continuity`() = testScope.runTest {
        engine.startDeepWorkSession(
            activityType = SessionActivityType.CODING,
            overallGoal = "Finish graph problems",
            initialObjective = "Solve problem A",
            initialMindset = DeveloperState.READY,
            totalPlannedHours = 3,
            workDurationMinutes = 45,
            recoveryDurationMinutes = 15
        )
        advanceUntilIdle()

        // Fast forward work block -> Handoff
        engine.fastForwardCurrentBlock()
        advanceUntilIdle()

        // Submit handoff
        engine.submitWorkBlockHandoff(
            accomplished = "Solved the BFS problem",
            nextObjective = "Need to solve the DFS problem"
        )
        advanceUntilIdle()

        // State is Recovery
        val recoveryState = engine.state.value
        assertTrue("Expected Recovery state but was $recoveryState", recoveryState is SessionEngineState.Recovery)
        val recovery = recoveryState as SessionEngineState.Recovery
        assertEquals("Need to solve the DFS problem", recovery.nextObjective)

        // Fast forward break
        engine.fastForwardCurrentBlock()
        advanceUntilIdle()

        // State is ReadyForNextBlock with objective carried over
        val readyState = engine.state.value
        assertTrue("Expected ReadyForNextBlock but was $readyState", readyState is SessionEngineState.ReadyForNextBlock)
        val ready = readyState as SessionEngineState.ReadyForNextBlock
        assertEquals("Need to solve the DFS problem", ready.carryoverObjective)

        // Continue into next work block
        engine.continueNextBlock()
        advanceUntilIdle()

        val workingState = engine.state.value
        assertTrue("Expected Working state but was $workingState", workingState is SessionEngineState.Working)
        val working = workingState as SessionEngineState.Working
        assertEquals(1, working.session.currentBlockIndex)
        assertEquals("Need to solve the DFS problem", working.currentBlock.objective)
    }

    // 17. Fast-forward works through multiple blocks to final completion
    @Test
    fun `test 17 - Fast forward works through multiple blocks to final completion`() = testScope.runTest {
        engine.startDeepWorkSession(
            activityType = SessionActivityType.CODING,
            overallGoal = "Build 2 Block Deep Work",
            initialObjective = "Block 0",
            initialMindset = DeveloperState.READY,
            totalPlannedHours = 2,
            workDurationMinutes = 50,
            recoveryDurationMinutes = 10
        )
        advanceUntilIdle()

        // Block 0 Fast Forward
        engine.fastForwardCurrentBlock()
        advanceUntilIdle()
        engine.submitWorkBlockHandoff("Accomplished 0", "Block 1 Obj")
        advanceUntilIdle()

        // Recovery Fast Forward
        engine.fastForwardCurrentBlock()
        advanceUntilIdle()

        // Continue to Block 1 (final block of 2)
        engine.continueNextBlock()
        advanceUntilIdle()

        // Block 1 Fast Forward
        engine.fastForwardCurrentBlock()
        advanceUntilIdle()

        val finalHandoffState = engine.state.value as SessionEngineState.WorkBlockHandoff
        assertTrue("Block 1 should be recognized as final block", finalHandoffState.isFinalBlock)

        engine.submitWorkBlockHandoff("Accomplished Block 1", "Session Complete")
        advanceUntilIdle()

        val reflectionState = engine.state.value
        assertTrue("Expected SessionCompleteReflection after final block", reflectionState is SessionEngineState.SessionCompleteReflection)
    }

    // 18. Fast-forward works while paused without corrupting state
    @Test
    fun `test 18 - Fast forward works while paused without corrupting state`() = testScope.runTest {
        engine.startFocusSession(
            activityType = SessionActivityType.DEBUGGING,
            overallGoal = "Pause and FF",
            initialMindset = DeveloperState.READY,
            durationMinutes = 20
        )
        advanceUntilIdle()

        testTime.advanceSeconds(300)
        engine.pause()
        advanceUntilIdle()
        assertTrue(engine.state.value is SessionEngineState.Paused)

        // Fast forward while paused
        engine.fastForwardCurrentBlock()
        advanceUntilIdle()

        val handoffState = engine.state.value
        assertTrue(handoffState is SessionEngineState.WorkBlockHandoff)
        val handoff = handoffState as SessionEngineState.WorkBlockHandoff
        assertEquals(BlockStatus.COMPLETED, handoff.completedBlock.status)
    }
}

// In-Memory Test Doubles

class TestTimeProvider(var initialTime: Long) : TimeProvider {
    private var currentTime = initialTime
    override fun now(): Long = currentTime
    fun advanceSeconds(seconds: Long) {
        currentTime += seconds * 1000L
    }
}

class FakeDevSessionDao : DevSessionDao {
    private val map = mutableMapOf<String, DevSessionEntity>()

    override suspend fun upsertSession(session: DevSessionEntity) {
        map[session.id] = session
    }

    override fun observeAllSessions(): Flow<List<DevSessionEntity>> = flowOf(map.values.toList())
    override fun observeSessionsBetween(startTime: Long, endTime: Long): Flow<List<DevSessionEntity>> = flowOf(emptyList())
    override fun observeTotalSessionsCount(): Flow<Int> = flowOf(map.size)
    override fun observeSessionsCountBetween(startTime: Long, endTime: Long): Flow<Int> = flowOf(0)
    override suspend fun getSessionById(id: String): DevSessionEntity? = map[id]

    override suspend fun getActiveSession(): DevSessionEntity? {
        return map.values.firstOrNull { it.status == "IN_PROGRESS" || it.status == "PAUSED" }
    }

    override fun observeActiveSession(): Flow<DevSessionEntity?> = flowOf(map.values.firstOrNull { it.status == "IN_PROGRESS" || it.status == "PAUSED" })

    override suspend fun updateCurrentBlock(sessionId: String, blockIndex: Int, objective: String) {
        map[sessionId]?.let {
            map[sessionId] = it.copy(currentBlockIndex = blockIndex, currentObjective = objective)
        }
    }

    override suspend fun updateMindset(sessionId: String, mindset: String) {
        map[sessionId]?.let {
            map[sessionId] = it.copy(currentMindset = mindset)
        }
    }

    override suspend fun updateStatus(sessionId: String, status: String, completedAt: Long?, actualDurationMinutes: Int) {
        map[sessionId]?.let {
            map[sessionId] = it.copy(status = status, completedAt = completedAt, actualDurationMinutes = actualDurationMinutes)
        }
    }
}

class FakeSessionBlockDao : SessionBlockDao {
    private val map = mutableMapOf<String, SessionBlockEntity>()

    override suspend fun upsertBlock(block: SessionBlockEntity) {
        map[block.id] = block
    }

    override suspend fun upsertBlocks(blocks: List<SessionBlockEntity>) {
        blocks.forEach { map[it.id] = it }
    }

    override suspend fun getBlockById(id: String): SessionBlockEntity? = map[id]

    override suspend fun getActiveBlock(sessionId: String): SessionBlockEntity? {
        return map.values.filter { it.sessionId == sessionId }
            .firstOrNull { it.status == BlockStatus.WORKING.name || it.status == BlockStatus.PAUSED.name }
    }

    override fun observeActiveBlock(sessionId: String): Flow<SessionBlockEntity?> = flowOf(
        map.values.filter { it.sessionId == sessionId }
            .firstOrNull { it.status == BlockStatus.WORKING.name || it.status == BlockStatus.PAUSED.name }
    )

    override suspend fun getBlocksForSession(sessionId: String): List<SessionBlockEntity> {
        return map.values.filter { it.sessionId == sessionId }.sortedBy { it.blockIndex }
    }

    override fun observeBlocksForSession(sessionId: String): Flow<List<SessionBlockEntity>> = flowOf(
        map.values.filter { it.sessionId == sessionId }.sortedBy { it.blockIndex }
    )

    override suspend fun updateBlockState(id: String, pausedAt: Long?, totalPausedDurationMs: Long, status: String, completedAt: Long?) {
        map[id]?.let {
            map[id] = it.copy(
                pausedAt = pausedAt,
                totalPausedDurationMs = totalPausedDurationMs,
                status = status,
                completedAt = completedAt
            )
        }
    }
}

class FakeSessionHandoffDao : SessionHandoffDao {
    private val map = mutableMapOf<String, SessionHandoffEntity>()

    override suspend fun upsertHandoff(handoff: SessionHandoffEntity) {
        map[handoff.id] = handoff
    }

    override suspend fun getLatestUnfinishedHandoff(): SessionHandoffEntity? {
        return map.values.filter { it.isUnfinished }.maxByOrNull { it.createdAt }
    }

    override fun observeLatestUnfinishedHandoff(): Flow<SessionHandoffEntity?> = flowOf(
        map.values.filter { it.isUnfinished }.maxByOrNull { it.createdAt }
    )

    override suspend fun markHandoffCompleted(id: String) {
        map[id]?.let {
            map[id] = it.copy(isUnfinished = false)
        }
    }

    override suspend fun getHandoffsForSession(sessionId: String): List<SessionHandoffEntity> {
        return map.values.filter { it.sessionId == sessionId }.sortedBy { it.blockIndex }
    }

    override fun observeHandoffsForSession(sessionId: String): Flow<List<SessionHandoffEntity>> = flowOf(
        map.values.filter { it.sessionId == sessionId }.sortedBy { it.blockIndex }
    )
}

class FakeOnePercentImprovementDao : OnePercentImprovementDao {
    private val list = mutableListOf<OnePercentImprovementEntity>()

    override suspend fun upsertImprovement(improvement: OnePercentImprovementEntity) {
        list.add(improvement)
    }

    override fun observeAllImprovements(): Flow<List<OnePercentImprovementEntity>> = flowOf(list)
    override fun observeRecentImprovements(limit: Int): Flow<List<OnePercentImprovementEntity>> = flowOf(list.take(limit))
    override fun observeTotalImprovementsCount(): Flow<Int> = flowOf(list.size)
    override fun observeImprovementsCountBetween(startTime: Long, endTime: Long): Flow<Int> = flowOf(0)
    override suspend fun getImprovementForSession(sessionId: String): OnePercentImprovementEntity? {
        return list.firstOrNull { it.sessionId == sessionId }
    }
}
