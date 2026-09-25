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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devpulse.ai.data.local.entity.DevSessionEntity
import com.devpulse.ai.data.local.entity.SessionHandoffEntity
import com.devpulse.ai.domain.session.SessionActivityType
import com.devpulse.ai.domain.session.SessionEngineState
import com.devpulse.ai.domain.session.SessionMode
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
import com.devpulse.ai.viewmodel.SessionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SessionsScreen(
    homeViewModel: HomeViewModel,
    sessionViewModel: SessionViewModel,
    activeSessionState: SessionEngineState,
    onNavigateToSessionSetup: () -> Unit,
    onNavigateToActiveSession: (String) -> Unit
) {
    val currentIntention by homeViewModel.currentIntention.collectAsState()
    val todaySessions by homeViewModel.todaySessions.collectAsState()
    val latestUnfinishedHandoff by homeViewModel.latestUnfinishedHandoff.collectAsState()
    val greetingTitle = homeViewModel.getGreetingTitle()

    val activeSessionId = when (activeSessionState) {
        is SessionEngineState.Working -> activeSessionState.session.id
        is SessionEngineState.Paused -> activeSessionState.session.id
        is SessionEngineState.WorkBlockHandoff -> activeSessionState.session.id
        is SessionEngineState.Recovery -> activeSessionState.session.id
        is SessionEngineState.ReadyForNextBlock -> activeSessionState.session.id
        is SessionEngineState.SessionCompleteReflection -> activeSessionState.session.id
        else -> null
    }

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
                    text = "DEV SESSIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = SageGreen
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = greetingTitle,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "A focused chapter of your work.",
                    fontSize = 14.sp,
                    color = TextSecondaryDark
                )
            }
        }

        // 1. Active Running Session Banner (if session is in progress)
        if (activeSessionId != null) {
            item {
                ActiveSessionResumeCard(
                    state = activeSessionState,
                    onResume = { onNavigateToActiveSession(activeSessionId) }
                )
            }
        }

        // 2. Continuity / Unfinished Handoff (Where you left off)
        if (latestUnfinishedHandoff != null && latestUnfinishedHandoff?.nextObjective?.isNotBlank() == true) {
            item {
                ContinuityHandoffCard(
                    handoff = latestUnfinishedHandoff!!,
                    onContinue = {
                        sessionViewModel.updateGoal(latestUnfinishedHandoff!!.sessionGoal)
                        sessionViewModel.updateBlockObjective(latestUnfinishedHandoff!!.nextObjective)
                        onNavigateToSessionSetup()
                    }
                )
            }
        }

        // 3. Start Something New
        item {
            StartSessionCard(
                intention = currentIntention,
                onIntentionChange = { homeViewModel.setIntention(it) },
                onStartClick = {
                    if (currentIntention.isNotBlank()) {
                        sessionViewModel.updateGoal(currentIntention)
                    }
                    onNavigateToSessionSetup()
                }
            )
        }

        // Subtle Divider
        item {
            HorizontalDivider(
                color = BorderSubtle,
                thickness = 1.dp
            )
        }

        // 4. Recent Work Context
        item {
            RecentSessionsSection(sessions = todaySessions)
        }
    }
}

/**
 * Calm active session banner showing real running status.
 */
@Composable
private fun ActiveSessionResumeCard(
    state: SessionEngineState,
    onResume: () -> Unit
) {
    val (label, statusText, isPaused) = when (state) {
        is SessionEngineState.Working -> {
            val blockNum = state.session.currentBlockIndex + 1
            val total = state.session.totalBlocks
            Triple("Work Block $blockNum of $total", "In Progress", false)
        }
        is SessionEngineState.Paused -> {
            Triple("Session Paused", "Paused", true)
        }
        is SessionEngineState.Recovery -> {
            Triple("Recovery Block", "Resting", false)
        }
        is SessionEngineState.WorkBlockHandoff -> {
            Triple("Block Handoff", "Action Needed", false)
        }
        is SessionEngineState.ReadyForNextBlock -> {
            Triple("Next Block Ready", "Ready", false)
        }
        is SessionEngineState.SessionCompleteReflection -> {
            Triple("Session Complete", "Reflection", false)
        }
        else -> Triple("Dev Session", "Active", false)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, if (isPaused) MutedAmber.copy(alpha = 0.6f) else SageGreen.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable(onClick = onResume)
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isPaused) MutedAmber else SageGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimaryDark
                    )
                }

                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    color = if (isPaused) MutedAmber else SageGreen,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onResume,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPaused) MutedAmber else SageGreen,
                    contentColor = BackgroundDark
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Resume Active Session",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Handoff card: Reminds developer where they left off and enables 1-tap continuation.
 */
@Composable
private fun ContinuityHandoffCard(
    handoff: SessionHandoffEntity,
    onContinue: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
            .padding(18.dp)
    ) {
        Column {
            Text(
                text = "CONTINUE WHERE YOU LEFT OFF",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = TextSecondaryDark
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "\"${handoff.nextObjective}\"",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Italic,
                color = TextPrimaryDark,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "From session: ${handoff.sessionGoal}",
                fontSize = 13.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MutedLavender,
                    contentColor = BackgroundDark
                )
            ) {
                Text(
                    text = "Continue This Objective",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Clean card to start a fresh Dev Session.
 */
@Composable
private fun StartSessionCard(
    intention: String,
    onIntentionChange: (String) -> Unit,
    onStartClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
            .padding(18.dp)
    ) {
        Column {
            Text(
                text = "START SOMETHING NEW",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = TextSecondaryDark
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = intention,
                onValueChange = onIntentionChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "What problem are you tackling next?",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark,
                    focusedContainerColor = SurfaceVariantDark,
                    unfocusedContainerColor = SurfaceVariantDark,
                    focusedBorderColor = MutedLavender,
                    unfocusedBorderColor = BorderSubtle,
                    cursorColor = MutedLavender
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onStartClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (intention.isNotBlank()) SageGreen else SurfaceVariantDark,
                    contentColor = if (intention.isNotBlank()) BackgroundDark else TextPrimaryDark
                )
            ) {
                Text(
                    text = if (intention.isNotBlank()) "Enter Dev Session" else "+ Start Dev Session",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Recent sessions list: clean, peaceful, no cards-inside-cards overload.
 */
@Composable
private fun RecentSessionsSection(
    sessions: List<DevSessionEntity>
) {
    Column {
        Text(
            text = "RECENT CONTEXT",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = TextSecondaryDark
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceDark.copy(alpha = 0.5f))
                    .padding(16.dp)
            ) {
                Text(
                    text = "No sessions recorded today yet. Ready to enter your first focused block?",
                    fontSize = 13.sp,
                    color = TextMuted,
                    lineHeight = 18.sp
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                sessions.take(4).forEach { session ->
                    RecentSessionRow(session)
                }
            }
        }
    }
}

@Composable
private fun RecentSessionRow(session: DevSessionEntity) {
    val durationText = if (session.actualDurationMinutes > 0) {
        "${session.actualDurationMinutes}m"
    } else {
        "${session.targetDurationMinutes}m"
    }

    val activityDisplayName = runCatching {
        SessionActivityType.valueOf(session.activityType).displayName
    }.getOrDefault(session.activityType)

    val iconEmoji = runCatching {
        SessionActivityType.valueOf(session.activityType).icon
    }.getOrDefault("💻")

    val timeFormatted = rememberFormattedTime(session.startedAt)

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
            Text(
                text = iconEmoji,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = session.goal?.ifBlank { activityDisplayName } ?: activityDisplayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimaryDark,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$activityDisplayName · $timeFormatted",
                    fontSize = 12.sp,
                    color = TextSecondaryDark
                )
            }
        }

        Text(
            text = durationText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = SageGreen
        )
    }
}

@Composable
private fun rememberFormattedTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
