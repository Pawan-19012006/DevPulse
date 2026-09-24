package com.devpulse.ai.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    val todaySessionsCount by viewModel.todaySessionsCount.collectAsState()
    val todayImprovementsCount by viewModel.todayImprovementsCount.collectAsState()
    val totalSessionsCount by viewModel.totalSessionsCount.collectAsState()
    val totalImprovementsCount by viewModel.totalImprovementsCount.collectAsState()
    val recentImprovements by viewModel.recentImprovements.collectAsState()
    val connectedGitHubUser by viewModel.connectedGitHubUser.collectAsState()

    val greeting = viewModel.getDynamicGreeting()

    Scaffold(
        containerColor = BackgroundDark
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header / Brand & Greeting
            item {
                HomeHeader(
                    greeting = greeting,
                    connectedUser = connectedGitHubUser
                )
            }

            // Developer State Check-in
            item {
                DeveloperStateCheckIn(
                    currentState = currentState,
                    onStateSelected = { viewModel.setDeveloperState(it) }
                )
            }

            // Central Call-To-Action: Start Dev Session
            item {
                StartSessionHeroCard(
                    onStartSessionClick = onNavigateToSessionSetup
                )
            }

            // Daily Pulse & Lifetime Symbolic Progress
            item {
                DailyPulseProgressCard(
                    todaySessions = todaySessionsCount,
                    todayImprovements = todayImprovementsCount,
                    totalSessions = totalSessionsCount,
                    totalImprovements = totalImprovementsCount
                )
            }

            // Recent 1% Better highlight
            item {
                RecentImprovementSection(
                    recentImprovements = recentImprovements,
                    onViewJourneyClick = onNavigateToJourney
                )
            }

            // Developer Context / GitHub Integration link
            item {
                DeveloperContextBanner(
                    connectedUser = connectedGitHubUser,
                    onOpenGitHubContext = { onNavigateToGitHubContext(connectedGitHubUser) }
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(
    greeting: String,
    connectedUser: String?
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
                    fontSize = 24.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "DevPulse",
                    color = TextPrimaryDark,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceVariantDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "1% Better Daily",
                    color = Color(0xFF10B981),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = greeting,
            color = TextPrimaryDark,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Build. Recover. Reflect. Improve.",
            color = TextSecondaryDark,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun DeveloperStateCheckIn(
    currentState: DeveloperState,
    onStateSelected: (DeveloperState) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "CURRENT STATE",
            color = TextSecondaryDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 8.dp)
        ) {
            items(DeveloperState.values()) { state ->
                val isSelected = state == currentState
                val bg = if (isSelected) SurfaceVariantDark else SurfaceDark
                val borderColor = if (isSelected) Primary else BorderDark

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(bg)
                        .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                        .clickable { onStateSelected(state) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = state.emoji, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = state.displayName,
                        color = if (isSelected) TextPrimaryDark else TextSecondaryDark,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun StartSessionHeroCard(
    onStartSessionClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(Primary, Secondary)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NEW WORK SESSION",
                        color = Secondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Focus Deeply. Gain Your 1%.",
                        color = TextPrimaryDark,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariantDark),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "⚡", fontSize = 20.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Prepare your mindset, execute focused work without distraction, take an intentional recovery break, and capture one insight.",
                color = TextSecondaryDark,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onStartSessionClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = BackgroundDark
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start Dev Session",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun DailyPulseProgressCard(
    todaySessions: Int,
    todayImprovements: Int,
    totalSessions: Int,
    totalImprovements: Int
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
                    text = "DAILY & LIFETIME PROGRESS",
                    color = TextSecondaryDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Every session = +1%",
                    color = Color(0xFF10B981),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn(
                    label = "Today's Sessions",
                    value = todaySessions.toString(),
                    icon = "⏱️"
                )
                MetricColumn(
                    label = "Today's 1% Better",
                    value = "+$todayImprovements",
                    icon = "🌱"
                )
                MetricColumn(
                    label = "Lifetime Sessions",
                    value = totalSessions.toString(),
                    icon = "🏆"
                )
                MetricColumn(
                    label = "Total 1% Gains",
                    value = "+$totalImprovements",
                    icon = "⭐"
                )
            }
        }
    }
}

@Composable
private fun MetricColumn(
    label: String,
    value: String,
    icon: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = icon, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = TextPrimaryDark,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = TextSecondaryDark,
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun RecentImprovementSection(
    recentImprovements: List<OnePercentImprovementEntity>,
    onViewJourneyClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🌱 1% BETTER HIGHLIGHTS",
                color = TextSecondaryDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "View Journey →",
                color = Primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onViewJourneyClick() }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (recentImprovements.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(BorderDark, BorderDark)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🌱", fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No improvements recorded yet",
                        color = TextPrimaryDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Start your first session and reflect on one thing you learned or solved.",
                        color = TextSecondaryDark,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recentImprovements.take(3).forEach { improvement ->
                    val category = runCatching {
                        ImprovementCategory.valueOf(improvement.category)
                    }.getOrDefault(ImprovementCategory.SKILL)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(BorderDark, BorderDark)))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SurfaceVariantDark)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = category.prefixBadge,
                                    color = Color(0xFF10B981),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = improvement.reflectionText,
                                    color = TextPrimaryDark,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                                        .format(Date(improvement.timestamp)),
                                    color = TextSecondaryDark,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeveloperContextBanner(
    connectedUser: String?,
    onOpenGitHubContext: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenGitHubContext() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(BorderDark, BorderDark)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🐙", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (connectedUser != null) "Developer Context: @$connectedUser" else "Connect GitHub Context",
                        color = TextPrimaryDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (connectedUser != null) "Repository telemetry & commits synced in Room" else "Tap to sync commits & repository telemetry",
                        color = TextSecondaryDark,
                        fontSize = 11.sp
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = TextSecondaryDark,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
