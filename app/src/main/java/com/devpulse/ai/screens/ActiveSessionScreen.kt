package com.devpulse.ai.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devpulse.ai.BuildConfig
import com.devpulse.ai.domain.session.*
import com.devpulse.ai.ui.theme.*
import com.devpulse.ai.viewmodel.SessionViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionScreen(
    sessionId: String,
    viewModel: SessionViewModel,
    onNavigateHome: () -> Unit
) {
    val engineState by viewModel.engineState.collectAsState()
    val activeNudge by viewModel.activeNudge.collectAsState()
    var showEndEarlyDialog by remember { mutableStateOf(false) }
    var endEarlyAccomplished by remember { mutableStateOf("") }
    var endEarlyRemaining by remember { mutableStateOf("") }

    // Intercept back button to prompt instead of accidental cancel
    BackHandler {
        when (engineState) {
            is SessionEngineState.Working, is SessionEngineState.Paused, is SessionEngineState.Recovery -> {
                showEndEarlyDialog = true
            }
            is SessionEngineState.Finished -> {
                viewModel.resetSession()
                onNavigateHome()
            }
            else -> onNavigateHome()
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    val subtitle = when (val s = engineState) {
                        is SessionEngineState.Working -> "${s.session.activityType.displayName} • Block ${s.currentBlock.blockIndex + 1}/${s.session.totalBlocks}"
                        is SessionEngineState.Paused -> "Session Paused"
                        is SessionEngineState.Recovery -> "Dev Recovery • Block ${s.session.currentBlockIndex + 1}/${s.session.totalBlocks}"
                        is SessionEngineState.WorkBlockHandoff -> "Block Handoff"
                        is SessionEngineState.ReadyForNextBlock -> "Next Block"
                        is SessionEngineState.SessionCompleteReflection -> "Session Complete"
                        is SessionEngineState.Finished -> "🌱 +1% Better"
                        else -> "Dev Session"
                    }
                    Text(
                        text = subtitle,
                        color = TextPrimaryDark,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when (engineState) {
                            is SessionEngineState.Working, is SessionEngineState.Paused, is SessionEngineState.Recovery -> {
                                showEndEarlyDialog = true
                            }
                            is SessionEngineState.Finished -> {
                                viewModel.resetSession()
                                onNavigateHome()
                            }
                            else -> onNavigateHome()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit Session",
                            tint = TextSecondaryDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = engineState) {
                is SessionEngineState.Working -> {
                    RunningClockContent(
                        session = state.session,
                        block = state.currentBlock,
                        elapsedSeconds = state.elapsedSeconds,
                        remainingSeconds = state.remainingSeconds,
                        progressRatio = state.progressRatio,
                        isPaused = false,
                        activeNudge = activeNudge,
                        onCompleteNudge = { viewModel.completeHealthNudge(it) },
                        onRemindLaterNudge = { viewModel.dismissHealthNudge() },
                        onTriggerPrototypeNudge = { viewModel.triggerPrototypeNudge(it) },
                        onPause = { viewModel.pause() },
                        onResume = { viewModel.resume() },
                        onEndEarly = { showEndEarlyDialog = true },
                        onFastForward = { viewModel.fastForwardCurrentBlock() }
                    )
                }

                is SessionEngineState.Paused -> {
                    RunningClockContent(
                        session = state.session,
                        block = state.currentBlock,
                        elapsedSeconds = state.elapsedSeconds,
                        remainingSeconds = state.remainingSeconds,
                        progressRatio = state.progressRatio,
                        isPaused = true,
                        activeNudge = activeNudge,
                        onCompleteNudge = { viewModel.completeHealthNudge(it) },
                        onRemindLaterNudge = { viewModel.dismissHealthNudge() },
                        onTriggerPrototypeNudge = { viewModel.triggerPrototypeNudge(it) },
                        onPause = { viewModel.pause() },
                        onResume = { viewModel.resume() },
                        onEndEarly = { showEndEarlyDialog = true },
                        onFastForward = { viewModel.fastForwardCurrentBlock() }
                    )
                }

                is SessionEngineState.WorkBlockHandoff -> {
                    WorkBlockHandoffContent(
                        session = state.session,
                        completedBlock = state.completedBlock,
                        isFinalBlock = state.isFinalBlock,
                        onSubmitHandoff = { accomplished, remaining ->
                            viewModel.submitWorkBlockHandoff(accomplished, remaining)
                        }
                    )
                }

                is SessionEngineState.Recovery -> {
                    RecoveryClockContent(
                        session = state.session,
                        recoveryBlock = state.recoveryBlock,
                        nextObjective = state.nextObjective,
                        remainingSeconds = state.remainingSeconds,
                        progressRatio = state.progressRatio,
                        onEndRecoveryEarly = { viewModel.endRecoveryEarly() },
                        onLogRecoveryHealthEvent = { viewModel.recordGuidedRecoveryHealthEvent(it) },
                        onFastForward = { viewModel.fastForwardCurrentBlock() }
                    )
                }

                is SessionEngineState.ReadyForNextBlock -> {
                    ReadyForNextBlockContent(
                        session = state.session,
                        nextBlockIndex = state.nextBlockIndex,
                        carryoverObjective = state.carryoverObjective,
                        onContinue = { customObj ->
                            viewModel.continueNextBlock(customObj)
                        }
                    )
                }

                is SessionEngineState.SessionCompleteReflection -> {
                    SessionCompleteReflectionContent(
                        session = state.session,
                        lastHandoff = state.lastHandoff,
                        onSubmitReflection = { category, reflection ->
                            viewModel.submitFinalReflection(category, reflection)
                        }
                    )
                }

                is SessionEngineState.Finished -> {
                    SessionFinishedContent(
                        improvement = state.improvement,
                        onFinish = {
                            viewModel.resetSession()
                            onNavigateHome()
                        }
                    )
                }

                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
            }
        }
    }

    if (showEndEarlyDialog) {
        AlertDialog(
            onDismissRequest = { showEndEarlyDialog = false },
            containerColor = SurfaceDark,
            title = {
                Text(
                    text = "End Session Early?",
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "You're allowed to stop. Even stopping early preserves what you accomplished and what remains.",
                        color = TextSecondaryDark,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    OutlinedTextField(
                        value = endEarlyAccomplished,
                        onValueChange = { endEarlyAccomplished = it },
                        placeholder = { Text("What did you accomplish so far?", color = TextSecondaryDark, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = BorderDark
                        )
                    )
                    OutlinedTextField(
                        value = endEarlyRemaining,
                        onValueChange = { endEarlyRemaining = it },
                        placeholder = { Text("What remains to be done?", color = TextSecondaryDark, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = BorderDark
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEndEarlyDialog = false
                        viewModel.completeSessionEarly(
                            accomplished = endEarlyAccomplished.ifBlank { "Focused session ended early." },
                            nextObjective = endEarlyRemaining.ifBlank { "Pick up where left off." }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = BackgroundDark)
                ) {
                    Text("Save Handoff & End", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndEarlyDialog = false }) {
                    Text("Keep Focusing", color = TextSecondaryDark)
                }
            }
        )
    }
}

@Composable
private fun RunningClockContent(
    session: DevSession,
    block: SessionBlock,
    elapsedSeconds: Long,
    remainingSeconds: Long,
    progressRatio: Float,
    isPaused: Boolean,
    activeNudge: HealthNudge?,
    onCompleteNudge: (HealthEventType) -> Unit,
    onRemindLaterNudge: () -> Unit,
    onTriggerPrototypeNudge: (HealthEventType) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onEndEarly: () -> Unit,
    onFastForward: () -> Unit = {}
) {
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Activity & Goal Header
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = session.activityType.icon, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = session.activityType.displayName.uppercase(),
                        color = Secondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = session.overallGoal,
                    color = TextPrimaryDark,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Current Block Objective (Level 2)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(BorderDark, BorderDark)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "CURRENT BLOCK OBJECTIVE",
                        color = TextSecondaryDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = block.objective,
                        color = TextPrimaryDark,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Circular Real Clock
        item {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Track
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = SurfaceVariantDark,
                        style = Stroke(width = 12.dp.toPx())
                    )
                }

                // Progress Arc
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sweepAngle = (1.0f - progressRatio) * 360f
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Primary, Secondary, Primary)
                        ),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Time Display
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timeFormatted,
                        color = TextPrimaryDark,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isPaused) MutedAmber else SageGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPaused) "PAUSED" else "FOCUSING",
                            color = if (isPaused) MutedAmber else SageGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        // Session-Centered Health Nudge (Appears non-disruptively during work)
        if (activeNudge != null) {
            item {
                SessionHealthNudgeCard(
                    nudge = activeNudge,
                    onComplete = { onCompleteNudge(activeNudge.type) },
                    onRemindLater = onRemindLaterNudge
                )
            }
        }

        // Fast-Forward & Health Nudge Prototype Controls (Debug/Demo only)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PrototypeFastForwardButton(
                    label = "⚡ FAST FORWARD",
                    sublabel = "Simulate Block Finish",
                    onClick = onFastForward
                )
                if (BuildConfig.DEBUG) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onTriggerPrototypeNudge(HealthEventType.HYDRATION) },
                            modifier = Modifier.weight(1f).height(34.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("💧 Water", fontSize = 11.sp, color = TextSecondaryDark)
                        }
                        OutlinedButton(
                            onClick = { onTriggerPrototypeNudge(HealthEventType.EYE_RECOVERY) },
                            modifier = Modifier.weight(1f).height(34.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("👀 Eyes", fontSize = 11.sp, color = TextSecondaryDark)
                        }
                        OutlinedButton(
                            onClick = { onTriggerPrototypeNudge(HealthEventType.MOVEMENT) },
                            modifier = Modifier.weight(1f).height(34.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("🧍 Move", fontSize = 11.sp, color = TextSecondaryDark)
                        }
                    }
                }
            }
        }

        // Session Controls: Pause / Resume / End Early
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isPaused) {
                    Button(
                        onClick = onResume,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = BackgroundDark)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Resume Session", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = onPause,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryDark),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(listOf(BorderDark, BorderDark))
                        )
                    ) {
                        Text("Pause Session", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }

                TextButton(
                    onClick = onEndEarly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("End Session Early & Save Handoff", color = TextSecondaryDark, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun WorkBlockHandoffContent(
    session: DevSession,
    completedBlock: SessionBlock,
    isFinalBlock: Boolean,
    onSubmitHandoff: (accomplished: String, remaining: String) -> Unit
) {
    var accomplishedText by remember { mutableStateOf("") }
    var remainingText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🌱", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isFinalBlock) "Focus Session Complete" else "Block Complete",
                        color = SageGreen,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Capture where you are before moving forward.",
                    color = TextSecondaryDark,
                    fontSize = 14.sp
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(BorderDark, BorderDark)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "1. WHAT DID YOU ACCOMPLISH?",
                        color = TextSecondaryDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    OutlinedTextField(
                        value = accomplishedText,
                        onValueChange = { accomplishedText = it },
                        placeholder = { Text("e.g. Implemented refresh token generation", color = TextSecondaryDark, fontSize = 13.sp) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = BorderDark
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "2. WHAT'S STILL ON YOUR MIND?",
                        color = TextSecondaryDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    OutlinedTextField(
                        value = remainingText,
                        onValueChange = { remainingText = it },
                        placeholder = { Text("e.g. Need to test expiration and 401 response", color = TextSecondaryDark, fontSize = 13.sp) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = BorderDark
                        )
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    val acc = accomplishedText.ifBlank { "Completed work block." }
                    val rem = remainingText.ifBlank { "Continue next objective." }
                    onSubmitHandoff(acc, rem)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = BackgroundDark)
            ) {
                Text(
                    text = if (isFinalBlock) "Proceed to 1% Better" else "Start Recovery Break",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun RecoveryClockContent(
    session: DevSession,
    recoveryBlock: SessionBlock,
    nextObjective: String,
    remainingSeconds: Long,
    progressRatio: Float,
    onEndRecoveryEarly: () -> Unit,
    onLogRecoveryHealthEvent: (HealthEventType) -> Unit,
    onFastForward: () -> Unit = {}
) {
    var selectedRecoveryType by remember { mutableStateOf<GuidedRecoveryType?>(null) }

    when (val activeRecovery = selectedRecoveryType) {
        null -> {
            RecoverySelectionContent(
                session = session,
                nextObjective = nextObjective,
                remainingSeconds = remainingSeconds,
                onSelectRecovery = { selectedRecoveryType = it },
                onSkipRecovery = onEndRecoveryEarly,
                onFastForward = onFastForward
            )
        }
        GuidedRecoveryType.BREATHING -> {
            GuidedBreathingExperience(
                onFinish = {
                    onLogRecoveryHealthEvent(HealthEventType.BREATHING)
                    onEndRecoveryEarly()
                },
                onBack = { selectedRecoveryType = null }
            )
        }
        GuidedRecoveryType.EYE_RECOVERY -> {
            GuidedEyeRecoveryExperience(
                onFinish = {
                    onLogRecoveryHealthEvent(HealthEventType.EYE_RECOVERY)
                    onEndRecoveryEarly()
                },
                onBack = { selectedRecoveryType = null }
            )
        }
        GuidedRecoveryType.MOVEMENT -> {
            GuidedMovementResetExperience(
                onFinish = {
                    onLogRecoveryHealthEvent(HealthEventType.MOVEMENT)
                    onEndRecoveryEarly()
                },
                onBack = { selectedRecoveryType = null }
            )
        }
    }
}

@Composable
private fun RecoverySelectionContent(
    session: DevSession,
    nextObjective: String,
    remainingSeconds: Long,
    onSelectRecovery: (GuidedRecoveryType) -> Unit,
    onSkipRecovery: () -> Unit,
    onFastForward: () -> Unit
) {
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column {
                Text(
                    text = "RECOVERY",
                    color = SageGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "You've finished this work block.",
                    color = TextPrimaryDark,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose how you want to recover ($timeFormatted remaining):",
                    color = TextSecondaryDark,
                    fontSize = 13.sp
                )
            }
        }

        // Three Guided Recovery Cards
        item {
            RecoveryChoiceCard(
                icon = "🌬️",
                title = "Guided Breathing",
                durationLabel = "10 min",
                description = "Box-breathing rhythm (4-4-4-4) to reset your nervous system.",
                onClick = { onSelectRecovery(GuidedRecoveryType.BREATHING) }
            )
        }

        item {
            RecoveryChoiceCard(
                icon = "👀",
                title = "Eye Recovery",
                durationLabel = "3 min",
                description = "20-20-20 distance gazing to relieve screen and monitor strain.",
                onClick = { onSelectRecovery(GuidedRecoveryType.EYE_RECOVERY) }
            )
        }

        item {
            RecoveryChoiceCard(
                icon = "🧍",
                title = "Movement Reset",
                durationLabel = "5 min",
                description = "Guided physical sequence: stand up, roll shoulders, and stretch.",
                onClick = { onSelectRecovery(GuidedRecoveryType.MOVEMENT) }
            )
        }

        // Fast-Forward Break Prototype Control
        item {
            PrototypeFastForwardButton(
                label = "⚡ FAST FORWARD BREAK",
                sublabel = "Demo only",
                onClick = onFastForward
            )
        }

        // Next Objective Preview (DevPulse remembers!)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "NEXT OBJECTIVE WAITING",
                        color = MutedLavender,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = nextObjective,
                        color = TextPrimaryDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onSkipRecovery,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle)
                )
            ) {
                Text("Skip Break & Continue", color = TextSecondaryDark, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun RecoveryChoiceCard(
    icon: String,
    title: String,
    durationLabel: String,
    description: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = durationLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SageGreen
                    )
                }
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
}

@Composable
private fun GuidedBreathingExperience(
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    // 4s Inhale -> 4s Hold -> 4s Exhale -> 4s Hold (16s cycle)
    var secondsInCycle by remember { mutableStateOf(0) }
    var isRunning by remember { mutableStateOf(true) }

    LaunchedEffect(isRunning) {
        while (isRunning) {
            kotlinx.coroutines.delay(1000L)
            secondsInCycle = (secondsInCycle + 1) % 16
        }
    }

    val (phaseTitle, phaseRemaining, targetScale) = when (secondsInCycle) {
        in 0..3 -> Triple("BREATHE IN", 4 - secondsInCycle, 0.7f + (secondsInCycle + 1) * 0.1f)
        in 4..7 -> Triple("HOLD", 8 - secondsInCycle, 1.15f)
        in 8..11 -> Triple("BREATHE OUT", 12 - secondsInCycle, 1.15f - (secondsInCycle - 7) * 0.1f)
        else -> Triple("HOLD", 16 - secondsInCycle, 0.7f)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "GUIDED BREATHING",
                color = SageGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Box Breathing (4-4-4-4)",
                color = TextSecondaryDark,
                fontSize = 13.sp
            )
        }

        // Animated Breathing Guide
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = phaseTitle,
                color = TextPrimaryDark,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .size((160 * targetScale).dp)
                    .clip(CircleShape)
                    .background(SageGreen.copy(alpha = 0.2f))
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
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Inhale deep through your nose, exhale slowly.",
                color = TextSecondaryDark,
                fontSize = 12.sp
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SageGreen, contentColor = BackgroundDark)
            ) {
                Text("Recovery Complete", fontWeight = FontWeight.Bold)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { isRunning = !isRunning },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isRunning) "Pause" else "Resume", color = TextPrimaryDark, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Change", color = TextSecondaryDark, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun GuidedEyeRecoveryExperience(
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    var secondsLeft by remember { mutableStateOf(20) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            kotlinx.coroutines.delay(1000L)
            secondsLeft -= 1
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "EYE RECOVERY",
                color = SageGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "20-20-20 Rule",
                color = TextSecondaryDark,
                fontSize = 13.sp
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "LOOK AWAY",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "$secondsLeft",
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                color = SageGreen
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Look away from all screens.\nFocus on an object at least 20 feet away.\nBlink naturally and soften your gaze.",
                fontSize = 14.sp,
                color = TextSecondaryDark,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SageGreen, contentColor = BackgroundDark)
            ) {
                Text("Eyes Rested & Ready", fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Choose another recovery", color = TextSecondaryDark, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun GuidedMovementResetExperience(
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    val steps = listOf(
        Pair("1. STAND UP", "Step away from your desk, chair, and screens."),
        Pair("2. ROLL YOUR SHOULDERS", "Slow backward rolls 5 times to release neck tension."),
        Pair("3. EXTEND YOUR SPINE", "Reach both hands overhead and lengthen your lower back."),
        Pair("4. SHORT WALK", "Walk around the room, shake out your arms and grab water."),
        Pair("5. RETURN WHEN READY", "Take a slow breath and return to the code with clarity.")
    )
    var currentStepIndex by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "MOVEMENT RESET",
                color = SageGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Step ${currentStepIndex + 1} of ${steps.size}",
                color = TextSecondaryDark,
                fontSize = 13.sp
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = steps[currentStepIndex].first,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SageGreen,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = steps[currentStepIndex].second,
                    fontSize = 15.sp,
                    color = TextPrimaryDark,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (currentStepIndex < steps.size - 1) {
                Button(
                    onClick = { currentStepIndex++ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MutedLavender, contentColor = BackgroundDark)
                ) {
                    Text("Next Step", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SageGreen, contentColor = BackgroundDark)
                ) {
                    Text("Movement Complete", fontWeight = FontWeight.Bold)
                }
            }

            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Choose another recovery", color = TextSecondaryDark, fontSize = 13.sp)
            }
        }
    }
}

/**
 * Non-disruptive card displayed inside active work session when health reminder fires.
 */
@Composable
private fun SessionHealthNudgeCard(
    nudge: HealthNudge,
    onComplete: () -> Unit,
    onRemindLater: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, SageGreen.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = nudge.type.icon, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = nudge.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = SageGreen
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = nudge.message,
                fontSize = 14.sp,
                color = TextPrimaryDark,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onComplete,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SageGreen, contentColor = BackgroundDark)
                ) {
                    Text(text = nudge.actionLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onRemindLater,
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(BorderDark)
                    )
                ) {
                    Text(text = nudge.secondaryLabel, color = TextSecondaryDark, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun ReadyForNextBlockContent(
    session: DevSession,
    nextBlockIndex: Int,
    carryoverObjective: String,
    onContinue: (customObjective: String?) -> Unit
) {
    var isEditingObjective by remember { mutableStateOf(false) }
    var objectiveText by remember { mutableStateOf(carryoverObjective) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "⚡", fontSize = 32.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "READY TO CONTINUE?",
            color = Secondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Block ${nextBlockIndex + 1} of ${session.totalBlocks}",
            color = TextPrimaryDark,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

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
                    text = "CONTINUE WHERE YOU LEFT OFF:",
                    color = TextSecondaryDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (isEditingObjective) {
                    OutlinedTextField(
                        value = objectiveText,
                        onValueChange = { objectiveText = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark,
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = BorderDark
                        )
                    )
                } else {
                    Text(
                        text = "\"$objectiveText\"",
                        color = TextPrimaryDark,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onContinue(if (isEditingObjective) objectiveText else null) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = BackgroundDark)
        ) {
            Text("Continue Block", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(onClick = { isEditingObjective = !isEditingObjective }) {
            Text(
                text = if (isEditingObjective) "Use Previous Objective" else "Change Objective",
                color = TextSecondaryDark
            )
        }
    }
}

@Composable
private fun SessionCompleteReflectionContent(
    session: DevSession,
    lastHandoff: com.devpulse.ai.data.local.entity.SessionHandoffEntity?,
    onSubmitReflection: (category: ImprovementCategory, reflection: String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(ImprovementCategory.SKILL) }
    var reflectionText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🌱", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Session Complete",
                        color = SageGreen,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "You showed up.",
                    color = TextPrimaryDark,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Every session makes you slightly better than before.",
                    color = TextSecondaryDark,
                    fontSize = 13.sp
                )
            }
        }

        if (lastHandoff != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(BorderDark, BorderDark)))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "ACCOMPLISHED",
                            color = Secondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = lastHandoff.accomplished,
                            color = TextPrimaryDark,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "UNFINISHED / NEXT UP",
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = lastHandoff.nextObjective,
                            color = TextPrimaryDark,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "WHAT DID YOU GAIN FROM THIS SESSION?",
                color = TextSecondaryDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ImprovementCategory.values()) { category ->
                    val isSelected = category == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) SurfaceVariantDark else SurfaceDark)
                            .border(1.dp, if (isSelected) Primary else BorderDark, RoundedCornerShape(12.dp))
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${category.icon} ${category.displayName}",
                            color = if (isSelected) SageGreen else TextSecondaryDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "WHAT MADE YOU 1% BETTER?",
                color = TextSecondaryDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = reflectionText,
                onValueChange = { reflectionText = it },
                placeholder = {
                    Text(
                        text = "e.g. Discovered where token expiration was failing, or learned Docker bridge networking...",
                        color = TextSecondaryDark,
                        fontSize = 13.sp
                    )
                },
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark,
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = BorderDark
                )
            )
        }

        item {
            Button(
                onClick = {
                    val reflection = reflectionText.ifBlank { "Kept building and moved forward." }
                    onSubmitReflection(selectedCategory, reflection)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = BackgroundDark)
            ) {
                Text("🌱 Save +1% Better", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun SessionFinishedContent(
    improvement: com.devpulse.ai.data.local.entity.OnePercentImprovementEntity?,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "🌱", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "+1% BETTER",
            color = SageGreen,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Every session. One small improvement.",
            color = TextSecondaryDark,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (improvement != null) {
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
                    Text(
                        text = "\"${improvement.reflectionText}\"",
                        color = TextPrimaryDark,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = BackgroundDark)
        ) {
            Text("Return to Daily Pulse", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

/**
 * Clearly identifiable prototype/development control for demo and validation flows.
 * Rendered strictly in DEBUG builds.
 */
@Composable
private fun PrototypeFastForwardButton(
    label: String,
    sublabel: String = "Demo only",
    onClick: () -> Unit
) {
    if (!BuildConfig.DEBUG) return

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color(0x1AF59E0B),
        border = BorderStroke(1.dp, Color(0x66F59E0B))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                color = MutedAmber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MutedAmber.copy(alpha = 0.2f)
            ) {
                Text(
                    text = sublabel,
                    color = MutedAmber,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
        }
    }
}

