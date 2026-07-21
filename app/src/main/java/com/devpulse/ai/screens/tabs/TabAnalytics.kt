package com.devpulse.ai.screens.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devpulse.ai.components.GlowCard
import com.devpulse.ai.components.PremiumBarChart
import com.devpulse.ai.components.PremiumDonutChart
import com.devpulse.ai.components.PremiumLineChart
import com.devpulse.ai.ui.theme.Primary
import com.devpulse.ai.ui.theme.Secondary
import com.devpulse.ai.ui.theme.Tertiary
import com.devpulse.ai.ui.theme.TextPrimaryDark
import com.devpulse.ai.ui.theme.TextSecondaryDark
import com.devpulse.ai.utils.AnalyzedProfile
import com.devpulse.ai.utils.LanguageShare

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TabAnalytics(
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
        // SECTION 4: LANGUAGE ANALYSIS
        Text(
            text = "Language Analysis",
            color = TextPrimaryDark,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        GlowCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Donut Chart
                PremiumDonutChart(
                    shares = profile.languageAnalysis.distribution,
                    modifier = Modifier
                        .size(200.dp)
                        .padding(bottom = 16.dp)
                )

                // Top 3 summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LanguageRankItem(rank = "1st", lang = profile.languageAnalysis.mostUsed, color = Primary)
                    LanguageRankItem(rank = "2nd", lang = profile.languageAnalysis.secondMostUsed, color = Secondary)
                    LanguageRankItem(rank = "3rd", lang = profile.languageAnalysis.thirdMostUsed, color = Tertiary)
                }

                Spacer(modifier = Modifier.height(20.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF222533)))
                Spacer(modifier = Modifier.height(20.dp))

                // Language chips distribution
                Text(
                    text = "Language Share Breakdown",
                    color = TextPrimaryDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // FlowRow automatically wraps items to the next line
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    profile.languageAnalysis.distribution.forEach { share ->
                        LanguageChip(share)
                    }
                }
            }
        }

        // SECTION 8: ACTIVITY ANALYSIS
        Text(
            text = "Activity Analysis",
            color = TextPrimaryDark,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        // Line Chart: Repos Created Per Year
        GlowCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Repositories Created Per Year",
                    color = TextPrimaryDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tracks repository count initialization timelines",
                    color = TextSecondaryDark,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                PremiumLineChart(
                    points = profile.activityAnalysis.reposCreatedByYear,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
        }

        // Bar Chart: Repos Updated Per Year
        GlowCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Active Repository Updates",
                    color = TextPrimaryDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tracks pushes and updates across calendar years",
                    color = TextSecondaryDark,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                PremiumBarChart(
                    points = profile.activityAnalysis.reposUpdatedByYear,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
        }
    }
}

@Composable
fun LanguageRankItem(rank: String, lang: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = rank, color = TextSecondaryDark, fontSize = 11.sp)
        Text(text = lang, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LanguageChip(share: LanguageShare) {
    val parsedColor = try {
        Color(android.graphics.Color.parseColor(share.color))
    } catch (e: Exception) {
        Primary
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF151722))
            .background(parsedColor.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(parsedColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = share.name, color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = String.format("%.1f%%", share.percentage), color = TextSecondaryDark, fontSize = 11.sp)
    }
}
