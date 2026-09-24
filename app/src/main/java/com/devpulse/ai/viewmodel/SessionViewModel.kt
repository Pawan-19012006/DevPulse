package com.devpulse.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devpulse.ai.DevPulseApp
import com.devpulse.ai.data.local.entity.OnePercentImprovementEntity
import com.devpulse.ai.data.local.entity.PreSessionChecklistItemEntity
import com.devpulse.ai.data.local.entity.RecoveryActivityEntity
import com.devpulse.ai.data.local.entity.SessionHandoffEntity
import com.devpulse.ai.domain.session.*
import com.devpulse.ai.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SessionViewModel(
    private val sessionRepository: SessionRepository = DevPulseApp.instance.sessionRepository,
    private val sessionEngine: SessionEngine = DevPulseApp.instance.sessionEngine
) : ViewModel() {

    val engineState: StateFlow<SessionEngineState> = sessionEngine.state

    val latestUnfinishedHandoff: StateFlow<SessionHandoffEntity?> =
        sessionRepository.observeLatestUnfinishedHandoff()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val checklistItems: StateFlow<List<PreSessionChecklistItemEntity>> =
        sessionRepository.observeChecklist()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recoveryActivities: StateFlow<List<RecoveryActivityEntity>> =
        sessionRepository.observeRecoveryActivities()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allImprovements: StateFlow<List<OnePercentImprovementEntity>> =
        sessionRepository.observeAllImprovements()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Setup state
    private val _sessionMode = MutableStateFlow(SessionMode.FOCUS)
    val sessionMode: StateFlow<SessionMode> = _sessionMode.asStateFlow()

    private val _selectedActivity = MutableStateFlow(SessionActivityType.CODING)
    val selectedActivity: StateFlow<SessionActivityType> = _selectedActivity.asStateFlow()

    private val _selectedDurationMinutes = MutableStateFlow(25)
    val selectedDurationMinutes: StateFlow<Int> = _selectedDurationMinutes.asStateFlow()

    private val _deepWorkPreset = MutableStateFlow(DeepWorkPresets.THREE_HOURS)
    val deepWorkPreset: StateFlow<DeepWorkPreset> = _deepWorkPreset.asStateFlow()

    private val _deepWorkWorkMinutes = MutableStateFlow(45)
    val deepWorkWorkMinutes: StateFlow<Int> = _deepWorkWorkMinutes.asStateFlow()

    private val _deepWorkRecoveryMinutes = MutableStateFlow(15)
    val deepWorkRecoveryMinutes: StateFlow<Int> = _deepWorkRecoveryMinutes.asStateFlow()

    private val _selectedState = MutableStateFlow(DeveloperState.READY)
    val selectedState: StateFlow<DeveloperState> = _selectedState.asStateFlow()

    // Level 1: Overall Goal
    private val _goal = MutableStateFlow("")
    val goal: StateFlow<String> = _goal.asStateFlow()

    // Level 2: Current Block Objective (for Deep Work)
    private val _currentBlockObjective = MutableStateFlow("")
    val currentBlockObjective: StateFlow<String> = _currentBlockObjective.asStateFlow()

    private val _checkedItemIds = MutableStateFlow<Set<String>>(emptySet())
    val checkedItemIds: StateFlow<Set<String>> = _checkedItemIds.asStateFlow()

    private val _ignoredHandoffId = MutableStateFlow<String?>(null)
    val ignoredHandoffId: StateFlow<String?> = _ignoredHandoffId.asStateFlow()

    fun selectSessionMode(mode: SessionMode) {
        _sessionMode.value = mode
    }

    fun selectActivity(type: SessionActivityType) {
        _selectedActivity.value = type
    }

    fun selectDuration(minutes: Int) {
        _selectedDurationMinutes.value = minutes
    }

    fun selectDeepWorkPreset(preset: DeepWorkPreset) {
        _deepWorkPreset.value = preset
        _deepWorkWorkMinutes.value = preset.workMinutes
        _deepWorkRecoveryMinutes.value = preset.recoveryMinutes
    }

    fun selectState(state: DeveloperState) {
        _selectedState.value = state
    }

    fun updateGoal(newGoal: String) {
        _goal.value = newGoal
        if (_currentBlockObjective.value.isBlank()) {
            _currentBlockObjective.value = newGoal
        }
    }

    fun updateBlockObjective(objective: String) {
        _currentBlockObjective.value = objective
    }

    fun applyHandoff(handoff: SessionHandoffEntity) {
        _goal.value = handoff.sessionGoal
        _currentBlockObjective.value = handoff.nextObjective
    }

    fun dismissHandoff(handoffId: String) {
        _ignoredHandoffId.value = handoffId
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

    suspend fun startConfiguredSession(): String {
        val targetGoal = _goal.value.trim().ifBlank { "Focused ${_selectedActivity.value.displayName} Session" }
        val targetObjective = _currentBlockObjective.value.trim().ifBlank { targetGoal }

        return if (_sessionMode.value == SessionMode.FOCUS) {
            sessionEngine.startFocusSession(
                activityType = _selectedActivity.value,
                overallGoal = targetGoal,
                initialMindset = _selectedState.value,
                durationMinutes = _selectedDurationMinutes.value
            )
        } else {
            sessionEngine.startDeepWorkSession(
                activityType = _selectedActivity.value,
                overallGoal = targetGoal,
                initialObjective = targetObjective,
                initialMindset = _selectedState.value,
                totalPlannedHours = _deepWorkPreset.value.totalHours,
                workDurationMinutes = _deepWorkWorkMinutes.value,
                recoveryDurationMinutes = _deepWorkRecoveryMinutes.value
            )
        }
    }

    fun pause() {
        sessionEngine.pause()
    }

    fun resume() {
        sessionEngine.resume()
    }

    fun reportMindset(state: DeveloperState) {
        sessionEngine.reportMindset(state)
    }

    fun submitWorkBlockHandoff(accomplished: String, nextObjective: String) {
        viewModelScope.launch {
            sessionEngine.submitWorkBlockHandoff(accomplished, nextObjective)
        }
    }

    fun endRecoveryEarly() {
        sessionEngine.endRecoveryEarly()
    }

    fun continueNextBlock(customObjective: String? = null) {
        viewModelScope.launch {
            sessionEngine.continueNextBlock(customObjective)
        }
    }

    fun completeSessionEarly(accomplished: String, nextObjective: String) {
        viewModelScope.launch {
            sessionEngine.completeSessionEarly(accomplished, nextObjective)
        }
    }

    fun submitFinalReflection(category: ImprovementCategory, reflection: String) {
        viewModelScope.launch {
            sessionEngine.submitFinalReflection(category, reflection)
        }
    }

    fun resetSession() {
        sessionEngine.resetToIdle()
    }

    fun recordImprovement(category: ImprovementCategory, reflection: String) {
        if (reflection.isBlank()) return
        viewModelScope.launch {
            sessionRepository.recordStandaloneImprovement(category, reflection)
        }
    }
}
