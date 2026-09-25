package com.devpulse.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devpulse.ai.DevPulseApp
import com.devpulse.ai.data.local.DevPulseDatabase
import com.devpulse.ai.data.local.entity.DevSessionEntity
import com.devpulse.ai.data.local.entity.OnePercentImprovementEntity
import com.devpulse.ai.domain.session.DeveloperState
import com.devpulse.ai.domain.session.ImprovementCategory
import com.devpulse.ai.domain.session.SessionActivityType
import com.devpulse.ai.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class DailyJourneySummary(
    val totalFocusedMinutes: Int = 0,
    val completedSessionsCount: Int = 0,
    val improvementsCount: Int = 0,
    val activityBreakdown: String = ""
)

class HomeViewModel(
    private val sessionRepository: SessionRepository = DevPulseApp.instance.sessionRepository,
    private val database: DevPulseDatabase = DevPulseApp.instance.database
) : ViewModel() {

    private val _currentState = MutableStateFlow(DeveloperState.READY)
    val currentState: StateFlow<DeveloperState> = _currentState.asStateFlow()

    private val _currentIntention = MutableStateFlow("")
    val currentIntention: StateFlow<String> = _currentIntention.asStateFlow()

    // Dev Health - Observable Daily Wellness Check-ins
    private val _hydrationCount = MutableStateFlow(3)
    val hydrationCount: StateFlow<Int> = _hydrationCount.asStateFlow()

    private val _screenRecoveryCount = MutableStateFlow(2)
    val screenRecoveryCount: StateFlow<Int> = _screenRecoveryCount.asStateFlow()

    private val _movementCount = MutableStateFlow(2)
    val movementCount: StateFlow<Int> = _movementCount.asStateFlow()

    val latestUnfinishedHandoff = sessionRepository.observeLatestUnfinishedHandoff()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todaySessionsCount = sessionRepository.observeTodaySessionsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)


    val todayImprovementsCount = sessionRepository.observeTodayImprovementsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalSessionsCount = sessionRepository.observeTotalSessionsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalImprovementsCount = sessionRepository.observeTotalImprovementsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentImprovements = sessionRepository.observeRecentImprovements(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todaySessions = sessionRepository.observeTodaySessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailySummary: StateFlow<DailyJourneySummary> = todaySessions.map { sessions ->
        val totalMinutes = sessions.sumOf {
            if (it.actualDurationMinutes > 0) it.actualDurationMinutes else it.targetDurationMinutes
        }
        val activities = sessions.map { it.activityType }.distinct()
            .joinToString(", ") { typeName ->
                runCatching { SessionActivityType.valueOf(typeName).displayName }.getOrDefault(typeName)
            }
        DailyJourneySummary(
            totalFocusedMinutes = totalMinutes,
            completedSessionsCount = sessions.size,
            improvementsCount = sessions.size, // each session represents +1
            activityBreakdown = activities
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyJourneySummary())

    private val _connectedGitHubUser = MutableStateFlow<String?>(null)
    val connectedGitHubUser: StateFlow<String?> = _connectedGitHubUser.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.ensureDefaultsSeeded()
            checkConnectedGitHubProfile()
        }
    }

    private suspend fun checkConnectedGitHubProfile() {
        val profiles = database.developerProfileDao().getProfile("octocat")
        if (profiles != null) {
            _connectedGitHubUser.value = profiles.username
        }
    }

    fun setDeveloperState(state: DeveloperState) {
        _currentState.value = state
    }

    fun setIntention(text: String) {
        _currentIntention.value = text
    }

    fun getGreetingTitle(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good morning, Developer."
            in 12..16 -> "Good afternoon, Developer."
            in 17..21 -> "Good evening, Developer."
            else -> "Night owl building, Developer."
        }
    }

    fun getGreetingSubtitle(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "A fresh terminal. A clean slate to build and learn."
            in 12..16 -> "In the middle of the work. Take it one problem at a time."
            in 17..21 -> "You've had a long day.\nBut you're here."
            else -> "Quiet hours. Deep focus. One problem at a time."
        }
    }

    fun logHydration() {
        _hydrationCount.value += 1
    }

    fun logScreenRecovery() {
        _screenRecoveryCount.value += 1
    }

    fun logMovement() {
        _movementCount.value += 1
    }

    fun recordQuickImprovement(category: ImprovementCategory, reflection: String) {
        if (reflection.isBlank()) return
        viewModelScope.launch {
            sessionRepository.recordStandaloneImprovement(category, reflection)
        }
    }

    fun getCoachGuidanceList(): List<CoachGuidance> {
        val list = mutableListOf<CoachGuidance>()

        // 1. Unfinished Handoff Guidance
        val handoff = latestUnfinishedHandoff.value
        if (handoff != null && handoff.nextObjective.isNotBlank()) {
            list.add(
                CoachGuidance(
                    title = "Pending Objective",
                    message = "Your unfinished objective \"${handoff.nextObjective}\" from \"${handoff.sessionGoal}\" is still waiting. Ready to continue?",
                    category = "Continuity"
                )
            )
        }

        // 2. Session Pattern / Debugging Observation
        val sessions = todaySessions.value
        val debugSessions = sessions.filter { it.activityType == SessionActivityType.DEBUGGING.name }
        val totalDebugMinutes = debugSessions.sumOf { if (it.actualDurationMinutes > 0) it.actualDurationMinutes else it.targetDurationMinutes }
        if (totalDebugMinutes >= 45) {
            list.add(
                CoachGuidance(
                    title = "Debugging Resistance",
                    message = "You've spent $totalDebugMinutes minutes debugging today. When debugging stalls, a short reset away from the screen often surfaces the missing assumption.",
                    category = "Focus Pattern"
                )
            )
        } else if (sessions.size >= 2) {
            list.add(
                CoachGuidance(
                    title = "Consistent Rhythm",
                    message = "You've completed ${sessions.size} focused work sessions today. Take your recovery blocks seriously to sustain mental endurance.",
                    category = "Focus Pattern"
                )
            )
        } else {
            list.add(
                CoachGuidance(
                    title = "Clean Workspace",
                    message = "Every session begins with a clear objective. Start small: define one tangible problem you can solve in 25 or 45 minutes.",
                    category = "Focus Pattern"
                )
            )
        }

        // 3. Developer State & Energy
        when (_currentState.value) {
            DeveloperState.FRUSTRATED -> {
                list.add(
                    CoachGuidance(
                        title = "Patience Over Velocity",
                        message = "You reported frustration. Remember: encountering friction is how technical depth is built. Don't fight the compiler—step back and breathe.",
                        category = "Mental State"
                    )
                )
            }
            DeveloperState.TIRED -> {
                list.add(
                    CoachGuidance(
                        title = "Cognitive Fatigue",
                        message = "Problem solving degrades rapidly under fatigue. Consider a lighter task like documentation or a walk before the next deep work block.",
                        category = "Mental State"
                    )
                )
            }
            DeveloperState.FLOWING -> {
                list.add(
                    CoachGuidance(
                        title = "Protect Your Flow",
                        message = "You're in flow state. Silence notifications and keep external inputs minimal while clarity is high.",
                        category = "Mental State"
                    )
                )
            }
            else -> {
                list.add(
                    CoachGuidance(
                        title = "Intentional Arrival",
                        message = "You showed up with a ready mindset. Trust the process: one problem, one block at a time.",
                        category = "Mental State"
                    )
                )
            }
        }

        // 4. 1% Better Growth
        val count = todayImprovementsCount.value
        if (count > 0) {
            list.add(
                CoachGuidance(
                    title = "Compounding Growth",
                    message = "You've recorded $count improvement${if (count > 1) "s" else ""} today. Small increments daily compound into extraordinary engineering capability.",
                    category = "1% Better"
                )
            )
        } else {
            list.add(
                CoachGuidance(
                    title = "One Lesson Per Session",
                    message = "Every session yields one insight—a debugging pattern, a syntax lesson, or a clean refactor. Capture it in the Tracker.",
                    category = "1% Better"
                )
            )
        }

        return list
    }
}

data class CoachGuidance(
    val title: String,
    val message: String,
    val category: String,
    val timestamp: String = "Today"
)

