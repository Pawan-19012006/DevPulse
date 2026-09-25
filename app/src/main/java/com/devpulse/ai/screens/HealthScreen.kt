package com.devpulse.ai.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devpulse.ai.domain.session.DeveloperState
import com.devpulse.ai.ui.theme.BackgroundDark
import com.devpulse.ai.ui.theme.BorderDark
import com.devpulse.ai.ui.theme.BorderSubtle
import com.devpulse.ai.ui.theme.MutedAmber
import com.devpulse.ai.ui.theme.MutedLavender
import com.devpulse.ai.ui.theme.SageGreen
import com.devpulse.ai.ui.theme.SurfaceDark
import com.devpulse.ai.ui.theme.SurfaceVariantDark
import com.devpulse.ai.ui.theme.TextMuted
import com.devpulse.ai.ui.theme.TextPrimaryDark
import com.devpulse.ai.ui.theme.TextSecondaryDark
import com.devpulse.ai.viewmodel.HomeViewModel

@Composable
fun HealthScreen(
    homeViewModel: HomeViewModel
) {
    val currentState by homeViewModel.currentState.collectAsState()
    val hydrationCount by homeViewModel.hydrationCount.collectAsState()
    val screenRecoveryCount by homeViewModel.screenRecoveryCount.collectAsState()
    val movementCount by homeViewModel.movementCount.collectAsState()
    val todaySessions by homeViewModel.todaySessions.collectAsState()

    val frustratedCount = todaySessions.count { it.initialState == DeveloperState.FRUSTRATED.name }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 22.dp),
        contentPadding = PaddingValues(top = 28.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Screen Header
        item {
            Column {
                Text(
                    text = "DEV HEALTH",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = SageGreen
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "How am I doing while I build?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Physical and mental awareness for sustainable engineering.",
                    fontSize = 14.sp,
                    color = TextSecondaryDark
                )
            }
        }

        // 1. Current State Check-in
        item {
            DeveloperStateSelectorSection(
                currentState = currentState,
                onStateSelected = { homeViewModel.setDeveloperState(it) }
            )
        }

        // 2. Today's Physical Wellness Pulse
        item {
            Column {
                Text(
                    text = "TODAY'S WELLBEING CHECKS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextSecondaryDark
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HealthMetricRow(
                        emoji = "💧",
                        title = "Hydration",
                        statusText = "$hydrationCount check-in${if (hydrationCount == 1) "" else "s"}",
                        actionLabel = "+ Log Water",
                        onAction = { homeViewModel.logHydration() }
                    )

                    HealthMetricRow(
                        emoji = "👀",
                        title = "Screen Recovery",
                        statusText = "$screenRecoveryCount break${if (screenRecoveryCount == 1) "" else "s"} taken",
                        actionLabel = "+ Eye Reset",
                        onAction = { homeViewModel.logScreenRecovery() }
                    )

                    HealthMetricRow(
                        emoji = "🧍",
                        title = "Movement & Posture",
                        statusText = "$movementCount reset${if (movementCount == 1) "" else "s"}",
                        actionLabel = "+ Stretch",
                        onAction = { homeViewModel.logMovement() }
                    )

                    HealthMetricRow(
                        emoji = "🧠",
                        title = "Mindset State",
                        statusText = if (frustratedCount > 0) "${currentState.displayName} ($frustratedCount frustrated session)" else "Mostly ${currentState.displayName.lowercase()}",
                        actionLabel = null,
                        onAction = null
                    )
                }
            }
        }

        item {
            HorizontalDivider(
                color = BorderSubtle,
                thickness = 1.dp
            )
        }

        // 3. Ergonomic Habits for Software Developers
        item {
            Column {
                Text(
                    text = "SUSTAINABLE WORKSPACE HABITS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextSecondaryDark
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    WorkspaceHabitItem(
                        title = "The 20-20-20 Eye Rule",
                        description = "Every 20 minutes, gaze at an object 20 feet away for 20 seconds to prevent ciliary muscle spasm."
                    )
                    WorkspaceHabitItem(
                        title = "Shoulder & Cervical Reset",
                        description = "Drop your shoulders away from your ears. Tuck your chin back slightly to decompress the upper spine."
                    )
                    WorkspaceHabitItem(
                        title = "Circular Debugging Walk",
                        description = "When a bug defies logic for over 30 minutes, step away from the monitor. Unconscious diffuse-mode thinking often solves the edge case."
                    )
                }
            }
        }
    }
}

@Composable
private fun DeveloperStateSelectorSection(
    currentState: DeveloperState,
    onStateSelected: (DeveloperState) -> Unit
) {
    Column {
        Text(
            text = "HOW ARE YOU FEELING RIGHT NOW?",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = TextSecondaryDark
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(DeveloperState.values()) { state ->
                val isSelected = state == currentState
                val emoji = state.emoji

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) SurfaceVariantDark else SurfaceDark)
                        .border(
                            1.dp,
                            if (isSelected) MutedLavender else BorderDark,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onStateSelected(state) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = emoji, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = state.displayName,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) TextPrimaryDark else TextSecondaryDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthMetricRow(
    emoji: String,
    title: String,
    statusText: String,
    actionLabel: String?,
    onAction: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )
            }
        }

        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceVariantDark,
                    contentColor = TextPrimaryDark
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = actionLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun WorkspaceHabitItem(
    title: String,
    description: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark.copy(alpha = 0.6f))
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimaryDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = TextSecondaryDark,
                lineHeight = 17.sp
            )
        }
    }
}
