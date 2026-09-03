package com.buysloans.hub

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AdaptiveDashboardContractTest {
    @Test
    fun dashboardUsesAdaptiveNavigationBackHandlingAndEdgeToEdge() {
        val source = File("src/main/java/com/buysloans/hub/DashboardActivity.kt").readText()
        assertTrue(source.contains("enableEdgeToEdge()"))
        assertTrue(source.contains("morleyAdaptiveSize()"))
        assertTrue(source.contains("AdaptiveBackHandler"))
        assertTrue(source.contains("MorleyAdaptiveNavigation"))
        assertTrue(source.contains("AdaptiveContentFrame"))
        assertTrue(source.contains("MorleyAdaptiveSize.Compact"))
    }
}
