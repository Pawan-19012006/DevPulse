package com.devpulse.ai.domain

import com.devpulse.ai.data.local.entity.DeveloperEventEntity
import com.devpulse.ai.data.local.entity.RepositoryEntity
import com.devpulse.ai.model.EventType
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class SkillEvidenceEngineTest {

    @Test
    fun evaluateSkills_assignsStrongConfidence_forActiveMultiRepoLanguage() {
        val now = System.currentTimeMillis()
        val oneWeekAgo = now - TimeUnit.DAYS.toMillis(7)

        val repos = listOf(
            RepositoryEntity(
                id = 1, owner = "dev1", name = "app1", fullName = "dev1/app1",
                description = "Android app", primaryLanguage = "Kotlin",
                stargazersCount = 5, forksCount = 0, openIssuesCount = 0, sizeKb = 100,
                isFork = false, isArchived = false, defaultBranch = "main",
                createdAt = oneWeekAgo, updatedAt = oneWeekAgo, pushedAt = oneWeekAgo,
                topicsJson = "[\"android\", \"compose\"]", languagesJson = "{\"Kotlin\": 45000}",
                lastSyncedAt = now
            ),
            RepositoryEntity(
                id = 2, owner = "dev1", name = "app2", fullName = "dev1/app2",
                description = "Backend service", primaryLanguage = "Kotlin",
                stargazersCount = 2, forksCount = 0, openIssuesCount = 0, sizeKb = 80,
                isFork = false, isArchived = false, defaultBranch = "main",
                createdAt = oneWeekAgo, updatedAt = oneWeekAgo, pushedAt = oneWeekAgo,
                topicsJson = "[\"backend\"]", languagesJson = "{\"Kotlin\": 35000}",
                lastSyncedAt = now
            ),
            RepositoryEntity(
                id = 3, owner = "dev1", name = "lib1", fullName = "dev1/lib1",
                description = "Kotlin library", primaryLanguage = "Kotlin",
                stargazersCount = 10, forksCount = 1, openIssuesCount = 0, sizeKb = 50,
                isFork = false, isArchived = false, defaultBranch = "main",
                createdAt = oneWeekAgo, updatedAt = oneWeekAgo, pushedAt = oneWeekAgo,
                topicsJson = "[]", languagesJson = "{}",
                lastSyncedAt = now
            )
        )

        val events = listOf(
            DeveloperEventEntity(
                id = "c1", developerUsername = "dev1", repositoryName = "dev1/app1",
                eventType = EventType.COMMIT.name, timestamp = oneWeekAgo, actor = "dev1",
                summary = "Update Kotlin version", language = "Kotlin"
            )
        )

        val skills = SkillEvidenceEngine.evaluateSkills("dev1", repos, events, now)

        val kotlinSkill = skills.find { it.skillName == "Kotlin" }
        assertNotNull(kotlinSkill)
        assertEquals(ConfidenceLevel.STRONG, kotlinSkill!!.confidence)
        assertEquals(3, kotlinSkill.primaryLanguageRepoCount)
        assertEquals(80000L, kotlinSkill.languageBytes)
        assertEquals(1, kotlinSkill.commitCount)
    }

    @Test
    fun evaluateSkills_assignsEmergingConfidence_forSingleTopicMention() {
        val now = System.currentTimeMillis()
        val twoHundredDaysAgo = now - TimeUnit.DAYS.toMillis(200)
        val repos = listOf(
            RepositoryEntity(
                id = 1, owner = "dev1", name = "ml-notes", fullName = "dev1/ml-notes",
                description = "Notes on ML", primaryLanguage = "Markdown",
                stargazersCount = 0, forksCount = 0, openIssuesCount = 0, sizeKb = 10,
                isFork = false, isArchived = false, defaultBranch = "main",
                createdAt = twoHundredDaysAgo, updatedAt = twoHundredDaysAgo, pushedAt = twoHundredDaysAgo,
                topicsJson = "[\"tensorflow\"]", languagesJson = "{}",
                lastSyncedAt = now
            )
        )

        val skills = SkillEvidenceEngine.evaluateSkills("dev1", repos, emptyList(), now)

        val tfSkill = skills.find { it.skillName.equals("Tensorflow", ignoreCase = true) }
        assertNotNull(tfSkill)
        assertEquals(ConfidenceLevel.EMERGING, tfSkill!!.confidence)
        assertEquals(0, tfSkill.primaryLanguageRepoCount)
    }
}
