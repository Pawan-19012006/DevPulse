package com.devpulse.ai.domain

import com.devpulse.ai.data.local.entity.DeveloperEventEntity
import com.devpulse.ai.model.EventType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ActivityAggregator {

    fun computeSignals(events: List<DeveloperEventEntity>, window: TimeWindow): ActivitySignals {
        val windowEvents = events.filter { it.timestamp in window.startTime..window.endTime }
        if (windowEvents.isEmpty()) return ActivitySignals()

        var commits = 0
        var prsOpened = 0
        var prsMerged = 0
        var issuesOpened = 0
        var issuesClosed = 0
        var reviews = 0
        var additions = 0
        var deletions = 0

        val touchedRepos = mutableSetOf<String>()
        val repoCounts = mutableMapOf<String, Int>()
        val languageCounts = mutableMapOf<String, Int>()
        val activeDaySet = mutableSetOf<String>()
        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        windowEvents.forEach { event ->
            touchedRepos.add(event.repositoryName)
            repoCounts[event.repositoryName] = (repoCounts[event.repositoryName] ?: 0) + 1
            activeDaySet.add(dayFormat.format(Date(event.timestamp)))
            additions += event.additions
            deletions += event.deletions

            event.language?.let {
                languageCounts[it] = (languageCounts[it] ?: 0) + 1
            }

            when (event.eventType) {
                EventType.COMMIT.name -> commits++
                EventType.PULL_REQUEST_OPENED.name -> prsOpened++
                EventType.PULL_REQUEST_MERGED.name -> prsMerged++
                EventType.PULL_REQUEST_REVIEW.name -> reviews++
                EventType.ISSUE_OPENED.name -> issuesOpened++
                EventType.ISSUE_CLOSED.name -> issuesClosed++
            }
        }

        val topLanguages = languageCounts.entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }

        val activeRepositories = repoCounts.entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }

        return ActivitySignals(
            commitsCount = commits,
            prsOpenedCount = prsOpened,
            prsMergedCount = prsMerged,
            issuesOpenedCount = issuesOpened,
            issuesClosedCount = issuesClosed,
            reviewsCount = reviews,
            activeRepositoriesCount = touchedRepos.size,
            activeDaysCount = activeDaySet.size,
            additions = additions,
            deletions = deletions,
            topLanguages = topLanguages,
            activeRepositories = activeRepositories
        )
    }

    /**
     * Calculates period-over-period percentage change between two integer signals.
     * Returns InsufficientHistory if the previous period has no recorded baseline events.
     */
    fun compareMetric(
        currentVal: Int,
        previousVal: Int,
        hasHistoricalData: Boolean
    ): ComparisonResult<Int> {
        if (!hasHistoricalData) {
            return ComparisonResult.InsufficientHistory(
                current = currentVal,
                reason = "No prior activity recorded for comparison period"
            )
        }

        if (previousVal == 0) {
            return if (currentVal == 0) {
                ComparisonResult.Available(current = 0, previous = 0, percentageChange = 0.0)
            } else {
                ComparisonResult.InsufficientHistory(
                    current = currentVal,
                    reason = "Baseline was 0 events (trend percentage not mathematically defined)"
                )
            }
        }

        val change = ((currentVal.toDouble() - previousVal.toDouble()) / previousVal.toDouble()) * 100.0
        return ComparisonResult.Available(
            current = currentVal,
            previous = previousVal,
            percentageChange = change
        )
    }
}
