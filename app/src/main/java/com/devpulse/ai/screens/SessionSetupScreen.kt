package com.devpulse.ai.screens

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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val checklistItems by viewModel.checklistItems.collectAsState()
    val checkedItemIds by viewModel.checkedItemIds.collectAsState()
    val latestHandoff by viewModel.latestUnfinishedHandoff.collectAsState()
    val ignoredHandoffId by viewModel.ignoredHandoffId.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var showAddChecklistDialog by remember { mutableStateOf(false) }
    var newChecklistText by remember { mutableStateOf("") }

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
            // Unfinished Session Handoff Continuity Card
            if (latestHandoff != null && latestHandoff?.id != ignoredHandoffId) {
                item {
                    SessionContinuityCard(
                        handoff = latestHandoff!!,
                        onContinue = { viewModel.applyHandoff(latestHandoff!!) },
                        onDismiss = { viewModel.dismissHandoff(latestHandoff!!.id) }
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

            // Pre-Session Mindset
            item {
                PreSessionStateSection(
                    selectedState = selectedState,
                    onSelectState = { viewModel.selectState(it) }
                )
            }

            // Pre-Session Ritual Checklist
            item {
                PreSessionChecklistSection(
                    items = checklistItems,
                    checkedIds = checkedItemIds,
                    onToggleCheck = { viewModel.toggleCheckItem(it) },
                    onAddCustomClick = { showAddChecklistDialog = true }
                )
            }

            // Action: Begin Session
            item {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val sessionId = viewModel.startConfiguredSession()
                            onSessionStarted(sessionId)
                        }
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
                            "Begin Focus Session (${selectedDuration}m)"
                        } else {
                            "Begin Deep Work (${deepWorkPreset.label})"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }

    // Dialog for adding custom checklist item
    if (showAddChecklistDialog) {
        AlertDialog(
            onDismissRequest = { showAddChecklistDialog = false },
            containerColor = SurfaceDark,
            title = {
                Text(
                    text = "Add Environment Item",
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = newChecklistText,
                    onValueChange = { newChecklistText = it },
                    placeholder = { Text("e.g. Put on headphones", color = TextSecondaryDark) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = BorderDark
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newChecklistText.isNotBlank()) {
                            viewModel.addCustomChecklistItem(newChecklistText)
                            newChecklistText = ""
                            showAddChecklistDialog = false
                        }
                    }
                ) {
                    Text("Add", color = Primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddChecklistDialog = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            }
        )
    }
}

@Composable
private fun SessionContinuityCard(
    handoff: SessionHandoffEntity,
    onContinue: () -> Unit,
    onDismiss: () -> Unit
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
                    color = Color(0xFF10B981),
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
                text = "Unfinished: \"${handoff.nextObjective}\"",
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
                    onClick = onDismiss,
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
private fun PreSessionStateSection(
    selectedState: DeveloperState,
    onSelectState: (DeveloperState) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "PRE-SESSION MINDSET",
            color = TextSecondaryDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        val preOptions = listOf(DeveloperState.READY, DeveloperState.GOOD, DeveloperState.TIRED)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            preOptions.forEach { state ->
                val isSelected = state == selectedState
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) SurfaceVariantDark else SurfaceDark)
                        .border(1.dp, if (isSelected) Secondary else BorderDark, RoundedCornerShape(12.dp))
                        .clickable { onSelectState(state) }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = state.emoji, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.displayName,
                            color = if (isSelected) TextPrimaryDark else TextSecondaryDark,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreSessionChecklistSection(
    items: List<PreSessionChecklistItemEntity>,
    checkedIds: Set<String>,
    onToggleCheck: (String) -> Unit,
    onAddCustomClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PREPARE YOUR ENVIRONMENT",
                    color = TextSecondaryDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Give yourself a clean space to build.",
                    color = TextSecondaryDark,
                    fontSize = 12.sp
                )
            }
            Text(
                text = "+ Add Item",
                color = Primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onAddCustomClick() }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(BorderDark, BorderDark)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 10.dp)
            ) {
                items.forEach { item ->
                    val isChecked = checkedIds.contains(item.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleCheck(item.id) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { onToggleCheck(item.id) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF10B981),
                                uncheckedColor = TextSecondaryDark,
                                checkmarkColor = BackgroundDark
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.title,
                            color = if (isChecked) Color(0xFF10B981) else TextPrimaryDark,
                            fontSize = 13.sp,
                            fontWeight = if (isChecked) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
