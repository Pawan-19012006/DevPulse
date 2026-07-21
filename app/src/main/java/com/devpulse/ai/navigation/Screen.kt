package com.devpulse.ai.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard/{username}") {
        fun createRoute(username: String) = "dashboard/$username"
    }
}
