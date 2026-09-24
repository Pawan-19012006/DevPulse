package com.devpulse.ai.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.devpulse.ai.screens.DashboardScreen
import com.devpulse.ai.screens.HomeScreen
import com.devpulse.ai.screens.JourneyScreen
import com.devpulse.ai.screens.LoginScreen
import com.devpulse.ai.screens.SessionSetupScreen
import com.devpulse.ai.viewmodel.HomeViewModel
import com.devpulse.ai.viewmodel.LoginViewModel
import com.devpulse.ai.viewmodel.ProfileViewModel
import com.devpulse.ai.viewmodel.SessionViewModel

@Composable
fun NavGraph(navController: NavHostController) {
    // Shared ViewModel for GitHub telemetry & developer context
    val profileViewModel: ProfileViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel()
    val sessionViewModel: SessionViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // Daily Pulse Home - Main entry point
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToSessionSetup = {
                    val intention = homeViewModel.currentIntention.value
                    if (intention.isNotBlank()) {
                        sessionViewModel.updateGoal(intention)
                    }
                    sessionViewModel.selectState(homeViewModel.currentState.value)
                    navController.navigate(Screen.SessionSetup.route)
                },
                onNavigateToJourney = {
                    navController.navigate(Screen.Journey.route)
                },
                onNavigateToGitHubContext = { username ->
                    if (!username.isNullOrBlank()) {
                        navController.navigate(Screen.Dashboard.createRoute(username))
                    } else {
                        navController.navigate(Screen.Login.route)
                    }
                }
            )
        }

        // Session Setup & Pre-Session Ritual
        composable(Screen.SessionSetup.route) {
            SessionSetupScreen(
                viewModel = sessionViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSessionStarted = {
                    navController.popBackStack()
                }
            )
        }

        // My Journey & 1% Better Log
        composable(Screen.Journey.route) {
            JourneyScreen(
                viewModel = sessionViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // GitHub Connect Screen (Preserved developer context)
        composable(Screen.Login.route) {
            val loginViewModel: LoginViewModel = viewModel()
            LoginScreen(
                viewModel = loginViewModel,
                profileViewModel = profileViewModel,
                onNavigateToDashboard = { username ->
                    navController.navigate(Screen.Dashboard.createRoute(username))
                }
            )
        }

        // GitHub Telemetry & Developer Context Dashboard (Preserved)
        composable(
            route = Screen.Dashboard.route,
            arguments = listOf(
                navArgument("username") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            DashboardScreen(
                username = username,
                viewModel = profileViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
