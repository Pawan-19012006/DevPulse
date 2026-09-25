package com.devpulse.ai.domain.session

import com.devpulse.ai.data.local.dao.HealthEventDao
import com.devpulse.ai.data.local.entity.DevSessionEntity
import com.devpulse.ai.data.local.entity.HealthEventEntity
import com.devpulse.ai.data.local.entity.OnePercentImprovementEntity
import com.devpulse.ai.data.local.entity.SessionHandoffEntity
import com.devpulse.ai.domain.context.DeveloperContext
import com.devpulse.ai.domain.quotes.DailyQuotes
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
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class Phase3ConnectedFlowTest {

    private lateinit var testTime: TestTimeProvider
    private lateinit var sessionDao: FakeDevSessionDao
    private lateinit var blockDao: FakeSessionBlockDao
    private lateinit var handoffDao: FakeSessionHandoffDao
    private lateinit var improvementDao: FakeOnePercentImprovementDao
    private lateinit var healthEventDao: FakeHealthEventDao
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
        healthEventDao = FakeHealthEventDao()

        engine = SessionEngine(
            sessionDao = sessionDao,
            blockDao = blockDao,
            handoffDao = handoffDao,
            improvementDao = improvementDao,
            healthEventDao = healthEventDao,
            timeProvider = testTime,
            scope = testScope,
            autoStartTicker = false
        )
    }

    // 1. Daily quote tests
    @Test
    fun `Daily quote is deterministic and stable for same calendar day`() {
        val cal1 = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 25, 9, 30, 0)
        }
        val cal2 = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 25, 22, 15, 0)
        }

        val quote1 = DailyQuotes.getQuoteForToday(cal1)
        val quote2 = DailyQuotes.getQuoteForToday(cal2)

        assertEquals("Quote text should be identical across same day", quote1.text, quote2.text)
        assertTrue("Quote must not be empty", quote1.text.isNotBlank())
    }

    @Test
    fun `Daily quote changes on different days`() {
        val day1 = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 25)
        }
        val day2 = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 26)
        }

        val quote1 = DailyQuotes.getQuoteForToday(day1)
        val quote2 = DailyQuotes.getQuoteForToday(day2)

        assertNotEquals("Quote should vary across distinct days", quote1.text, quote2.text)
    }

    // 2. Health Nudge and Persistence
    @Test
    fun `Session health nudge completes and persists HealthEventEntity with sessionId`() = testScope.runTest {
        val sessionId = engine.startFocusSession(
            activityType = SessionActivityType.CODING,
            overallGoal = "Implement Session Health Nudges",
            initialMindset = DeveloperState.READY,
            durationMinutes = 25
        )
        advanceUntilIdle()

        // Trigger prototype hydration nudge
        engine.triggerPrototypeNudge(HealthEventType.HYDRATION)
        val activeNudge = engine.activeNudge.value
        assertNotNull("Health nudge should be active", activeNudge)
        assertEquals(HealthEventType.HYDRATION, activeNudge?.type)

        // Complete the nudge
        engine.completeHealthNudge()
        advanceUntilIdle()

        // Active nudge must be cleared
        assertNull("Active nudge should be null after completion", engine.activeNudge.value)

        // Health event must be saved in DAO
        val events = healthEventDao.events
        assertEquals(1, events.size)
        val savedEvent = events.first()
        assertEquals(sessionId, savedEvent.sessionId)
        assertEquals("HYDRATION", savedEvent.type)
        assertTrue(savedEvent.completed)
        assertEquals("SESSION_NUDGE", savedEvent.source)
    }

    @Test
    fun `Dismissing health nudge does not record completion event`() = testScope.runTest {
        engine.startFocusSession(
            activityType = SessionActivityType.DEBUGGING,
            overallGoal = "Fix Auth Bug",
            initialMindset = DeveloperState.READY,
            durationMinutes = 25
        )
        advanceUntilIdle()

        engine.triggerPrototypeNudge(HealthEventType.EYE_RECOVERY)
        assertNotNull(engine.activeNudge.value)

        engine.dismissHealthNudge()
        assertNull("Active nudge should be cleared after dismissal", engine.activeNudge.value)
        assertEquals("No health events should be saved on dismissal", 0, healthEventDao.events.size)
    }

    // 3. Guided Recovery logs appropriate HealthEvent
    @Test
    fun `Guided Recovery records recovery event referencing the active session`() = testScope.runTest {
        val sessionId = engine.startDeepWorkSession(
            activityType = SessionActivityType.CODING,
            overallGoal = "Design Lakehouse Integration",
            initialObjective = "Draft architecture diagram",
            initialMindset = DeveloperState.READY,
            totalPlannedHours = 2,
            workDurationMinutes = 45,
            recoveryDurationMinutes = 15
        )
        advanceUntilIdle()

        // User finishes block and selects Guided Breathing
        engine.recordGuidedRecoveryHealthEvent(HealthEventType.BREATHING)
        advanceUntilIdle()

        val events = healthEventDao.events
        assertEquals(1, events.size)
        val breathingEvent = events.first()
        assertEquals(sessionId, breathingEvent.sessionId)
        assertEquals("BREATHING", breathingEvent.type)
        assertTrue(breathingEvent.completed)
        assertEquals("GUIDED_RECOVERY", breathingEvent.source)
    }

    // 4. DeveloperContext shared domain model
    @Test
    fun `DeveloperContext aggregates sessions, handoff, improvements, and health`() {
        val session = DevSessionEntity(
            id = "sess-1",
            title = "Coding Session",
            activityType = "CODING",
            sessionMode = "FOCUS",
            goal = "Finish Phase 3",
            targetDurationMinutes = 45,
            actualDurationMinutes = 45,
            startedAt = 1000L,
            completedAt = 2000L,
            status = "COMPLETED"
        )
        val handoff = SessionHandoffEntity(
            id = "hand-1",
            sessionId = "sess-1",
            blockIndex = 1,
            sessionGoal = "Finish Phase 3",
            accomplished = "Connected sessions to health",
            nextObjective = "Write full test suite",
            isUnfinished = true,
            createdAt = 2000L
        )
        val healthEvent = HealthEventEntity(
            id = "health-1",
            sessionId = "sess-1",
            type = "HYDRATION",
            timestamp = 1500L,
            completed = true,
            source = "SESSION_NUDGE"
        )
        val improvement = OnePercentImprovementEntity(
            id = "imp-1",
            sessionId = "sess-1",
            category = "ARCHITECTURE",
            reflectionText = "Connected all 4 pillars cleanly",
            timestamp = 2000L
        )

        val context = DeveloperContext(
            recentSessions = listOf(session),
            unfinishedHandoff = handoff,
            recentImprovements = listOf(improvement),
            todayHealthEvents = listOf(healthEvent),
            connectedGitHubUser = "developer"
        )

        assertEquals(1, context.recentSessions.size)
        assertEquals("Write full test suite", context.unfinishedHandoff?.nextObjective)
        assertEquals(1, context.todayHealthEvents.size)
        assertEquals("HYDRATION", context.todayHealthEvents.first().type)
        assertEquals("sess-1", context.todayHealthEvents.first().sessionId)
        assertEquals("developer", context.connectedGitHubUser)
    }
}

class FakeHealthEventDao : HealthEventDao {
    val events = mutableListOf<HealthEventEntity>()

    override suspend fun upsertHealthEvent(event: HealthEventEntity) {
        events.add(event)
    }

    override suspend fun upsertHealthEvents(events: List<HealthEventEntity>) {
        this.events.addAll(events)
    }

    override fun observeAllHealthEvents(): Flow<List<HealthEventEntity>> = flowOf(events.toList())

    override fun observeHealthEventsBetween(startTime: Long, endTime: Long): Flow<List<HealthEventEntity>> =
        flowOf(events.filter { it.timestamp in startTime..endTime })

    override fun observeCountByTypeBetween(type: String, startTime: Long, endTime: Long): Flow<Int> =
        flowOf(events.count { it.type == type && it.timestamp in startTime..endTime })

    override fun observeHealthEventsForSession(sessionId: String): Flow<List<HealthEventEntity>> =
        flowOf(events.filter { it.sessionId == sessionId })

    override suspend fun getHealthEventsForSession(sessionId: String): List<HealthEventEntity> =
        events.filter { it.sessionId == sessionId }

    override fun observeTotalHealthEventsBetween(startTime: Long, endTime: Long): Flow<Int> =
        flowOf(events.count { it.timestamp in startTime..endTime })

    override fun observeRecentHealthEvents(limit: Int): Flow<List<HealthEventEntity>> =
        flowOf(events.take(limit))
}
