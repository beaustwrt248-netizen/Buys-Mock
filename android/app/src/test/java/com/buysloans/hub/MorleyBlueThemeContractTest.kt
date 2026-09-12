package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MorleyBlueThemeContractTest {
    @Test
    fun appAndCategoryChromeUseTheSharedMorleyBlue() {
        val theme = File("src/main/java/com/buysloans/hub/MorleyVisualTheme.kt").readText()
        val dashboard = File("src/main/java/com/buysloans/hub/DashboardActivity.kt").readText()
        val pricingVisuals = File("src/main/java/com/buysloans/hub/PricingVisuals.kt").readText()

        assertTrue(theme.contains("internal val MorleyAccent = Color(0xFF0878F9)"))
        assertTrue(theme.contains("internal val MorleyAccentSoft = Color(0xFFEAF4FF)"))
        assertFalse(theme.contains("0xFF167A5A"))
        assertFalse(theme.contains("0xFF0F684C"))

        assertTrue(dashboard.contains("private val ReferenceNavy = MorleyAccent"))
        assertTrue(dashboard.contains("private val ReferenceBlue = MorleyAccent"))
        assertTrue(dashboard.contains("private val ReferencePaleBlue = MorleyAccentSoft"))

        assertTrue(pricingVisuals.contains("val background = MorleyAccentSoft"))
        assertTrue(pricingVisuals.contains("MorleyAccent, Modifier.size(31.dp)"))
    }
}
