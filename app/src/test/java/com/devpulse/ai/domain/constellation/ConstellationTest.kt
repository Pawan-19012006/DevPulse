package com.devpulse.ai.domain.constellation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConstellationTest {

    @Test
    fun mockConstellation_hasExpectedCountAndValidAttributes() {
        val stars = MockConstellation.stars
        assertEquals(11, stars.size)

        stars.forEachIndexed { index, star ->
            assertTrue("Star id must not be empty", star.id.isNotEmpty())
            assertTrue("Star title must not be empty", star.title.isNotEmpty())
            assertTrue("Star category must not be empty", star.category.isNotEmpty())
            assertTrue("Star must be marked as mock", star.isMock)
            assertTrue("Star X coordinate must be in safe bounds", star.position.x in 0.05f..0.95f)
            assertTrue("Star Y coordinate must be in safe bounds", star.position.y in 0.05f..0.95f)
        }
    }

    @Test
    fun constellationLayout_positionsAreDeterministic() {
        val pos0_first = ConstellationLayout.getStarPosition(0)
        val pos0_second = ConstellationLayout.getStarPosition(0)
        assertEquals(pos0_first.x, pos0_second.x, 0.0001f)
        assertEquals(pos0_first.y, pos0_second.y, 0.0001f)

        // Check fallback beyond 60 stars
        val pos70 = ConstellationLayout.getStarPosition(70)
        assertNotNull(pos70)
        assertTrue(pos70.x in 0.05f..0.95f)
        assertTrue(pos70.y in 0.05f..0.95f)
    }

    @Test
    fun constellationLayout_edgesDoNotCrashForVariousCounts() {
        val testCounts = listOf(0, 1, 2, 5, 11, 12, 25, 60, 100)
        for (count in testCounts) {
            val edges = ConstellationLayout.getEdgesForStarCount(count)
            assertNotNull(edges)
            for (edge in edges) {
                assertTrue("Edge source must be < count", edge.first < count)
                assertTrue("Edge target must be < count", edge.second < count)
            }
        }
    }
}
