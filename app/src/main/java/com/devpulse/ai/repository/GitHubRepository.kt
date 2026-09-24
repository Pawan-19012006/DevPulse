package com.devpulse.ai.repository

import com.devpulse.ai.auth.BuildConfigTokenProvider
import com.devpulse.ai.auth.TokenProvider
import com.devpulse.ai.data.local.DevPulseDatabase
import com.devpulse.ai.data.local.entity.*
import com.devpulse.ai.domain.*
import com.devpulse.ai.network.GitHubApiService
import com.devpulse.ai.network.GitHubPaginator
import com.devpulse.ai.utils.EventNormalizer
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class GitHubRepository(
    private val tokenProvider: TokenProvider = BuildConfigTokenProvider(),
    private val apiService: GitHubApiService = GitHubApiService.create(tokenProvider),
    private val database: DevPulseDatabase
) {
    private val gson = Gson()

    suspend fun syncDeveloperData(username: String, forceRefresh: Boolean = false): SyncStatus = withContext(Dispatchers.IO) {
        if (!tokenProvider.hasConfiguredToken()) {
            return@withContext SyncStatus.Failed(
                error = "GitHub access token not configured. Please configure your token in local.properties.",
                errorType = SyncErrorType.TOKEN_MISSING
            )
        }

        val cleanUsername = username.trim()
        val now = System.currentTimeMillis()

        // Incremental sync check: if synced recently and not forced, succeed early
        val existingProfile = database.developerProfileDao().getProfile(cleanUsername)
        if (!forceRefresh && existingProfile != null && (now - existingProfile.lastSyncedAt) < 5 * 60 * 1000L) {
            return@withContext SyncStatus.Success(existingProfile.lastSyncedAt)
        }

        var partialWarning: String? = null

        try {
            // 1. Fetch user profile
            val user = apiService.getUser(cleanUsername)
            val profileEntity = DeveloperProfileEntity(
                username = user.login,
                name = user.name,
                avatarUrl = user.avatarUrl,
                bio = user.bio,
                company = user.company,
                location = user.location,
                email = user.email,
                blog = user.blog,
                twitterUsername = user.twitterUsername,
                hireable = user.hireable,
                followers = user.followers,
                following = user.following,
                publicRepos = user.publicRepos,
                publicGists = user.publicGists,
                githubCreatedAt = user.createdAt,
                githubUpdatedAt = user.updatedAt,
                lastSyncedAt = now
            )
            database.developerProfileDao().upsertProfile(profileEntity)

            // 2. Fetch all repositories using reusable pagination
            val allRepos = GitHubPaginator.fetchAll(
                maxPages = 10,
                expectedPageSize = 100
            ) { page ->
                apiService.getRepos(username = cleanUsername, page = page, perPage = 100)
            }

            if (allRepos.isEmpty()) {
                return@withContext SyncStatus.Failed(
                    error = "User @$cleanUsername has no public repositories to analyze.",
                    errorType = SyncErrorType.EMPTY_REPOS
                )
            }

            // 3. Upsert repositories into Room
            val repoEntities = allRepos.map { repo ->
                RepositoryEntity(
                    id = repo.id,
                    owner = repo.owner?.login ?: cleanUsername,
                    name = repo.name,
                    fullName = repo.fullName ?: "${cleanUsername}/${repo.name}",
                    description = repo.description,
                    primaryLanguage = repo.language,
                    stargazersCount = repo.stargazersCount,
                    forksCount = repo.forksCount,
                    openIssuesCount = repo.openIssuesCount,
                    sizeKb = repo.size,
                    isFork = repo.isFork,
                    isArchived = repo.isArchived,
                    defaultBranch = repo.defaultBranch ?: "main",
                    createdAt = EventNormalizer.parseIsoToEpochMillis(repo.createdAt),
                    updatedAt = EventNormalizer.parseIsoToEpochMillis(repo.updatedAt),
                    pushedAt = EventNormalizer.parseIsoToEpochMillis(repo.pushedAt),
                    topicsJson = gson.toJson(repo.topics ?: emptyList<String>()),
                    languagesJson = "{}",
                    lastSyncedAt = now
                )
            }
            database.repositoryDao().upsertRepositories(repoEntities)

            // 4. Capture language breakdowns for top 5 active repositories
            val topActiveRepos = allRepos
                .filter { !it.isFork }
                .sortedByDescending { it.pushedAt ?: "" }
                .take(5)

            for (repo in topActiveRepos) {
                try {
                    val ownerLogin = repo.owner?.login ?: cleanUsername
                    val langBytes = apiService.getRepoLanguages(ownerLogin, repo.name)
                    if (langBytes.isNotEmpty()) {
                        val languagesJson = gson.toJson(langBytes)
                        val existing = database.repositoryDao().getRepositoriesForOwner(cleanUsername)
                            .find { it.id == repo.id }
                        if (existing != null) {
                            database.repositoryDao().upsertRepositories(
                                listOf(existing.copy(languagesJson = languagesJson, lastSyncedAt = now))
                            )
                        }
                    }
                } catch (e: Exception) {
                    partialWarning = "Language byte details partially incomplete: ${e.message}"
                }
            }

            // 5. Fetch developer events
            try {
                val rawEvents = apiService.getUserEvents(cleanUsername, page = 1, perPage = 100)
                val normalizedEvents = rawEvents.flatMap { EventNormalizer.normalizeGitHubEvent(it, cleanUsername) }
                val eventEntities = normalizedEvents.map { ev ->
                    DeveloperEventEntity(
                        id = ev.id,
                        developerUsername = ev.developerUsername,
                        repositoryName = ev.repositoryName,
                        eventType = ev.eventType.name,
                        timestamp = ev.timestamp,
                        actor = ev.actor,
                        summary = ev.summary,
                        additions = ev.additions,
                        deletions = ev.deletions,
                        changedFiles = ev.changedFiles,
                        language = ev.language,
                        source = ev.source,
                        metadataJson = ev.metadataJson
                    )
                }
                database.developerEventDao().insertEvents(eventEntities)
            } catch (e: Exception) {
                partialWarning = "Developer activity events partially incomplete: ${e.message}"
            }

            // 6. Compute Skill Evidence from factual repos and events
            val storedRepos = database.repositoryDao().getRepositoriesForOwner(cleanUsername)
            val storedEvents = database.developerEventDao().getEventsForDeveloper(cleanUsername)
            val skills = SkillEvidenceEngine.evaluateSkills(cleanUsername, storedRepos, storedEvents, now)
            val skillEntities = skills.map { SkillEvidenceEngine.toEntity(cleanUsername, it) }
            database.skillEvidenceDao().deleteSkillsForDeveloper(cleanUsername)
            database.skillEvidenceDao().upsertSkills(skillEntities)

            // 7. Compute Snapshot
            val signals30d = ActivityAggregator.computeSignals(storedEvents, TimeWindow.Last30Days(now))
            val languagesMap = mutableMapOf<String, Int>()
            storedRepos.forEach { r ->
                r.primaryLanguage?.let { l ->
                    languagesMap[l] = (languagesMap[l] ?: 0) + 1
                }
            }

            val snapshot = ActivitySnapshotEntity(
                id = "${cleanUsername}_$now",
                developerUsername = cleanUsername,
                timestamp = now,
                totalRepositories = storedRepos.size,
                activeRepositoriesCount = signals30d.activeRepositoriesCount,
                totalStars = storedRepos.sumOf { it.stargazersCount },
                totalForks = storedRepos.sumOf { it.forksCount },
                commitsCount = signals30d.commitsCount,
                prsOpenedCount = signals30d.prsOpenedCount,
                prsMergedCount = signals30d.prsMergedCount,
                issuesOpenedCount = signals30d.issuesOpenedCount,
                issuesClosedCount = signals30d.issuesClosedCount,
                languagesJson = gson.toJson(languagesMap),
                topLanguage = languagesMap.maxByOrNull { it.value }?.key,
                activeDaysLast30Days = signals30d.activeDaysCount
            )
            database.activitySnapshotDao().insertSnapshot(snapshot)

            return@withContext if (partialWarning != null) {
                SyncStatus.PartialSuccess(now, partialWarning)
            } else {
                SyncStatus.Success(now)
            }

        } catch (e: HttpException) {
            val errorBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
            val errorMessage = try {
                if (!errorBody.isNullOrBlank()) {
                    com.google.gson.JsonParser.parseString(errorBody).asJsonObject.get("message").asString
                } else {
                    e.message() ?: "API Error"
                }
            } catch (_: Exception) {
                e.message() ?: "API Error"
            }

            when (e.code()) {
                404 -> SyncStatus.Failed("User @$cleanUsername not found on GitHub", SyncErrorType.USER_NOT_FOUND)
                403 -> {
                    val rateLimitHeader = e.response()?.headers()?.get("X-RateLimit-Remaining")
                    val isRateLimit = rateLimitHeader == "0" || errorMessage.contains("rate limit", ignoreCase = true)
                    if (isRateLimit) {
                        SyncStatus.Failed("GitHub API rate limit exceeded. Please check your Personal Access Token.", SyncErrorType.RATE_LIMIT)
                    } else {
                        SyncStatus.Failed("GitHub API Forbidden (403): $errorMessage", SyncErrorType.UNKNOWN)
                    }
                }
                else -> SyncStatus.Failed(errorMessage, SyncErrorType.UNKNOWN)
            }
        } catch (e: IOException) {
            SyncStatus.Failed("No internet connection or network failure: ${e.message}", SyncErrorType.NETWORK)
        } catch (e: Exception) {
            SyncStatus.Failed("An unexpected error occurred: ${e.message}", SyncErrorType.UNKNOWN)
        }
    }

    fun observeProfile(username: String): Flow<DeveloperProfileEntity?> =
        database.developerProfileDao().observeProfile(username)

    suspend fun getProfile(username: String): DeveloperProfileEntity? =
        database.developerProfileDao().getProfile(username)

    suspend fun getRepositories(username: String): List<RepositoryEntity> =
        database.repositoryDao().getRepositoriesForOwner(username)

    suspend fun getEvents(username: String): List<DeveloperEventEntity> =
        database.developerEventDao().getEventsForDeveloper(username)

    suspend fun getSkills(username: String): List<SkillEvidenceEntity> =
        database.skillEvidenceDao().getSkillsForDeveloper(username)

    suspend fun getLatestSnapshot(username: String): ActivitySnapshotEntity? =
        database.activitySnapshotDao().getLatestSnapshot(username)

    suspend fun getAllSnapshots(username: String): List<ActivitySnapshotEntity> =
        database.activitySnapshotDao().getAllSnapshots(username)

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        database.clearAllTables()
    }
}
