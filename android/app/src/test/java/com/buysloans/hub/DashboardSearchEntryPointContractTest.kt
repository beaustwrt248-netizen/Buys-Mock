package com.buysloans.hub

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardSearchEntryPointContractTest {
    private fun source() = File("src/main/java/com/buysloans/hub/DashboardActivity.kt").readText()

    @Test
    fun `dashboard routes search manual and price check with explicit modes`() {
        val src = source()
        assertTrue(src.contains("UniversalBuySearchMode.QUICK_SEARCH.wireValue"))
        assertTrue(src.contains("UniversalBuySearchMode.MANUAL_SEARCH.wireValue"))
        assertTrue(src.contains("UniversalBuySearchMode.PRICE_CHECK.wireValue"))
    }

    @Test
    fun `dashboard keeps AI scan camera-first`() {
        val src = source()
        assertTrue(src.contains("title = \"Scan Device\""))
        assertTrue(src.contains("Intent(context, DeviceLensActivity::class.java)"))
    }

    @Test
    fun `redundant Google camera dashboard card is removed`() {
        val src = source()
        assertFalse(src.contains("title = \"Google camera search\""))
        assertFalse(src.contains("Text(\"Google camera search\""))
    }
}