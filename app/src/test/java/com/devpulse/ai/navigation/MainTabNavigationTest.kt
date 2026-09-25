package com.devpulse.ai.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainTabNavigationTest {

    @Test
    fun `verify exactly four primary pillar destinations exist`() {
        val tabs = MainTab.values()
        assertEquals("Primary navigation must have exactly 4 destinations", 4, tabs.size)
        assertEquals(MainTab.SESSIONS, tabs[0])
        assertEquals(MainTab.HEALTH, tabs[1])
        assertEquals(MainTab.COACH, tabs[2])
        assertEquals(MainTab.TRACKER, tabs[3])
    }

    @Test
    fun `verify tab titles match DevPulse four pillars`() {
        assertEquals("Sessions", MainTab.SESSIONS.title)
        assertEquals("Health", MainTab.HEALTH.title)
        assertEquals("Coach", MainTab.COACH.title)
        assertEquals("Tracker", MainTab.TRACKER.title)
    }

    @Test
    fun `verify default entry point routes to Main container`() {
        assertEquals("main", Screen.Main.route)
        assertEquals("main", Screen.Home.route)
    }
}
