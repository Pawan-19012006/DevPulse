package com.devpulse.ai.domain

import com.devpulse.ai.data.local.entity.DeveloperEventEntity
import com.devpulse.ai.data.local.entity.RepositoryEntity
import com.devpulse.ai.data.local.entity.SkillEvidenceEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.concurrent.TimeUnit

object SkillEvidenceEngine {

    private val gson = Gson()
    private val stringListType = object : TypeToken<List<String>>() {}.type
    private val languageMapType = object : TypeToken<Map<String, Long>>() {}.type

    fun evaluateSkills(
        username: String,
        repos: List<RepositoryEntity>,
        events: List<DeveloperEventEntity>,
        now: Long = System.currentTimeMillis()
    ): List<SkillEvidence> {
        val skillMap = mutableMapOf<String, SkillAccumulator>()

        // 1. Process Repository Language and Byte Breakdown
        repos.forEach { repo ->
            // Primary language
            repo.primaryLanguage?.let { lang ->
                val acc = skillMap.getOrPut(lang) { SkillAccumulator(lang) }
                acc.primaryRepoCount++
                acc.evidenceCount++
                acc.sampleRepos.add(repo.name)
                acc.updateDates(repo.createdAt, repo.pushedAt)
            }

            // Detailed language bytes if parsed
            if (repo.languagesJson.isNotBlank() && repo.languagesJson != "{}") {
                try {
                    val bytesMap: Map<String, Long> = gson.fromJson(repo.languagesJson, languageMapType)
                    bytesMap.forEach { (lang, bytes) ->
                        val acc = skillMap.getOrPut(lang) { SkillAccumulator(lang) }
                        acc.totalBytes += bytes
                        acc.evidenceCount++
                        acc.sampleRepos.add(repo.name)
                        acc.updateDates(repo.createdAt, repo.pushedAt)
                    }
                } catch (_: Exception) {}
            }

            // Topics tags
            if (repo.topicsJson.isNotBlank() && repo.topicsJson != "[]") {
                try {
                    val topics: List<String> = gson.fromJson(repo.topicsJson, stringListType)
                    topics.forEach { topic ->
                        val formattedTopic = topic.replace("-", " ").capitalizeWords()
                        val acc = skillMap.getOrPut(formattedTopic) { SkillAccumulator(formattedTopic) }
                        acc.evidenceCount++
                        acc.sampleRepos.add(repo.name)
                        acc.updateDates(repo.createdAt, repo.pushedAt)
                    }
                } catch (_: Exception) {}
            }
        }

        // 2. Process Events
        events.forEach { event ->
            event.language?.let { lang ->
                val acc = skillMap.getOrPut(lang) { SkillAccumulator(lang) }
                acc.commitCount++
                acc.evidenceCount++
                acc.updateDates(event.timestamp, event.timestamp)
            }
        }

        // 3. Convert to SkillEvidence with factual Confidence Levels
        val ninetyDaysAgo = now - TimeUnit.DAYS.toMillis(90)
        val oneEightyDaysAgo = now - TimeUnit.DAYS.toMillis(180)

        return skillMap.values.map { acc ->
            val isRecent = acc.lastObserved >= ninetyDaysAgo
            val isSemiRecent = acc.lastObserved >= oneEightyDaysAgo

            val confidence = when {
                (acc.primaryRepoCount >= 3 || acc.totalBytes > 50_000) && isRecent -> ConfidenceLevel.STRONG
                acc.primaryRepoCount >= 1 || isSemiRecent || acc.evidenceCount >= 3 -> ConfidenceLevel.MODERATE
                else -> ConfidenceLevel.EMERGING
            }

            SkillEvidence(
                skillName = acc.name,
                evidenceCount = acc.evidenceCount,
                primaryLanguageRepoCount = acc.primaryRepoCount,
                languageBytes = acc.totalBytes,
                commitCount = acc.commitCount,
                firstObservedTimestamp = if (acc.firstObserved == Long.MAX_VALUE) now else acc.firstObserved,
                lastObservedTimestamp = acc.lastObserved,
                confidence = confidence,
                sampleRepositories = acc.sampleRepos.take(5).toList()
            )
        }.sortedWith(
            compareByDescending<SkillEvidence> { it.confidence }
                .thenByDescending { it.primaryLanguageRepoCount }
                .thenByDescending { it.evidenceCount }
        )
    }

    fun toEntity(username: String, evidence: SkillEvidence): SkillEvidenceEntity {
        return SkillEvidenceEntity(
            id = "${username}_${evidence.skillName}",
            developerUsername = username,
            skillName = evidence.skillName,
            evidenceCount = evidence.evidenceCount,
            primaryLanguageRepoCount = evidence.primaryLanguageRepoCount,
            languageByteCount = evidence.languageBytes,
            commitCount = evidence.commitCount,
            firstObservedTimestamp = evidence.firstObservedTimestamp,
            lastObservedTimestamp = evidence.lastObservedTimestamp,
            confidenceLevel = evidence.confidence.name,
            sampleRepositoriesJson = gson.toJson(evidence.sampleRepositories)
        )
    }

    private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private class SkillAccumulator(val name: String) {
        var evidenceCount: Int = 0
        var primaryRepoCount: Int = 0
        var totalBytes: Long = 0L
        var commitCount: Int = 0
        var firstObserved: Long = Long.MAX_VALUE
        var lastObserved: Long = 0L
        val sampleRepos = mutableSetOf<String>()

        fun updateDates(created: Long, updated: Long) {
            if (created in 1 until firstObserved) firstObserved = created
            if (updated > lastObserved) lastObserved = updated
        }
    }
}
