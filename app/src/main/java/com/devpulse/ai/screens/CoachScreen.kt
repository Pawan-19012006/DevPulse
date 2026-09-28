package com.devpulse.ai.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devpulse.ai.domain.coach.CoachAction
import com.devpulse.ai.domain.coach.CoachChatMessage
import com.devpulse.ai.domain.coach.DeveloperContextSummary
import com.devpulse.ai.domain.coach.MessageSender
import com.devpulse.ai.domain.constellation.MockConstellation
import com.devpulse.ai.ui.theme.BackgroundDark
import com.devpulse.ai.ui.theme.BorderDark
import com.devpulse.ai.ui.theme.BorderSubtle
import com.devpulse.ai.ui.theme.SageGreen
import com.devpulse.ai.ui.theme.SurfaceDark
import com.devpulse.ai.ui.theme.SurfaceVariantDark
import com.devpulse.ai.ui.theme.TextMuted
import com.devpulse.ai.ui.theme.TextPrimaryDark
import com.devpulse.ai.ui.theme.TextSecondaryDark
import com.devpulse.ai.viewmodel.CoachViewModel
import com.devpulse.ai.viewmodel.HomeViewModel
import com.devpulse.ai.viewmodel.SessionViewModel

@Composable
fun CoachScreen(
    homeViewModel: HomeViewModel,
    sessionViewModel: SessionViewModel,
    coachViewModel: CoachViewModel = viewModel(),
    onNavigateToSessionSetup: () -> Unit,
    onNavigateToSessions: () -> Unit = {},
    onNavigateToHealth: () -> Unit = {},
    onNavigateToTracker: () -> Unit = {}
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val prefs = remember(context) {
        context.getSharedPreferences("devpulse_constellation_prefs", Context.MODE_PRIVATE)
    }

    val goalTitle = prefs.getString("goal_title", "Become a Backend Developer") ?: "Become a Backend Developer"
    val targetDays = prefs.getInt("target_days", 60)

    val todaySessions by homeViewModel.todaySessions.collectAsState()
    val todayHealthEvents by homeViewModel.todayHealthEvents.collectAsState()
    val dailySummary by homeViewModel.dailySummary.collectAsState()
    val latestHandoff by homeViewModel.latestUnfinishedHandoff.collectAsState()
    val improvements by sessionViewModel.allImprovements.collectAsState()
    val connectedGitHubUser by homeViewModel.connectedGitHubUser.collectAsState()

    val meaningfulDaysCount = MockConstellation.stars.size + improvements.size

    // Build structured DevPulse context snapshot for the coach
    val contextSummary = remember(
        goalTitle,
        targetDays,
        meaningfulDaysCount,
        todaySessions,
        todayHealthEvents,
        dailySummary,
        latestHandoff,
        improvements,
        connectedGitHubUser
    ) {
        DeveloperContextSummary(
            currentGoal = goalTitle,
            targetDays = targetDays,
            meaningfulDays = meaningfulDaysCount,
            currentDailyShip = latestHandoff?.sessionGoal ?: "Implement refresh-token handling",
            dailyShipStatus = if (latestHandoff != null) "In Progress" else "Active",
            sessionsToday = todaySessions.size,
            totalFocusMinutesToday = dailySummary.totalFocusedMinutes,
            averageSessionDuration = if (todaySessions.isNotEmpty()) dailySummary.totalFocusedMinutes / todaySessions.size else 25,
            recoverySessionsToday = todayHealthEvents.size,
            recentRecoveryTypes = todayHealthEvents.map { it.type }.distinct(),
            recentOnePercentImprovements = improvements.takeLast(3).map { it.reflectionText },
            recentOnePercentCategories = improvements.takeLast(3).map { it.category },
            githubConnected = connectedGitHubUser != null,
            githubUsername = connectedGitHubUser
        )
    }

    val messages by coachViewModel.messages.collectAsState()
    val isLoading by coachViewModel.isLoading.collectAsState()
    val inputText by coachViewModel.inputText.collectAsState()

    val listState = rememberLazyListState()

    // Auto-scroll to bottom whenever a new message arrives or loading state changes
    LaunchedEffect(messages.size, isLoading) {
        val targetIndex = (messages.size - 1 + if (isLoading) 1 else 0).coerceAtLeast(0)
        listState.animateScrollToItem(targetIndex)
    }

    val handleAction: (CoachAction) -> Unit = { action ->
        when (action) {
            CoachAction.START_RECOVERY -> onNavigateToHealth()
            CoachAction.CONTINUE_DAILY_SHIP -> {
                latestHandoff?.let { handoff ->
                    sessionViewModel.updateGoal(handoff.sessionGoal)
                    sessionViewModel.updateBlockObjective(handoff.nextObjective)
                }
                onNavigateToSessions()
            }
            CoachAction.START_FOCUS_SESSION -> onNavigateToSessionSetup()
            CoachAction.VIEW_TRACKER -> onNavigateToTracker()
            CoachAction.NONE -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // TOP HEADER: DEV COACH
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 12.dp)
        ) {
            Text(
                text = "DEV COACH",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = SageGreen
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Your developer wellness & progress companion.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = TextSecondaryDark
            )
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = BorderSubtle.copy(alpha = 0.5f)
        )

        // CHAT CONVERSATION AREA
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                ChatMessageItem(
                    message = message,
                    onActionClick = { handleAction(message.action) }
                )
            }

            if (isLoading) {
                item(key = "loading_typing_indicator") {
                    CoachTypingIndicator()
                }
            }
        }

        // INITIAL STARTER PROMPTS (only show when conversation is at the start)
        if (messages.size <= 1) {
            val starterPrompts = listOf(
                "Should I take a break?",
                "What should I work on next?",
                "Am I overworking?",
                "How is my progress?"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                starterPrompts.forEach { prompt ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(SurfaceDark)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                            .clickable {
                                coachViewModel.selectPromptSuggestion(prompt, contextSummary)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = prompt,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimaryDark
                        )
                    }
                }
            }
        }

        // BOTTOM INPUT BAR
        HorizontalDivider(
            thickness = 1.dp,
            color = BorderSubtle.copy(alpha = 0.4f)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BackgroundDark)
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { coachViewModel.updateInputText(it) },
                    placeholder = {
                        Text(
                            text = "Ask your coach...",
                            fontSize = 14.sp,
                            color = TextMuted
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = SageGreen,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank() && !isLoading) {
                                val text = inputText
                                coachViewModel.sendMessage(text, contextSummary)
                                keyboardController?.hide()
                            }
                        }
                    )
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank() && !isLoading) SageGreen else SurfaceDark)
                        .clickable(enabled = inputText.isNotBlank() && !isLoading) {
                            val text = inputText
                            coachViewModel.sendMessage(text, contextSummary)
                            keyboardController?.hide()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank() && !isLoading) BackgroundDark else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}


@Composable
private fun ChatMessageItem(
    message: CoachChatMessage,
    onActionClick: () -> Unit
) {
    if (message.sender == MessageSender.USER) {
        // User message bubble (right-aligned, contrasting surface)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 4.dp,
                            bottomEnd = 16.dp,
                            bottomStart = 16.dp
                        )
                    )
                    .background(SurfaceDark)
                    .border(
                        1.dp,
                        SageGreen.copy(alpha = 0.35f),
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 4.dp,
                            bottomEnd = 16.dp,
                            bottomStart = 16.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    color = TextPrimaryDark,
                    lineHeight = 20.sp
                )
            }
        }
    } else {
        // Coach message bubble (left-aligned, dark elevated surface with subtle green accent)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(
                        RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 16.dp,
                            bottomEnd = 16.dp,
                            bottomStart = 16.dp
                        )
                    )
                    .background(SurfaceDark)
                    .border(
                        1.dp,
                        BorderSubtle,
                        RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 16.dp,
                            bottomEnd = 16.dp,
                            bottomStart = 16.dp
                        )
                    )
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Small header badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(SageGreen)
                        )
                        Text(
                            text = if (message.isFromAi) "✦ Gemini" else "✦ DevPulse Coach",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = SageGreen
                        )
                    }


                    Text(
                        text = message.text,
                        fontSize = 14.sp,
                        color = TextPrimaryDark,
                        lineHeight = 21.sp
                    )

                    // Optional Contextual Action Button
                    if (message.action != CoachAction.NONE && message.actionLabel.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Button(
                            onClick = onActionClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SageGreen,
                                contentColor = BackgroundDark
                            )
                        ) {
                            Text(
                                text = message.actionLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoachTypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 16.dp,
                        bottomEnd = 16.dp,
                        bottomStart = 16.dp
                    )
                )
                .background(SurfaceDark)
                .border(1.dp, BorderSubtle, RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(SageGreen)
                )
                Text(
                    text = "Coach is thinking...",
                    fontSize = 12.sp,
                    color = TextSecondaryDark,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
