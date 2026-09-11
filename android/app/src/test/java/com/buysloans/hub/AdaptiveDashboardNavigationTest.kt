package com.buysloans.hub

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AdaptiveDashboardNavigationTest {
    @Test
    fun compactNavigationUsesApprovedScanFirstDestinations() {
        val source = File("src/main/java/com/buysloans/hub/DashboardActivity.kt").readText()

        assertTrue(source.contains("BottomDestination.entries.forEach"))
        assertTrue(source.contains("STOCK(\"Stock\", MorleyIcons.Categories)"))
        assertTrue(source.contains("SCAN(\"Scan\", MorleyIcons.Phone)"))
        assertTrue(source.contains("TRADE(\"Trade\", MorleyIcons.Money)"))
        assertTrue(source.contains("BottomDestination.STOCK ->"))
        assertTrue(source.contains("Intent(context, DeviceLensActivity::class.java)"))
        assertTrue(source.contains("BottomDestination.TRADE -> { showMenu = false; page = Page.GP }"))
        assertTrue(source.contains("BottomDestination.MORE -> openMenu()"))
    }

    @Test
    fun expandedNavigationPreservesCategoriesAndGeneralBuysRoutes() {
        val source = File("src/main/java/com/buysloans/hub/DashboardActivity.kt").readText()

        assertTrue(source.contains("ExpandedDestination.entries.map"))
        assertTrue(source.contains("CATEGORIES(\"Categories\", MorleyIcons.Categories)"))
        assertTrue(source.contains("GP(\"General Buys\", MorleyIcons.Money)"))
        assertTrue(source.contains("ExpandedDestination.CATEGORIES -> { showMenu = false; page = Page.Laptop }"))
        assertTrue(source.contains("ExpandedDestination.GP -> { showMenu = false; page = Page.GP }"))
        assertTrue(source.contains("ExpandedDestination.MORE -> openMenu()"))
        assertTrue(source.contains("MorleyAdaptiveNavigation(size = adaptiveSize, items = adaptiveNavItems, compact = {})"))
    }
}
