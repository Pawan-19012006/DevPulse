package com.devpulse.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devpulse.ai.DevPulseApp
import com.devpulse.ai.data.local.entity.OnePercentImprovementEntity
import com.devpulse.ai.data.local.entity.PreSessionChecklistItemEntity
import com.devpulse.ai.data.local.entity.RecoveryActivityEntity
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

class SessionViewModel(
    private val sessionRepository: SessionRepository = DevPulseApp.instance.sessionRepository
) : ViewModel() {

    val checklistItems: StateFlow<List<PreSessionChecklistItemEntity>> =
        sessionRepository.observeChecklist()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recoveryActivities: StateFlow<List<RecoveryActivityEntity>> =
        sessionRepository.observeRecoveryActivities()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allImprovements: StateFlow<List<OnePercentImprovementEntity>> =
        sessionRepository.observeAllImprovements()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedActivity = MutableStateFlow(SessionActivityType.CODING)
    val selectedActivity: StateFlow<SessionActivityType> = _selectedActivity.asStateFlow()

    private val _selectedDurationMinutes = MutableStateFlow(25)
    val selectedDurationMinutes: StateFlow<Int> = _selectedDurationMinutes.asStateFlow()

    private val _selectedState = MutableStateFlow(DeveloperState.READY)
    val selectedState: StateFlow<DeveloperState> = _selectedState.asStateFlow()

    private val _goal = MutableStateFlow("")
    val goal: StateFlow<String> = _goal.asStateFlow()

    private val _checkedItemIds = MutableStateFlow<Set<String>>(emptySet())
    val checkedItemIds: StateFlow<Set<String>> = _checkedItemIds.asStateFlow()

    fun selectActivity(type: SessionActivityType) {
        _selectedActivity.value = type
    }

    fun selectDuration(minutes: Int) {
        _selectedDurationMinutes.value = minutes
    }

    fun selectState(state: DeveloperState) {
        _selectedState.value = state
    }

    fun updateGoal(newGoal: String) {
        _goal.value = newGoal
    }

    fun toggleCheckItem(id: String) {
        val current = _checkedItemIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _checkedItemIds.value = current
    }

    fun addCustomChecklistItem(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            sessionRepository.addCustomChecklistItem(title)
        }
    }

    suspend fun createAndStartSession(): String {
        return sessionRepository.createSession(
            activityType = _selectedActivity.value,
            targetDurationMinutes = _selectedDurationMinutes.value,
            goal = _goal.value.ifBlank { null },
            initialState = _selectedState.value
        )
    }

    fun recordImprovement(category: ImprovementCategory, reflection: String) {
        if (reflection.isBlank()) return
        viewModelScope.launch {
            sessionRepository.recordStandaloneImprovement(category, reflection)
        }
    }
}
