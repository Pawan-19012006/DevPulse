package com.devpulse.ai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devpulse.ai.domain.coach.CoachAction
import com.devpulse.ai.domain.coach.CoachChatMessage
import com.devpulse.ai.domain.coach.CoachFallbackEngine
import com.devpulse.ai.domain.coach.DeveloperContextSummary
import com.devpulse.ai.domain.coach.MessageSender
import com.devpulse.ai.network.gemini.GeminiCoachService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CoachViewModel(
    private val geminiService: GeminiCoachService = GeminiCoachService()
) : ViewModel() {

    private val initialGreeting = CoachChatMessage(
        sender = MessageSender.COACH,
        text = "Hey. I'm your DevPulse Coach.\n\nI can help you manage your development workload, focus, recovery, stress, and long-term progress.\n\nWhat's on your mind?"
    )

    private val _messages = MutableStateFlow<List<CoachChatMessage>>(listOf(initialGreeting))
    val messages: StateFlow<List<CoachChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun sendMessage(text: String, contextSummary: DeveloperContextSummary) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _isLoading.value) return
        android.util.Log.d("CoachViewModel", "sendMessage invoked: $trimmed")

        val userMessage = CoachChatMessage(
            sender = MessageSender.USER,
            text = trimmed
        )

        val updatedMessages = _messages.value + userMessage
        _messages.value = updatedMessages
        _inputText.value = ""
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val coachResponse = geminiService.sendChatMessage(
                    userMessage = trimmed,
                    contextSummary = contextSummary,
                    conversationHistory = updatedMessages
                )
                _messages.value = _messages.value + coachResponse
            } catch (e: Exception) {
                // Guaranteed safety fallback
                val fallbackResponse = CoachFallbackEngine.generateConversationalReply(
                    userMessage = trimmed,
                    context = contextSummary,
                    conversationHistory = updatedMessages
                )
                _messages.value = _messages.value + fallbackResponse
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectPromptSuggestion(prompt: String, contextSummary: DeveloperContextSummary) {
        sendMessage(prompt, contextSummary)
    }

    fun clearConversation() {
        _messages.value = listOf(initialGreeting)
    }
}
