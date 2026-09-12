package com.buysloans.hub

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class AiScanHistoryNavigationContractTest {
    @Test
    fun `more workspace exposes retained AI scan history`() {
        val src = File("src/main/java/com/buysloans/hub/DashboardActivity.kt").readText()
        assertTrue(src.contains("\"AI scan history\""))
        assertTrue(src.contains("Intent(context, AiScanHistoryActivity::class.java)"))
    }
}
