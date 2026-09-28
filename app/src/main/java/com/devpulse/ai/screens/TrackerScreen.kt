package com.devpulse.ai.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devpulse.ai.data.local.entity.OnePercentImprovementEntity
import com.devpulse.ai.domain.constellation.ConstellationLayout
import com.devpulse.ai.domain.constellation.ConstellationStar
import com.devpulse.ai.domain.constellation.MockConstellation
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
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.hypot

@Composable
fun TrackerScreen(
    homeViewModel: HomeViewModel,
    sessionViewModel: SessionViewModel,
    onNavigateToGitHubContext: (String?) -> Unit
) {
    val improvements by sessionViewModel.allImprovements.collectAsState()
    val connectedGitHubUser by homeViewModel.connectedGitHubUser.collectAsState()
    val todaySessions by homeViewModel.todaySessions.collectAsState()
    val todayHealthEvents by homeViewModel.todayHealthEvents.collectAsState()

    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("devpulse_constellation_prefs", Context.MODE_PRIVATE)
    }

    var goalTitle by remember {
        mutableStateOf(prefs.getString("goal_title", "Become a Backend Developer") ?: "Become a Backend Developer")
    }
    var targetDays by remember {
        mutableIntStateOf(prefs.getInt("target_days", 60))
    }
    var showGoalDialog by remember { mutableStateOf(false) }

    var selectedStar by remember { mutableStateOf<ConstellationStar?>(null) }
    var showSuccessBanner by remember { mutableStateOf(false) }

    var selectedCategoryFilter by remember { mutableStateOf<ImprovementCategory?>(null) }
    var quickReflectionText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ImprovementCategory.BUILDING) }

    // Map Room improvements + mock history into deterministic constellation stars
    val constellationStars = remember(improvements) {
        val list = mutableListOf<ConstellationStar>()
        // 1. Mock baseline stars
        list.addAll(MockConstellation.stars)

        // 2. Real improvements mapped sequentially
        val sortedReal = improvements.sortedBy { it.timestamp }
        sortedReal.forEachIndexed { index, real ->
            val starIndex = MockConstellation.stars.size + index
            list.add(
                ConstellationStar(
                    id = real.id,
                    title = real.reflectionText,
                    category = runCatching {
                        ImprovementCategory.valueOf(real.category).displayName
                    }.getOrDefault(real.category),
                    timestamp = real.timestamp,
                    position = ConstellationLayout.getStarPosition(starIndex),
                    isMock = false,
                    isLatest = (index == sortedReal.size - 1)
                )
            )
        }

        // If no real improvements exist yet, the last mock star is highlighted as latest
        if (sortedReal.isEmpty() && list.isNotEmpty()) {
            val last = list.last()
            list[list.lastIndex] = last.copy(isLatest = true)
        }

        list
    }

    // New star appearance animation
    val newStarAnim = remember { Animatable(1f) }
    val prevImprovementCount = remember { mutableIntStateOf(-1) }

    LaunchedEffect(improvements.size) {
        if (prevImprovementCount.intValue != -1 && improvements.size > prevImprovementCount.intValue) {
            newStarAnim.snapTo(0f)
            newStarAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
            )
            showSuccessBanner = true
        }
        prevImprovementCount.intValue = improvements.size
    }

    LaunchedEffect(showSuccessBanner) {
        if (showSuccessBanner) {
            delay(3500L)
            showSuccessBanner = false
        }
    }

    val meaningfulDaysCount = constellationStars.size
    val progressRatio = (meaningfulDaysCount.toFloat() / targetDays.toFloat()).coerceIn(0f, 1f)

    val filteredImprovements = if (selectedCategoryFilter == null) {
        improvements
    } else {
        improvements.filter { it.category == selectedCategoryFilter?.name }
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
            contentPadding = PaddingValues(top = 28.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
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
                        text = "Your development journey",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Every meaningful improvement adds another star.",
                        fontSize = 14.sp,
                        color = TextSecondaryDark
                    )
                }
            }

            // 1. Long-Term Goal Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
                        .clickable { showGoalDialog = true }
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DEV JOURNEY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = TextSecondaryDark
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Edit Goal",
                                    fontSize = 11.sp,
                                    color = SageGreen,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Goal",
                                    tint = SageGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "“$goalTitle”",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$meaningfulDaysCount / $targetDays meaningful days",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SageGreen
                            )
                            Text(
                                text = "${(progressRatio * 100).toInt()}%",
                                fontSize = 12.sp,
                                color = TextSecondaryDark
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { progressRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = SageGreen,
                            trackColor = SurfaceVariantDark
                        )
                    }
                }
            }

            // 2. HERO FEATURE: Developer Constellation Canvas
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ConstellationSkyView(
                        stars = constellationStars,
                        animProgress = newStarAnim.value,
                        onStarClick = { star -> selectedStar = star }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "✦ Tap any star to view what you built or learned ✦",
                        fontSize = 11.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Success feedback banner
            item {
                AnimatedVisibility(
                    visible = showSuccessBanner,
                    enter = fadeIn(tween(250)),
                    exit = fadeOut(tween(250))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SageGreen.copy(alpha = 0.15f))
                            .border(1.dp, SageGreen, RoundedCornerShape(10.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = SageGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "★ Added to your constellation",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SageGreen
                            )
                        }
                    }
                }
            }

            // 3. Direct 1% Better Input Card
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
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SageGreen,
                                contentColor = BackgroundDark,
                                disabledContainerColor = SurfaceVariantDark,
                                disabledContentColor = TextMuted
                            )
                        ) {
                            Text(
                                text = "Add to Constellation",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Divider
            item {
                HorizontalDivider(
                    color = BorderSubtle,
                    thickness = 1.dp
                )
            }

            // 4. Recent Progress Timeline
            item {
                Column {
                    Text(
                        text = "RECENT PROGRESS",
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
                            text = "No recorded improvements in this category yet.\nEvery session adds to your journey.",
                            fontSize = 13.sp,
                            color = TextMuted,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(filteredImprovements, key = { it.id }) { item ->
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

            // 5. GitHub Developer Context (Telemetry)
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
        }

        // Star Details Dialog
        if (selectedStar != null) {
            StarDetailDialog(
                star = selectedStar!!,
                goalTitle = goalTitle,
                onDismiss = { selectedStar = null }
            )
        }

        // Long-term Goal Edit Dialog
        if (showGoalDialog) {
            GoalEditDialog(
                initialGoal = goalTitle,
                initialDays = targetDays,
                onSave = { newGoal, newDays ->
                    goalTitle = newGoal
                    targetDays = newDays
                    prefs.edit()
                        .putString("goal_title", newGoal)
                        .putInt("target_days", newDays)
                        .apply()
                    showGoalDialog = false
                },
                onDismiss = { showGoalDialog = false }
            )
        }
    }
}

/**
 * Celestial Night-Sky Canvas rendering the Developer Constellation.
 */
@Composable
private fun ConstellationSkyView(
    stars: List<ConstellationStar>,
    animProgress: Float,
    onStarClick: (ConstellationStar) -> Unit
) {
    // Deterministic background stardust dots
    val stardust = remember {
        List(40) { index ->
            val x = ((index * 37 + 13) % 100) / 100f
            val y = ((index * 61 + 29) % 100) / 100f
            val radius = 0.8f + (index % 3) * 0.4f
            val alpha = 0.12f + (index % 5) * 0.05f
            Triple(Offset(x, y), radius, alpha)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(stars) {
                    detectTapGestures { tapOffset ->
                        val w = size.width
                        val h = size.height
                        val thresholdPx = 36.dp.toPx()

                        var closest: ConstellationStar? = null
                        var minDistance = Float.MAX_VALUE

                        for (star in stars) {
                            val starX = star.position.x * w
                            val starY = star.position.y * h
                            val dist = hypot(starX - tapOffset.x, starY - tapOffset.y)
                            if (dist < thresholdPx && dist < minDistance) {
                                minDistance = dist
                                closest = star
                            }
                        }
                        if (closest != null) {
                            onStarClick(closest)
                        }
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // 1. Subtle radial space glow in the center
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SageGreen.copy(alpha = 0.09f),
                        Color.Transparent
                    ),
                    center = Offset(canvasWidth * 0.5f, canvasHeight * 0.45f),
                    radius = canvasWidth * 0.55f
                )
            )

            // 2. Background stardust
            for ((stardustPos, r, a) in stardust) {
                drawCircle(
                    color = TextPrimaryDark.copy(alpha = a),
                    radius = r.dp.toPx(),
                    center = Offset(stardustPos.x * canvasWidth, stardustPos.y * canvasHeight)
                )
            }

            // 3. Connecting Constellation Lines
            val edges = ConstellationLayout.getEdgesForStarCount(stars.size)
            for (edge in edges) {
                if (edge.first < stars.size && edge.second < stars.size) {
                    val p1 = stars[edge.first].position
                    val p2 = stars[edge.second].position
                    val startOffset = Offset(p1.x * canvasWidth, p1.y * canvasHeight)
                    val targetOffset = Offset(p2.x * canvasWidth, p2.y * canvasHeight)

                    // If edge connects to the newest star, animate its line length
                    val endOffset = if (edge.second == stars.size - 1 && animProgress < 1f) {
                        Offset(
                            x = startOffset.x + (targetOffset.x - startOffset.x) * animProgress.coerceIn(0f, 1f),
                            y = startOffset.y + (targetOffset.y - startOffset.y) * animProgress.coerceIn(0f, 1f)
                        )
                    } else {
                        targetOffset
                    }

                    // Soft ambient line glow
                    drawLine(
                        color = SageGreen.copy(alpha = 0.12f),
                        start = startOffset,
                        end = endOffset,
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Crisp constellation line
                    drawLine(
                        color = SageGreen.copy(alpha = 0.38f),
                        start = startOffset,
                        end = endOffset,
                        strokeWidth = 1.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // 4. Star Nodes
            stars.forEachIndexed { index, star ->
                val center = Offset(star.position.x * canvasWidth, star.position.y * canvasHeight)
                val isNewest = (index == stars.size - 1)

                val scale = if (isNewest && animProgress < 1f) {
                    // Smooth bounce / scale-in
                    (animProgress * 1.25f).coerceIn(0f, 1.25f)
                } else 1f

                val baseRadius = when {
                    star.isLatest -> 6.dp.toPx()
                    star.isMock && index % 3 == 0 -> 4.8.dp.toPx()
                    else -> 4.dp.toPx()
                } * scale

                // Outer Halo
                val haloRadius = if (star.isLatest) 16.dp.toPx() * scale else 9.dp.toPx() * scale
                val haloAlpha = if (star.isLatest) 0.30f else 0.14f

                drawCircle(
                    color = SageGreen.copy(alpha = haloAlpha),
                    radius = haloRadius,
                    center = center
                )

                // Mid glow ring
                if (star.isLatest) {
                    drawCircle(
                        color = SageGreen.copy(alpha = 0.5f),
                        radius = 10.dp.toPx() * scale,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Core Star
                drawCircle(
                    color = if (star.isLatest) TextPrimaryDark else SageGreen.copy(alpha = 0.9f),
                    radius = baseRadius,
                    center = center
                )
            }
        }
    }
}

/**
 * Clean star detail card displayed when a star is tapped.
 */
@Composable
private fun StarDetailDialog(
    star: ConstellationStar,
    goalTitle: String,
    onDismiss: () -> Unit
) {
    val dateFormatted = rememberFormattedDate(star.timestamp)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "★", fontSize = 18.sp, color = SageGreen)
                    Text(
                        text = dateFormatted,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Category Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceVariantDark)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = star.category.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = SageGreen
                    )
                }

                Text(
                    text = "“${star.title}”",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "🌱", fontSize = 12.sp)
                    Text(
                        text = if (star.isMock) "Historical Progress · Journey to $goalTitle" else "1% Better Improvement · Journey to $goalTitle",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SageGreen,
                    contentColor = BackgroundDark
                )
            ) {
                Text("Done", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    )
}

/**
 * Goal editor modal allowing the developer to set their milestone journey.
 */
@Composable
private fun GoalEditDialog(
    initialGoal: String,
    initialDays: Int,
    onSave: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var goalInput by remember { mutableStateOf(initialGoal) }
    var selectedDays by remember { mutableIntStateOf(initialDays) }

    val quickGoals = listOf(
        "Become a Backend Developer",
        "Build my first production app",
        "Master DSA",
        "Learn Machine Learning",
        "Ship my startup MVP"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "What's your long-term goal?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = goalInput,
                    onValueChange = { goalInput = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark,
                        focusedBorderColor = SageGreen,
                        unfocusedBorderColor = BorderDark,
                        cursorColor = SageGreen
                    )
                )

                // Quick Goal Suggestions
                Text(
                    text = "QUICK GOALS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextSecondaryDark
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    quickGoals.forEach { goal ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (goalInput == goal) SurfaceVariantDark else Color.Transparent)
                                .border(1.dp, if (goalInput == goal) SageGreen else BorderSubtle, RoundedCornerShape(8.dp))
                                .clickable { goalInput = goal }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = goal,
                                fontSize = 12.sp,
                                color = if (goalInput == goal) TextPrimaryDark else TextSecondaryDark
                            )
                        }
                    }
                }

                // Meaningful Days Target
                Text(
                    text = "HOW MANY MEANINGFUL DAYS?",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextSecondaryDark
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(30, 60, 90).forEach { days ->
                        val isSelected = selectedDays == days
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) SurfaceVariantDark else SurfaceDark)
                                .border(1.dp, if (isSelected) SageGreen else BorderDark, RoundedCornerShape(8.dp))
                                .clickable { selectedDays = days }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$days days",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) SageGreen else TextSecondaryDark
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (goalInput.isNotBlank()) {
                        onSave(goalInput.trim(), selectedDays)
                    }
                },
                enabled = goalInput.isNotBlank(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SageGreen,
                    contentColor = BackgroundDark
                )
            ) {
                Text("Start Journey", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondaryDark)
            }
        }
    )
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
