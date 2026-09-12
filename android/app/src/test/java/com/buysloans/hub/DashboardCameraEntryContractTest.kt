package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DashboardCameraEntryContractTest {
    @Test
    fun dashboardKeepsScanDeviceButDoesNotDuplicateGoogleCameraSearch() {
        val source = File("src/main/java/com/buysloans/hub/DashboardActivity.kt").readText()

        assertTrue(source.contains("title = \"Scan Device\""))
        assertTrue(source.contains("title = \"Manual Search\""))
        assertFalse(source.contains("Text(\"Google camera search\""))
        assertFalse(source.contains("Scan Device and Google camera search now use the same"))
    }
}
