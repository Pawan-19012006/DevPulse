package com.devpulse.ai.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.devpulse.ai.viewmodel.CoachGuidance
import com.devpulse.ai.viewmodel.HomeViewModel
import com.devpulse.ai.viewmodel.SessionViewModel

@Composable
fun CoachScreen(
    homeViewModel: HomeViewModel,
    sessionViewModel: SessionViewModel,
    onNavigateToSessionSetup: () -> Unit
) {
    val guidanceList = homeViewModel.getCoachGuidanceList()
    val latestUnfinishedHandoff by homeViewModel.latestUnfinishedHandoff.collectAsState()

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
                    text = "DEV COACH",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = SageGreen
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "A quiet voice in your journey.",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Grounded observations from your actual sessions and patterns.",
                    fontSize = 14.sp,
                    color = TextSecondaryDark
                )
            }
        }

        // Section: Active Observations & Guidance
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "ACTIVE GUIDANCE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextSecondaryDark
                )

                guidanceList.forEach { guidance ->
                    CoachGuidanceCard(
                        guidance = guidance,
                        isHandoff = guidance.category == "Continuity",
                        onContinueHandoff = {
                            latestUnfinishedHandoff?.let { handoff ->
                                sessionViewModel.updateGoal(handoff.sessionGoal)
                                sessionViewModel.updateBlockObjective(handoff.nextObjective)
                                onNavigateToSessionSetup()
                            }
                        }
                    )
                }
            }
        }

        item {
            HorizontalDivider(
                color = BorderSubtle,
                thickness = 1.dp
            )
        }

        // Section: Mental Anchors for Developers
        item {
            Column {
                Text(
                    text = "ENGINEERING PRINCIPLES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextSecondaryDark
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuoteAnchor(
                        quote = "“Make it work, make it right, make it fast.”",
                        author = "Kent Beck"
                    )
                    QuoteAnchor(
                        quote = "“Code is read much more often than it is written.”",
                        author = "Guido van Rossum"
                    )
                    QuoteAnchor(
                        quote = "“Simplicity is prerequisite for reliability.”",
                        author = "Edsger W. Dijkstra"
                    )
                }
            }
        }
    }
}

@Composable
private fun CoachGuidanceCard(
    guidance: CoachGuidance,
    isHandoff: Boolean,
    onContinueHandoff: () -> Unit
) {
    val categoryColor = when (guidance.category) {
        "Continuity" -> MutedAmber
        "Focus Pattern" -> MutedLavender
        "Mental State" -> SageGreen
        "1% Better" -> SageGreen
        else -> TextSecondaryDark
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = guidance.category.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = categoryColor
                )

                Text(
                    text = guidance.timestamp,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = guidance.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimaryDark
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = guidance.message,
                fontSize = 13.sp,
                color = TextSecondaryDark,
                lineHeight = 19.sp
            )

            if (isHandoff) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onContinueHandoff,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceVariantDark,
                        contentColor = TextPrimaryDark
                    ),
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Continue Objective",
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

@Composable
private fun QuoteAnchor(
    quote: String,
    author: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark.copy(alpha = 0.5f))
            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Column {
            Text(
                text = quote,
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                color = TextPrimaryDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "— $author",
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}
