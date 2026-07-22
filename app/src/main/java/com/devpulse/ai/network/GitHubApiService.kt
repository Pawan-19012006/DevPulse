package com.devpulse.ai.network

import com.devpulse.ai.model.GitHubRepo
import com.devpulse.ai.model.GitHubUser
import com.devpulse.ai.model.GitHubUserSummary
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GitHubApiService {
    @GET("users/{username}")
    suspend fun getUser(
        @Path("username") username: String
    ): GitHubUser

    @GET("users/{username}/repos")
    suspend fun getRepos(
        @Path("username") username: String,
        @Query("per_page") perPage: Int = 100
    ): List<GitHubRepo>

    @GET("users/{username}/followers")
    suspend fun getFollowers(
        @Path("username") username: String,
        @Query("per_page") perPage: Int = 100
    ): List<GitHubUserSummary>

    @GET("users/{username}/following")
    suspend fun getFollowing(
        @Path("username") username: String,
        @Query("per_page") perPage: Int = 100
    ): List<GitHubUserSummary>

    companion object {
        private const val BASE_URL = "https://api.github.com/"

        fun create(): GitHubApiService {
            val client = okhttp3.OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val original = chain.request()
                    val requestBuilder = original.newBuilder()
                        .header("Accept", "application/vnd.github+json")
                        .header("User-Agent", "DevPulse-AI")

                    val token = com.devpulse.ai.BuildConfig.GITHUB_TOKEN
                    if (token.isNotBlank()) {
                        requestBuilder.header("Authorization", "Bearer $token")
                    }

                    chain.proceed(requestBuilder.build())
                }
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GitHubApiService::class.java)
        }
    }
}
