package com.devpulse.ai.screens.tabs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devpulse.ai.components.GlowCard
import com.devpulse.ai.ui.theme.Primary
import com.devpulse.ai.ui.theme.Secondary
import com.devpulse.ai.ui.theme.TextPrimaryDark
import com.devpulse.ai.ui.theme.TextSecondaryDark

@Composable
fun TabSettings(
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    
    var forceRefreshEnabled by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Settings",
            color = TextPrimaryDark,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        // Preferences Card
        GlowCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Preferences", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                SettingsActionRow(
                    title = "Force Live Reload",
                    description = "Bypass local analysis cache, fetching fresh raw data from GitHub APIs.",
                    actionText = if (forceRefreshEnabled) "ON" else "OFF",
                    onClick = {
                        forceRefreshEnabled = !forceRefreshEnabled
                        val status = if (forceRefreshEnabled) "enabled" else "disabled"
                        Toast.makeText(context, "Force Live Reload $status", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // Cache Management
        GlowCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Cache Management", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                SettingsActionRow(
                    title = "Clear Local Profile Cache",
                    description = "Deletes all cached metadata, analysis figures, and session repositories.",
                    actionText = "Clear",
                    onClick = {
                        Toast.makeText(context, "Local Profile Cache Cleared", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // About Application
        GlowCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "About DevPulse AI", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                AboutRow(label = "Application Version", value = "1.0.0")
                AboutRow(label = "Aesthetic Base", value = "Linear / GitHub Mobile Dark")
                AboutRow(label = "Design System", value = "Compose Material 3")
                AboutRow(label = "API Limit status", value = "Dynamic REST (Rate limit aware)")
            }
        }
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    description: String,
    actionText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, color = TextSecondaryDark, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E212E))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(text = actionText, color = Secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondaryDark, fontSize = 13.sp)
        Text(text = value, color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
