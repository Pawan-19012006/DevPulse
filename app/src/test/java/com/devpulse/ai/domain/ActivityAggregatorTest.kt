package com.devpulse.ai.domain

import com.devpulse.ai.data.local.entity.DeveloperEventEntity
import com.devpulse.ai.model.EventType
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class ActivityAggregatorTest {

    @Test
    fun computeSignals_emptyEvents_returnsZeroCounts() {
        val window = TimeWindow.Last30Days()
        val signals = ActivityAggregator.computeSignals(emptyList(), window)

        assertEquals(0, signals.commitsCount)
        assertEquals(0, signals.prsOpenedCount)
        assertEquals(0, signals.prsMergedCount)
        assertEquals(0, signals.issuesOpenedCount)
        assertEquals(0, signals.issuesClosedCount)
        assertEquals(0, signals.reviewsCount)
        assertEquals(0, signals.activeRepositoriesCount)
        assertEquals(0, signals.activeDaysCount)
        assertEquals(0, signals.totalSignals)
        assertTrue(signals.topLanguages.isEmpty())
    }

    @Test
    fun computeSignals_filtersByTimeWindowAndCalculatesAccurateTotals() {
        val now = System.currentTimeMillis()
        val twoDaysAgo = now - TimeUnit.DAYS.toMillis(2)
        val tenDaysAgo = now - TimeUnit.DAYS.toMillis(10)
        val fortyDaysAgo = now - TimeUnit.DAYS.toMillis(40)

        val events = listOf(
            // In 7-day window
            DeveloperEventEntity(
                id = "c1",
                developerUsername = "dev1",
                repositoryName = "dev1/RepoA",
                eventType = EventType.COMMIT.name,
                timestamp = twoDaysAgo,
                actor = "dev1",
                summary = "Commit 1",
                additions = 50,
                deletions = 10,
                language = "Kotlin"
            ),
            DeveloperEventEntity(
                id = "pr1",
                developerUsername = "dev1",
                repositoryName = "dev1/RepoB",
                eventType = EventType.PULL_REQUEST_OPENED.name,
                timestamp = twoDaysAgo,
                actor = "dev1",
                summary = "PR 1",
                language = "Python"
            ),
            // In 30-day window, but not 7-day
            DeveloperEventEntity(
                id = "c2",
                developerUsername = "dev1",
                repositoryName = "dev1/RepoA",
                eventType = EventType.COMMIT.name,
                timestamp = tenDaysAgo,
                actor = "dev1",
                summary = "Commit 2",
                additions = 30,
                deletions = 5,
                language = "Kotlin"
            ),
            // Outside 30-day window
            DeveloperEventEntity(
                id = "c3",
                developerUsername = "dev1",
                repositoryName = "dev1/RepoA",
                eventType = EventType.COMMIT.name,
                timestamp = fortyDaysAgo,
                actor = "dev1",
                summary = "Old commit"
            )
        )

        // Test 7-day window
        val signals7d = ActivityAggregator.computeSignals(events, TimeWindow.Last7Days(now))
        assertEquals(1, signals7d.commitsCount)
        assertEquals(1, signals7d.prsOpenedCount)
        assertEquals(2, signals7d.activeRepositoriesCount)
        assertEquals(1, signals7d.activeDaysCount)
        assertEquals(2, signals7d.totalSignals)

        // Test 30-day window
        val signals30d = ActivityAggregator.computeSignals(events, TimeWindow.Last30Days(now))
        assertEquals(2, signals30d.commitsCount)
        assertEquals(1, signals30d.prsOpenedCount)
        assertEquals(80, signals30d.additions)
        assertEquals(15, signals30d.deletions)
        assertEquals(3, signals30d.totalSignals)
    }

    @Test
    fun compareMetric_withSufficientHistory_calculatesAccuratePercentage() {
        val comparison = ActivityAggregator.compareMetric(
            currentVal = 42,
            previousVal = 30,
            hasHistoricalData = true
        )

        assertTrue(comparison is ComparisonResult.Available)
        val available = comparison as ComparisonResult.Available
        assertEquals(42, available.current)
        assertEquals(30, available.previous)
        assertEquals(40.0, available.percentageChange!!, 0.01)
    }

    @Test
    fun compareMetric_withInsufficientHistory_returnsExplicitState() {
        val comparison = ActivityAggregator.compareMetric(
            currentVal = 15,
            previousVal = 0,
            hasHistoricalData = false
        )

        assertTrue(comparison is ComparisonResult.InsufficientHistory)
        val insufficient = comparison as ComparisonResult.InsufficientHistory
        assertEquals(15, insufficient.current)
        assertTrue(insufficient.reason.contains("No prior activity recorded"))
    }

    @Test
    fun compareMetric_whenZeroToZero_returnsZeroChange() {
        val comparison = ActivityAggregator.compareMetric(
            currentVal = 0,
            previousVal = 0,
            hasHistoricalData = true
        )

        assertTrue(comparison is ComparisonResult.Available)
        val available = comparison as ComparisonResult.Available
        assertEquals(0.0, available.percentageChange!!, 0.01)
    }
}
