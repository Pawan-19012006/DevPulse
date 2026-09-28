package com.devpulse.ai.domain.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryCenterTest {

    @Test
    fun `recovery categories contain all 4 primary developer strain areas`() {
        val categories = RecoveryCategory.values()
        assertEquals(4, categories.size)
        assertTrue(categories.contains(RecoveryCategory.EYES))
        assertTrue(categories.contains(RecoveryCategory.FOCUS))
        assertTrue(categories.contains(RecoveryCategory.BODY))
        assertTrue(categories.contains(RecoveryCategory.STRESS))
    }

    @Test
    fun `recovery catalog contains activities for all 4 categories`() {
        val eyesActivities = RecoveryCatalog.getActivitiesForCategory(RecoveryCategory.EYES)
        val focusActivities = RecoveryCatalog.getActivitiesForCategory(RecoveryCategory.FOCUS)
        val bodyActivities = RecoveryCatalog.getActivitiesForCategory(RecoveryCategory.BODY)
        val stressActivities = RecoveryCatalog.getActivitiesForCategory(RecoveryCategory.STRESS)

        assertTrue("Eyes should have at least 3 activities", eyesActivities.size >= 3)
        assertTrue("Focus should have at least 3 activities", focusActivities.size >= 3)
        assertTrue("Body should have at least 3 activities", bodyActivities.size >= 3)
        assertTrue("Stress should have at least 3 activities", stressActivities.size >= 3)
    }

    @Test
    fun `eye activities contain eye focus reset, screen rest, and blink refocus`() {
        val activities = RecoveryCatalog.getActivitiesForCategory(RecoveryCategory.EYES)
        val ids = activities.map { it.id }

        assertTrue(ids.contains("eye_focus_reset"))
        assertTrue(ids.contains("screen_rest"))
        assertTrue(ids.contains("blink_refocus"))

        val eyeFocus = activities.first { it.id == "eye_focus_reset" }
        assertEquals(RecoveryExperienceType.EYE_FOCUS_POINT, eyeFocus.type)
        assertEquals(4, eyeFocus.steps.size)
    }

    @Test
    fun `focus activities contain focus reset, single-task reset, and breathing focus`() {
        val activities = RecoveryCatalog.getActivitiesForCategory(RecoveryCategory.FOCUS)
        val ids = activities.map { it.id }

        assertTrue(ids.contains("focus_reset"))
        assertTrue(ids.contains("single_task_reset"))
        assertTrue(ids.contains("breathing_focus"))

        val singleTask = activities.first { it.id == "single_task_reset" }
        assertEquals(RecoveryExperienceType.SINGLE_TASK_INPUT, singleTask.type)

        val breathingFocus = activities.first { it.id == "breathing_focus" }
        assertEquals(RecoveryExperienceType.BOX_BREATHING, breathingFocus.type)
    }

    @Test
    fun `body activities contain posture reset, shoulder neck reset, and movement break`() {
        val activities = RecoveryCatalog.getActivitiesForCategory(RecoveryCategory.BODY)
        val ids = activities.map { it.id }

        assertTrue(ids.contains("posture_reset"))
        assertTrue(ids.contains("shoulder_neck_reset"))
        assertTrue(ids.contains("movement_break"))

        activities.forEach {
            assertTrue(it.steps.isNotEmpty())
            assertEquals(HealthEventType.MOVEMENT, it.healthEventType)
        }
    }

    @Test
    fun `stress activities contain box breathing, guided calm, and quick reset`() {
        val activities = RecoveryCatalog.getActivitiesForCategory(RecoveryCategory.STRESS)
        val ids = activities.map { it.id }

        assertTrue(ids.contains("box_breathing"))
        assertTrue(ids.contains("guided_calm"))
        assertTrue(ids.contains("quick_reset"))

        val boxBreathing = activities.first { it.id == "box_breathing" }
        assertEquals(RecoveryExperienceType.BOX_BREATHING, boxBreathing.type)
    }

    @Test
    fun `all activities map to valid health event types for Room persistence`() {
        RecoveryCatalog.activities.forEach { activity ->
            assertNotNull(activity.healthEventType)
            assertTrue(activity.durationMinutes > 0)
            assertTrue(activity.title.isNotBlank())
            assertTrue(activity.description.isNotBlank())
        }
    }
}
