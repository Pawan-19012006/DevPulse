package com.devpulse.ai.utils

import com.devpulse.ai.model.*
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object EventNormalizer {

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun parseIsoToEpochMillis(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            isoDateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    /**
     * Converts a raw GitHubEvent into one or more NormalizedEvents.
     * (E.g. A single PushEvent containing 3 commits is normalized into individual commit events).
     */
    fun normalizeGitHubEvent(event: GitHubEvent, username: String): List<NormalizedEvent> {
        val timestamp = parseIsoToEpochMillis(event.createdAt)
        val repoName = event.repo.name
        val actor = event.actor.login
        val payload = event.payload

        return when (event.type) {
            "PushEvent" -> {
                val commits = payload?.commits
                if (!commits.isNullOrEmpty()) {
                    commits.map { commit ->
                        NormalizedEvent(
                            id = commit.sha,
                            developerUsername = username,
                            repositoryName = repoName,
                            eventType = EventType.COMMIT,
                            timestamp = timestamp,
                            actor = actor,
                            summary = commit.message.lines().firstOrNull() ?: "Commit ${commit.sha.take(7)}",
                            source = "GITHUB_EVENT",
                            metadataJson = "{\"sha\":\"${commit.sha}\",\"branch\":\"${payload.ref ?: ""}\"}"
                        )
                    }
                } else {
                    listOf(
                        NormalizedEvent(
                            id = event.id,
                            developerUsername = username,
                            repositoryName = repoName,
                            eventType = EventType.REPOSITORY_PUSH,
                            timestamp = timestamp,
                            actor = actor,
                            summary = "Pushed to ${payload?.ref ?: "repository"}",
                            source = "GITHUB_EVENT"
                        )
                    )
                }
            }

            "PullRequestEvent" -> {
                val pr = payload?.pullRequest
                val action = payload?.action?.lowercase(Locale.US)
                val eventType = when {
                    action == "opened" -> EventType.PULL_REQUEST_OPENED
                    action == "closed" && pr?.merged == true -> EventType.PULL_REQUEST_MERGED
                    action == "closed" -> EventType.PULL_REQUEST_CLOSED
                    else -> EventType.PULL_REQUEST_OPENED
                }

                listOf(
                    NormalizedEvent(
                        id = "${event.id}_pr_${pr?.number ?: 0}",
                        developerUsername = username,
                        repositoryName = repoName,
                        eventType = eventType,
                        timestamp = timestamp,
                        actor = actor,
                        summary = pr?.title ?: "Pull Request #${pr?.number ?: 0}",
                        additions = pr?.additions ?: 0,
                        deletions = pr?.deletions ?: 0,
                        changedFiles = pr?.changedFiles ?: 0,
                        source = "GITHUB_EVENT"
                    )
                )
            }

            "PullRequestReviewEvent" -> {
                listOf(
                    NormalizedEvent(
                        id = "${event.id}_review",
                        developerUsername = username,
                        repositoryName = repoName,
                        eventType = EventType.PULL_REQUEST_REVIEW,
                        timestamp = timestamp,
                        actor = actor,
                        summary = "Reviewed PR #${payload?.pullRequest?.number ?: 0} (${payload?.review?.state ?: "REVIEW"})",
                        source = "GITHUB_EVENT"
                    )
                )
            }

            "IssuesEvent" -> {
                val action = payload?.action?.lowercase(Locale.US)
                val eventType = if (action == "closed") EventType.ISSUE_CLOSED else EventType.ISSUE_OPENED
                listOf(
                    NormalizedEvent(
                        id = "${event.id}_issue_${payload?.issue?.number ?: 0}",
                        developerUsername = username,
                        repositoryName = repoName,
                        eventType = eventType,
                        timestamp = timestamp,
                        actor = actor,
                        summary = payload?.issue?.title ?: "Issue #${payload?.issue?.number ?: 0}",
                        source = "GITHUB_EVENT"
                    )
                )
            }

            "IssueCommentEvent" -> {
                listOf(
                    NormalizedEvent(
                        id = "${event.id}_comment",
                        developerUsername = username,
                        repositoryName = repoName,
                        eventType = EventType.ISSUE_COMMENT,
                        timestamp = timestamp,
                        actor = actor,
                        summary = "Commented on #${payload?.issue?.number ?: 0}: ${payload?.issue?.title ?: ""}",
                        source = "GITHUB_EVENT"
                    )
                )
            }

            "CreateEvent" -> {
                val refType = payload?.refType?.lowercase(Locale.US)
                val eventType = if (refType == "repository") EventType.REPOSITORY_CREATED else EventType.BRANCH_OR_TAG_CREATED
                listOf(
                    NormalizedEvent(
                        id = event.id,
                        developerUsername = username,
                        repositoryName = repoName,
                        eventType = eventType,
                        timestamp = timestamp,
                        actor = actor,
                        summary = "Created $refType ${payload?.ref ?: ""}".trim(),
                        source = "GITHUB_EVENT"
                    )
                )
            }

            "ReleaseEvent" -> {
                listOf(
                    NormalizedEvent(
                        id = event.id,
                        developerUsername = username,
                        repositoryName = repoName,
                        eventType = EventType.RELEASE_CREATED,
                        timestamp = timestamp,
                        actor = actor,
                        summary = "Published release in $repoName",
                        source = "GITHUB_EVENT"
                    )
                )
            }

            "WatchEvent" -> {
                listOf(
                    NormalizedEvent(
                        id = event.id,
                        developerUsername = username,
                        repositoryName = repoName,
                        eventType = EventType.WATCH_STAR,
                        timestamp = timestamp,
                        actor = actor,
                        summary = "Starred $repoName",
                        source = "GITHUB_EVENT"
                    )
                )
            }

            "ForkEvent" -> {
                listOf(
                    NormalizedEvent(
                        id = event.id,
                        developerUsername = username,
                        repositoryName = repoName,
                        eventType = EventType.FORK,
                        timestamp = timestamp,
                        actor = actor,
                        summary = "Forked $repoName",
                        source = "GITHUB_EVENT"
                    )
                )
            }

            else -> emptyList()
        }
    }

    /**
     * Converts a raw GitHubCommitItem into a NormalizedEvent.
     */
    fun normalizeRepoCommit(commitItem: GitHubCommitItem, repoName: String, username: String): NormalizedEvent {
        val timestamp = parseIsoToEpochMillis(commitItem.commit.author?.date ?: commitItem.commit.committer?.date)
        return NormalizedEvent(
            id = commitItem.sha,
            developerUsername = username,
            repositoryName = repoName,
            eventType = EventType.COMMIT,
            timestamp = timestamp,
            actor = commitItem.author?.login ?: commitItem.commit.author?.name ?: username,
            summary = commitItem.commit.message.lines().firstOrNull() ?: "Commit ${commitItem.sha.take(7)}",
            source = "GITHUB_COMMIT",
            metadataJson = "{\"sha\":\"${commitItem.sha}\",\"url\":\"${commitItem.htmlUrl ?: ""}\"}"
        )
    }
}
