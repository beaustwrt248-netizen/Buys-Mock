package com.buysloans.hub

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AdaptiveDashboardLargeScreenContractTest {
    @Test
    fun liveDashboardUsesBoundedAdaptiveContentOnWideWindows() {
        val dashboard = File("src/main/java/com/buysloans/hub/DashboardActivity.kt").readText()
        val adaptive = File("src/main/java/com/buysloans/hub/AdaptiveDashboard.kt").readText()
        assertTrue(dashboard.contains("AdaptiveContentFrame"))
        assertTrue(adaptive.contains("maxWidth >= 1200.dp -> 1120.dp"))
        assertTrue(adaptive.contains("maxWidth >= 840.dp -> 960.dp"))
        assertTrue(adaptive.contains("maxWidth >= 600.dp -> 760.dp"))
    }
}
