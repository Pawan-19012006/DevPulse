package com.devpulse.ai.domain.session

/**
 * Supported health action types recorded during or between Dev Sessions.
 */
enum class HealthEventType(val displayName: String, val icon: String) {
    HYDRATION("Hydration", "💧"),
    EYE_RECOVERY("Eye Recovery", "👀"),
    MOVEMENT("Movement", "🧍"),
    BREATHING("Guided Breathing", "🌬️"),
    RECOVERY_SESSION("Guided Recovery", "🌿")
}

/**
 * Non-disruptive health nudges surfaced inside an active work session.
 */
data class HealthNudge(
    val type: HealthEventType,
    val title: String,
    val message: String,
    val actionLabel: String = "DONE",
    val secondaryLabel: String = "REMIND ME LATER"
)

/**
 * Three guided recovery experiences for Deep Work breaks.
 */
enum class GuidedRecoveryType(
    val title: String,
    val defaultDurationMinutes: Int,
    val icon: String,
    val description: String
) {
    BREATHING(
        title = "Guided Breathing",
        defaultDurationMinutes = 10,
        icon = "🌬️",
        description = "Calm box-breathing to center and recharge your focus."
    ),
    EYE_RECOVERY(
        title = "Eye Recovery",
        defaultDurationMinutes = 3,
        icon = "👀",
        description = "Look away from the screen to relieve eye strain."
    ),
    MOVEMENT(
        title = "Movement Reset",
        defaultDurationMinutes = 5,
        icon = "🧍",
        description = "Physical sequence: stand up, roll shoulders, and stretch."
    )
}
