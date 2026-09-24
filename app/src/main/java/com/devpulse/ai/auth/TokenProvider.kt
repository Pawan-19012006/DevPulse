package com.devpulse.ai.auth

import com.devpulse.ai.BuildConfig

enum class AuthType {
    PERSONAL_ACCESS_TOKEN,
    OAUTH_PKCE
}

/**
 * Interface abstracting GitHub authentication tokens.
 * Decouples the networking layer from hardcoded BuildConfig access and prepares
 * the platform for the Phase 2 OAuth 2.0 PKCE implementation.
 */
interface TokenProvider {
    fun getToken(): String?
    fun hasConfiguredToken(): Boolean
    fun getAuthType(): AuthType
}

/**
 * Development token provider reading the Personal Access Token injected into BuildConfig.
 */
class BuildConfigTokenProvider : TokenProvider {
    override fun getToken(): String? {
        val token = BuildConfig.GITHUB_TOKEN.trim()
            .removeSurrounding("\"")
            .removeSurrounding("'")
            .trim()
        return if (token.isNotEmpty()) token else null
    }

    override fun hasConfiguredToken(): Boolean {
        return getToken() != null
    }

    override fun getAuthType(): AuthType = AuthType.PERSONAL_ACCESS_TOKEN
}
