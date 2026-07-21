package com.devpulse.ai.screens.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.devpulse.ai.components.GlowCard
import com.devpulse.ai.ui.theme.Primary
import com.devpulse.ai.ui.theme.Secondary
import com.devpulse.ai.ui.theme.Tertiary
import com.devpulse.ai.ui.theme.TextPrimaryDark
import com.devpulse.ai.ui.theme.TextSecondaryDark
import com.devpulse.ai.utils.AnalyzedProfile
import com.devpulse.ai.utils.RepoAnalysisMetrics

@Composable
fun TabDashboard(
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
        // SECTION 1: PROFILE HEADER
        ProfileHeaderSection(profile)

        // SECTION 2: PROFILE STATISTICS
        ProfileStatisticsSection(profile)

        // SECTION 3: REPOSITORY ANALYSIS
        RepositoryAnalysisSection(profile.repoAnalysis)

        // SECTION 5: PROJECT ANALYSIS (Top 5 Repositories)
        TopProjectsSection(profile)
    }
}

@Composable
fun ProfileHeaderSection(profile: AnalyzedProfile) {
    val user = profile.user
    GlowCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Image with Coil
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E212E)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name ?: user.login,
                        color = TextPrimaryDark,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "@${user.login}",
                        color = Primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    // Hireable Status Badge
                    if (user.hireable == true) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF03DAC6).copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Open to Work",
                                color = Secondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (!user.bio.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = user.bio,
                    color = TextPrimaryDark,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF222533))
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Metadata items
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!user.company.isNullOrBlank()) {
                    MetadataRow(label = "Company", value = user.company)
                }
                if (!user.location.isNullOrBlank()) {
                    MetadataRow(label = "Location", value = user.location)
                }
                if (!user.email.isNullOrBlank()) {
                    MetadataRow(label = "Email", value = user.email)
                }
                if (!user.blog.isNullOrBlank()) {
                    MetadataRow(label = "Website", value = user.blog)
                }
                if (!user.twitterUsername.isNullOrBlank()) {
                    MetadataRow(label = "Twitter", value = "@${user.twitterUsername}")
                }
            }
        }
    }
}

@Composable
fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondaryDark, fontSize = 13.sp)
        Text(
            text = value,
            color = TextPrimaryDark,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 200.dp)
        )
    }
}

@Composable
fun ProfileStatisticsSection(profile: AnalyzedProfile) {
    Column {
        Text(
            text = "Profile Statistics",
            color = TextPrimaryDark,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(label = "Followers", value = profile.followersCount.toString(), modifier = Modifier.weight(1f))
            StatCard(label = "Following", value = profile.followingCount.toString(), modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(label = "Public Repos", value = profile.publicReposCount.toString(), modifier = Modifier.weight(1f))
            StatCard(label = "Public Gists", value = profile.publicGistsCount.toString(), modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(label = "Joined On", value = profile.formattedCreatedAt, modifier = Modifier.weight(1f))
            StatCard(label = "Last Active", value = profile.formattedUpdatedAt, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    GlowCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = label, color = TextSecondaryDark, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, color = TextPrimaryDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RepositoryAnalysisSection(metrics: RepoAnalysisMetrics) {
    Column {
        Text(
            text = "Repository Analysis",
            color = TextPrimaryDark,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        GlowCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricRow(label = "Total Stars", value = metrics.totalStars.toString(), highlightColor = Primary)
                MetricRow(label = "Total Forks", value = metrics.totalForks.toString(), highlightColor = Secondary)
                MetricRow(label = "Average Stars", value = String.format("%.2f", metrics.averageStars), highlightColor = TextPrimaryDark)
                MetricRow(label = "Average Forks", value = String.format("%.2f", metrics.averageForks), highlightColor = TextPrimaryDark)

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF222533)))

                metrics.mostStarredRepo?.let {
                    RepositoryStatRow(label = "Most Starred", name = it.name, stat = "${it.stargazersCount} ★")
                }
                metrics.largestRepo?.let {
                    RepositoryStatRow(label = "Largest Repo", name = it.name, stat = "${it.size / 1024} MB")
                }
                metrics.recentlyUpdatedRepo?.let {
                    RepositoryStatRow(label = "Recent Update", name = it.name, stat = it.pushedAt.take(10))
                }
                metrics.newestRepo?.let {
                    RepositoryStatRow(label = "Newest Repo", name = it.name, stat = it.createdAt.take(4))
                }
                metrics.oldestRepo?.let {
                    RepositoryStatRow(label = "Oldest Repo", name = it.name, stat = it.createdAt.take(4))
                }
            }
        }
    }
}

@Composable
fun MetricRow(label: String, value: String, highlightColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondaryDark, fontSize = 14.sp)
        Text(text = value, color = highlightColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RepositoryStatRow(label: String, name: String, stat: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = TextSecondaryDark, fontSize = 11.sp)
            Text(
                text = name,
                color = TextPrimaryDark,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(text = stat, color = Tertiary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TopProjectsSection(profile: AnalyzedProfile) {
    Column {
        Text(
            text = "Top 5 Repositories",
            color = TextPrimaryDark,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            profile.topProjects.forEach { repo ->
                GlowCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = repo.name,
                                color = Primary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF222533))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (repo.isPrivate) "Private" else "Public",
                                    color = TextSecondaryDark,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (!repo.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = repo.description,
                                color = TextPrimaryDark,
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repo.language?.let {
                                Text(text = it, color = Secondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            Text(text = "★ ${repo.stargazersCount}", color = TextSecondaryDark, fontSize = 12.sp)
                            Text(text = "⑂ ${repo.forksCount}", color = TextSecondaryDark, fontSize = 12.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "Updated: ${repo.pushedAt.take(10)}",
                                color = TextSecondaryDark,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
