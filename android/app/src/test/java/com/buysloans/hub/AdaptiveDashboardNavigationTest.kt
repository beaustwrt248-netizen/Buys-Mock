package com.buysloans.hub

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
        assertTrue(source.contains("BottomDestination.SCAN -> {\n                showMenu = false\n                context.startActivity(Intent(context, DeviceLensActivity::class.java))"))
        assertTrue(source.contains("title = \"Scan Device\""))
        assertTrue(source.contains("subtitle = \"AI device scan\""))
        assertTrue(source.contains("title = \"Manual Search\""))
        assertTrue(source.contains("Intent(context, UniversalBuySearchActivity::class.java)"))
        assertTrue(source.contains("MenuRow(\"◉\", \"Device scan\", \"Take front and back photos for model, damage and condition analysis.\") { context.startActivity(Intent(context, DeviceLensActivity::class.java)) }"))
        assertTrue(source.contains("Google camera search"))
        assertTrue(source.contains("Scan a barcode, QR code or encoded model/stock label"))
        assertTrue(source.contains("Device Scan uses Morley Vision for the two-photo model, condition and damage assessment. Google Play services is used separately for code scanning in Morley Search."))
        assertTrue(source.contains("BottomDestination.TRADE -> { showMenu = false; page = Page.GP }"))
        assertTrue(!source.contains("BottomDestination.MORE"))
        assertTrue(!source.contains("BottomDestination.STOCK"))
        assertTrue(source.contains("MenuRow(\"▣\", \"Stock\", \"Current inventory, costs and resale values.\") { open(\"inventory\") }"))
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
