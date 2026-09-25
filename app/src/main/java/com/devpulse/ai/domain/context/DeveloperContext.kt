package com.devpulse.ai.domain.context

import com.devpulse.ai.data.local.entity.DevSessionEntity
import com.devpulse.ai.data.local.entity.HealthEventEntity
import com.devpulse.ai.data.local.entity.OnePercentImprovementEntity
import com.devpulse.ai.data.local.entity.SessionHandoffEntity
import kotlinx.coroutines.flow.Flow

/**
 * Shared context snapshot connecting the developer's four pillars:
 * Sessions, Health, Coach (future Gemini intelligence), and Tracker.
 */
data class DeveloperContext(
    val recentSessions: List<DevSessionEntity> = emptyList(),
    val unfinishedHandoff: SessionHandoffEntity? = null,
    val recentImprovements: List<OnePercentImprovementEntity> = emptyList(),
    val todayHealthEvents: List<HealthEventEntity> = emptyList(),
    val connectedGitHubUser: String? = null
)

/**
 * Clean domain interface exposing developer context for Coach and future Gemini intelligence.
 */
interface DeveloperContextProvider {
    fun observeDeveloperContext(): Flow<DeveloperContext>
}
