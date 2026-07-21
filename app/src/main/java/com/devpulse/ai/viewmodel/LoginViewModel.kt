package com.devpulse.ai.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoginViewModel : ViewModel() {
    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _isButtonEnabled = MutableStateFlow(false)
    val isButtonEnabled: StateFlow<Boolean> = _isButtonEnabled.asStateFlow()

    fun onUsernameChanged(newUsername: String) {
        // Strip out spaces or newlines to maintain valid usernames
        val filtered = newUsername.replace("\\s".toRegex(), "")
        _username.value = filtered
        _isButtonEnabled.value = filtered.isNotEmpty()
    }
}
