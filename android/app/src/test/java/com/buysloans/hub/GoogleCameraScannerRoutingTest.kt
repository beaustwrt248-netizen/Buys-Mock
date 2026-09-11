package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GoogleCameraScannerRoutingTest {
    @Test
    fun scanDeviceAndGoogleCameraUseSameSearchCamera() {
        // All user-facing scan entry points must share the Google Play services scanner.
        val source = File("src/main/java/com/buysloans/hub/DashboardActivity.kt").readText()
        assertTrue(source.contains("title = \"Scan Device\""))
        assertTrue(source.contains("subtitle = \"Google camera scan\""))
        assertTrue(source.contains("BottomDestination.SCAN ->"))
        assertTrue(source.contains("Device scan\", \"Use the same Google camera scanner as Morley search."))
        assertTrue(source.split("Intent(context, UniversalBuySearchActivity::class.java)").size >= 4)
        assertFalse(source.contains("Intent(context, DeviceLensActivity::class.java)"))
    }
}
