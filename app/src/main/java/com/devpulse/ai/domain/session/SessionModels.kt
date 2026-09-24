package com.devpulse.ai.domain.session

/**
 * Developer-specific activities supported by DevPulse work sessions.
 */
enum class SessionActivityType(val displayName: String, val icon: String, val description: String) {
    CODING("Coding", "💻", "Feature building & application implementation"),
    DEBUGGING("Debugging", "🐛", "Root-cause investigation & bug resolution"),
    DSA("DSA & Algorithms", "🧩", "Problem solving & data structures"),
    LEARNING("Learning & Docs", "📚", "Reading documentation & exploring technologies"),
    CODE_REVIEW("Code Review", "👀", "Reviewing PRs & peer architecture"),
    PROJECT_WORK("Project Work", "🚀", "Side-projects & milestone progress"),
    PLANNING("Planning & Architecture", "📐", "System design & task breakdown"),
    CUSTOM("Custom Session", "⚡", "Personalized deep focus activity")
}

/**
 * Explicit developer state reported before, during, or after a work session.
 * (Not automated or AI-inferred).
 */
enum class DeveloperState(val displayName: String, val emoji: String, val isPreSessionOption: Boolean) {
    READY("Ready & Energized", "⚡", true),
    LOW_ENERGY("Low Energy", "🔋", true),
    MENTALLY_TIRED("Mentally Tired", "🥱", true),
    FLOWING("In The Flow", "🌊", false),
    GOOD("Good & Steady", "👍", false),
    STUCK("Stuck on a Problem", "🧱", false),
    FRUSTRATED("Frustrated", "😤", false),
    TIRED("Tired / Needs Rest", "🛑", false)
}

/**
 * The core 1% Better improvement categories.
 * "Every session. One improvement."
 */
enum class ImprovementCategory(val displayName: String, val prefixBadge: String, val icon: String) {
    KNOWLEDGE("Knowledge", "+1% Knowledge", "📚"),
    SKILL("Technical Skill", "+1% Skill", "🛠️"),
    BUILDING("Building & Shipping", "+1% Building", "🚀"),
    PROBLEM_SOLVING("Problem Solving", "+1% Problem Solving", "🧩"),
    FOCUS("Focus & Flow", "+1% Focus", "🎯"),
    RECOVERY("Recovery & Balance", "+1% Recovery", "🌿"),
    DISCIPLINE("Discipline & Consistency", "+1% Discipline", "⏳"),
    CREATIVITY("Creativity & Architecture", "+1% Creativity", "💡")
}

/**
 * Lifecycle states of a DevSession.
 */
enum class SessionStatus {
    PENDING,
    IN_PROGRESS,
    ON_BREAK,
    COMPLETED,
    CANCELLED
}
