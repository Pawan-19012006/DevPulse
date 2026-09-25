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

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.devpulse.ai.screens.ActiveSessionScreen

@Composable
fun NavGraph(navController: NavHostController) {
    // Shared ViewModel for GitHub telemetry & developer context
    val profileViewModel: ProfileViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel()
    val sessionViewModel: SessionViewModel = viewModel()

    val activeSessionState by sessionViewModel.engineState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // Primary Destination: 4 Pillars of DevPulse (Sessions, Health, Coach, Tracker)
        composable(Screen.Main.route) {
            com.devpulse.ai.screens.MainContainerScreen(
                homeViewModel = homeViewModel,
                sessionViewModel = sessionViewModel,
                activeSessionState = activeSessionState,
                onNavigateToSessionSetup = {
                    val intention = homeViewModel.currentIntention.value
                    if (intention.isNotBlank()) {
                        sessionViewModel.updateGoal(intention)
                    }
                    navController.navigate(Screen.SessionSetup.route)
                },
                onNavigateToActiveSession = { sessionId ->
                    navController.navigate(Screen.ActiveSession.createRoute(sessionId))
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
                onSessionStarted = { sessionId ->
                    navController.navigate(Screen.ActiveSession.createRoute(sessionId)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        // Active Session Engine & Real Running Clock
        composable(
            route = Screen.ActiveSession.route,
            arguments = listOf(
                navArgument("sessionId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            ActiveSessionScreen(
                sessionId = sessionId,
                viewModel = sessionViewModel,
                onNavigateHome = {
                    navController.popBackStack(Screen.Home.route, false)
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
