package com.devpulse.ai.domain.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionModelsTest {

    @Test
    fun `SessionActivityType has expected developer activities`() {
        val activities = SessionActivityType.values()
        assertTrue(activities.any { it == SessionActivityType.CODING })
        assertTrue(activities.any { it == SessionActivityType.DEBUGGING })
        assertTrue(activities.any { it == SessionActivityType.DSA })
        assertTrue(activities.any { it == SessionActivityType.LEARNING })
        assertTrue(activities.any { it == SessionActivityType.CODE_REVIEW })
        assertTrue(activities.any { it == SessionActivityType.PROJECT_WORK })
        assertTrue(activities.any { it == SessionActivityType.PLANNING })
        assertTrue(activities.any { it == SessionActivityType.CUSTOM })

        assertEquals("Coding", SessionActivityType.CODING.displayName)
        assertEquals("Debugging", SessionActivityType.DEBUGGING.displayName)
    }

    @Test
    fun `DeveloperState separates pre-session states from in-session states`() {
        val preSessionStates = DeveloperState.values().filter { it.isPreSessionOption }
        assertEquals(3, preSessionStates.size)
        assertTrue(preSessionStates.contains(DeveloperState.READY))
        assertTrue(preSessionStates.contains(DeveloperState.LOW_ENERGY))
        assertTrue(preSessionStates.contains(DeveloperState.MENTALLY_TIRED))

        // Ensure post/in-session states exist
        assertTrue(DeveloperState.values().contains(DeveloperState.FLOWING))
        assertTrue(DeveloperState.values().contains(DeveloperState.STUCK))
        assertTrue(DeveloperState.values().contains(DeveloperState.FRUSTRATED))
    }

    @Test
    fun `ImprovementCategory has expected 1 percent better badges`() {
        assertEquals("+1% Knowledge", ImprovementCategory.KNOWLEDGE.prefixBadge)
        assertEquals("+1% Skill", ImprovementCategory.SKILL.prefixBadge)
        assertEquals("+1% Building", ImprovementCategory.BUILDING.prefixBadge)
        assertEquals("+1% Problem Solving", ImprovementCategory.PROBLEM_SOLVING.prefixBadge)
        assertEquals("+1% Focus", ImprovementCategory.FOCUS.prefixBadge)
        assertEquals("+1% Recovery", ImprovementCategory.RECOVERY.prefixBadge)
        assertEquals("+1% Discipline", ImprovementCategory.DISCIPLINE.prefixBadge)
        assertEquals("+1% Creativity", ImprovementCategory.CREATIVITY.prefixBadge)
    }

    @Test
    fun `SessionStatus covers all phases`() {
        val statuses = SessionStatus.values()
        assertEquals(5, statuses.size)
        assertNotNull(SessionStatus.valueOf("PENDING"))
        assertNotNull(SessionStatus.valueOf("IN_PROGRESS"))
        assertNotNull(SessionStatus.valueOf("ON_BREAK"))
        assertNotNull(SessionStatus.valueOf("COMPLETED"))
        assertNotNull(SessionStatus.valueOf("CANCELLED"))
    }
}
