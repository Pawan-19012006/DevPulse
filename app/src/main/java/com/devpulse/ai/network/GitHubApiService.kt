package com.devpulse.ai.network

import com.devpulse.ai.auth.BuildConfigTokenProvider
import com.devpulse.ai.auth.TokenProvider
import com.devpulse.ai.model.GitHubCommitItem
import com.devpulse.ai.model.GitHubEvent
import com.devpulse.ai.model.GitHubRepo
import com.devpulse.ai.model.GitHubUser
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GitHubApiService {

    @GET("users/{username}")
    suspend fun getUser(
        @Path("username") username: String
    ): GitHubUser

    @GET("users/{username}/repos")
    suspend fun getRepos(
        @Path("username") username: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 100,
        @Query("sort") sort: String = "pushed"
    ): List<GitHubRepo>

    @GET("users/{username}/events")
    suspend fun getUserEvents(
        @Path("username") username: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 100
    ): List<GitHubEvent>

    @GET("repos/{owner}/{repo}/languages")
    suspend fun getRepoLanguages(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Map<String, Long>

    @GET("repos/{owner}/{repo}/commits")
    suspend fun getRepoCommits(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("author") author: String? = null,
        @Query("per_page") perPage: Int = 30
    ): List<GitHubCommitItem>

    companion object {
        private const val BASE_URL = "https://api.github.com/"

        fun create(tokenProvider: TokenProvider = BuildConfigTokenProvider()): GitHubApiService {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val original = chain.request()
                    val requestBuilder = original.newBuilder()
                        .header("Accept", "application/vnd.github+json")
                        .header("User-Agent", "DevPulse-AI")

                    val token = tokenProvider.getToken()
                    if (!token.isNullOrBlank()) {
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
