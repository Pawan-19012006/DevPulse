package com.devpulse.ai.model

enum class EventType {
    COMMIT,
    PULL_REQUEST_OPENED,
    PULL_REQUEST_MERGED,
    PULL_REQUEST_CLOSED,
    PULL_REQUEST_REVIEW,
    ISSUE_OPENED,
    ISSUE_CLOSED,
    ISSUE_COMMENT,
    REPOSITORY_CREATED,
    REPOSITORY_PUSH,
    RELEASE_CREATED,
    BRANCH_OR_TAG_CREATED,
    WATCH_STAR,
    FORK
}

data class NormalizedEvent(
    val id: String,
    val developerUsername: String,
    val repositoryName: String,
    val eventType: EventType,
    val timestamp: Long, // Epoch milliseconds
    val actor: String,
    val summary: String,
    val additions: Int = 0,
    val deletions: Int = 0,
    val changedFiles: Int = 0,
    val language: String? = null,
    val source: String = "GITHUB_EVENT",
    val metadataJson: String = "{}"
)
