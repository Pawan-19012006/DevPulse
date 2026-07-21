package com.devpulse.ai.model

import com.google.gson.annotations.SerializedName

data class GitHubUser(
    val login: String,
    val name: String?,
    @SerializedName("avatar_url") val avatarUrl: String,
    val bio: String?,
    val company: String?,
    val location: String?,
    val email: String?,
    val blog: String?,
    @SerializedName("twitter_username") val twitterUsername: String?,
    val hireable: Boolean?,
    val followers: Int,
    val following: Int,
    @SerializedName("public_repos") val publicRepos: Int,
    @SerializedName("public_gists") val publicGists: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)
