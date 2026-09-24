package com.devpulse.ai.model

import com.google.gson.annotations.SerializedName

data class GitHubRepoOwner(
    val login: String,
    @SerializedName("avatar_url") val avatarUrl: String? = null
)

data class GitHubRepo(
    val id: Long = 0L,
    val name: String,
    @SerializedName("full_name") val fullName: String? = null,
    val owner: GitHubRepoOwner? = null,
    val description: String? = null,
    val language: String? = null,
    val topics: List<String>? = emptyList(),
    @SerializedName("stargazers_count") val stargazersCount: Int = 0,
    @SerializedName("forks_count") val forksCount: Int = 0,
    @SerializedName("watchers_count") val watchersCount: Int? = null,
    @SerializedName("open_issues_count") val openIssuesCount: Int = 0,
    @SerializedName("pushed_at") val pushedAt: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    val size: Int = 0, // Size in KB
    @SerializedName("private") val private: Boolean = false,
    @SerializedName("fork") val isFork: Boolean = false,
    @SerializedName("archived") val isArchived: Boolean = false,
    @SerializedName("default_branch") val defaultBranch: String? = "main"
) {
    val isPrivate: Boolean
        get() = private
}
