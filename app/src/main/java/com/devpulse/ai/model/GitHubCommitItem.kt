package com.devpulse.ai.model

import com.google.gson.annotations.SerializedName

data class GitHubCommitUserInfo(
    val name: String? = null,
    val email: String? = null,
    val date: String? = null
)

data class GitHubCommitInfo(
    val message: String,
    val author: GitHubCommitUserInfo? = null,
    val committer: GitHubCommitUserInfo? = null,
    @SerializedName("comment_count") val commentCount: Int = 0
)

data class GitHubCommitItem(
    val sha: String,
    val commit: GitHubCommitInfo,
    val author: GitHubEventActor? = null,
    @SerializedName("html_url") val htmlUrl: String? = null
)
