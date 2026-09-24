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

    fun recordQuickImprovement(category: ImprovementCategory, reflection: String) {
        if (reflection.isBlank()) return
        viewModelScope.launch {
            sessionRepository.recordStandaloneImprovement(category, reflection)
        }
    }
}
