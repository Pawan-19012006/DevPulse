package com.devpulse.ai.screens.tabs

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devpulse.ai.components.GlowCard
import com.devpulse.ai.data.local.entity.SkillEvidenceEntity
import com.devpulse.ai.domain.ComparisonResult
import com.devpulse.ai.ui.theme.Primary
import com.devpulse.ai.ui.theme.Secondary
import com.devpulse.ai.ui.theme.Tertiary
import com.devpulse.ai.ui.theme.TextPrimaryDark
import com.devpulse.ai.ui.theme.TextSecondaryDark
import com.devpulse.ai.utils.AnalyzedProfile

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
        // SECTION 1: REAL DEVELOPER ACTIVITY SIGNALS
        Text(
            text = "Developer Activity Signals",
            color = TextPrimaryDark,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        GlowCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header with Total Signals & Period Comparisons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "30-Day Activity Footprint",
                            color = TextPrimaryDark,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Aggregated from verified Git commits, PRs, issues, and reviews",
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Primary.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${profile.devMetrics.totalSignals} Signals",
                            color = Primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Factual Comparison Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ComparisonCard(
                        label = "7-Day Velocity",
                        comparison = profile.comparison7Days,
                        modifier = Modifier.weight(1f)
                    )
                    ComparisonCard(
                        label = "30-Day Velocity",
                        comparison = profile.comparison30Days,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF222533)))
                Spacer(modifier = Modifier.height(20.dp))

                // Factual Activity Signal Metrics Grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SignalMetricTile(label = "Commits", count = profile.devMetrics.commitsCount, color = Primary, modifier = Modifier.weight(1f))
                    SignalMetricTile(label = "Pull Requests", count = profile.devMetrics.prsCount, color = Secondary, modifier = Modifier.weight(1f))
                    SignalMetricTile(label = "Issues", count = profile.devMetrics.issuesCount, color = Tertiary, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SignalMetricTile(label = "Active Repos", count = profile.devMetrics.activeReposCount, color = Secondary, modifier = Modifier.weight(1f))
                    SignalMetricTile(label = "Active Days", count = profile.devMetrics.activeDaysCount, color = Primary, modifier = Modifier.weight(1f))
                    SignalMetricTile(label = "Code Reviews", count = profile.devMetrics.reviewsCount, color = Tertiary, modifier = Modifier.weight(1f))
                }
            }
        }

        // SECTION 2: VERIFIED SKILL EVIDENCE
        Text(
            text = "Verified Skill Evidence",
            color = TextPrimaryDark,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        GlowCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Technology Evidence Stack",
                    color = TextPrimaryDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Grounded in repository language bytes, commit records, and active dependencies",
                    color = TextSecondaryDark,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (profile.skillEvidences.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        profile.skillEvidences.take(8).forEach { skill ->
                            SkillEvidenceRow(skill)
                        }
                    }
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        profile.detectedSkills.forEach { skill ->
                            SimpleSkillChip(skill)
                        }
                    }
                }
            }
        }

        // SECTION 3: FACTUAL DEVELOPER INSIGHTS
        Text(
            text = "Developer Intelligence Insights",
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

                // Strengths & Observation Areas
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Demonstrated Strengths", color = Secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        profile.aiInsights.strengths.forEach { str ->
                            BulletPoint(text = str)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Observation Areas", color = Tertiary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        profile.aiInsights.weaknesses.forEach { weak ->
                            BulletPoint(text = weak)
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF222533)))

                // Key factual metrics
                InsightMetaRow("Project Quality", profile.aiInsights.projectQuality)
                InsightMetaRow("Project Diversity", profile.aiInsights.projectDiversity)
                InsightMetaRow("OS Contribution", profile.aiInsights.openSourceContributionLevel)
                InsightMetaRow("Activity Cadence", profile.aiInsights.consistencyText)

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF222533)))

                // Role Alignments
                Column {
                    Text(text = "Stack-Aligned Engineering Profiles", color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
fun SignalMetricTile(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF151722))
            .padding(12.dp)
    ) {
        Column {
            Text(text = count.toString(), color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, color = TextSecondaryDark, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ComparisonCard(
    label: String,
    comparison: ComparisonResult<Int>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF151722))
            .padding(10.dp)
    ) {
        Column {
            Text(text = label, color = TextSecondaryDark, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            when (comparison) {
                is ComparisonResult.Available -> {
                    val pct = comparison.percentageChange
                    if (pct != null) {
                        val sign = if (pct >= 0) "+" else ""
                        val color = if (pct >= 0) Primary else Tertiary
                        Text(
                            text = "$sign${String.format("%.0f", pct)}% vs prior",
                            color = color,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Steady",
                            color = TextPrimaryDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                is ComparisonResult.InsufficientHistory -> {
                    Text(
                        text = "INSUFFICIENT HISTORY",
                        color = TextSecondaryDark,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SkillEvidenceRow(skill: SkillEvidenceEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF151722))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = skill.skillName,
                color = TextPrimaryDark,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            val subText = if (skill.primaryLanguageRepoCount > 0) {
                "${skill.primaryLanguageRepoCount} repos • ${skill.evidenceCount} signals"
            } else {
                "${skill.evidenceCount} signals observed"
            }
            Text(
                text = subText,
                color = TextSecondaryDark,
                fontSize = 11.sp
            )
        }

        val badgeColor = when (skill.confidenceLevel) {
            "STRONG" -> Primary
            "MODERATE" -> Secondary
            else -> TextSecondaryDark
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(badgeColor.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = skill.confidenceLevel,
                color = badgeColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SimpleSkillChip(skill: String) {
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
