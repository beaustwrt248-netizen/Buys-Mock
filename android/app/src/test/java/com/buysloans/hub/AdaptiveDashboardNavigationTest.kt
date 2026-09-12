package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AdaptiveDashboardNavigationTest {
    @Test
    fun compactNavigationUsesApprovedAiScanDestinations() {
        val source = File("src/main/java/com/buysloans/hub/DashboardActivity.kt").readText()

        assertTrue(source.contains("BottomDestination.entries.forEach"))
        assertTrue(source.contains("CATALOGUE(\"Catalogue\", MorleyIcons.Categories)"))
        assertTrue(source.contains("SCAN(\"Scan\", MorleyIcons.Phone)"))
        assertTrue(source.contains("TRADE(\"Trade\", MorleyIcons.Money)"))
        assertTrue(source.contains("BottomDestination.CATALOGUE -> { showMenu = false; page = Page.Laptop }"))
        assertTrue(source.contains("BottomDestination.SCAN ->"))
        assertTrue(source.contains("Intent(context, DeviceLensActivity::class.java)"))
        assertTrue(source.contains("title = \"Scan Device\""))
        assertTrue(source.contains("subtitle = \"AI device scan\""))
        assertTrue(source.contains("title = \"Manual Search\""))
        assertTrue(source.contains("openSearch(UniversalBuySearchMode.MANUAL_SEARCH)"))
        assertTrue(source.contains("title = \"Add to Catalogue\""))
        assertTrue(source.contains("\"Device scan\""))
        assertTrue(source.contains("Take front and back photos for model, damage and condition analysis."))
        assertTrue(source.contains("Google code scanning available inside the search flow."))
        assertTrue(source.contains("openSearch(UniversalBuySearchMode.PRICE_CHECK)"))
        assertTrue(source.contains("BottomDestination.TRADE -> { showMenu = false; page = Page.GP }"))
        assertFalse(source.contains("BottomDestination.MORE"))
        assertFalse(source.contains("BottomDestination.STOCK"))
        assertTrue(source.contains("\"Stock\""))
        assertTrue(source.contains("Current inventory, costs and resale values."))
        assertFalse(source.contains("title = \"Google camera search\""))
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
