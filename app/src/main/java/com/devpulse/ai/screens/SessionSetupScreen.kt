package com.devpulse.ai.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devpulse.ai.domain.session.DeveloperState
import com.devpulse.ai.domain.session.ImprovementCategory
import com.devpulse.ai.domain.session.SessionActivityType
import com.devpulse.ai.ui.theme.BackgroundDark
import com.devpulse.ai.ui.theme.BorderDark
import com.devpulse.ai.ui.theme.Primary
import com.devpulse.ai.ui.theme.Secondary
import com.devpulse.ai.ui.theme.SurfaceDark
import com.devpulse.ai.ui.theme.SurfaceVariantDark
import com.devpulse.ai.ui.theme.TextPrimaryDark
import com.devpulse.ai.ui.theme.TextSecondaryDark
import com.devpulse.ai.viewmodel.SessionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSetupScreen(
    viewModel: SessionViewModel,
    onNavigateBack: () -> Unit,
    onSessionStarted: () -> Unit
) {
    val selectedActivity by viewModel.selectedActivity.collectAsState()
    val selectedDuration by viewModel.selectedDurationMinutes.collectAsState()
    val selectedState by viewModel.selectedState.collectAsState()
    val goal by viewModel.goal.collectAsState()
    val checklistItems by viewModel.checklistItems.collectAsState()
    val checkedItemIds by viewModel.checkedItemIds.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showAddChecklistDialog by remember { mutableStateOf(false) }
    var newChecklistText by remember { mutableStateOf("") }
    var showSessionCompleteDialog by remember { mutableStateOf(false) }
    var reflectionText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ImprovementCategory.SKILL) }
    var activeSessionId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Prepare Session",
                        color = TextPrimaryDark,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimaryDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark
                )
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
            // Activity Selection
            item {
                ActivitySelectionSection(
                    selectedActivity = selectedActivity,
                    onSelectActivity = { viewModel.selectActivity(it) }
                )
            }

            // Duration Selector
            item {
                DurationSelectionSection(
                    selectedDuration = selectedDuration,
                    onSelectDuration = { viewModel.selectDuration(it) }
                )
            }

            // Goal Input
            item {
                GoalInputSection(
                    goal = goal,
                    onGoalChange = { viewModel.updateGoal(it) }
                )
            }

            // Initial Developer State
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
                            val id = viewModel.createAndStartSession()
                            activeSessionId = id
                            showSessionCompleteDialog = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
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
                        text = "Begin Focus Session (${selectedDuration}m)",
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
                    text = "Add Checklist Item",
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

    // Phase 0 Session Completion & 1% Better Capture Modal
    if (showSessionCompleteDialog) {
        AlertDialog(
            onDismissRequest = { showSessionCompleteDialog = false },
            containerColor = SurfaceDark,
            title = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🌱", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Session Complete",
                            color = Color(0xFF10B981),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "You showed up.",
                        color = TextPrimaryDark,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "What made you 1% better this session?",
                        color = TextSecondaryDark,
                        fontSize = 13.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Select what you gained:",
                        color = TextSecondaryDark,
                        fontSize = 12.sp
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ImprovementCategory.values()) { category ->
                            val isSelected = category == selectedCategory
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) SurfaceVariantDark else BackgroundDark)
                                    .border(1.dp, if (isSelected) Primary else BorderDark, RoundedCornerShape(12.dp))
                                    .clickable { selectedCategory = category }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = category.prefixBadge,
                                    color = if (isSelected) Color(0xFF10B981) else TextSecondaryDark,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = reflectionText,
                        onValueChange = { reflectionText = it },
                        placeholder = { Text("e.g. Understood coroutine dispatchers", color = TextSecondaryDark) },
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
            },
            confirmButton = {
                Button(
                    onClick = {
                        val text = reflectionText.ifBlank { "Focused ${selectedActivity.displayName} session." }
                        viewModel.recordImprovement(selectedCategory, text)
                        Toast.makeText(context, "🌱 +1% Better recorded!", Toast.LENGTH_SHORT).show()
                        showSessionCompleteDialog = false
                        onSessionStarted()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = BackgroundDark)
                ) {
                    Text("Save 1% Improvement", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSessionCompleteDialog = false
                    onSessionStarted()
                }) {
                    Text("Skip Reflection", color = TextSecondaryDark)
                }
            }
        )
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
                                Column {
                                    Text(
                                        text = activity.displayName,
                                        color = if (isSelected) TextPrimaryDark else TextSecondaryDark,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
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
private fun DurationSelectionSection(
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
private fun GoalInputSection(
    goal: String,
    onGoalChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "SESSION GOAL (OPTIONAL)",
            color = TextSecondaryDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = goal,
            onValueChange = onGoalChange,
            placeholder = { Text("What ONE thing will you achieve?", color = TextSecondaryDark, fontSize = 13.sp) },
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

        val preOptions = listOf(DeveloperState.READY, DeveloperState.LOW_ENERGY, DeveloperState.MENTALLY_TIRED)
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
    items: List<com.devpulse.ai.data.local.entity.PreSessionChecklistItemEntity>,
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
