package com.devpulse.ai.utils

import com.devpulse.ai.model.*
import org.junit.Assert.*
import org.junit.Test

class EventNormalizerTest {

    @Test
    fun pushEvent_withMultipleCommits_decomposesIntoIndividualCommitEvents() {
        val pushEvent = GitHubEvent(
            id = "event_1001",
            type = "PushEvent",
            actor = GitHubEventActor(login = "testdev"),
            repo = GitHubEventRepo(name = "testdev/DevPulse"),
            createdAt = "2026-09-20T10:00:00Z",
            payload = GitHubEventPayload(
                ref = "refs/heads/main",
                commits = listOf(
                    GitHubCommitSummary(sha = "sha_abc1", message = "Add Room database entities"),
                    GitHubCommitSummary(sha = "sha_abc2", message = "Refactor GitHubRepository\nDetailed message"),
                    GitHubCommitSummary(sha = "sha_abc3", message = "Fix Compose theme")
                )
            )
        )

        val normalized = EventNormalizer.normalizeGitHubEvent(pushEvent, "testdev")

        assertEquals(3, normalized.size)
        assertEquals("sha_abc1", normalized[0].id)
        assertEquals(EventType.COMMIT, normalized[0].eventType)
        assertEquals("Add Room database entities", normalized[0].summary)
        assertEquals("testdev/DevPulse", normalized[0].repositoryName)

        assertEquals("sha_abc2", normalized[1].id)
        assertEquals("Refactor GitHubRepository", normalized[1].summary)

        assertEquals("sha_abc3", normalized[2].id)
    }

    @Test
    fun pushEvent_withoutCommits_normalizesToPushEvent() {
        val pushEvent = GitHubEvent(
            id = "event_1002",
            type = "PushEvent",
            actor = GitHubEventActor(login = "testdev"),
            repo = GitHubEventRepo(name = "testdev/DevPulse"),
            createdAt = "2026-09-20T10:00:00Z",
            payload = GitHubEventPayload(
                ref = "refs/heads/main",
                commits = emptyList()
            )
        )

        val normalized = EventNormalizer.normalizeGitHubEvent(pushEvent, "testdev")

        assertEquals(1, normalized.size)
        assertEquals("event_1002", normalized[0].id)
        assertEquals(EventType.REPOSITORY_PUSH, normalized[0].eventType)
    }

    @Test
    fun pullRequestEvent_merged_normalizesToMerged() {
        val prEvent = GitHubEvent(
            id = "event_2001",
            type = "PullRequestEvent",
            actor = GitHubEventActor(login = "octocat"),
            repo = GitHubEventRepo(name = "octocat/Spoon-Knife"),
            createdAt = "2026-09-21T14:30:00Z",
            payload = GitHubEventPayload(
                action = "closed",
                pullRequest = GitHubPullRequestItem(
                    id = 42L,
                    number = 15,
                    title = "Feature: Support Room Persistence",
                    state = "closed",
                    merged = true,
                    additions = 150,
                    deletions = 20,
                    changedFiles = 4
                )
            )
        )

        val normalized = EventNormalizer.normalizeGitHubEvent(prEvent, "octocat")

        assertEquals(1, normalized.size)
        val event = normalized[0]
        assertEquals(EventType.PULL_REQUEST_MERGED, event.eventType)
        assertEquals(150, event.additions)
        assertEquals(20, event.deletions)
        assertEquals(4, event.changedFiles)
    }

    @Test
    fun issuesEvent_closed_normalizesToIssueClosed() {
        val issueEvent = GitHubEvent(
            id = "event_3001",
            type = "IssuesEvent",
            actor = GitHubEventActor(login = "octocat"),
            repo = GitHubEventRepo(name = "octocat/Hello-World"),
            createdAt = "2026-09-22T08:00:00Z",
            payload = GitHubEventPayload(
                action = "closed",
                issue = GitHubIssueItem(
                    id = 999L,
                    number = 12,
                    title = "Fix network timeout on pagination",
                    state = "closed"
                )
            )
        )

        val normalized = EventNormalizer.normalizeGitHubEvent(issueEvent, "octocat")

        assertEquals(1, normalized.size)
        assertEquals(EventType.ISSUE_CLOSED, normalized[0].eventType)
        assertEquals("Fix network timeout on pagination", normalized[0].summary)
    }

    @Test
    fun unknownEventType_returnsEmptyListSafely() {
        val unknownEvent = GitHubEvent(
            id = "event_9999",
            type = "MemberEvent",
            actor = GitHubEventActor(login = "testdev"),
            repo = GitHubEventRepo(name = "testdev/DevPulse"),
            createdAt = "2026-09-22T08:00:00Z",
            payload = null
        )

        val normalized = EventNormalizer.normalizeGitHubEvent(unknownEvent, "testdev")
        assertTrue(normalized.isEmpty())
    }
}
