package com.devpulse.ai.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "developer_profiles")
data class DeveloperProfileEntity(
    @PrimaryKey val username: String,
    val name: String?,
    val avatarUrl: String,
    val bio: String?,
    val company: String?,
    val location: String?,
    val email: String?,
    val blog: String?,
    val twitterUsername: String?,
    val hireable: Boolean?,
    val followers: Int,
    val following: Int,
    val publicRepos: Int,
    val publicGists: Int,
    val githubCreatedAt: String,
    val githubUpdatedAt: String,
    val lastSyncedAt: Long
)

@Entity(
    tableName = "repositories",
    indices = [Index(value = ["owner", "name"], unique = true)]
)
data class RepositoryEntity(
    @PrimaryKey val id: Long,
    val owner: String,
    val name: String,
    val fullName: String,
    val description: String?,
    val primaryLanguage: String?,
    val stargazersCount: Int,
    val forksCount: Int,
    val openIssuesCount: Int,
    val sizeKb: Int,
    val isFork: Boolean,
    val isArchived: Boolean,
    val defaultBranch: String,
    val createdAt: Long,
    val updatedAt: Long,
    val pushedAt: Long,
    val topicsJson: String,
    val languagesJson: String,
    val lastSyncedAt: Long
)

@Entity(
    tableName = "developer_events",
    indices = [
        Index(value = ["developerUsername", "timestamp"]),
        Index(value = ["repositoryName"]),
        Index(value = ["eventType"])
    ]
)
data class DeveloperEventEntity(
    @PrimaryKey val id: String,
    val developerUsername: String,
    val repositoryName: String,
    val eventType: String,
    val timestamp: Long,
    val actor: String,
    val summary: String,
    val additions: Int = 0,
    val deletions: Int = 0,
    val changedFiles: Int = 0,
    val language: String? = null,
    val source: String = "GITHUB_EVENT",
    val metadataJson: String = "{}"
)

@Entity(
    tableName = "activity_snapshots",
    indices = [Index(value = ["developerUsername", "timestamp"])]
)
data class ActivitySnapshotEntity(
    @PrimaryKey val id: String,
    val developerUsername: String,
    val timestamp: Long,
    val totalRepositories: Int,
    val activeRepositoriesCount: Int,
    val totalStars: Int,
    val totalForks: Int,
    val commitsCount: Int,
    val prsOpenedCount: Int,
    val prsMergedCount: Int,
    val issuesOpenedCount: Int,
    val issuesClosedCount: Int,
    val languagesJson: String,
    val topLanguage: String?,
    val activeDaysLast30Days: Int
)

@Entity(
    tableName = "skill_evidences",
    indices = [Index(value = ["developerUsername", "skillName"], unique = true)]
)
data class SkillEvidenceEntity(
    @PrimaryKey val id: String,
    val developerUsername: String,
    val skillName: String,
    val evidenceCount: Int,
    val primaryLanguageRepoCount: Int,
    val languageByteCount: Long,
    val commitCount: Int,
    val firstObservedTimestamp: Long,
    val lastObservedTimestamp: Long,
    val confidenceLevel: String,
    val sampleRepositoriesJson: String
)
