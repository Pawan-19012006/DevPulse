package com.devpulse.ai.utils

import com.devpulse.ai.data.local.entity.*
import com.devpulse.ai.domain.*
import com.devpulse.ai.model.GitHubRepo
import com.devpulse.ai.model.GitHubRepoOwner
import com.devpulse.ai.model.GitHubUser
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data wrapper containing real factual metrics for the UI
data class AnalyzedProfile(
    val user: GitHubUser,
    val formattedCreatedAt: String,
    val formattedUpdatedAt: String,

    // Profile Statistics
    val followersCount: Int,
    val followingCount: Int,
    val publicReposCount: Int,
    val publicGistsCount: Int,

    // Repository Analysis
    val repoAnalysis: RepoAnalysisMetrics,

    // Language Analysis
    val languageAnalysis: LanguageAnalysisMetrics,

    // Top Projects
    val topProjects: List<GitHubRepo>,

    // Real Developer Activity Signals & Metrics
    val devMetrics: DeveloperMetricsScores,

    // Skill Evidences
    val detectedSkills: List<String>,
    val skillEvidences: List<SkillEvidenceEntity>,

    // Real Activity Signals
    val activityAnalysis: ActivityAnalysisMetrics,
    val activitySignals: ActivitySignals,
    val comparison7Days: ComparisonResult<Int>,
    val comparison30Days: ComparisonResult<Int>,

    // AI Insights (100% grounded in real activity)
    val aiInsights: DeveloperAiInsights,

    // Dynamic Learning Roadmap based on detected skills
    val learningRoadmap: List<RoadmapStep>
)

data class RepoAnalysisMetrics(
    val totalRepos: Int,
    val totalStars: Int,
    val totalForks: Int,
    val averageStars: Float,
    val averageForks: Float,
    val largestRepo: GitHubRepo?,
    val mostStarredRepo: GitHubRepo?,
    val recentlyUpdatedRepo: GitHubRepo?,
    val oldestRepo: GitHubRepo?,
    val newestRepo: GitHubRepo?
)

data class LanguageAnalysisMetrics(
    val mostUsed: String,
    val secondMostUsed: String,
    val thirdMostUsed: String,
    val distribution: List<LanguageShare>
)

data class LanguageShare(
    val name: String,
    val percentage: Float,
    val color: String
)

data class DeveloperMetricsScores(
    val commitsCount: Int,
    val prsCount: Int,
    val issuesCount: Int,
    val activeReposCount: Int,
    val activeDaysCount: Int,
    val reviewsCount: Int,
    val totalSignals: Int
)

data class DeveloperAiInsights(
    val summary: String,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val skillGaps: List<String>,
    val projectQuality: String,
    val projectDiversity: String,
    val openSourceContributionLevel: String,
    val consistencyText: String,
    val growthText: String,
    val careerRecommendations: List<String>
)

data class RoadmapStep(
    val title: String,
    val description: String,
    val skills: List<String>
)

data class ActivityPoint(
    val label: String,
    val value: Int
)

data class ActivityAnalysisMetrics(
    val reposCreatedByYear: List<ActivityPoint>,
    val reposUpdatedByYear: List<ActivityPoint>,
    val starsEarned: Int,
    val forksEarned: Int
)

object AnalysisEngine {

    private val gson = Gson()
    private val stringListType = object : TypeToken<List<String>>() {}.type

    fun buildAnalyzedProfile(
        profile: DeveloperProfileEntity,
        repos: List<RepositoryEntity>,
        events: List<DeveloperEventEntity>,
        skills: List<SkillEvidenceEntity>,
        snapshots: List<ActivitySnapshotEntity>,
        now: Long = System.currentTimeMillis()
    ): AnalyzedProfile {
        val user = GitHubUser(
            login = profile.username,
            avatarUrl = profile.avatarUrl,
            name = profile.name,
            bio = profile.bio,
            company = profile.company,
            location = profile.location,
            blog = profile.blog,
            twitterUsername = profile.twitterUsername,
            hireable = profile.hireable,
            followers = profile.followers,
            following = profile.following,
            publicRepos = profile.publicRepos,
            publicGists = profile.publicGists,
            createdAt = profile.githubCreatedAt,
            updatedAt = profile.githubUpdatedAt,
            email = profile.email
        )

        val formattedCreated = formatGitHubDate(profile.githubCreatedAt)
        val formattedUpdated = formatGitHubDate(profile.githubUpdatedAt)

        // Convert RepositoryEntity to GitHubRepo for UI compatibility
        val mappedRepos = repos.map { r ->
            GitHubRepo(
                id = r.id,
                name = r.name,
                fullName = r.fullName,
                owner = GitHubRepoOwner(r.owner, profile.avatarUrl),
                description = r.description,
                language = r.primaryLanguage,
                stargazersCount = r.stargazersCount,
                forksCount = r.forksCount,
                watchersCount = r.forksCount,
                openIssuesCount = r.openIssuesCount,
                isFork = r.isFork,
                isArchived = r.isArchived,
                defaultBranch = r.defaultBranch,
                createdAt = formatEpochToIso(r.createdAt),
                updatedAt = formatEpochToIso(r.updatedAt),
                pushedAt = formatEpochToIso(r.pushedAt),
                size = r.sizeKb,
                topics = parseTopics(r.topicsJson),
                private = false
            )
        }

        // Repository metrics
        val totalRepos = mappedRepos.size
        val totalStars = mappedRepos.sumOf { it.stargazersCount }
        val totalForks = mappedRepos.sumOf { it.forksCount }
        val avgStars = if (totalRepos > 0) totalStars.toFloat() / totalRepos else 0f
        val avgForks = if (totalRepos > 0) totalForks.toFloat() / totalRepos else 0f

        val largestRepo = mappedRepos.maxByOrNull { it.size }
        val mostStarredRepo = mappedRepos.maxByOrNull { it.stargazersCount }
        val recentlyUpdatedRepo = mappedRepos.maxByOrNull { it.pushedAt ?: "" }
        val oldestRepo = mappedRepos.filter { !it.createdAt.isNullOrBlank() }.minByOrNull { it.createdAt ?: "" }
        val newestRepo = mappedRepos.filter { !it.createdAt.isNullOrBlank() }.maxByOrNull { it.createdAt ?: "" }

        val repoMetrics = RepoAnalysisMetrics(
            totalRepos = totalRepos,
            totalStars = totalStars,
            totalForks = totalForks,
            averageStars = avgStars,
            averageForks = avgForks,
            largestRepo = largestRepo,
            mostStarredRepo = mostStarredRepo,
            recentlyUpdatedRepo = recentlyUpdatedRepo,
            oldestRepo = oldestRepo,
            newestRepo = newestRepo
        )

        // Language analysis
        val languageCounts = mutableMapOf<String, Int>()
        mappedRepos.forEach { repo ->
            repo.language?.let { lang ->
                languageCounts[lang] = (languageCounts[lang] ?: 0) + 1
            }
        }

        val sortedLanguages = languageCounts.entries.sortedByDescending { it.value }
        val mostUsed = sortedLanguages.getOrNull(0)?.key ?: "None"
        val secondMostUsed = sortedLanguages.getOrNull(1)?.key ?: "None"
        val thirdMostUsed = sortedLanguages.getOrNull(2)?.key ?: "None"

        val totalLanguagesCount = languageCounts.values.sum().toFloat()
        val languageColors = mapOf(
            "Kotlin" to "#A855F7",
            "Java" to "#B07219",
            "Python" to "#3572A5",
            "JavaScript" to "#F1E05A",
            "TypeScript" to "#3178C6",
            "Go" to "#00ADD8",
            "Rust" to "#DEA584",
            "C++" to "#F34B7D",
            "C#" to "#178600",
            "HTML" to "#E34C26",
            "CSS" to "#563D7C",
            "Ruby" to "#701516",
            "Swift" to "#F05138"
        )
        val defaultColors = listOf("#EC4899", "#8B5CF6", "#3B82F6", "#10B981", "#F59E0B", "#EF4444")

        val languageShares = sortedLanguages.mapIndexed { index, entry ->
            val percentage = if (totalLanguagesCount > 0) (entry.value / totalLanguagesCount) * 100f else 0f
            LanguageShare(
                name = entry.key,
                percentage = percentage,
                color = languageColors[entry.key] ?: defaultColors[index % defaultColors.size]
            )
        }

        val languageMetrics = LanguageAnalysisMetrics(
            mostUsed = mostUsed,
            secondMostUsed = secondMostUsed,
            thirdMostUsed = thirdMostUsed,
            distribution = languageShares
        )

        val topProjects = mappedRepos.sortedByDescending { it.stargazersCount }.take(5)

        // Compute real activity signals
        val signals30d = ActivityAggregator.computeSignals(events, TimeWindow.Last30Days(now))
        val signals7d = ActivityAggregator.computeSignals(events, TimeWindow.Last7Days(now))
        val prev7d = ActivityAggregator.computeSignals(events, TimeWindow.Previous7Days(now))
        val prev30d = ActivityAggregator.computeSignals(events, TimeWindow.Previous30Days(now))

        val hasPrior7dData = events.any { it.timestamp in TimeWindow.Previous7Days(now).startTime..TimeWindow.Previous7Days(now).endTime }
        val hasPrior30dData = events.any { it.timestamp in TimeWindow.Previous30Days(now).startTime..TimeWindow.Previous30Days(now).endTime }

        val comparison7Days = ActivityAggregator.compareMetric(
            signals7d.commitsCount,
            prev7d.commitsCount,
            hasHistoricalData = hasPrior7dData
        )

        val comparison30Days = ActivityAggregator.compareMetric(
            signals30d.commitsCount,
            prev30d.commitsCount,
            hasHistoricalData = hasPrior30dData
        )

        val devMetrics = DeveloperMetricsScores(
            commitsCount = signals30d.commitsCount,
            prsCount = signals30d.prsOpenedCount + signals30d.prsMergedCount,
            issuesCount = signals30d.issuesOpenedCount + signals30d.issuesClosedCount,
            activeReposCount = signals30d.activeRepositoriesCount,
            activeDaysCount = signals30d.activeDaysCount,
            reviewsCount = signals30d.reviewsCount,
            totalSignals = signals30d.totalSignals
        )

        // Detected skills from SkillEvidenceEntity
        val detectedSkills = skills.map { it.skillName }

        // Activity points by year (derived from actual creation & push dates)
        val createdYears = mutableMapOf<String, Int>()
        val updatedYears = mutableMapOf<String, Int>()
        val yearFormat = SimpleDateFormat("yyyy", Locale.US)

        repos.forEach { repo ->
            if (repo.createdAt > 0) {
                val cYear = yearFormat.format(Date(repo.createdAt))
                createdYears[cYear] = (createdYears[cYear] ?: 0) + 1
            }
            if (repo.pushedAt > 0) {
                val uYear = yearFormat.format(Date(repo.pushedAt))
                updatedYears[uYear] = (updatedYears[uYear] ?: 0) + 1
            }
        }

        val activityAnalysis = ActivityAnalysisMetrics(
            reposCreatedByYear = createdYears.entries.sortedBy { it.key }.map { ActivityPoint(it.key, it.value) }.takeLast(5),
            reposUpdatedByYear = updatedYears.entries.sortedBy { it.key }.map { ActivityPoint(it.key, it.value) }.takeLast(5),
            starsEarned = totalStars,
            forksEarned = totalForks
        )

        // Factual AI insights (derived strictly from collected real data)
        val summary = "Developer @${user.login} maintains $totalRepos public repositories with primary language focus on $mostUsed. In recorded activity, they logged ${signals30d.commitsCount} commits across ${signals30d.activeRepositoriesCount} repositories."

        val strengths = mutableListOf<String>()
        val weaknesses = mutableListOf<String>()
        val skillGaps = mutableListOf<String>()
        val careers = mutableListOf<String>()

        val strongSkills = skills.filter { it.confidenceLevel == "STRONG" }.map { it.skillName }
        if (strongSkills.isNotEmpty()) {
            strengths.add("Established competencies with verified multi-repo evidence: ${strongSkills.take(3).joinToString(", ")}.")
        }
        if (signals30d.activeDaysCount >= 5) {
            strengths.add("Active code writing cadence (${signals30d.activeDaysCount} distinct active days recorded in the last 30 days).")
        }
        if (totalStars > 0) {
            strengths.add("Public open source validation with $totalStars total stars across repositories.")
        } else {
            weaknesses.add("Zero community stars detected on public repositories.")
        }

        if (signals30d.prsOpenedCount + signals30d.prsMergedCount == 0) {
            weaknesses.add("No recent Pull Request activity observed in the current tracking window.")
        }
        if (signals30d.reviewsCount == 0) {
            weaknesses.add("No code review participation recorded in the current activity stream.")
        }

        val commonDevOps = listOf("Docker", "Kubernetes", "Ci/cd", "Github Actions")
        if (skills.none { it.skillName in commonDevOps }) {
            skillGaps.add("DevOps & CI/CD workflow automation")
        }
        val commonCloud = listOf("Aws", "Gcp", "Azure")
        if (skills.none { it.skillName in commonCloud }) {
            skillGaps.add("Public cloud deployment & infrastructure")
        }

        if (strengths.isEmpty()) strengths.add("Active repository maintenance on GitHub.")
        if (weaknesses.isEmpty()) weaknesses.add("Could benefit from more frequent public contribution events.")
        if (skillGaps.isEmpty()) skillGaps.add("Advanced distributed systems and asynchronous orchestration.")

        // Grounded career roles based on actual detected skills
        when {
            mostUsed in listOf("Kotlin", "Swift") -> {
                careers.add("Mobile Engineer")
                careers.add("Android / iOS Developer")
            }
            mostUsed in listOf("Python") -> {
                careers.add("Backend Engineer")
                careers.add("Data / AI Engineer")
            }
            mostUsed in listOf("JavaScript", "TypeScript", "HTML", "CSS") -> {
                careers.add("Frontend Engineer")
                careers.add("Full Stack Developer")
            }
            mostUsed in listOf("Go", "Rust") -> {
                careers.add("Systems Engineer")
                careers.add("Cloud Infrastructure Engineer")
            }
            else -> {
                careers.add("Software Engineer")
                careers.add("Full Stack Developer")
            }
        }

        val projectQuality = when {
            avgStars > 10 -> "High (Averaging ${String.format("%.1f", avgStars)} stars per repository)"
            avgStars > 1 -> "Moderate (Active community interaction and stars)"
            else -> "Personal / Utility (${totalRepos} repositories currently maintained)"
        }

        val projectDiversity = when {
            languageCounts.size >= 5 -> "High (Spanning ${languageCounts.size} distinct languages)"
            languageCounts.size >= 2 -> "Moderate (${languageCounts.size} languages utilized)"
            else -> "Focused (Concentrated primarily in $mostUsed)"
        }

        val osContribution = when {
            user.followers > 20 || totalForks > 10 -> "Active Community Contributor"
            totalStars > 5 -> "Growing Contributor"
            else -> "Independent Developer"
        }

        val consistency = if (signals30d.activeDaysCount > 0) {
            "Active (${signals30d.activeDaysCount} active days in the last 30 days)"
        } else {
            "Periodic (No recorded events in the last 30-day window)"
        }

        val growthText = "Account registered on $formattedCreated with $totalRepos public repositories and $totalStars stars earned to date."

        val aiInsights = DeveloperAiInsights(
            summary = summary,
            strengths = strengths.take(3),
            weaknesses = weaknesses.take(2),
            skillGaps = skillGaps.take(2),
            projectQuality = projectQuality,
            projectDiversity = projectDiversity,
            openSourceContributionLevel = osContribution,
            consistencyText = consistency,
            growthText = growthText,
            careerRecommendations = careers.take(2)
        )

        // Dynamic roadmap grounded in actual detected languages
        val learningRoadmap = generateDynamicRoadmap(mostUsed, skills)

        return AnalyzedProfile(
            user = user,
            formattedCreatedAt = formattedCreated,
            formattedUpdatedAt = formattedUpdated,
            followersCount = user.followers,
            followingCount = user.following,
            publicReposCount = user.publicRepos,
            publicGistsCount = user.publicGists,
            repoAnalysis = repoMetrics,
            languageAnalysis = languageMetrics,
            topProjects = topProjects,
            devMetrics = devMetrics,
            detectedSkills = detectedSkills,
            skillEvidences = skills,
            activityAnalysis = activityAnalysis,
            activitySignals = signals30d,
            comparison7Days = comparison7Days,
            comparison30Days = comparison30Days,
            aiInsights = aiInsights,
            learningRoadmap = learningRoadmap
        )
    }

    private fun generateDynamicRoadmap(
        primaryLang: String,
        skills: List<SkillEvidenceEntity>
    ): List<RoadmapStep> {
        val roadmap = mutableListOf<RoadmapStep>()

        roadmap.add(
            RoadmapStep(
                title = "1. Advance $primaryLang Proficiency",
                description = "Focus on performance benchmarking, asynchronous primitives, and architectural design patterns in $primaryLang.",
                skills = listOf(primaryLang, "Architecture", "Performance")
            )
        )

        val skillNames = skills.map { it.skillName }
        if (!skillNames.any { it.contains("Test", ignoreCase = true) }) {
            roadmap.add(
                RoadmapStep(
                    title = "2. Automated Testing & Reliability",
                    description = "Integrate comprehensive unit testing, mock fixtures, and mutation testing suites into active repositories.",
                    skills = listOf("Unit Testing", "Mocking", "CI")
                )
            )
        }

        if (!skillNames.any { it.contains("Docker", ignoreCase = true) || it.contains("Actions", ignoreCase = true) }) {
            roadmap.add(
                RoadmapStep(
                    title = "3. CI/CD & Automated Pipelines",
                    description = "Automate linting, unit tests, and artifact publishing using GitHub Actions workflows.",
                    skills = listOf("GitHub Actions", "Docker", "Automation")
                )
            )
        }

        roadmap.add(
            RoadmapStep(
                title = "4. Open Source Collaboration",
                description = "Participate in pull request reviews and contribute upstream bug fixes to ecosystem dependencies.",
                skills = listOf("Code Review", "PR Workflows", "Open Source")
            )
        )

        return roadmap
    }

    private fun formatGitHubDate(dateStr: String): String {
        if (dateStr.length < 10) return dateStr
        val year = dateStr.substring(0, 4)
        val month = dateStr.substring(5, 7)
        val day = dateStr.substring(8, 10)
        val monthName = when (month) {
            "01" -> "Jan"
            "02" -> "Feb"
            "03" -> "Mar"
            "04" -> "Apr"
            "05" -> "May"
            "06" -> "Jun"
            "07" -> "Jul"
            "08" -> "Aug"
            "09" -> "Sep"
            "10" -> "Oct"
            "11" -> "Nov"
            "12" -> "Dec"
            else -> month
        }
        return "$monthName $day, $year"
    }

    private fun formatEpochToIso(millis: Long): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        return format.format(Date(millis))
    }

    private fun parseTopics(topicsJson: String): List<String> {
        return try {
            gson.fromJson(topicsJson, stringListType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
