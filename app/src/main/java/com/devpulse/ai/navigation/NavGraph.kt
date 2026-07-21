package com.devpulse.ai.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.devpulse.ai.screens.DashboardScreen
import com.devpulse.ai.screens.LoginScreen
import com.devpulse.ai.viewmodel.LoginViewModel

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            val loginViewModel: LoginViewModel = viewModel()
            LoginScreen(
                viewModel = loginViewModel,
                onNavigateToDashboard = { username ->
                    navController.navigate(Screen.Dashboard.createRoute(username))
                }
            )
        }
        composable(
            route = Screen.Dashboard.route,
            arguments = listOf(
                navArgument("username") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            DashboardScreen(username = username)
        }
    }
}
