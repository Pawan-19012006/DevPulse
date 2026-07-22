package com.devpulse.ai.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devpulse.ai.components.BrandBackground
import com.devpulse.ai.components.ShimmerDashboardLoader
import com.devpulse.ai.screens.tabs.*
import com.devpulse.ai.ui.theme.BackgroundDark
import com.devpulse.ai.ui.theme.Primary
import com.devpulse.ai.ui.theme.Secondary
import com.devpulse.ai.ui.theme.SurfaceDark
import com.devpulse.ai.ui.theme.TextPrimaryDark
import com.devpulse.ai.ui.theme.TextSecondaryDark
import com.devpulse.ai.viewmodel.ErrorType
import com.devpulse.ai.viewmodel.ProfileUiState
import com.devpulse.ai.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    username: String,
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val profile by viewModel.analyzedProfile.collectAsState()

    // Trigger analysis only if the cached profile is missing
    LaunchedEffect(username, profile) {
        if (profile == null) {
            viewModel.fetchAndAnalyzeProfile(username)
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    val currentProfile = profile
    if (currentProfile != null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = currentProfile.user.name ?: currentProfile.user.login,
                                color = TextPrimaryDark,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "AI-growth summary",
                                color = TextSecondaryDark,
                                fontSize = 11.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimaryDark
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BackgroundDark
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = BackgroundDark,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                        label = { Text("Dashboard", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BackgroundDark,
                            selectedTextColor = Primary,
                            indicatorColor = Primary,
                            unselectedIconColor = TextSecondaryDark,
                            unselectedTextColor = TextSecondaryDark
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Star, contentDescription = "Analytics") },
                        label = { Text("Analytics", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BackgroundDark,
                            selectedTextColor = Secondary,
                            indicatorColor = Secondary,
                            unselectedIconColor = TextSecondaryDark,
                            unselectedTextColor = TextSecondaryDark
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Info, contentDescription = "AI Insights") },
                        label = { Text("AI Insights", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BackgroundDark,
                            selectedTextColor = Primary,
                            indicatorColor = Primary,
                            unselectedIconColor = TextSecondaryDark,
                            unselectedTextColor = TextSecondaryDark
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Roadmap") },
                        label = { Text("Roadmap", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BackgroundDark,
                            selectedTextColor = Secondary,
                            indicatorColor = Secondary,
                            unselectedIconColor = TextSecondaryDark,
                            unselectedTextColor = TextSecondaryDark
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BackgroundDark,
                            selectedTextColor = TextPrimaryDark,
                            indicatorColor = TextPrimaryDark,
                            unselectedIconColor = TextSecondaryDark,
                            unselectedTextColor = TextSecondaryDark
                        )
                    )
                }
            },
            content = { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundDark)
                        .padding(paddingValues)
                ) {
                    when (selectedTab) {
                        0 -> TabDashboard(profile = currentProfile)
                        1 -> TabAnalytics(profile = currentProfile)
                        2 -> TabInsights(profile = currentProfile)
                        3 -> TabRoadmap(profile = currentProfile)
                        4 -> TabSettings()
                    }
                }
            }
        )
    } else {
        when (val state = uiState) {
            is ProfileUiState.Idle, is ProfileUiState.Loading -> {
                // Loading State: Show shimmers inside the premium theme
                BrandBackground {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        // Small header to keep back navigation available
                        TopAppBar(
                            title = { Text("Analyzing profile...", color = Color.White, fontSize = 16.sp) },
                            navigationIcon = {
                                IconButton(onClick = onNavigateBack) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent
                            )
                        )
                        ShimmerDashboardLoader()
                    }
                }
            }
            is ProfileUiState.Error -> {
                // Error State: Show M3 Dialog and clean redirection
                ErrorDialog(
                    title = when (state.errorType) {
                        ErrorType.USER_NOT_FOUND -> "User Not Found"
                        ErrorType.RATE_LIMIT -> "API Rate Limit"
                        ErrorType.NETWORK -> "No Internet"
                        ErrorType.EMPTY_REPOS -> "Empty Repositories"
                        ErrorType.TOKEN_MISSING -> "Token Missing"
                        else -> "Analysis Error"
                    },
                    message = state.message,
                    onDismiss = {
                        viewModel.clearError()
                        onNavigateBack()
                    }
                )
            }
            is ProfileUiState.Success -> {
                // Fallback (already covered by main if check, but required for compilation mapping)
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun ErrorDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Text(
                text = message,
                color = TextSecondaryDark,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK", color = Primary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(16.dp)
    )
}
