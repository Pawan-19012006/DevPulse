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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devpulse.ai.data.local.entity.OnePercentImprovementEntity
import com.devpulse.ai.domain.session.ImprovementCategory
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TrackerScreen(
    homeViewModel: HomeViewModel,
    sessionViewModel: SessionViewModel,
    onNavigateToGitHubContext: (String?) -> Unit
) {
    val improvements by sessionViewModel.allImprovements.collectAsState()
    val totalSessionsCount by homeViewModel.totalSessionsCount.collectAsState()
    val totalImprovementsCount by homeViewModel.totalImprovementsCount.collectAsState()
    val connectedGitHubUser by homeViewModel.connectedGitHubUser.collectAsState()
    val todaySessions by homeViewModel.todaySessions.collectAsState()
    val todayHealthEvents by homeViewModel.todayHealthEvents.collectAsState()

    var selectedCategoryFilter by remember { mutableStateOf<ImprovementCategory?>(null) }
    var quickReflectionText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ImprovementCategory.BUILDING) }

    val filteredImprovements = if (selectedCategoryFilter == null) {
        improvements
    } else {
        improvements.filter { it.category == selectedCategoryFilter?.name }
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
                    text = "DEV TRACKER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = SageGreen
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Who am I becoming?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Every session. One improvement.",
                    fontSize = 14.sp,
                    color = TextSecondaryDark
                )
            }
        }

        // 1. Long-term Journey Totals
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TrackerMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "SESSIONS",
                    value = "$totalSessionsCount"
                )
                TrackerMetricCard(
                    modifier = Modifier.weight(1f),
                    label = "1% BETTER",
                    value = "$totalImprovementsCount"
                )
            }
        }

        // 2. Record 1% Better Reflection Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🌱", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "WHAT MADE YOU 1% BETTER TODAY?",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = SageGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = quickReflectionText,
                        onValueChange = { quickReflectionText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "e.g. Understood coroutine cancellation hierarchy",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        },
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            focusedContainerColor = SurfaceVariantDark,
                            unfocusedContainerColor = SurfaceVariantDark,
                            focusedBorderColor = SageGreen,
                            unfocusedBorderColor = BorderSubtle,
                            cursorColor = SageGreen
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Selector
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ImprovementCategory.values()) { category ->
                            val isSelected = category == selectedCategory
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) SurfaceVariantDark else SurfaceDark)
                                    .border(
                                        1.dp,
                                        if (isSelected) SageGreen else BorderDark,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { selectedCategory = category }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = category.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) SageGreen else TextSecondaryDark
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (quickReflectionText.isNotBlank()) {
                                homeViewModel.recordQuickImprovement(selectedCategory, quickReflectionText)
                                quickReflectionText = ""
                            }
                        },
                        enabled = quickReflectionText.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SageGreen,
                            contentColor = BackgroundDark,
                            disabledContainerColor = SurfaceVariantDark,
                            disabledContentColor = TextMuted
                        )
                    ) {
                        Text(
                            text = "Record 1% Improvement",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // 3. GitHub Developer Context (Telemetry)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GITHUB CONTEXT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextSecondaryDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (connectedGitHubUser != null) "@$connectedGitHubUser connected" else "No account connected",
                            fontSize = 13.sp,
                            color = TextPrimaryDark
                        )
                    }

                    Button(
                        onClick = { onNavigateToGitHubContext(connectedGitHubUser) },
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceVariantDark,
                            contentColor = TextPrimaryDark
                        ),
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (connectedGitHubUser != null) "View Telemetry" else "Connect GitHub",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        item {
            HorizontalDivider(
                color = BorderSubtle,
                thickness = 1.dp
            )
        }

        // 4. Filter chips & 1% Better Timeline
        item {
            Column {
                Text(
                    text = "1% BETTER TIMELINE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextSecondaryDark
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        val isAllSelected = selectedCategoryFilter == null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isAllSelected) SurfaceVariantDark else SurfaceDark)
                                .border(
                                    1.dp,
                                    if (isAllSelected) MutedLavender else BorderDark,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedCategoryFilter = null }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "All (${improvements.size})",
                                fontSize = 11.sp,
                                color = if (isAllSelected) TextPrimaryDark else TextSecondaryDark
                            )
                        }
                    }

                    items(ImprovementCategory.values()) { category ->
                        val isSelected = selectedCategoryFilter == category
                        val count = improvements.count { it.category == category.name }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) SurfaceVariantDark else SurfaceDark)
                                .border(
                                    1.dp,
                                    if (isSelected) MutedLavender else BorderDark,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedCategoryFilter = category }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${category.displayName} ($count)",
                                fontSize = 11.sp,
                                color = if (isSelected) TextPrimaryDark else TextSecondaryDark
                            )
                        }
                    }
                }
            }
        }

        if (filteredImprovements.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceDark.copy(alpha = 0.5f))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No improvements logged yet for this category.\nComplete a session to record your first 1% step.",
                        fontSize = 13.sp,
                        color = TextMuted,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            items(filteredImprovements) { item ->
                val matchingSession = todaySessions.firstOrNull { it.id == item.sessionId }
                val sessionRecoveries = if (item.sessionId != null) {
                    todayHealthEvents.filter { it.sessionId == item.sessionId }
                } else emptyList()
                ImprovementHistoryRow(
                    item = item,
                    associatedSession = matchingSession,
                    recoveryEvents = sessionRecoveries
                )
            }
        }
    }
}

@Composable
private fun TrackerMetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextSecondaryDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        }
    }
}

@Composable
private fun ImprovementHistoryRow(
    item: OnePercentImprovementEntity,
    associatedSession: com.devpulse.ai.data.local.entity.DevSessionEntity? = null,
    recoveryEvents: List<com.devpulse.ai.data.local.entity.HealthEventEntity> = emptyList()
) {
    val categoryDisplayName = runCatching {
        ImprovementCategory.valueOf(item.category).displayName
    }.getOrDefault(item.category)

    val dateFormatted = rememberFormattedDate(item.timestamp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = categoryDisplayName.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = SageGreen
                )

                Text(
                    text = dateFormatted,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.reflectionText,
                fontSize = 14.sp,
                color = TextPrimaryDark,
                lineHeight = 20.sp
            )

            if (associatedSession != null || recoveryEvents.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (associatedSession != null) {
                        val duration = if (associatedSession.actualDurationMinutes > 0) "${associatedSession.actualDurationMinutes}m" else "${associatedSession.targetDurationMinutes}m"
                        Text(
                            text = "From session: $duration",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (recoveryEvents.isNotEmpty()) {
                        val recoveryIcons = recoveryEvents.map { ev ->
                            when (ev.type) {
                                "HYDRATION" -> "💧"
                                "EYE_RECOVERY" -> "👀"
                                "MOVEMENT" -> "🧍"
                                "BREATHING" -> "🌬️"
                                else -> "🌿"
                            }
                        }.distinct().joinToString(" ")
                        Text(
                            text = "Recovery: $recoveryIcons",
                            fontSize = 11.sp,
                            color = SageGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberFormattedDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d · h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
