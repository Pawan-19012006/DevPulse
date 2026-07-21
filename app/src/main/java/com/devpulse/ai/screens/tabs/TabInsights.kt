package com.devpulse.ai.screens.tabs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devpulse.ai.components.GlowCard
import com.devpulse.ai.ui.theme.Primary
import com.devpulse.ai.ui.theme.Secondary
import com.devpulse.ai.ui.theme.Tertiary
import com.devpulse.ai.ui.theme.TextPrimaryDark
import com.devpulse.ai.ui.theme.TextSecondaryDark
import com.devpulse.ai.utils.AnalyzedProfile
import com.devpulse.ai.utils.DeveloperMetricsScores

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TabInsights(
    profile: AnalyzedProfile,
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
        // SECTION 6: DEVELOPER METRICS
        Text(
            text = "Developer Metrics",
            color = TextPrimaryDark,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        GlowCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Main Overall Score
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressScore(
                        score = profile.devMetrics.overallScore,
                        label = "Overall",
                        color = Primary,
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text(
                            text = "AI Growth Index",
                            color = TextPrimaryDark,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Calculated heuristic rating matching repository size, star counts, followers, and language stack diversity.",
                            color = TextSecondaryDark,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF222533)))
                Spacer(modifier = Modifier.height(24.dp))

                // Breakdown Grid
                Row(modifier = Modifier.fillMaxWidth()) {
                    CircularProgressScore(
                        score = profile.devMetrics.backendScore,
                        label = "Backend",
                        color = Primary,
                        modifier = Modifier.weight(1f).aspectRatio(1f)
                    )
                    CircularProgressScore(
                        score = profile.devMetrics.frontendScore,
                        label = "Frontend",
                        color = Secondary,
                        modifier = Modifier.weight(1f).aspectRatio(1f)
                    )
                    CircularProgressScore(
                        score = profile.devMetrics.aiMlScore,
                        label = "AI/ML",
                        color = Tertiary,
                        modifier = Modifier.weight(1f).aspectRatio(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    CircularProgressScore(
                        score = profile.devMetrics.devopsScore,
                        label = "DevOps",
                        color = Secondary,
                        modifier = Modifier.weight(1f).aspectRatio(1f)
                    )
                    CircularProgressScore(
                        score = profile.devMetrics.openSourceScore,
                        label = "Open Source",
                        color = Primary,
                        modifier = Modifier.weight(1f).aspectRatio(1f)
                    )
                    CircularProgressScore(
                        score = profile.devMetrics.problemSolvingScore,
                        label = "Solving",
                        color = Tertiary,
                        modifier = Modifier.weight(1f).aspectRatio(1f)
                    )
                }
            }
        }

        // SECTION 7: SKILL DETECTION
        Text(
            text = "Detected Skills",
            color = TextPrimaryDark,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        GlowCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Inferred Tech Stack",
                    color = TextPrimaryDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Parsed based on repo language bindings and description keywords",
                    color = TextSecondaryDark,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    profile.detectedSkills.forEach { skill ->
                        SkillChip(skill)
                    }
                }
            }
        }

        // SECTION 9: AI INSIGHTS
        Text(
            text = "AI Developer Insights",
            color = TextPrimaryDark,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        GlowCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary
                Column {
                    Text(text = "Summary", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = profile.aiInsights.summary, color = TextPrimaryDark, fontSize = 14.sp, lineHeight = 20.sp)
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF222533)))

                // Strengths & Weaknesses
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Strengths", color = Secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        profile.aiInsights.strengths.forEach { str ->
                            BulletPoint(text = str)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Weaknesses", color = Tertiary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        profile.aiInsights.weaknesses.forEach { weak ->
                            BulletPoint(text = weak)
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF222533)))

                // Key metrics
                InsightMetaRow("Project Quality", profile.aiInsights.projectQuality)
                InsightMetaRow("Project Diversity", profile.aiInsights.projectDiversity)
                InsightMetaRow("OS Contribution", profile.aiInsights.openSourceContributionLevel)
                InsightMetaRow("Consistency", profile.aiInsights.consistencyText)

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF222533)))

                // Career Recommendations
                Column {
                    Text(text = "Suitable Roles", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        profile.aiInsights.careerRecommendations.forEach { role ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E212E))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(text = role, color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CircularProgressScore(
    score: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animateFraction by animateFloatAsState(
        targetValue = if (animationPlayed) (score / 100f) else 0f,
        animationSpec = tween(durationMillis = 1000)
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 6.dp.toPx()
                // Background Track
                drawCircle(
                    color = Color(0xFF1E212E),
                    style = Stroke(width = strokeWidth)
                )
                // Score Arc
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = animateFraction * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            Text(
                text = score.toString(),
                color = TextPrimaryDark,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = TextSecondaryDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SkillChip(skill: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF151722))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = skill, color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text = "•", color = Primary, modifier = Modifier.padding(end = 6.dp), fontSize = 14.sp)
        Text(text = text, color = TextPrimaryDark, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

@Composable
fun InsightMetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondaryDark, fontSize = 13.sp)
        Text(text = value, color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
