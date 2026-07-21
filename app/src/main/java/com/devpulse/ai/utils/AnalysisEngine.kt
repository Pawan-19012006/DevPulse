package com.devpulse.ai.utils

import com.devpulse.ai.model.GitHubRepo
import com.devpulse.ai.model.GitHubUser
import com.devpulse.ai.repository.GitHubDataPackage
import java.util.Locale

// Data wrapper containing all calculated metrics for the Dashboard
data class AnalyzedProfile(
    val user: GitHubUser,
    val formattedCreatedAt: String,
    val formattedUpdatedAt: String,
    
    // Section 2 Statistics
    val followersCount: Int,
    val followingCount: Int,
    val publicReposCount: Int,
    val publicGistsCount: Int,
    
    // Section 3 Repository Analysis
    val repoAnalysis: RepoAnalysisMetrics,
    
    // Section 4 Language Analysis
    val languageAnalysis: LanguageAnalysisMetrics,
    
    // Section 5 Project Analysis (Top 5)
    val topProjects: List<GitHubRepo>,
    
    // Section 6 Developer Metrics
    val devMetrics: DeveloperMetricsScores,
    
    // Section 7 Skill Detection
    val detectedSkills: List<String>,
    
    // Section 8 Activity Analysis
    val activityAnalysis: ActivityAnalysisMetrics,
    
    // Section 9 AI Insights
    val aiInsights: DeveloperAiInsights,
    
    // Section 10 Learning Roadmap
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
    val distribution: List<LanguageShare> // Name, percentage
)

data class LanguageShare(
    val name: String,
    val percentage: Float,
    val color: String
)

data class DeveloperMetricsScores(
    val backendScore: Int,
    val frontendScore: Int,
    val aiMlScore: Int,
    val devopsScore: Int,
    val openSourceScore: Int,
    val problemSolvingScore: Int,
    val overallScore: Int
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
    val label: String, // E.g., Year "2023"
    val value: Int
)

data class ActivityAnalysisMetrics(
    val reposCreatedByYear: List<ActivityPoint>,
    val reposUpdatedByYear: List<ActivityPoint>,
    val starsEarned: Int,
    val forksEarned: Int
)

object AnalysisEngine {

    fun analyze(data: GitHubDataPackage): AnalyzedProfile {
        val user = data.user
        val repos = data.repos

        // 1. Date Formats
        val formattedCreated = formatGitHubDate(user.createdAt)
        val formattedUpdated = formatGitHubDate(user.updatedAt)

        // 2. Repo Analysis
        val totalRepos = repos.size
        val totalStars = repos.sumOf { it.stargazersCount }
        val totalForks = repos.sumOf { it.forksCount }
        val avgStars = if (totalRepos > 0) totalStars.toFloat() / totalRepos else 0f
        val avgForks = if (totalRepos > 0) totalForks.toFloat() / totalRepos else 0f

        val largestRepo = repos.maxByOrNull { it.size }
        val mostStarredRepo = repos.maxByOrNull { it.stargazersCount }
        val recentlyUpdatedRepo = repos.maxByOrNull { it.pushedAt }
        val oldestRepo = repos.minByOrNull { it.createdAt }
        val newestRepo = repos.maxByOrNull { it.createdAt }

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

        // 3. Language Analysis
        val languageCounts = mutableMapOf<String, Int>()
        repos.forEach { repo ->
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

        // 4. Top Projects
        val topProjects = repos.sortedByDescending { it.stargazersCount }.take(5)

        // 5. Skill Detection
        val detectedSkills = mutableSetOf<String>()
        val skillKeywords = mapOf(
            "fastapi" to "FastAPI", "django" to "Django", "flask" to "Flask",
            "react" to "React", "angular" to "Angular", "springboot" to "Spring Boot",
            "spring boot" to "Spring Boot", "nodejs" to "Node.js", "node.js" to "Node.js",
            "docker" to "Docker", "kubernetes" to "Kubernetes", "k8s" to "Kubernetes",
            "tensorflow" to "TensorFlow", "pytorch" to "PyTorch", "sql" to "SQL",
            "mongodb" to "MongoDB", "redis" to "Redis", "firebase" to "Firebase",
            "aws" to "AWS", "azure" to "Azure", "gcp" to "GCP", "google cloud" to "GCP",
            "git" to "Git", "github" to "GitHub", "linux" to "Linux"
        )

        // Populate primary languages
        repos.forEach { repo ->
            repo.language?.let { detectedSkills.add(it) }
            val nameLower = repo.name.lowercase(Locale.getDefault())
            val descLower = (repo.description ?: "").lowercase(Locale.getDefault())
            skillKeywords.forEach { (key, value) ->
                if (nameLower.contains(key) || descLower.contains(key)) {
                    detectedSkills.add(value)
                }
            }
        }
        val finalSkills = detectedSkills.take(15).toList() // Limit to 15 key skills

        // 6. Heuristic Scoring (Calculated realistically)
        val hasBackendLangs = repos.any { it.language in listOf("Kotlin", "Java", "Go", "Rust", "Python", "C#", "C++") }
        val hasFrontendLangs = repos.any { it.language in listOf("JavaScript", "TypeScript", "HTML", "CSS") }
        val hasAiLangs = repos.any { it.language == "Python" || it.name.lowercase(Locale.getDefault()).contains("ml") || it.name.lowercase(Locale.getDefault()).contains("ai") }
        
        var backendPoints = 0
        var frontendPoints = 0
        var aiPoints = 0
        var devopsPoints = 0
        
        repos.forEach { repo ->
            val lang = repo.language
            val name = repo.name.lowercase(Locale.getDefault())
            val desc = (repo.description ?: "").lowercase(Locale.getDefault())
            
            // Backend rules
            if (lang in listOf("Kotlin", "Java", "Go", "Rust", "C#", "C++")) {
                backendPoints += 15 + (repo.stargazersCount * 2)
            }
            if (name.contains("server") || name.contains("api") || desc.contains("backend") || desc.contains("database")) {
                backendPoints += 10
            }
            
            // Frontend rules
            if (lang in listOf("JavaScript", "TypeScript", "HTML", "CSS")) {
                frontendPoints += 15 + (repo.stargazersCount * 2)
            }
            if (name.contains("client") || name.contains("web") || name.contains("ui") || desc.contains("frontend") || desc.contains("css")) {
                frontendPoints += 10
            }

            // AI/ML rules
            if (lang == "Python" && (name.contains("learn") || name.contains("model") || desc.contains("pytorch") || desc.contains("tensorflow"))) {
                aiPoints += 25
            }
            if (name.contains("ai") || name.contains("nlp") || name.contains("gpt") || desc.contains("machine learning")) {
                aiPoints += 15
            }

            // DevOps rules
            if (lang == "Go" && (name.contains("infra") || name.contains("k8s") || name.contains("docker"))) {
                devopsPoints += 25
            }
            if (name.contains("docker") || name.contains("deploy") || name.contains("ci") || name.contains("cd") || desc.contains("kubernetes") || desc.contains("workflow")) {
                devopsPoints += 15
            }
        }

        val backendScore = capScore(if (hasBackendLangs) 40 + backendPoints else 10 + backendPoints)
        val frontendScore = capScore(if (hasFrontendLangs) 40 + frontendPoints else 10 + frontendPoints)
        val aiScore = capScore(if (hasAiLangs) 30 + aiPoints else 10 + aiPoints)
        val devopsScore = capScore(25 + devopsPoints)

        // Open Source: Based on stars, forks, followers, and how many repos of theirs are popular
        val osPoints = (totalStars * 3) + (totalForks * 5) + (user.followers * 2)
        val openSourceScore = capScore(20 + osPoints)

        // Problem Solving: Based on average stars, repos count, languages, and repo age
        val psPoints = (avgStars * 10).toInt() + (totalRepos * 2) + finalSkills.size
        val problemSolvingScore = capScore(35 + psPoints)

        // Overall: weighted average
        val overallScore = capScore(
            (backendScore * 0.20f +
             frontendScore * 0.15f +
             aiScore * 0.15f +
             devopsScore * 0.15f +
             openSourceScore * 0.15f +
             problemSolvingScore * 0.20f).toInt()
        )

        val scores = DeveloperMetricsScores(
            backendScore = backendScore,
            frontendScore = frontendScore,
            aiMlScore = aiScore,
            devopsScore = devopsScore,
            openSourceScore = openSourceScore,
            problemSolvingScore = problemSolvingScore,
            overallScore = overallScore
        )

        // 7. Activity Analysis (parse years from dates)
        val createdYears = mutableMapOf<String, Int>()
        val updatedYears = mutableMapOf<String, Int>()

        repos.forEach { repo ->
            val cYear = repo.createdAt.take(4)
            val uYear = repo.pushedAt.take(4)
            if (cYear.all { it.isDigit() }) {
                createdYears[cYear] = (createdYears[cYear] ?: 0) + 1
            }
            if (uYear.all { it.isDigit() }) {
                updatedYears[uYear] = (updatedYears[uYear] ?: 0) + 1
            }
        }

        val activityCreated = createdYears.entries.sortedBy { it.key }.map { ActivityPoint(it.key, it.value) }
        val activityUpdated = updatedYears.entries.sortedBy { it.key }.map { ActivityPoint(it.key, it.value) }

        val activityAnalysis = ActivityAnalysisMetrics(
            reposCreatedByYear = activityCreated.takeLast(5), // Keep last 5 active years
            reposUpdatedByYear = activityUpdated.takeLast(5),
            starsEarned = totalStars,
            forksEarned = totalForks
        )

        // 8. AI Insights (Rules-based Heuristic Generator)
        val summary = "Developer @${user.login} shows high competence in $mostUsed. With a total of $totalRepos public repositories, they have generated a repository base that has accumulated $totalStars stars and $totalForks forks. Their overall engineering score indicates a developer with strong ${getTopDomainName(scores)} expertise."
        
        val strengths = mutableListOf<String>()
        val weaknesses = mutableListOf<String>()
        val skillGaps = mutableListOf<String>()
        val careers = mutableListOf<String>()

        if (backendScore > 65) {
            strengths.add("Robust backend architecture design and language proficiency ($mostUsed).")
            careers.add("Backend Engineer")
        }
        if (frontendScore > 65) {
            strengths.add("Modern responsive UI development and frontend layout integration.")
            careers.add("Frontend Engineer")
        }
        if (openSourceScore > 50) {
            strengths.add("Excellent community engagement and open source contribution impact.")
        } else {
            weaknesses.add("Limited collaborative project distribution (forks & stars counts are low).")
        }

        if (devopsScore < 45) {
            weaknesses.add("Minimal Infrastructure and CI/CD pipelines tooling detected.")
            skillGaps.add("Docker containerization & GitHub Actions automated pipelines.")
            careers.add("DevOps Engineer")
        }
        if (aiScore < 45) {
            skillGaps.add("Machine Learning fundamentals (TensorFlow/PyTorch) and dataset manipulation.")
            careers.add("AI Engineer")
        }

        if (strengths.isEmpty()) strengths.add("Active code writing and project creation consistency.")
        if (weaknesses.isEmpty()) weaknesses.add("Could benefit from writing unit tests and API documentation templates.")
        if (skillGaps.isEmpty()) skillGaps.add("Cloud deployment strategies (AWS/GCP/Azure).")
        if (careers.isEmpty()) {
            careers.add("Full Stack Developer")
        } else if (careers.size == 1) {
            careers.add("Full Stack Developer")
        }

        val projectQuality = when {
            avgStars > 15 -> "High (Highly starred key repositories indicating community validation)"
            avgStars > 3 -> "Moderate (Healthy interaction and star ratings)"
            else -> "Initial (Developer has utility and personal projects requiring additional promotion)"
        }

        val projectDiversity = when {
            languageCounts.size >= 5 -> "High (Proficient across ${languageCounts.size} distinct languages)"
            languageCounts.size >= 3 -> "Moderate (Flexible with ${languageCounts.size} languages)"
            else -> "Specialized (Deeply focused on ${languageCounts.size} core languages)"
        }

        val osContribution = when {
            user.followers > 20 || totalForks > 10 -> "High Contributor"
            totalStars > 2 -> "Moderate Contributor"
            else -> "Solo Developer"
        }

        val consistency = if (repos.any { it.pushedAt.take(4) == "2026" }) {
            "Consistently Active (Pushed code recently in 2026)"
        } else {
            "Periodic (Last push detected in earlier years)"
        }

        val growthText = "Developer accounts show a growth path since $formattedCreated, with active pushes and repository creations peaking in ${activityCreated.maxByOrNull { it.value }?.label ?: "recent years"}."

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

        // 9. Personalized Roadmap Steps
        val learningRoadmap = generateRoadmap(scores, mostUsed, finalSkills)

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
            devMetrics = scores,
            detectedSkills = finalSkills,
            activityAnalysis = activityAnalysis,
            aiInsights = aiInsights,
            learningRoadmap = learningRoadmap
        )
    }

    private fun capScore(score: Int): Int = score.coerceIn(10, 98) // Production apps avoid 100 to feel realistic

    private fun getTopDomainName(scores: DeveloperMetricsScores): String {
        val list = listOf(
            "Backend" to scores.backendScore,
            "Frontend" to scores.frontendScore,
            "AI/ML" to scores.aiMlScore,
            "DevOps" to scores.devopsScore
        )
        return list.maxByOrNull { it.second }?.first ?: "Generalist"
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

    private fun generateRoadmap(
        scores: DeveloperMetricsScores,
        primaryLang: String,
        skills: List<String>
    ): List<RoadmapStep> {
        val roadmap = mutableListOf<RoadmapStep>()

        // Step 1: Language Mastery
        roadmap.add(
            RoadmapStep(
                title = "1. Master Advanced $primaryLang",
                description = "Deepen language understanding. Focus on asynchronous runtimes, multithreading memory management, and advanced features.",
                skills = listOf(primaryLang, "Algorithms", "Optimization")
            )
        )

        // Step 2: DevOps & Containers (If weak)
        if (scores.devopsScore < 50) {
            roadmap.add(
                RoadmapStep(
                    title = "2. Learn Containerization & Orchestration",
                    description = "Package applications using Docker container files and manage deployments with Kubernetes configurations.",
                    skills = listOf("Docker", "Kubernetes", "CI/CD")
                )
            )
        }

        // Step 3: Cloud Architectures
        if (skills.none { it in listOf("AWS", "Azure", "GCP") }) {
            roadmap.add(
                RoadmapStep(
                    title = "3. Deploy to Public Clouds",
                    description = "Migrate simple utilities to AWS or GCP. Utilize serverless configurations, database persistence nodes, and network gateways.",
                    skills = listOf("AWS", "GCP", "Serverless")
                )
            )
        } else {
            roadmap.add(
                RoadmapStep(
                    title = "3. System Architecture Design",
                    description = "Learn distributed caching, microservice synchronization, queue messaging, and high availability systems design.",
                    skills = listOf("System Design", "Redis", "Kafka")
                )
            )
        }

        // Step 4: AI & Future Toolings
        if (scores.aiMlScore < 50) {
            roadmap.add(
                RoadmapStep(
                    title = "4. AI Integration & LLM APIs",
                    description = "Integrate LLM API calls, embeddings, vector databases (Pinecone/Chroma), and prompt configurations into your backend applications.",
                    skills = listOf("Python", "LangChain", "Vector DBs")
                )
            )
        } else {
            roadmap.add(
                RoadmapStep(
                    title = "4. Deep Learning Pipelines",
                    description = "Train neural network models using PyTorch on GPUs. Optimize weights and integrate with inference servers.",
                    skills = listOf("PyTorch", "CUDA", "Model Ops")
                )
            )
        }

        // Step 5: Advanced Engineering
        roadmap.add(
            RoadmapStep(
                title = "5. Deploy & Monitor Production Systems",
                description = "Apply Prometheus monitoring dashboards, logging nodes, telemetry metrics tracking, and load balancing rules.",
                skills = listOf("Prometheus", "Grafana", "Telemetry")
            )
        )

        return roadmap
    }
}
