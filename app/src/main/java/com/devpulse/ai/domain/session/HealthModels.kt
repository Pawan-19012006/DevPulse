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

/**
 * Four primary developer strain categories for the Recovery Center.
 */
enum class RecoveryCategory(
    val displayName: String,
    val icon: String,
    val sectionTitle: String,
    val subtitle: String
) {
    EYES(
        displayName = "Eyes",
        icon = "👁️",
        sectionTitle = "Eye Recovery",
        subtitle = "Give your eyes a quick reset."
    ),
    FOCUS(
        displayName = "Focus",
        icon = "🧠",
        sectionTitle = "Focus Reset",
        subtitle = "Clear your mind before your next task."
    ),
    BODY(
        displayName = "Body",
        icon = "🧍",
        sectionTitle = "Body Reset",
        subtitle = "Release tension from long coding sessions."
    ),
    STRESS(
        displayName = "Stress",
        icon = "😮‍💨",
        sectionTitle = "Stress Reset",
        subtitle = "Slow down and reset before continuing."
    )
}

enum class RecoveryExperienceType {
    STEP_SEQUENCE,
    BOX_BREATHING,
    SINGLE_TASK_INPUT,
    EYE_FOCUS_POINT
}

data class RecoveryStep(
    val title: String,
    val instruction: String,
    val durationSeconds: Int = 30
)

data class RecoveryActivity(
    val id: String,
    val category: RecoveryCategory,
    val title: String,
    val durationMinutes: Int,
    val durationLabel: String,
    val description: String,
    val icon: String,
    val type: RecoveryExperienceType,
    val steps: List<RecoveryStep> = emptyList(),
    val healthEventType: HealthEventType = HealthEventType.RECOVERY_SESSION
)

object RecoveryCatalog {
    val activities: List<RecoveryActivity> = listOf(
        // EYES
        RecoveryActivity(
            id = "eye_focus_reset",
            category = RecoveryCategory.EYES,
            title = "Eye Focus Reset",
            durationMinutes = 2,
            durationLabel = "2 min",
            description = "Look away and refocus",
            icon = "👁️",
            type = RecoveryExperienceType.EYE_FOCUS_POINT,
            steps = listOf(
                RecoveryStep("Look at distant object", "Look at an object at least 20 feet away to relax your ciliary muscles.", 30),
                RecoveryStep("Follow moving focus point", "Track the smooth green focus dot with your eyes without turning your head.", 30),
                RecoveryStep("Blink slowly", "Perform 10 slow, intentional blinks to refresh and moisturize your corneas.", 30),
                RecoveryStep("Short visual rest", "Close your eyes softly and let your eye muscles completely rest.", 30)
            ),
            healthEventType = HealthEventType.EYE_RECOVERY
        ),
        RecoveryActivity(
            id = "screen_rest",
            category = RecoveryCategory.EYES,
            title = "Screen Rest",
            durationMinutes = 1,
            durationLabel = "1 min",
            description = "Give your eyes a break",
            icon = "🌿",
            type = RecoveryExperienceType.STEP_SEQUENCE,
            steps = listOf(
                RecoveryStep("Look away from screen", "Turn your gaze away from all monitors, phones, and illuminated displays.", 20),
                RecoveryStep("Keep eyes relaxed", "Soften your vision. Gaze toward a natural light source or quiet wall.", 20),
                RecoveryStep("Finish with slow blinking", "Take 5 slow deliberate blinks to rehydrate and soothe eye strain.", 20)
            ),
            healthEventType = HealthEventType.EYE_RECOVERY
        ),
        RecoveryActivity(
            id = "blink_refocus",
            category = RecoveryCategory.EYES,
            title = "Blink & Refocus",
            durationMinutes = 1,
            durationLabel = "60 sec",
            description = "Relax and refocus",
            icon = "👀",
            type = RecoveryExperienceType.STEP_SEQUENCE,
            steps = listOf(
                RecoveryStep("Slow blink", "Close eyes fully for two seconds, then open gently. Repeat 3 times.", 15),
                RecoveryStep("Look left", "Keep head still, smoothly direct your gaze all the way to your left.", 15),
                RecoveryStep("Look right", "Smoothly sweep your gaze to the far right, holding comfortably.", 15),
                RecoveryStep("Look at a distant object", "Cast your focus on a distant landmark or outside the window.", 15)
            ),
            healthEventType = HealthEventType.EYE_RECOVERY
        ),

        // FOCUS
        RecoveryActivity(
            id = "focus_reset",
            category = RecoveryCategory.FOCUS,
            title = "Focus Reset",
            durationMinutes = 2,
            durationLabel = "2 min",
            description = "Clear mental cache",
            icon = "🧠",
            type = RecoveryExperienceType.STEP_SEQUENCE,
            steps = listOf(
                RecoveryStep("Close / relax eyes", "Shut your eyes and let go of the code problem you were just working on.", 30),
                RecoveryStep("Slow breathing", "Take 4 slow nasal breaths to settle your nervous system.", 30),
                RecoveryStep("Remove visual distractions", "Minimize unnecessary editor tabs, terminal panes, and notifications.", 30),
                RecoveryStep("Prepare for next task", "Mentally formulate the exact next milestone you wish to tackle.", 30)
            ),
            healthEventType = HealthEventType.BREATHING
        ),
        RecoveryActivity(
            id = "single_task_reset",
            category = RecoveryCategory.FOCUS,
            title = "Single-Task Reset",
            durationMinutes = 1,
            durationLabel = "1 min",
            description = "Define your next focus",
            icon = "🎯",
            type = RecoveryExperienceType.SINGLE_TASK_INPUT,
            steps = listOf(
                RecoveryStep("Define the next priority", "What is the ONE thing you want to accomplish next?", 60)
            ),
            healthEventType = HealthEventType.RECOVERY_SESSION
        ),
        RecoveryActivity(
            id = "breathing_focus",
            category = RecoveryCategory.FOCUS,
            title = "Breathing Focus",
            durationMinutes = 2,
            durationLabel = "2 min",
            description = "Center your attention",
            icon = "🌬️",
            type = RecoveryExperienceType.BOX_BREATHING,
            healthEventType = HealthEventType.BREATHING
        ),

        // BODY
        RecoveryActivity(
            id = "posture_reset",
            category = RecoveryCategory.BODY,
            title = "Posture Reset",
            durationMinutes = 2,
            durationLabel = "2 min",
            description = "Re-align spine and shoulders",
            icon = "🧍",
            type = RecoveryExperienceType.STEP_SEQUENCE,
            steps = listOf(
                RecoveryStep("Sit upright", "Plant feet flat on the floor, bring ears in line with your shoulders.", 24),
                RecoveryStep("Relax shoulders", "Drop shoulders actively away from your ears, releasing tension.", 24),
                RecoveryStep("Neck rotation", "Slowly trace smooth head circles, 2 to the left, then 2 to the right.", 24),
                RecoveryStep("Shoulder roll", "Roll both shoulder blades back and down in wide circular sweeps.", 24),
                RecoveryStep("Back reset", "Place hands on lower back, gently arch upward, and open the chest.", 24)
            ),
            healthEventType = HealthEventType.MOVEMENT
        ),
        RecoveryActivity(
            id = "shoulder_neck_reset",
            category = RecoveryCategory.BODY,
            title = "Shoulder & Neck Reset",
            durationMinutes = 3,
            durationLabel = "3 min",
            description = "Release upper back tightness",
            icon = "🙆",
            type = RecoveryExperienceType.STEP_SEQUENCE,
            steps = listOf(
                RecoveryStep("Chin tucks", "Pull chin straight backward horizontally, holding for 5 seconds.", 36),
                RecoveryStep("Side neck stretch", "Tilt ear gently to shoulder, breathe out, then switch sides.", 36),
                RecoveryStep("Shoulder blade squeeze", "Draw shoulder blades firmly together as if gripping a pencil.", 36),
                RecoveryStep("Wrist & forearm stretch", "Extend arms forward, pull fingers back gently to stretch carpal nerves.", 36),
                RecoveryStep("Exhale & shake out", "Shake hands out, let shoulders fall limp, and take a deep breath.", 36)
            ),
            healthEventType = HealthEventType.MOVEMENT
        ),
        RecoveryActivity(
            id = "movement_break",
            category = RecoveryCategory.BODY,
            title = "Movement Break",
            durationMinutes = 2,
            durationLabel = "2 min",
            description = "Get blood moving",
            icon = "🚶",
            type = RecoveryExperienceType.STEP_SEQUENCE,
            steps = listOf(
                RecoveryStep("Stand up", "Push back your chair and stand completely straight.", 30),
                RecoveryStep("Walk / move around", "Take a short walk across the room to re-engage lower body blood flow.", 30),
                RecoveryStep("Shoulder movement", "Swing arms loosely front-to-back and twist your torso gently.", 30),
                RecoveryStep("Return to workstation", "Take a sip of water and sit down with refreshed, aligned posture.", 30)
            ),
            healthEventType = HealthEventType.MOVEMENT
        ),

        // STRESS
        RecoveryActivity(
            id = "box_breathing",
            category = RecoveryCategory.STRESS,
            title = "Box Breathing",
            durationMinutes = 2,
            durationLabel = "2 min",
            description = "Regulate nervous system",
            icon = "🌬️",
            type = RecoveryExperienceType.BOX_BREATHING,
            healthEventType = HealthEventType.BREATHING
        ),
        RecoveryActivity(
            id = "guided_calm",
            category = RecoveryCategory.STRESS,
            title = "Guided Calm",
            durationMinutes = 3,
            durationLabel = "3 min",
            description = "Decompress mental strain",
            icon = "🧘",
            type = RecoveryExperienceType.STEP_SEQUENCE,
            steps = listOf(
                RecoveryStep("Slow breathing", "Inhale softly for 4s, exhale quietly for 6s. Feel the heart rate drop.", 36),
                RecoveryStep("Relax shoulders", "Allow gravity to carry shoulders down away from your neck.", 36),
                RecoveryStep("Release jaw tension", "Unclench molars, loosen tongue, and soften facial expression.", 36),
                RecoveryStep("Mindful pause", "Rest in silence without reacting to thoughts or notification urges.", 36),
                RecoveryStep("Prepare to continue", "Gently open eyes. Approach your next commit with calm precision.", 36)
            ),
            healthEventType = HealthEventType.BREATHING
        ),
        RecoveryActivity(
            id = "quick_reset",
            category = RecoveryCategory.STRESS,
            title = "Quick Reset",
            durationMinutes = 1,
            durationLabel = "1 min",
            description = "Fast grounding reset",
            icon = "🌿",
            type = RecoveryExperienceType.STEP_SEQUENCE,
            steps = listOf(
                RecoveryStep("Deep grounding breath", "Take a slow, full belly breath and exhale with a soft sigh.", 20),
                RecoveryStep("Body scan", "Check neck, shoulders, and lower back; release any bracing.", 20),
                RecoveryStep("Quiet intention", "Ground yourself in the present moment. You are doing fine.", 20)
            ),
            healthEventType = HealthEventType.BREATHING
        )
    )

    fun getActivitiesForCategory(category: RecoveryCategory): List<RecoveryActivity> {
        return activities.filter { it.category == category }
    }
}

