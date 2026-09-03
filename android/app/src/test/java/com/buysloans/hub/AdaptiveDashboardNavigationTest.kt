package com.buysloans.hub

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AdaptiveDashboardNavigationTest {
    @Test
    fun compactAndRailNavigationShareTheSameDestinations() {
        val source = File("src/main/java/com/buysloans/hub/DashboardActivity.kt").readText()
        assertTrue(source.contains("BottomDestination.entries.map"))
        assertTrue(source.contains("CompactDashboardNavigation"))
        assertTrue(source.contains("BottomDestination.CATEGORIES -> { showMenu = false; page = Page.Laptop }"))
        assertTrue(source.contains("BottomDestination.GP -> { showMenu = false; page = Page.GP }"))
        assertTrue(source.contains("BottomDestination.MORE -> openMenu()"))
    }
}
