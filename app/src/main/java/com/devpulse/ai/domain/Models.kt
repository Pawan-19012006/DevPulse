package com.devpulse.ai.domain

import java.util.concurrent.TimeUnit

sealed class TimeWindow(val label: String) {
    abstract val startTime: Long
    abstract val endTime: Long

    class Today(now: Long = System.currentTimeMillis()) : TimeWindow("Today") {
        override val endTime: Long = now
        override val startTime: Long = now - TimeUnit.DAYS.toMillis(1)
    }

    class Last7Days(now: Long = System.currentTimeMillis()) : TimeWindow("Last 7 Days") {
        override val endTime: Long = now
        override val startTime: Long = now - TimeUnit.DAYS.toMillis(7)
    }

    class Last30Days(now: Long = System.currentTimeMillis()) : TimeWindow("Last 30 Days") {
        override val endTime: Long = now
        override val startTime: Long = now - TimeUnit.DAYS.toMillis(30)
    }

    class Last90Days(now: Long = System.currentTimeMillis()) : TimeWindow("Last 90 Days") {
        override val endTime: Long = now
        override val startTime: Long = now - TimeUnit.DAYS.toMillis(90)
    }

    class Previous7Days(now: Long = System.currentTimeMillis()) : TimeWindow("Previous 7 Days") {
        override val endTime: Long = now - TimeUnit.DAYS.toMillis(7)
        override val startTime: Long = now - TimeUnit.DAYS.toMillis(14)
    }

    class Previous30Days(now: Long = System.currentTimeMillis()) : TimeWindow("Previous 30 Days") {
        override val endTime: Long = now - TimeUnit.DAYS.toMillis(30)
        override val startTime: Long = now - TimeUnit.DAYS.toMillis(60)
    }

    data class Custom(
        val name: String,
        override val startTime: Long,
        override val endTime: Long
    ) : TimeWindow(name)
}

data class ActivitySignals(
    val commitsCount: Int = 0,
    val prsOpenedCount: Int = 0,
    val prsMergedCount: Int = 0,
    val issuesOpenedCount: Int = 0,
    val issuesClosedCount: Int = 0,
    val reviewsCount: Int = 0,
    val activeRepositoriesCount: Int = 0,
    val activeDaysCount: Int = 0,
    val additions: Int = 0,
    val deletions: Int = 0,
    val topLanguages: List<Pair<String, Int>> = emptyList(),
    val activeRepositories: List<Pair<String, Int>> = emptyList()
) {
    val totalSignals: Int
        get() = commitsCount + prsOpenedCount + prsMergedCount + issuesOpenedCount + issuesClosedCount + reviewsCount
}

sealed class ComparisonResult<out T> {
    abstract val current: T

    data class Available<T>(
        override val current: T,
        val previous: T,
        val percentageChange: Double?
    ) : ComparisonResult<T>()

    data class InsufficientHistory<T>(
        override val current: T,
        val reason: String = "Insufficient historical activity to compute trend"
    ) : ComparisonResult<T>()
}

enum class ConfidenceLevel {
    STRONG,
    MODERATE,
    EMERGING
}

data class SkillEvidence(
    val skillName: String,
    val evidenceCount: Int,
    val primaryLanguageRepoCount: Int,
    val languageBytes: Long,
    val commitCount: Int,
    val firstObservedTimestamp: Long,
    val lastObservedTimestamp: Long,
    val confidence: ConfidenceLevel,
    val sampleRepositories: List<String>
)

enum class SyncErrorType {
    USER_NOT_FOUND,
    RATE_LIMIT,
    NETWORK,
    EMPTY_REPOS,
    TOKEN_MISSING,
    UNKNOWN
}

sealed interface SyncStatus {
    object Idle : SyncStatus
    object Syncing : SyncStatus
    data class Success(val lastSyncTime: Long) : SyncStatus
    data class PartialSuccess(val lastSyncTime: Long, val warning: String) : SyncStatus
    data class Failed(val error: String, val errorType: SyncErrorType) : SyncStatus
}

