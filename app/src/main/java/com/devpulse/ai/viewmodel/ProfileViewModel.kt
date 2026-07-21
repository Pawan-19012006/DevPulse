package com.devpulse.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devpulse.ai.network.GitHubApiService
import com.devpulse.ai.repository.*
import com.devpulse.ai.utils.AnalyzedProfile
import com.devpulse.ai.utils.AnalysisEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ProfileUiState {
    object Idle : ProfileUiState
    object Loading : ProfileUiState
    data class Success(val profile: AnalyzedProfile) : ProfileUiState
    data class Error(val message: String, val errorType: ErrorType) : ProfileUiState
}

enum class ErrorType {
    USER_NOT_FOUND,
    RATE_LIMIT,
    NETWORK,
    EMPTY_REPOS,
    UNKNOWN
}

class ProfileViewModel(
    private val repository: GitHubRepository = GitHubRepository(GitHubApiService.create())
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _analyzedProfile = MutableStateFlow<AnalyzedProfile?>(null)
    val analyzedProfile: StateFlow<AnalyzedProfile?> = _analyzedProfile.asStateFlow()

    fun fetchAndAnalyzeProfile(username: String) {
        if (username.isBlank()) {
            _uiState.value = ProfileUiState.Error("Username cannot be empty", ErrorType.UNKNOWN)
            return
        }

        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                val dataPackage = repository.getFullProfileData(username.trim())
                
                if (dataPackage.repos.isEmpty()) {
                    _uiState.value = ProfileUiState.Error(
                        "This user does not have any public repositories to analyze.",
                        ErrorType.EMPTY_REPOS
                    )
                    return@launch
                }

                val analyzed = AnalysisEngine.analyze(dataPackage)
                _analyzedProfile.value = analyzed
                _uiState.value = ProfileUiState.Success(analyzed)
            } catch (e: UserNotFoundException) {
                _uiState.value = ProfileUiState.Error(e.message ?: "User not found", ErrorType.USER_NOT_FOUND)
            } catch (e: RateLimitException) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Rate limit exceeded", ErrorType.RATE_LIMIT)
            } catch (e: NetworkException) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Network error", ErrorType.NETWORK)
            } catch (e: ApiException) {
                _uiState.value = ProfileUiState.Error(e.message ?: "API error", ErrorType.UNKNOWN)
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "An unexpected error occurred", ErrorType.UNKNOWN)
            }
        }
    }

    fun clearError() {
        _uiState.value = ProfileUiState.Idle
    }

    fun resetState() {
        _uiState.value = ProfileUiState.Idle
    }
}
