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
            when (e.code()) {
                404 -> throw UserNotFoundException("User not found on GitHub")
                403 -> {
                    val rateLimitHeader = e.response()?.headers()?.get("X-RateLimit-Remaining")
                    if (rateLimitHeader == "0") {
                        throw RateLimitException("GitHub API rate limit exceeded")
                    } else {
                        throw ApiException("Access Forbidden: ${e.message()}", e.code())
                    }
                }
                else -> throw ApiException("API request failed with code ${e.code()}", e.code())
            }
        } catch (e: IOException) {
            throw NetworkException("No internet connection or network failure", e)
        } catch (e: Exception) {
            throw UnknownException("An unexpected error occurred", e)
        }
    }
}

// Custom exception hierarchy for clean error handling in the UI
open class GitHubAnalysisException(message: String, cause: Throwable? = null) : Exception(message, cause)
class UserNotFoundException(message: String) : GitHubAnalysisException(message)
class RateLimitException(message: String) : GitHubAnalysisException(message)
class NetworkException(message: String, cause: Throwable) : GitHubAnalysisException(message, cause)
class ApiException(message: String, val code: Int) : GitHubAnalysisException(message)
class UnknownException(message: String, cause: Throwable) : GitHubAnalysisException(message, cause)
