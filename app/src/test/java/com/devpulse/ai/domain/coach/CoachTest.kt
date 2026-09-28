package com.devpulse.ai.domain.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachTest {

    @Test
    fun developerContextSummary_buildsCleanPromptSummary() {
        val summary = DeveloperContextSummary(
            currentGoal = "Become a Backend Developer",
            targetDays = 60,
            meaningfulDays = 13,
            currentDailyShip = "Implement refresh-token handling",
            dailyShipStatus = "In Progress",
            sessionsToday = 2,
            totalFocusMinutesToday = 50,
            averageSessionDuration = 25,
            recoverySessionsToday = 1,
            recentRecoveryTypes = listOf("EYE_REST"),
            latestWellnessCheck = "Energized",
            impedimentText = "None",
            recentOnePercentImprovements = listOf("Understood 401 interceptor chain"),
            recentOnePercentCategories = listOf("Technical Skill"),
            githubConnected = false
        )

        val prompt = summary.toPromptSummary()
        assertTrue(prompt.contains("Become a Backend Developer"))
        assertTrue(prompt.contains("Implement refresh-token handling"))
        assertTrue(prompt.contains("Today's sessions: 2"))
        assertTrue(prompt.contains("Recovery sessions: 1"))
        assertFalse(prompt.contains("token=")) // Ensures no secrets
    }

    @Test
    fun coachFallbackEngine_handlesHighStressCheckIn() {
        val summary = DeveloperContextSummary(
            latestWellnessCheck = "Stressed"
        )
        val insight = CoachFallbackEngine.generateGuidance(summary)
        assertEquals(CoachAction.START_RECOVERY, insight.action)
        assertTrue(insight.actionLabel.contains("Breathing"))
        assertFalse(insight.isFromAi)
    }

    @Test
    fun coachFallbackEngine_handlesLongSessionsWithoutRecovery() {
        val summary = DeveloperContextSummary(
            sessionsToday = 3,
            totalFocusMinutesToday = 60,
            recoverySessionsToday = 0
        )
        val insight = CoachFallbackEngine.generateGuidance(summary)
        assertEquals(CoachAction.START_RECOVERY, insight.action)
        assertEquals("Start Recovery", insight.actionLabel)
    }

    @Test
    fun coachFallbackEngine_conversationalReply_fatigueRecommendsRecovery() {
        val summary = DeveloperContextSummary(
            totalFocusMinutesToday = 180,
            recoverySessionsToday = 1,
            currentDailyShip = "Implement refresh-token handling"
        )
        val reply = CoachFallbackEngine.generateConversationalReply(
            userMessage = "I've been coding for 3 hours and I'm feeling exhausted.",
            context = summary
        )
        assertEquals(MessageSender.COACH, reply.sender)
        assertEquals(CoachAction.START_RECOVERY, reply.action)
        assertEquals("Start Recovery", reply.actionLabel)
        assertTrue(reply.text.contains("recovery break"))
        assertTrue(reply.text.contains("Daily Ship") || reply.text.contains("refresh-token handling"))
    }

    @Test
    fun coachFallbackEngine_conversationalReply_finishTaskReducesScope() {
        val summary = DeveloperContextSummary(
            currentDailyShip = "Implement refresh-token handling"
        )
        val reply = CoachFallbackEngine.generateConversationalReply(
            userMessage = "But I really want to finish my task.",
            context = summary
        )
        assertEquals(MessageSender.COACH, reply.sender)
        assertEquals(CoachAction.CONTINUE_DAILY_SHIP, reply.action)
        assertEquals("Continue Daily Ship", reply.actionLabel)
        assertTrue(reply.text.contains("Reduce the scope"))
        assertTrue(reply.text.contains("refresh-token handling"))
    }

    @Test
    fun coachFallbackEngine_conversationalReply_whatToWorkOnNext() {
        val summary = DeveloperContextSummary(
            currentGoal = "Become a Backend Developer",
            currentDailyShip = "Implement refresh-token handling"
        )
        val reply = CoachFallbackEngine.generateConversationalReply(
            userMessage = "What should I work on next?",
            context = summary
        )
        assertEquals(CoachAction.CONTINUE_DAILY_SHIP, reply.action)
        assertTrue(reply.text.contains("refresh-token handling"))
    }

    @Test
    fun coachFallbackEngine_conversationalReply_progressInquiry() {
        val summary = DeveloperContextSummary(
            currentGoal = "Become a Backend Developer",
            meaningfulDays = 15,
            targetDays = 30
        )
        val reply = CoachFallbackEngine.generateConversationalReply(
            userMessage = "How is my progress?",
            context = summary
        )
        assertEquals(CoachAction.VIEW_TRACKER, reply.action)
        assertEquals("View Constellation", reply.actionLabel)
        assertTrue(reply.text.contains("15 of 30"))
    }

    @Test
    fun coachFallbackEngine_conversationalReply_overworkingInquiry() {
        val summary = DeveloperContextSummary(
            totalFocusMinutesToday = 120,
            sessionsToday = 4,
            recoverySessionsToday = 0
        )
        val reply = CoachFallbackEngine.generateConversationalReply(
            userMessage = "Am I overworking?",
            context = summary
        )
        assertEquals(CoachAction.START_RECOVERY, reply.action)
        assertTrue(reply.text.contains("relatively high"))
    }
}
