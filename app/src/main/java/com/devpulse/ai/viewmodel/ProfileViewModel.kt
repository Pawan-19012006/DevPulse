package com.devpulse.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devpulse.ai.DevPulseApp
import com.devpulse.ai.domain.SyncErrorType
import com.devpulse.ai.domain.SyncStatus
import com.devpulse.ai.repository.GitHubRepository
import com.devpulse.ai.utils.AnalysisEngine
import com.devpulse.ai.utils.AnalyzedProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

typealias ErrorType = SyncErrorType

sealed interface ProfileUiState {
    object Idle : ProfileUiState
    object Loading : ProfileUiState
    data class Success(val profile: AnalyzedProfile) : ProfileUiState
    data class Error(val message: String, val errorType: SyncErrorType) : ProfileUiState
}

class ProfileViewModel(
    private val repository: GitHubRepository = GitHubRepository(
        database = DevPulseApp.instance.database
    )
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _analyzedProfile = MutableStateFlow<AnalyzedProfile?>(null)
    val analyzedProfile: StateFlow<AnalyzedProfile?> = _analyzedProfile.asStateFlow()

    fun fetchAndAnalyzeProfile(username: String, forceRefresh: Boolean = false) {
        if (username.isBlank()) {
            _uiState.value = ProfileUiState.Error("Username cannot be empty", SyncErrorType.UNKNOWN)
            return
        }

        val cleanUsername = username.trim()

        viewModelScope.launch {
            if (_analyzedProfile.value == null) {
                _uiState.value = ProfileUiState.Loading
            }
            _syncStatus.value = SyncStatus.Syncing

            val syncResult = repository.syncDeveloperData(cleanUsername, forceRefresh = forceRefresh)
            _syncStatus.value = syncResult

            // Load data from Room database
            val profileEntity = repository.getProfile(cleanUsername)
            val repos = repository.getRepositories(cleanUsername)
            val events = repository.getEvents(cleanUsername)
            val skills = repository.getSkills(cleanUsername)
            val snapshots = repository.getAllSnapshots(cleanUsername)

            if (profileEntity != null && repos.isNotEmpty()) {
                val analyzed = AnalysisEngine.buildAnalyzedProfile(
                    profile = profileEntity,
                    repos = repos,
                    events = events,
                    skills = skills,
                    snapshots = snapshots
                )
                _analyzedProfile.value = analyzed
                _uiState.value = ProfileUiState.Success(analyzed)
            } else {
                when (syncResult) {
                    is SyncStatus.Failed -> {
                        _uiState.value = ProfileUiState.Error(syncResult.error, syncResult.errorType)
                    }
                    else -> {
                        _uiState.value = ProfileUiState.Error(
                            "No repositories found for @$cleanUsername",
                            SyncErrorType.EMPTY_REPOS
                        )
                    }
                }
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearCache()
            _analyzedProfile.value = null
            _uiState.value = ProfileUiState.Idle
            _syncStatus.value = SyncStatus.Idle
        }
    }

    fun clearError() {
        _uiState.value = ProfileUiState.Idle
    }

    fun resetState() {
        _uiState.value = ProfileUiState.Idle
    }
}
