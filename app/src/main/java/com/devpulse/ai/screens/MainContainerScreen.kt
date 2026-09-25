package com.devpulse.ai.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devpulse.ai.domain.session.SessionEngineState
import com.devpulse.ai.navigation.MainTab
import com.devpulse.ai.ui.theme.BackgroundDark
import com.devpulse.ai.ui.theme.BorderDark
import com.devpulse.ai.ui.theme.BorderSubtle
import com.devpulse.ai.ui.theme.MutedLavender
import com.devpulse.ai.ui.theme.SageGreen
import com.devpulse.ai.ui.theme.SurfaceDark
import com.devpulse.ai.ui.theme.SurfaceVariantDark
import com.devpulse.ai.ui.theme.TextMuted
import com.devpulse.ai.ui.theme.TextPrimaryDark
import com.devpulse.ai.ui.theme.TextSecondaryDark
import com.devpulse.ai.viewmodel.HomeViewModel
import com.devpulse.ai.viewmodel.SessionViewModel

@Composable
fun MainContainerScreen(
    homeViewModel: HomeViewModel,
    sessionViewModel: SessionViewModel,
    activeSessionState: SessionEngineState,
    onNavigateToSessionSetup: () -> Unit,
    onNavigateToActiveSession: (String) -> Unit,
    onNavigateToGitHubContext: (String?) -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.SESSIONS) }

    Scaffold(
        containerColor = BackgroundDark,
        bottomBar = {
            DevPulseBottomNavigation(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Crossfade(
                targetState = selectedTab,
                label = "MainPillarCrossfade"
            ) { tab ->
                when (tab) {
                    MainTab.SESSIONS -> SessionsScreen(
                        homeViewModel = homeViewModel,
                        sessionViewModel = sessionViewModel,
                        activeSessionState = activeSessionState,
                        onNavigateToSessionSetup = onNavigateToSessionSetup,
                        onNavigateToActiveSession = onNavigateToActiveSession
                    )
                    MainTab.HEALTH -> HealthScreen(
                        homeViewModel = homeViewModel
                    )
                    MainTab.COACH -> CoachScreen(
                        homeViewModel = homeViewModel,
                        sessionViewModel = sessionViewModel,
                        onNavigateToSessionSetup = onNavigateToSessionSetup
                    )
                    MainTab.TRACKER -> TrackerScreen(
                        homeViewModel = homeViewModel,
                        sessionViewModel = sessionViewModel,
                        onNavigateToGitHubContext = onNavigateToGitHubContext
                    )
                }
            }
        }
    }
}

/**
 * Refined, quiet four-item bottom navigation.
 * No neon glow, no huge icons, calm selected indicator.
 */
@Composable
private fun DevPulseBottomNavigation(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .border(width = 1.dp, color = BorderSubtle)
            .navigationBarsPadding()
            .height(64.dp)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainTab.values().forEach { tab ->
                val isSelected = tab == selectedTab

                val (emoji, title) = when (tab) {
                    MainTab.SESSIONS -> "💻" to "Sessions"
                    MainTab.HEALTH -> "🌿" to "Health"
                    MainTab.COACH -> "🧠" to "Coach"
                    MainTab.TRACKER -> "🌱" to "Tracker"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) SurfaceVariantDark else SurfaceDark)
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) SageGreen else TextSecondaryDark
                        )
                    }
                }
            }
        }
    }
}
