package com.devpulse.ai.domain.coach

import java.util.UUID

enum class MessageSender {
    USER,
    COACH
}

enum class CoachAction {
    START_RECOVERY,
    CONTINUE_DAILY_SHIP,
    START_FOCUS_SESSION,
    VIEW_TRACKER,
    NONE
}

data class CoachChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val action: CoachAction = CoachAction.NONE,
    val actionLabel: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isFromAi: Boolean = false
)


data class CoachInsight(
    val title: String,
    val observation: String,
    val guidance: String,
    val action: CoachAction = CoachAction.NONE,
    val actionLabel: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isFromAi: Boolean = false
)

data class DeveloperContextSummary(
    val currentGoal: String = "Become a Backend Developer",
    val targetDays: Int = 60,
    val meaningfulDays: Int = 12,
    val currentDailyShip: String? = null,
    val dailyShipStatus: String? = null,
    val sessionsToday: Int = 0,
    val totalFocusMinutesToday: Int = 0,
    val averageSessionDuration: Int = 0,
    val recoverySessionsToday: Int = 0,
    val recentRecoveryTypes: List<String> = emptyList(),
    val latestWellnessCheck: String? = null,
    val impedimentText: String? = null,
    val recentOnePercentImprovements: List<String> = emptyList(),
    val recentOnePercentCategories: List<String> = emptyList(),
    val githubConnected: Boolean = false,
    val githubUsername: String? = null
) {
    fun toPromptSummary(): String {
        return buildString {
            appendLine("DEVPULSE USER CONTEXT:")
            appendLine("Long-term goal: \"$currentGoal\"")
            appendLine("Meaningful development days: $meaningfulDays / $targetDays")
            appendLine("Today's sessions: $sessionsToday")
            appendLine("Today's focus time: $totalFocusMinutesToday minutes")
            appendLine("Average session: $averageSessionDuration minutes")
            appendLine("Recovery sessions: $recoverySessionsToday")
            if (recentRecoveryTypes.isNotEmpty()) {
                appendLine("Recent recovery types: ${recentRecoveryTypes.joinToString(", ")}")
            }
            appendLine("Latest wellness state: ${latestWellnessCheck ?: "Not specified"}")
            if (!impedimentText.isNullOrBlank()) {
                appendLine("Latest impediment: \"$impedimentText\"")
            }
            appendLine("Today's Daily Ship: ${currentDailyShip?.let { "\"$it\"" } ?: "Not available"}")
            appendLine("Daily Ship status: ${dailyShipStatus ?: "Not available"}")
            if (recentOnePercentImprovements.isNotEmpty()) {
                appendLine("Recent 1% Better improvements:")
                recentOnePercentImprovements.take(3).forEach {
                    appendLine("  - \"$it\"")
                }
            } else {
                appendLine("Recent 1% Better improvements: None logged yet")
            }
            appendLine("GitHub: ${if (githubConnected) "Connected ($githubUsername)" else "Not connected"}")
        }
    }
}
