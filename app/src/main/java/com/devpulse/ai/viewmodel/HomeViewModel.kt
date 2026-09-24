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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
    val greeting: String = "Good day, Developer",
    val developerState: DeveloperState = DeveloperState.READY,
    val todaySessionsCount: Int = 0,
    val todayImprovementsCount: Int = 0,
    val totalSessionsCount: Int = 0,
    val totalImprovementsCount: Int = 0,
    val recentImprovements: List<OnePercentImprovementEntity> = emptyList(),
    val latestSession: DevSessionEntity? = null,
    val connectedGitHubUser: String? = null
)

class HomeViewModel(
    private val sessionRepository: SessionRepository = DevPulseApp.instance.sessionRepository,
    private val database: DevPulseDatabase = DevPulseApp.instance.database
) : ViewModel() {

    private val _currentState = MutableStateFlow(DeveloperState.READY)
    val currentState: StateFlow<DeveloperState> = _currentState.asStateFlow()

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

    fun getDynamicGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good morning, Developer"
            in 12..16 -> "Good afternoon, Developer"
            in 17..21 -> "Good evening, Developer"
            else -> "Night owl session, Developer"
        }
    }

    fun recordQuickImprovement(category: ImprovementCategory, reflection: String) {
        if (reflection.isBlank()) return
        viewModelScope.launch {
            sessionRepository.recordStandaloneImprovement(category, reflection)
        }
    }

    /**
     * Helper for quick testing/logging completed sessions in Phase 0.
     */
    fun recordSampleCompletedSession(
        activityType: SessionActivityType,
        durationMinutes: Int,
        category: ImprovementCategory,
        improvementText: String
    ) {
        viewModelScope.launch {
            val sessionId = sessionRepository.createSession(
                activityType = activityType,
                targetDurationMinutes = durationMinutes,
                goal = "Sample phase 0 session",
                initialState = _currentState.value
            )
            sessionRepository.completeSession(
                sessionId = sessionId,
                actualDurationMinutes = durationMinutes,
                finalState = DeveloperState.FLOWING,
                category = category,
                reflectionText = improvementText
            )
        }
    }
}
