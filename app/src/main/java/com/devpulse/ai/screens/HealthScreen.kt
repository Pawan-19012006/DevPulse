package com.devpulse.ai.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devpulse.ai.domain.session.HealthEventType
import com.devpulse.ai.domain.session.RecoveryActivity
import com.devpulse.ai.domain.session.RecoveryCatalog
import com.devpulse.ai.domain.session.RecoveryCategory
import com.devpulse.ai.domain.session.RecoveryExperienceType
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
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun HealthScreen(
    homeViewModel: HomeViewModel
) {
    var selectedCategory by remember { mutableStateOf(RecoveryCategory.EYES) }
    var activeActivity by remember { mutableStateOf<RecoveryActivity?>(null) }

    val todayHealthEvents by homeViewModel.todayHealthEvents.collectAsState()
    val hydrationCount by homeViewModel.todayHydrationCount.collectAsState()
    val screenRecoveryCount by homeViewModel.todayScreenRecoveryCount.collectAsState()
    val movementCount by homeViewModel.todayMovementCount.collectAsState()
    val breathingCount by homeViewModel.todayBreathingCount.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        AnimatedContent(
            targetState = activeActivity,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
            label = "RecoveryCenterContent"
        ) { currentActivity ->
            if (currentActivity != null) {
                // Interactive Guided Mini-Session in-place
                GuidedMiniSessionScreen(
                    activity = currentActivity,
                    onFinish = {
                        homeViewModel.recordHealthAction(
                            type = currentActivity.healthEventType.name,
                            source = "RECOVERY_CENTER"
                        )
                        activeActivity = null
                    },
                    onExit = {
                        activeActivity = null
                    }
                )
            } else {
                // Primary Recovery Center View
                RecoveryCenterOverview(
                    selectedCategory = selectedCategory,
                    onSelectCategory = { selectedCategory = it },
                    onStartActivity = { activeActivity = it },
                    totalSessions = todayHealthEvents.size,
                    hydrationCount = hydrationCount,
                    screenRecoveryCount = screenRecoveryCount,
                    movementCount = movementCount,
                    breathingCount = breathingCount
                )
            }
        }
    }
}

@Composable
private fun RecoveryCenterOverview(
    selectedCategory: RecoveryCategory,
    onSelectCategory: (RecoveryCategory) -> Unit,
    onStartActivity: (RecoveryActivity) -> Unit,
    totalSessions: Int,
    hydrationCount: Int,
    screenRecoveryCount: Int,
    movementCount: Int,
    breathingCount: Int
) {
    val activities = remember(selectedCategory) {
        RecoveryCatalog.getActivitiesForCategory(selectedCategory)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp),
        contentPadding = PaddingValues(top = 28.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // Top Header
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🌿",
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recovery Center",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        letterSpacing = (-0.5).sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Reset your body and mind between development sessions.",
                    fontSize = 13.sp,
                    color = TextSecondaryDark,
                    lineHeight = 18.sp
                )
            }
        }

        // What do you need right now?
        item {
            Column {
                Text(
                    text = "WHAT DO YOU NEED RIGHT NOW?",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = TextSecondaryDark
                )
                Spacer(modifier = Modifier.height(12.dp))

                // 2x2 Category Selector
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CategorySelectionCard(
                            category = RecoveryCategory.EYES,
                            isSelected = selectedCategory == RecoveryCategory.EYES,
                            onClick = { onSelectCategory(RecoveryCategory.EYES) },
                            modifier = Modifier.weight(1f)
                        )
                        CategorySelectionCard(
                            category = RecoveryCategory.FOCUS,
                            isSelected = selectedCategory == RecoveryCategory.FOCUS,
                            onClick = { onSelectCategory(RecoveryCategory.FOCUS) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CategorySelectionCard(
                            category = RecoveryCategory.BODY,
                            isSelected = selectedCategory == RecoveryCategory.BODY,
                            onClick = { onSelectCategory(RecoveryCategory.BODY) },
                            modifier = Modifier.weight(1f)
                        )
                        CategorySelectionCard(
                            category = RecoveryCategory.STRESS,
                            isSelected = selectedCategory == RecoveryCategory.STRESS,
                            onClick = { onSelectCategory(RecoveryCategory.STRESS) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Section Title & Subtitle for Selected Category
        item {
            Column {
                Text(
                    text = selectedCategory.sectionTitle,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = selectedCategory.subtitle,
                    fontSize = 13.sp,
                    color = TextSecondaryDark
                )
            }
        }

        // Dynamic Activity Cards
        items(activities, key = { it.id }) { activity ->
            RecoveryActivityCard(
                activity = activity,
                onStart = { onStartActivity(activity) }
            )
        }

        // Subtle Divider & Today's Recovery Secondary Section
        item {
            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(
                color = BorderSubtle,
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Column {
                Text(
                    text = "TODAY'S RECOVERY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextSecondaryDark
                )
                Spacer(modifier = Modifier.height(8.dp))

                val totalMinutes = maxOf(totalSessions * 2, if (totalSessions > 0) 1 else 0)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$totalSessions sessions · $totalMinutes min",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (totalSessions == 0) "Take a quick reset anytime to recharge" else "Consistent resets protect long-term endurance",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (screenRecoveryCount > 0) RecoveryMiniChip("👀 $screenRecoveryCount")
                        if (breathingCount > 0) RecoveryMiniChip("🌬️ $breathingCount")
                        if (movementCount > 0) RecoveryMiniChip("🧍 $movementCount")
                        if (hydrationCount > 0) RecoveryMiniChip("💧 $hydrationCount")
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySelectionCard(
    category: RecoveryCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) SageGreen else BorderDark
    val backgroundColor = if (isSelected) SurfaceVariantDark else SurfaceDark

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = category.icon,
                fontSize = 18.sp
            )
            Text(
                text = category.displayName,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) TextPrimaryDark else TextSecondaryDark
            )
        }
    }
}

@Composable
private fun RecoveryActivityCard(
    activity: RecoveryActivity,
    onStart: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = activity.icon,
                        fontSize = 20.sp
                    )
                    Text(
                        text = activity.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark
                    )
                }

                // Duration chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceVariantDark)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = activity.durationLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondaryDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = activity.description,
                fontSize = 13.sp,
                color = TextSecondaryDark
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SageGreen,
                    contentColor = BackgroundDark
                )
            ) {
                Text(
                    text = "Start",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RecoveryMiniChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceVariantDark)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondaryDark
        )
    }
}

/**
 * Interactive Guided Mini-Session view rendered directly within the Health screen.
 */
@Composable
private fun GuidedMiniSessionScreen(
    activity: RecoveryActivity,
    onFinish: () -> Unit,
    onExit: () -> Unit
) {
    val totalSeconds = remember(activity) { activity.durationMinutes * 60 }
    var remainingSeconds by remember(activity) { mutableIntStateOf(totalSeconds) }
    var isPaused by remember { mutableStateOf(false) }
    var isCompleted by remember { mutableStateOf(false) }
    var currentStepIndex by remember(activity) { mutableIntStateOf(0) }

    // Single task input state
    var singleTaskText by remember { mutableStateOf("") }
    var isTaskLocked by remember { mutableStateOf(false) }

    // Master timer
    LaunchedEffect(isPaused, isCompleted, remainingSeconds) {
        if (!isPaused && !isCompleted && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
            if (remainingSeconds <= 0) {
                isCompleted = true
            }
        }
    }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onExit) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Exit",
                    tint = TextSecondaryDark
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = activity.icon, fontSize = 16.sp)
                    Text(
                        text = activity.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }
                Text(
                    text = activity.category.sectionTitle,
                    fontSize = 11.sp,
                    color = TextSecondaryDark
                )
            }

            // Balanced spacer
            Box(modifier = Modifier.size(48.dp))
        }

        if (isCompleted) {
            // Completion State
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(SageGreen.copy(alpha = 0.15f))
                        .border(2.dp, SageGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Complete",
                        tint = SageGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "✓ Recovery Complete",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Nice reset. You're ready to continue.",
                    fontSize = 14.sp,
                    color = TextSecondaryDark,
                    textAlign = TextAlign.Center
                )

                if (isTaskLocked && singleTaskText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceDark)
                            .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Your next focus: $singleTaskText",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SageGreen,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Done action button
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SageGreen,
                    contentColor = BackgroundDark
                )
            ) {
                Text(
                    text = "Done",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            // Active Guided Recovery Experience
            when (activity.type) {
                RecoveryExperienceType.BOX_BREATHING -> {
                    BoxBreathingGuidedContent(
                        timeFormatted = timeFormatted,
                        isPaused = isPaused
                    )
                }
                RecoveryExperienceType.SINGLE_TASK_INPUT -> {
                    SingleTaskGuidedContent(
                        timeFormatted = timeFormatted,
                        taskText = singleTaskText,
                        onTaskChange = { singleTaskText = it },
                        isTaskLocked = isTaskLocked,
                        onLockTask = { isTaskLocked = true }
                    )
                }
                RecoveryExperienceType.EYE_FOCUS_POINT -> {
                    val currentStep = activity.steps.getOrNull(currentStepIndex)
                    EyeFocusGuidedContent(
                        timeFormatted = timeFormatted,
                        currentStep = currentStep,
                        stepIndex = currentStepIndex,
                        totalSteps = activity.steps.size
                    )
                }
                RecoveryExperienceType.STEP_SEQUENCE -> {
                    val currentStep = activity.steps.getOrNull(currentStepIndex)
                    StepSequenceGuidedContent(
                        timeFormatted = timeFormatted,
                        currentStep = currentStep,
                        stepIndex = currentStepIndex,
                        totalSteps = activity.steps.size
                    )
                }
            }

            // Controls Footer
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Next / Complete buttons
                if (activity.steps.isNotEmpty()) {
                    if (currentStepIndex < activity.steps.size - 1) {
                        Button(
                            onClick = { currentStepIndex++ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SageGreen,
                                contentColor = BackgroundDark
                            )
                        ) {
                            Text(
                                text = "Next Step",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = { isCompleted = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SageGreen,
                                contentColor = BackgroundDark
                            )
                        ) {
                            Text(
                                text = "Complete Reset",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { isCompleted = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SageGreen,
                            contentColor = BackgroundDark
                        )
                    ) {
                        Text(
                            text = "Finish Early",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Pause & Previous controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (activity.steps.isNotEmpty() && currentStepIndex > 0) {
                        OutlinedButton(
                            onClick = { currentStepIndex-- },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Previous",
                                fontSize = 12.sp,
                                color = TextSecondaryDark
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { isPaused = !isPaused },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isPaused) "Resume" else "Pause",
                            fontSize = 12.sp,
                            color = if (isPaused) MutedAmber else TextPrimaryDark
                        )
                    }
                }
            }
        }
    }
}

/**
 * Guided Box Breathing view with expanding / contracting animated circle.
 */
@Composable
private fun BoxBreathingGuidedContent(
    timeFormatted: String,
    isPaused: Boolean
) {
    var secondsInCycle by remember { mutableIntStateOf(0) }

    LaunchedEffect(isPaused) {
        while (!isPaused) {
            delay(1000L)
            secondsInCycle = (secondsInCycle + 1) % 16
        }
    }

    // 4s Inhale -> 4s Hold -> 4s Exhale -> 4s Hold
    val (phaseTitle, phaseRemaining, targetScale) = when (secondsInCycle) {
        in 0..3 -> Triple("INHALE", 4 - secondsInCycle, 0.72f + (secondsInCycle + 1) * 0.12f)
        in 4..7 -> Triple("HOLD", 8 - secondsInCycle, 1.20f)
        in 8..11 -> Triple("EXHALE", 12 - secondsInCycle, 1.20f - (secondsInCycle - 7) * 0.12f)
        else -> Triple("HOLD", 16 - secondsInCycle, 0.72f)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = timeFormatted,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryDark,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = phaseTitle,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = SageGreen,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        Box(
            modifier = Modifier
                .size((160 * targetScale).dp)
                .clip(CircleShape)
                .background(SageGreen.copy(alpha = 0.18f))
                .border(2.dp, SageGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$phaseRemaining",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Breathe in through nose (4s) · Hold (4s) · Out through mouth (4s) · Hold (4s)",
            fontSize = 12.sp,
            color = TextSecondaryDark,
            textAlign = TextAlign.Center,
            lineHeight = 17.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}

/**
 * Single-Task reset prompt view.
 */
@Composable
private fun SingleTaskGuidedContent(
    timeFormatted: String,
    taskText: String,
    onTaskChange: (String) -> Unit,
    isTaskLocked: Boolean,
    onLockTask: () -> Unit
) {
    val quickSuggestions = listOf(
        "Fix 401 token issue",
        "Write unit tests",
        "Review pull request",
        "Refactor data layer"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = timeFormatted,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryDark,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        if (!isTaskLocked) {
            Text(
                text = "What is the ONE thing you want to accomplish next?",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimaryDark,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = taskText,
                onValueChange = onTaskChange,
                placeholder = { Text("e.g. Implement token refresh flow", color = TextMuted, fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SageGreen,
                    unfocusedBorderColor = BorderDark,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickSuggestions) { suggestion ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceDark)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                            .clickable { onTaskChange(suggestion) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = suggestion,
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onLockTask,
                enabled = taskText.isNotBlank(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MutedLavender,
                    contentColor = BackgroundDark
                )
            ) {
                Text("Lock In Focus", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceDark)
                    .border(1.dp, SageGreen, RoundedCornerShape(14.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "YOUR NEXT FOCUS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = SageGreen
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "“$taskText”",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Hold this single thought in mind. Let all other secondary tasks wait.",
                        fontSize = 12.sp,
                        color = TextSecondaryDark,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

/**
 * Eye Focus Reset view with moving focus dot tracking.
 */
@Composable
private fun EyeFocusGuidedContent(
    timeFormatted: String,
    currentStep: com.devpulse.ai.domain.session.RecoveryStep?,
    stepIndex: Int,
    totalSteps: Int
) {
    val isMovingDotStep = stepIndex == 1 // Step 2: "Follow moving focus point"

    val infiniteTransition = rememberInfiniteTransition(label = "eyeDotTransition")
    val dotOffsetX by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotOffset"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = timeFormatted,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryDark,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Step Dots Indicator
        StepDotsIndicator(currentIndex = stepIndex, totalSteps = totalSteps)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Step ${stepIndex + 1} of $totalSteps",
            fontSize = 11.sp,
            color = TextSecondaryDark
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (isMovingDotStep) {
            // Visual field with moving focus dot
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(dotOffsetX.dp.roundToPx(), 0) }
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(SageGreen)
                        .border(3.dp, TextPrimaryDark, CircleShape)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = currentStep?.title ?: "Eye Recovery",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = SageGreen
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentStep?.instruction ?: "",
                    fontSize = 13.sp,
                    color = TextPrimaryDark,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
            }
        }
    }
}

/**
 * Standard Step Sequence view for Posture, Movement, Shoulder & Neck, etc.
 */
@Composable
private fun StepSequenceGuidedContent(
    timeFormatted: String,
    currentStep: com.devpulse.ai.domain.session.RecoveryStep?,
    stepIndex: Int,
    totalSteps: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = timeFormatted,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryDark,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        StepDotsIndicator(currentIndex = stepIndex, totalSteps = totalSteps)

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Step ${stepIndex + 1} of $totalSteps",
            fontSize = 11.sp,
            color = TextSecondaryDark
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
                .padding(22.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = currentStep?.title ?: "Recovery Step",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SageGreen,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = currentStep?.instruction ?: "",
                    fontSize = 14.sp,
                    color = TextPrimaryDark,
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp
                )
            }
        }
    }
}

@Composable
private fun StepDotsIndicator(
    currentIndex: Int,
    totalSteps: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalSteps) {
            Box(
                modifier = Modifier
                    .size(if (i == currentIndex) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (i <= currentIndex) SageGreen else TextMuted.copy(alpha = 0.4f))
            )
        }
    }
}
