package com.devpulse.ai.navigation

enum class MainTab(val title: String) {
    SESSIONS("Sessions"),
    HEALTH("Health"),
    COACH("Coach"),
    TRACKER("Tracker")
}

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Home : Screen("main") // Alias for Main container
    object SessionSetup : Screen("session_setup")
    object ActiveSession : Screen("active_session/{sessionId}") {
        fun createRoute(sessionId: String) = "active_session/$sessionId"
    }
    object Journey : Screen("journey")
    object Login : Screen("login")
    object Dashboard : Screen("dashboard/{username}") {
        fun createRoute(username: String) = "dashboard/$username"
    }
}

