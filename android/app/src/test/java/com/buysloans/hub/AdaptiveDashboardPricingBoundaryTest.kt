package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class AdaptiveDashboardPricingBoundaryTest {
    @Test
    fun dashboardIntegrationDoesNotEmbedPricingMath() {
        val source = File("src/main/java/com/buysloans/hub/DashboardActivity.kt").readText()
        assertFalse(source.contains("0.70"))
        assertFalse(source.contains("0.50"))
        assertFalse(source.contains("0.30"))
        assertFalse(source.contains("priceSheetValue *"))
    }
}
