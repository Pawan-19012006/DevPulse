package com.devpulse.ai.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.devpulse.ai.data.local.entity.HealthEventEntity
import com.devpulse.ai.domain.session.HealthEventType
import com.devpulse.ai.ui.theme.BackgroundDark
import com.devpulse.ai.ui.theme.BorderDark
import com.devpulse.ai.ui.theme.BorderSubtle
import com.devpulse.ai.ui.theme.SageGreen
import com.devpulse.ai.ui.theme.SurfaceDark
import com.devpulse.ai.ui.theme.TextMuted
import com.devpulse.ai.ui.theme.TextPrimaryDark
import com.devpulse.ai.ui.theme.TextSecondaryDark
import com.devpulse.ai.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HealthScreen(
    homeViewModel: HomeViewModel
) {
    val hydrationCount by homeViewModel.todayHydrationCount.collectAsState()
    val screenRecoveryCount by homeViewModel.todayScreenRecoveryCount.collectAsState()
    val movementCount by homeViewModel.todayMovementCount.collectAsState()
    val breathingCount by homeViewModel.todayBreathingCount.collectAsState()
    val todayHealthEvents by homeViewModel.todayHealthEvents.collectAsState()
    val todaySessions by homeViewModel.todaySessions.collectAsState()

    val totalRecoveryResets = hydrationCount + screenRecoveryCount + movementCount + breathingCount

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
                    text = "Physical and mental awareness grounded in actual work sessions.",
                    fontSize = 14.sp,
                    color = TextSecondaryDark
                )
            }
        }

        // 1. Today's Physical Wellbeing Checks (derived from real Room events)
        item {
            Column {
                Text(
                    text = "TODAY",
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
                        statusText = "$hydrationCount completed"
                    )

                    HealthMetricRow(
                        emoji = "👀",
                        title = "Eye recovery",
                        statusText = "$screenRecoveryCount completed"
                    )

                    HealthMetricRow(
                        emoji = "🧍",
                        title = "Movement",
                        statusText = "$movementCount completed"
                    )

                    HealthMetricRow(
                        emoji = "🌿",
                        title = "Guided recovery",
                        statusText = "$breathingCount completed"
                    )
                }
            }
        }

        // 2. Recent Pattern Observation
        item {
            Column {
                Text(
                    text = "RECENT PATTERN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextSecondaryDark
                )

                Spacer(modifier = Modifier.height(10.dp))

                val patternText = when {
                    totalRecoveryResets >= 3 ->
                        "Most of your recovery actions happened during longer sessions. You are pacing your mental endurance well."
                    todaySessions.isNotEmpty() && totalRecoveryResets == 0 ->
                        "You've been in deep focus without logging screen or posture resets. DevPulse will naturally surface recovery nudges during your next session."
                    totalRecoveryResets > 0 ->
                        "You took $totalRecoveryResets recovery resets across your work today. Consistent micro-resets prevent end-of-day cognitive exhaustion."
                    else ->
                        "DevPulse accompanies your work naturally. Enter a work session to experience non-disruptive hydration nudges and guided recovery."
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "“$patternText”",
                        fontSize = 13.sp,
                        color = TextPrimaryDark,
                        lineHeight = 19.sp
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

        // 3. Recovery Journey (Simple history of real events)
        item {
            Column {
                Text(
                    text = "RECOVERY JOURNEY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextSecondaryDark
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (todayHealthEvents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceDark.copy(alpha = 0.5f))
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "No recovery events logged today yet.\nHealth actions happen naturally during your Dev Sessions.",
                            fontSize = 13.sp,
                            color = TextMuted,
                            lineHeight = 18.sp
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        todayHealthEvents.forEach { event ->
                            HealthEventHistoryRow(event)
                        }
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
    statusText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = emoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimaryDark
            )
        }

        Text(
            text = statusText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = SageGreen
        )
    }
}

@Composable
private fun HealthEventHistoryRow(event: HealthEventEntity) {
    val icon = when (event.type) {
        HealthEventType.HYDRATION.name -> "💧"
        HealthEventType.EYE_RECOVERY.name -> "👀"
        HealthEventType.MOVEMENT.name -> "🧍"
        HealthEventType.BREATHING.name -> "🌬️"
        else -> "🌿"
    }

    val typeDisplayName = when (event.type) {
        HealthEventType.HYDRATION.name -> "Hydration Check-in"
        HealthEventType.EYE_RECOVERY.name -> "Screen & Eye Recovery"
        HealthEventType.MOVEMENT.name -> "Movement & Posture Reset"
        HealthEventType.BREATHING.name -> "Guided Box Breathing"
        else -> "Guided Recovery Session"
    }

    val timeFormatted = rememberFormattedEventTime(event.timestamp)
    val sourceLabel = if (event.source == "SESSION_NUDGE") "Session Nudge" else "Guided Recovery"

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
            Text(text = icon, fontSize = 18.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = typeDisplayName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = sourceLabel,
                    fontSize = 11.sp,
                    color = TextSecondaryDark
                )
            }
        }

        Text(
            text = timeFormatted,
            fontSize = 12.sp,
            color = TextMuted
        )
    }
}

@Composable
private fun rememberFormattedEventTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
