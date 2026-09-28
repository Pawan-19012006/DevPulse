package com.devpulse.ai.domain.constellation

import androidx.compose.ui.geometry.Offset
import java.util.concurrent.TimeUnit

data class ConstellationStar(
    val id: String,
    val title: String,
    val category: String,
    val timestamp: Long,
    val position: Offset, // Normalized (0f..1f, 0f..1f)
    val isMock: Boolean = false,
    val isLatest: Boolean = false
)

data class ConstellationGoal(
    val title: String = "Become a Backend Developer",
    val targetDays: Int = 60
)

object ConstellationLayout {

    /**
     * Handcrafted celestial coordinate layout for up to 60 stars.
     * Guaranteed to stay inside safe margins [0.10f..0.90f, 0.12f..0.88f].
     */
    val STAR_COORDINATES: List<Offset> = listOf(
        // 0..11: Core Constellation (Ascending Pegasus / Northern Cross shape)
        Offset(0.14f, 0.68f), // 0: Left wing lower
        Offset(0.26f, 0.54f), // 1: Left wing middle
        Offset(0.20f, 0.36f), // 2: Left wing tip
        Offset(0.36f, 0.40f), // 3: Left shoulder
        Offset(0.42f, 0.22f), // 4: Apex / Crown peak
        Offset(0.58f, 0.20f), // 5: Crown right
        Offset(0.52f, 0.44f), // 6: Center nexus / Heart
        Offset(0.66f, 0.38f), // 7: Right shoulder
        Offset(0.74f, 0.54f), // 8: Right wing middle
        Offset(0.60f, 0.70f), // 9: Lower tail anchor
        Offset(0.82f, 0.32f), // 10: Right wing tip
        Offset(0.88f, 0.18f), // 11: Soaring beacon

        // 12..23: Outer Crown & Orbit Arc
        Offset(0.74f, 0.14f), // 12
        Offset(0.50f, 0.10f), // 13
        Offset(0.30f, 0.14f), // 14
        Offset(0.12f, 0.22f), // 15
        Offset(0.08f, 0.46f), // 16
        Offset(0.28f, 0.76f), // 17
        Offset(0.44f, 0.82f), // 18
        Offset(0.68f, 0.82f), // 19
        Offset(0.84f, 0.70f), // 20
        Offset(0.86f, 0.48f), // 21
        Offset(0.64f, 0.56f), // 22
        Offset(0.40f, 0.58f), // 23

        // 24..35: Inner Helix
        Offset(0.48f, 0.32f), // 24
        Offset(0.32f, 0.28f), // 25
        Offset(0.24f, 0.44f), // 26
        Offset(0.36f, 0.66f), // 27
        Offset(0.52f, 0.72f), // 28
        Offset(0.68f, 0.64f), // 29
        Offset(0.72f, 0.44f), // 30
        Offset(0.62f, 0.28f), // 31
        Offset(0.78f, 0.22f), // 32
        Offset(0.86f, 0.38f), // 33
        Offset(0.78f, 0.78f), // 34
        Offset(0.58f, 0.86f), // 35

        // 36..47: Celestial Ribbon
        Offset(0.38f, 0.86f), // 36
        Offset(0.18f, 0.78f), // 37
        Offset(0.10f, 0.60f), // 38
        Offset(0.14f, 0.30f), // 39
        Offset(0.24f, 0.18f), // 40
        Offset(0.38f, 0.12f), // 41
        Offset(0.62f, 0.12f), // 42
        Offset(0.82f, 0.14f), // 43
        Offset(0.90f, 0.28f), // 44
        Offset(0.90f, 0.60f), // 45
        Offset(0.74f, 0.76f), // 46
        Offset(0.50f, 0.78f), // 47

        // 48..59: Radiant Horizon
        Offset(0.30f, 0.70f), // 48
        Offset(0.18f, 0.50f), // 49
        Offset(0.28f, 0.34f), // 50
        Offset(0.46f, 0.20f), // 51
        Offset(0.66f, 0.20f), // 52
        Offset(0.80f, 0.38f), // 53
        Offset(0.80f, 0.58f), // 54
        Offset(0.64f, 0.68f), // 55
        Offset(0.48f, 0.68f), // 56
        Offset(0.36f, 0.52f), // 57
        Offset(0.46f, 0.40f), // 58
        Offset(0.58f, 0.40f)  // 59
    )

    /**
     * Standard constellation edges connecting the stars.
     */
    val BASE_EDGES: List<Pair<Int, Int>> = listOf(
        0 to 1,
        1 to 2,
        2 to 3,
        1 to 3,
        3 to 4,
        4 to 5,
        5 to 6,
        3 to 6,
        6 to 7,
        7 to 8,
        8 to 9,
        6 to 9,
        7 to 10,
        10 to 11
    )

    /**
     * Returns the list of lines to draw for a given count of stars.
     */
    fun getEdgesForStarCount(count: Int): List<Pair<Int, Int>> {
        val edges = mutableListOf<Pair<Int, Int>>()
        // Add base edges that connect existing stars
        for (edge in BASE_EDGES) {
            if (edge.first < count && edge.second < count) {
                edges.add(edge)
            }
        }
        // For extended stars (index >= 12), connect each star to its predecessor
        for (i in 12 until count) {
            edges.add((i - 1) to i)
            if (i % 4 == 0 && (i - 4) >= 0) {
                edges.add((i - 4) to i)
            }
        }
        return edges
    }

    /**
     * Fallback position generator if star count exceeds pre-calculated points.
     */
    fun getStarPosition(index: Int): Offset {
        return if (index < STAR_COORDINATES.size) {
            STAR_COORDINATES[index]
        } else {
            val angle = (index * 0.4f)
            val radius = 0.35f + (index % 5) * 0.05f
            Offset(
                x = (0.5f + kotlin.math.cos(angle) * radius).toFloat().coerceIn(0.10f, 0.90f),
                y = (0.5f + kotlin.math.sin(angle) * radius).toFloat().coerceIn(0.12f, 0.88f)
            )
        }
    }
}

object MockConstellation {
    private val now = System.currentTimeMillis()
    private val dayMillis = TimeUnit.DAYS.toMillis(1)

    val stars: List<ConstellationStar> = listOf(
        ConstellationStar(
            id = "mock_star_1",
            title = "Learned coroutine cancellation hierarchy",
            category = "Technical Skill",
            timestamp = now - (11 * dayMillis),
            position = ConstellationLayout.getStarPosition(0),
            isMock = true
        ),
        ConstellationStar(
            id = "mock_star_2",
            title = "Solved 3 graph traversal problems",
            category = "Problem Solving",
            timestamp = now - (10 * dayMillis),
            position = ConstellationLayout.getStarPosition(1),
            isMock = true
        ),
        ConstellationStar(
            id = "mock_star_3",
            title = "Implemented JWT refresh-token handling",
            category = "Building & Shipping",
            timestamp = now - (9 * dayMillis),
            position = ConstellationLayout.getStarPosition(2),
            isMock = true
        ),
        ConstellationStar(
            id = "mock_star_4",
            title = "Improved API 401 error interceptor",
            category = "Technical Skill",
            timestamp = now - (8 * dayMillis),
            position = ConstellationLayout.getStarPosition(3),
            isMock = true
        ),
        ConstellationStar(
            id = "mock_star_5",
            title = "Designed Room database & schema",
            category = "Building & Shipping",
            timestamp = now - (7 * dayMillis),
            position = ConstellationLayout.getStarPosition(4),
            isMock = true
        ),
        ConstellationStar(
            id = "mock_star_6",
            title = "Refactored repository caching layer",
            category = "Technical Skill",
            timestamp = now - (6 * dayMillis),
            position = ConstellationLayout.getStarPosition(5),
            isMock = true
        ),
        ConstellationStar(
            id = "mock_star_7",
            title = "Fixed Compose state recomposition bug",
            category = "Technical Skill",
            timestamp = now - (5 * dayMillis),
            position = ConstellationLayout.getStarPosition(6),
            isMock = true
        ),
        ConstellationStar(
            id = "mock_star_8",
            title = "Connected GitHub GraphQL telemetry",
            category = "Building & Shipping",
            timestamp = now - (4 * dayMillis),
            position = ConstellationLayout.getStarPosition(7),
            isMock = true
        ),
        ConstellationStar(
            id = "mock_star_9",
            title = "Optimized list view memory footprint",
            category = "Problem Solving",
            timestamp = now - (3 * dayMillis),
            position = ConstellationLayout.getStarPosition(8),
            isMock = true
        ),
        ConstellationStar(
            id = "mock_star_10",
            title = "Understood structured concurrency scopes",
            category = "Knowledge",
            timestamp = now - (2 * dayMillis),
            position = ConstellationLayout.getStarPosition(9),
            isMock = true
        ),
        ConstellationStar(
            id = "mock_star_11",
            title = "Added unit test coverage for session engine",
            category = "Building & Shipping",
            timestamp = now - (1 * dayMillis),
            position = ConstellationLayout.getStarPosition(10),
            isMock = true
        )
    )
}
