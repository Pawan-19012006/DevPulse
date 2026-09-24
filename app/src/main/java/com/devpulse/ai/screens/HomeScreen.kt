package com.devpulse.ai.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devpulse.ai.data.local.entity.OnePercentImprovementEntity
import com.devpulse.ai.domain.session.DeveloperState
import com.devpulse.ai.domain.session.ImprovementCategory
import com.devpulse.ai.ui.theme.BackgroundDark
import com.devpulse.ai.ui.theme.BorderDark
import com.devpulse.ai.ui.theme.Primary
import com.devpulse.ai.ui.theme.Secondary
import com.devpulse.ai.ui.theme.SurfaceDark
import com.devpulse.ai.ui.theme.SurfaceVariantDark
import com.devpulse.ai.ui.theme.TextPrimaryDark
import com.devpulse.ai.ui.theme.TextSecondaryDark
import com.devpulse.ai.viewmodel.DailyJourneySummary
import com.devpulse.ai.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSessionSetup: () -> Unit,
    onNavigateToJourney: () -> Unit,
    onNavigateToGitHubContext: (String?) -> Unit
) {
    val currentState by viewModel.currentState.collectAsState()
    val currentIntention by viewModel.currentIntention.collectAsState()
    val totalSessionsCount by viewModel.totalSessionsCount.collectAsState()
    val totalImprovementsCount by viewModel.totalImprovementsCount.collectAsState()
    val recentImprovements by viewModel.recentImprovements.collectAsState()
    val dailySummary by viewModel.dailySummary.collectAsState()
    val connectedGitHubUser by viewModel.connectedGitHubUser.collectAsState()

    val greetingTitle = viewModel.getGreetingTitle()
    val greetingSubtitle = viewModel.getGreetingSubtitle()

    Scaffold(
        containerColor = BackgroundDark
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 26.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // 1. Emotional Greeting & Human Presence
            item {
                DeveloperGreetingSection(
                    title = greetingTitle,
                    subtitle = greetingSubtitle,
                    totalSessions = totalSessionsCount
                )
            }

            // 2. How Are You Feeling? (User-reported Developer State)
            item {
                EmotionalStateSection(
                    currentState = currentState,
                    onStateSelected = { viewModel.setDeveloperState(it) }
                )
            }

            // 3. What's On Your Mind? -> Begin Dev Session CTA
            item {
                BeginSessionCard(
                    intention = currentIntention,
                    onIntentionChange = { viewModel.setIntention(it) },
                    onStartSessionClick = onNavigateToSessionSetup
                )
            }

            // 4. Your Journey Today (Narrative chapters, NOT generic dashboard metrics)
            item {
                TodayJourneyNarrativeCard(
                    summary = dailySummary,
                    onViewFullJourney = onNavigateToJourney
                )
            }

            // 5. Your Last 1% Better (The central emotional reward)
            item {
                LatestOnePercentSection(
                    latestImprovement = recentImprovements.firstOrNull(),
                    onViewJourneyClick = onNavigateToJourney
                )
            }

            // 6. Hard Days & The Real Struggle (Validation for the developer)
            item {
                HardDaysValidationCard()
            }

            // 7. Developer Context (GitHub Activity in the background)
            item {
                DeveloperContextFooter(
                    connectedUser = connectedGitHubUser,
                    onOpenGitHub = { onNavigateToGitHubContext(connectedGitHubUser) }
                )
            }
        }
    }
}

@Composable
private fun DeveloperGreetingSection(
    title: String,
    subtitle: String,
    totalSessions: Int
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🌱",
                    fontSize = 22.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "DevPulse",
                    color = TextPrimaryDark,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceVariantDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "🌱 +1% Better",
                    color = Color(0xFF10B981),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = title,
            color = TextPrimaryDark,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            color = TextSecondaryDark,
            fontSize = 15.sp,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Lifetime Narrative Note
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(BorderDark, BorderDark)))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🌱", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (totalSessions == 0) {
                            "Your journey starts with session #1."
                        } else {
                            "$totalSessions sessions • $totalSessions times you chose to keep building."
                        },
                        color = TextPrimaryDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Every session. One small improvement.",
                        color = Color(0xFF10B981),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EmotionalStateSection(
    currentState: DeveloperState,
    onStateSelected: (DeveloperState) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "HOW ARE YOU FEELING RIGHT NOW?",
            color = TextSecondaryDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // The 5 primary feelings highlighted in product direction
        val feelingStates = listOf(
            DeveloperState.READY,
            DeveloperState.GOOD,
            DeveloperState.TIRED,
            DeveloperState.FRUSTRATED,
            DeveloperState.DRAINED
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(feelingStates) { state ->
                val isSelected = state == currentState
                val bg = if (isSelected) SurfaceVariantDark else SurfaceDark
                val borderColor = if (isSelected) Primary else BorderDark

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(bg)
                        .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                        .clickable { onStateSelected(state) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = state.emoji, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.displayName,
                        color = if (isSelected) TextPrimaryDark else TextSecondaryDark,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Dynamic Supportive Empathy Feedback
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark.copy(alpha = 0.6f))
                    .border(1.dp, BorderDark.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "💬", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = currentState.supportiveMessage,
                        color = TextSecondaryDark,
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BeginSessionCard(
    intention: String,
    onIntentionChange: (String) -> Unit,
    onStartSessionClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = Brush.horizontalGradient(listOf(Primary.copy(alpha = 0.8f), Secondary.copy(alpha = 0.8f)))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            Text(
                text = "WHAT'S ON YOUR MIND?",
                color = Secondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = intention,
                onValueChange = onIntentionChange,
                placeholder = {
                    Text(
                        text = "e.g. Finish authentication, or solve graph problem...",
                        color = TextSecondaryDark.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark,
                    focusedContainerColor = SurfaceVariantDark,
                    unfocusedContainerColor = SurfaceVariantDark,
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = BorderDark
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onStartSessionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = BackgroundDark
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Begin Dev Session",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter a focused chapter of your developer journey.",
                color = TextSecondaryDark,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun TodayJourneyNarrativeCard(
    summary: DailyJourneySummary,
    onViewFullJourney: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(BorderDark, BorderDark)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "YOUR JOURNEY TODAY",
                    color = TextSecondaryDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "History →",
                    color = Primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onViewFullJourney() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (summary.completedSessionsCount == 0) {
                Text(
                    text = "No sessions completed yet today.",
                    color = TextPrimaryDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "You don't need hours—one focused session is enough to be +1% better.",
                    color = TextSecondaryDark,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceVariantDark)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Column {
                            Text(
                                text = "💻 ${summary.totalFocusedMinutes}m focused",
                                color = TextPrimaryDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Across ${summary.completedSessionsCount} session(s)",
                                color = TextSecondaryDark,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceVariantDark)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Column {
                            Text(
                                text = "🌱 +${summary.completedSessionsCount} 1% Better",
                                color = Color(0xFF10B981),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "One gain per session",
                                color = TextSecondaryDark,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                if (summary.activityBreakdown.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Focus areas: ${summary.activityBreakdown}",
                        color = TextSecondaryDark,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LatestOnePercentSection(
    latestImprovement: OnePercentImprovementEntity?,
    onViewJourneyClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "YOUR LAST 1% BETTER",
                color = TextSecondaryDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "My Journey →",
                color = Primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onViewJourneyClick() }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (latestImprovement == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(BorderDark, BorderDark)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "\"Every session. One small improvement.\"",
                        color = TextPrimaryDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Every completed session gives you a dedicated moment to capture what you gained—a bug fixed, a concept understood, or learning to handle frustration.",
                        color = TextSecondaryDark,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        } else {
            val category = runCatching {
                ImprovementCategory.valueOf(latestImprovement.category)
            }.getOrDefault(ImprovementCategory.SKILL)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(BorderDark, BorderDark)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceVariantDark)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${category.icon} ${category.prefixBadge}",
                                color = Color(0xFF10B981),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                                .format(Date(latestImprovement.timestamp)),
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "\"${latestImprovement.reflectionText}\"",
                        color = TextPrimaryDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Another session. Another improvement.",
                        color = Color(0xFF10B981),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun HardDaysValidationCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.6f)),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = Brush.linearGradient(listOf(BorderDark.copy(alpha = 0.5f), BorderDark.copy(alpha = 0.5f)))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(text = "🌿", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "You're allowed to struggle.",
                    color = TextPrimaryDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "You don't have to solve every bug in one sitting. Learning where a bug wasn't is still +1% Better.",
                    color = TextSecondaryDark,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun DeveloperContextFooter(
    connectedUser: String?,
    onOpenGitHub: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenGitHub() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(BorderDark, BorderDark)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🐙", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (connectedUser != null) "Developer Context: @$connectedUser" else "Connect GitHub Context",
                        color = TextPrimaryDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (connectedUser != null) "Commits and repository telemetry synced in Room" else "Tap to sync repository telemetry",
                        color = TextSecondaryDark,
                        fontSize = 11.sp
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = TextSecondaryDark,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
