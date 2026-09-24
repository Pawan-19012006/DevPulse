package com.devpulse.ai.model

import com.google.gson.annotations.SerializedName

data class GitHubEventActor(
    val id: Long? = null,
    val login: String,
    @SerializedName("avatar_url") val avatarUrl: String? = null
)

data class GitHubEventRepo(
    val id: Long? = null,
    val name: String,
    val url: String? = null
)

data class GitHubCommitAuthor(
    val email: String? = null,
    val name: String? = null
)

data class GitHubCommitSummary(
    val sha: String,
    val message: String,
    val author: GitHubCommitAuthor? = null,
    val url: String? = null
)

data class GitHubPullRequestItem(
    val id: Long? = null,
    val number: Int = 0,
    val title: String? = null,
    val state: String? = null,
    val merged: Boolean? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("closed_at") val closedAt: String? = null,
    @SerializedName("merged_at") val mergedAt: String? = null,
    val additions: Int? = null,
    val deletions: Int? = null,
    @SerializedName("changed_files") val changedFiles: Int? = null
)

data class GitHubIssueItem(
    val id: Long? = null,
    val number: Int = 0,
    val title: String? = null,
    val state: String? = null,
    val comments: Int = 0,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("closed_at") val closedAt: String? = null
)

data class GitHubReviewItem(
    val id: Long? = null,
    val state: String? = null,
    @SerializedName("submitted_at") val submittedAt: String? = null
)

data class GitHubEventPayload(
    val action: String? = null,
    val commits: List<GitHubCommitSummary>? = null,
    val ref: String? = null,
    @SerializedName("ref_type") val refType: String? = null,
    val size: Int? = null,
    @SerializedName("pull_request") val pullRequest: GitHubPullRequestItem? = null,
    val issue: GitHubIssueItem? = null,
    val review: GitHubReviewItem? = null,
    val description: String? = null
)

data class GitHubEvent(
    val id: String,
    val type: String,
    val actor: GitHubEventActor,
    val repo: GitHubEventRepo,
    val payload: GitHubEventPayload? = null,
    val public: Boolean = true,
    @SerializedName("created_at") val createdAt: String
)
