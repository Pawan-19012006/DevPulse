package com.devpulse.ai.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
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
