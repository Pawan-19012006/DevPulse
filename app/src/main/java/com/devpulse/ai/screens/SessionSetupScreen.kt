package com.devpulse.ai.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.devpulse.ai.data.local.entity.PreSessionChecklistItemEntity
import com.devpulse.ai.data.local.entity.SessionHandoffEntity
import com.devpulse.ai.domain.session.*
import com.devpulse.ai.ui.theme.*
import com.devpulse.ai.viewmodel.SessionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSetupScreen(
    viewModel: SessionViewModel,
    onNavigateBack: () -> Unit,
    onSessionStarted: (sessionId: String) -> Unit
) {
    val sessionMode by viewModel.sessionMode.collectAsState()
    val selectedActivity by viewModel.selectedActivity.collectAsState()
    val selectedDuration by viewModel.selectedDurationMinutes.collectAsState()
    val deepWorkPreset by viewModel.deepWorkPreset.collectAsState()
    val selectedState by viewModel.selectedState.collectAsState()
    val goal by viewModel.goal.collectAsState()
    val currentBlockObjective by viewModel.currentBlockObjective.collectAsState()
    val latestHandoff by viewModel.latestUnfinishedHandoff.collectAsState()
    val ignoredHandoffId by viewModel.ignoredHandoffId.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var showPreparationDialog by remember { mutableStateOf(false) }

    val isGenuineHandoff = latestHandoff != null &&
            latestHandoff?.id != ignoredHandoffId &&
            !latestHandoff?.nextObjective.isNullOrBlank() &&
            !latestHandoff?.nextObjective.equals("Pick up where left off.", ignoreCase = true)

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Prepare Dev Session",
                        color = TextPrimaryDark,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimaryDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Unfinished Session Handoff Continuity Card (Only shown when genuine handoff exists)
            if (isGenuineHandoff) {
                item {
                    SessionContinuityCard(
                        handoff = latestHandoff!!,
                        onContinue = {
                            viewModel.applyHandoff(latestHandoff!!)
                            showPreparationDialog = true
                        },
                        onStartFresh = {
                            viewModel.startFresh()
                        }
                    )
                }
            }

            // Session Mode Selector: Focus Session vs Deep Work
            item {
                SessionModeSelector(
                    currentMode = sessionMode,
                    onSelectMode = { viewModel.selectSessionMode(it) }
                )
            }

            // Activity Selection
            item {
                ActivitySelectionSection(
                    selectedActivity = selectedActivity,
                    onSelectActivity = { viewModel.selectActivity(it) }
                )
            }

            // Duration / Deep Work Preset Selection
            item {
                if (sessionMode == SessionMode.FOCUS) {
                    FocusDurationSection(
                        selectedDuration = selectedDuration,
                        onSelectDuration = { viewModel.selectDuration(it) }
                    )
                } else {
                    DeepWorkPresetSection(
                        selectedPreset = deepWorkPreset,
                        onSelectPreset = { viewModel.selectDeepWorkPreset(it) }
                    )
                }
            }

            // Level 1: Overall Goal
            item {
                GoalInputSection(
                    label = if (sessionMode == SessionMode.FOCUS) "SESSION GOAL" else "LEVEL 1: OVERALL SESSION GOAL",
                    placeholder = if (sessionMode == SessionMode.FOCUS) "What ONE thing will you achieve?" else "Big goal for this deep work (e.g. Build GitHub sync)",
                    goal = goal,
                    onGoalChange = { viewModel.updateGoal(it) }
                )
            }

            // Level 2: Current Block Objective (for Deep Work)
            if (sessionMode == SessionMode.DEEP_WORK) {
                item {
                    GoalInputSection(
                        label = "LEVEL 2: BLOCK 1 OBJECTIVE",
                        placeholder = "Immediate objective for Block 1 (e.g. Implement repository fetching)",
                        goal = currentBlockObjective,
                        onGoalChange = { viewModel.updateBlockObjective(it) }
                    )
                }
            }

            // Action: Begin Session (Triggers Environment Preparation Modal)
            item {
                Button(
                    onClick = {
                        showPreparationDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = BackgroundDark)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (sessionMode == SessionMode.FOCUS) {
                            "Start Focus Session (${selectedDuration}m)"
                        } else {
                            "Start Deep Work (${deepWorkPreset.label})"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }

    // Modal: Environment Preparation Dialog
    if (showPreparationDialog) {
        EnvironmentPreparationDialog(
            onDismissRequest = { showPreparationDialog = false },
            onEnterSession = {
                coroutineScope.launch {
                    val sessionId = viewModel.startConfiguredSession()
                    showPreparationDialog = false
                    onSessionStarted(sessionId)
                }
            }
        )
    }
}

@Composable
private fun SessionContinuityCard(
    handoff: SessionHandoffEntity,
    onContinue: () -> Unit,
    onStartFresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(Secondary, Primary)))
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
                    text = "LAST SESSION HANDOFF",
                    color = Secondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "DevPulse remembers",
                    color = SageGreen,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "You were working on: \"${handoff.sessionGoal}\"",
                color = TextSecondaryDark,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "UNFINISHED: \"${handoff.nextObjective}\"",
                color = TextPrimaryDark,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = BackgroundDark)
                ) {
                    Text("Continue This", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onStartFresh,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Start Fresh", color = TextSecondaryDark, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun SessionModeSelector(
    currentMode: SessionMode,
    onSelectMode: (SessionMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        SessionMode.values().forEach { mode ->
            val isSelected = mode == currentMode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) SurfaceVariantDark else Color.Transparent)
                    .clickable { onSelectMode(mode) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.displayName,
                    color = if (isSelected) TextPrimaryDark else TextSecondaryDark,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun ActivitySelectionSection(
    selectedActivity: SessionActivityType,
    onSelectActivity: (SessionActivityType) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "SELECT ACTIVITY",
            color = TextSecondaryDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        val activities = SessionActivityType.values().toList().chunked(2)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            activities.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { activity ->
                        val isSelected = activity == selectedActivity
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectActivity(activity) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) SurfaceVariantDark else SurfaceDark
                            ),
                            border = CardDefaults.outlinedCardBorder().copy(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    if (isSelected) listOf(Primary, Secondary) else listOf(BorderDark, BorderDark)
                                )
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = activity.icon, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = activity.displayName,
                                    color = if (isSelected) TextPrimaryDark else TextSecondaryDark,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusDurationSection(
    selectedDuration: Int,
    onSelectDuration: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "FOCUS DURATION",
            color = TextSecondaryDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        val durations = listOf(15, 25, 45, 60, 90)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            durations.forEach { duration ->
                val isSelected = duration == selectedDuration
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Primary else SurfaceDark)
                        .border(1.dp, if (isSelected) Primary else BorderDark, RoundedCornerShape(12.dp))
                        .clickable { onSelectDuration(duration) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${duration}m",
                        color = if (isSelected) BackgroundDark else TextPrimaryDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DeepWorkPresetSection(
    selectedPreset: DeepWorkPreset,
    onSelectPreset: (DeepWorkPreset) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "DEEP WORK PRESETS",
            color = TextSecondaryDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DeepWorkPresets.ALL.forEach { preset ->
                val isSelected = preset == selectedPreset
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectPreset(preset) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) SurfaceVariantDark else SurfaceDark
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            if (isSelected) listOf(Primary, Secondary) else listOf(BorderDark, BorderDark)
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = preset.label,
                            color = TextPrimaryDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${preset.numberOfBlocks}x ${preset.workMinutes}m work\n${preset.recoveryMinutes}m breaks",
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalInputSection(
    label: String,
    placeholder: String,
    goal: String,
    onGoalChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = TextSecondaryDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = goal,
            onValueChange = onGoalChange,
            placeholder = { Text(placeholder, color = TextSecondaryDark, fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimaryDark,
                unfocusedTextColor = TextPrimaryDark,
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                focusedBorderColor = Primary,
                unfocusedBorderColor = BorderDark
            )
        )
    }
}


@Composable
private fun EnvironmentPreparationDialog(
    onDismissRequest: () -> Unit,
    onEnterSession: () -> Unit
) {
    val items = listOf(
        "Turn off distracting notifications",
        "Keep water nearby",
        "Define ONE specific problem to solve",
        "Open required IDE, tools & docs",
        "Put phone out of sight / focus mode"
    )
    val checkedIndices = remember { mutableStateListOf<Int>() }
    val allCompleted = checkedIndices.size == items.size

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = SurfaceDark,
            border = BorderStroke(1.dp, BorderDark),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PREPARE TO FOCUS",
                    color = SageGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Give yourself a clean space to build.",
                    color = TextSecondaryDark,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items.forEachIndexed { index, text ->
                        val isChecked = checkedIndices.contains(index)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isChecked) SurfaceVariantDark.copy(alpha = 0.6f) else SurfaceVariantDark.copy(alpha = 0.25f))
                                .border(
                                    1.dp,
                                    if (isChecked) SageGreen.copy(alpha = 0.5f) else BorderDark,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (isChecked) {
                                        checkedIndices.remove(index)
                                    } else {
                                        checkedIndices.add(index)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    if (isChecked) {
                                        checkedIndices.remove(index)
                                    } else {
                                        checkedIndices.add(index)
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Primary,
                                    uncheckedColor = TextSecondaryDark,
                                    checkmarkColor = BackgroundDark
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = text,
                                color = if (isChecked) TextPrimaryDark else TextSecondaryDark,
                                fontSize = 13.sp,
                                fontWeight = if (isChecked) FontWeight.Medium else FontWeight.Normal,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                AnimatedContent(
                    targetState = allCompleted,
                    label = "ChecklistCompletionStatus"
                ) { ready ->
                    if (ready) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = SageGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "✓ Environment ready",
                                color = SageGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = "${checkedIndices.size} / ${items.size} completed",
                            color = TextSecondaryDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onEnterSession,
                    enabled = allCompleted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = BackgroundDark,
                        disabledContainerColor = SurfaceVariantDark,
                        disabledContentColor = TextMuted
                    )
                ) {
                    Text(
                        text = if (allCompleted) "Enter Session →" else "Enter Session",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cancel",
                        color = TextSecondaryDark,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
