package com.devpulse.ai.repository

import com.devpulse.ai.model.GitHubRepo
import com.devpulse.ai.model.GitHubUser
import com.devpulse.ai.model.GitHubUserSummary
import com.devpulse.ai.network.GitHubApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

data class GitHubDataPackage(
    val user: GitHubUser,
    val repos: List<GitHubRepo>,
    val followers: List<GitHubUserSummary>,
    val following: List<GitHubUserSummary>
)

class GitHubRepository(private val apiService: GitHubApiService) {

    suspend fun getFullProfileData(username: String): GitHubDataPackage = withContext(Dispatchers.IO) {
        val token = com.devpulse.ai.BuildConfig.GITHUB_TOKEN
        if (token.isBlank()) {
            throw GitHubTokenMissingException("GitHub token not configured.")
        }

        try {
            // Initiate parallel API requests using Coroutines async
            val userDeferred = async { apiService.getUser(username) }
            val reposDeferred = async { apiService.getRepos(username) }
            val followersDeferred = async { apiService.getFollowers(username) }
            val followingDeferred = async { apiService.getFollowing(username) }

            GitHubDataPackage(
                user = userDeferred.await(),
                repos = reposDeferred.await(),
                followers = followersDeferred.await(),
                following = followingDeferred.await()
            )
        } catch (e: HttpException) {
            val errorBody = try {
                e.response()?.errorBody()?.string()
            } catch (ioEx: Exception) {
                null
            }

            val errorMessage = try {
                if (!errorBody.isNullOrBlank()) {
                    com.google.gson.JsonParser.parseString(errorBody).asJsonObject.get("message").asString
                } else {
                    e.message() ?: "API Error"
                }
            } catch (parseEx: Exception) {
                e.message() ?: "API Error"
            }

            when (e.code()) {
                404 -> throw UserNotFoundException("User not found on GitHub")
                403 -> {
                    val rateLimitHeader = e.response()?.headers()?.get("X-RateLimit-Remaining")
                    val isRateLimit = rateLimitHeader == "0" || errorMessage.contains("rate limit", ignoreCase = true)
                    if (isRateLimit) {
                        throw RateLimitException("GitHub API rate limit exceeded.\nPlease configure a Personal Access Token.")
                    } else {
                        throw ApiException(errorMessage, 403)
                    }
                }
                else -> throw ApiException(errorMessage, e.code())
            }
        } catch (e: IOException) {
            throw NetworkException("No internet connection or network failure", e)
        } catch (e: GitHubAnalysisException) {
            throw e
        } catch (e: Exception) {
            throw UnknownException("An unexpected error occurred", e)
        }
    }
}

// Custom exception hierarchy for clean error handling in the UI
open class GitHubAnalysisException(message: String, cause: Throwable? = null) : Exception(message, cause)
class GitHubTokenMissingException(message: String) : GitHubAnalysisException(message)
class UserNotFoundException(message: String) : GitHubAnalysisException(message)
class RateLimitException(message: String) : GitHubAnalysisException(message)
class NetworkException(message: String, cause: Throwable) : GitHubAnalysisException(message, cause)
class ApiException(message: String, val code: Int) : GitHubAnalysisException(message)
class UnknownException(message: String, cause: Throwable) : GitHubAnalysisException(message, cause)
