package com.devpulse.ai.model

import com.google.gson.annotations.SerializedName

data class GitHubRepo(
    val name: String,
    val description: String?,
    val language: String?,
    @SerializedName("stargazers_count") val stargazersCount: Int,
    @SerializedName("forks_count") val forksCount: Int,
    @SerializedName("pushed_at") val pushedAt: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    val size: Int, // Size in KB
    val private: Boolean
) {
    val isPrivate: Boolean
        get() = private
}
