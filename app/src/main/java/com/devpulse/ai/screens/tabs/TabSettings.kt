package com.devpulse.ai.screens.tabs

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devpulse.ai.components.GlowCard
import com.devpulse.ai.domain.SyncStatus
import com.devpulse.ai.ui.theme.Primary
import com.devpulse.ai.ui.theme.Secondary
import com.devpulse.ai.ui.theme.Tertiary
import com.devpulse.ai.ui.theme.TextPrimaryDark
import com.devpulse.ai.ui.theme.TextSecondaryDark

@Composable
fun TabSettings(
    syncStatus: SyncStatus = SyncStatus.Idle,
    onForceReload: () -> Unit = {},
    onClearCache: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Settings & Data Foundation",
            color = TextPrimaryDark,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        // Synchronization Card
        GlowCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Synchronization Engine", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                SettingsActionRow(
                    title = "Force Real-time Sync",
                    description = "Bypass 5-minute incremental threshold and fetch fresh event signals directly from GitHub APIs.",
                    actionText = if (syncStatus is SyncStatus.Syncing) "SYNCING..." else "SYNC NOW",
                    enabled = syncStatus !is SyncStatus.Syncing,
                    onClick = onForceReload
                )

                Spacer(modifier = Modifier.height(12.dp))
                val statusText = when (syncStatus) {
                    is SyncStatus.Syncing -> "Currently fetching repositories and event streams..."
                    is SyncStatus.Success -> "Up to date (persisted in local Room database)"
                    is SyncStatus.PartialSuccess -> "Partially synced: ${syncStatus.warning}"
                    is SyncStatus.Failed -> "Sync failed: ${syncStatus.error}"
                    is SyncStatus.Idle -> "Idle / Cached"
                }
                Text(
                    text = "Status: $statusText",
                    color = if (syncStatus is SyncStatus.Failed) Tertiary else TextSecondaryDark,
                    fontSize = 12.sp
                )
            }
        }

        // Cache & Database Management
        GlowCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Local Room Persistence", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                SettingsActionRow(
                    title = "Wipe Local Intelligence Cache",
                    description = "Deletes all locally stored profiles, repositories, normalized events, skill evidences, and snapshots from Room.",
                    actionText = "WIPE DB",
                    actionColor = Tertiary,
                    onClick = onClearCache
                )
            }
        }

        // Security Architecture Card
        GlowCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Security & Auth Architecture", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                AboutRow(label = "Current Auth Provider", value = "BuildConfigTokenProvider")
                AboutRow(label = "Target Production Architecture", value = "OAuth 2.0 + PKCE")
                AboutRow(label = "Token Storage Target", value = "EncryptedSharedPreferences")
                AboutRow(label = "Rate Limit Tier", value = "5,000 req/hr (Authenticated)")
            }
        }

        // About Application
        GlowCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "About DevPulse AI", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                AboutRow(label = "Architecture Version", value = "Phase 1: Real Data Foundation")
                AboutRow(label = "Local Storage", value = "Room 2.6.1 + KSP")
                AboutRow(label = "Event Normalization", value = "Multi-event decomposition")
                AboutRow(label = "Evidence Model", value = "Factual SkillConfidence")
            }
        }
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    description: String,
    actionText: String,
    enabled: Boolean = true,
    actionColor: Color = Secondary,
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
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = actionText,
                color = if (enabled) actionColor else TextSecondaryDark,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
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
        Text(text = label, color = TextSecondaryDark, fontSize = 12.sp)
        Text(text = value, color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
