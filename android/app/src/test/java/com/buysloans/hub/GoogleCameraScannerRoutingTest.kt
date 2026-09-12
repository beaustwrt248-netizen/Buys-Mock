package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GoogleCameraScannerRoutingTest {
    @Test
    fun scanDeviceKeepsGoogleCameraWhileDashboardDuplicateIsRemoved() {
        // Scan Device remains the dedicated camera entry point and Manual Search stays available.
        val source = File("src/main/java/com/buysloans/hub/DashboardActivity.kt").readText()
        assertTrue(source.contains("title = \"Scan Device\""))
        assertTrue(source.contains("subtitle = \"Google camera scan\""))
        assertTrue(source.contains("title = \"Manual Search\""))
        assertTrue(source.contains("BottomDestination.SCAN ->"))
        assertTrue(source.contains("Device scan\", \"Use the same Google camera scanner as Morley search."))
        assertTrue(source.split("Intent(context, UniversalBuySearchActivity::class.java)").size >= 4)

        // The dashboard no longer repeats the camera scanner as a standalone Google Camera card/banner.
        assertFalse(source.contains("Text(\"Google camera search\""))
        assertFalse(source.contains("Scan Device and Google camera search now use the same Google Play services camera scanner"))

        // Add to Catalogue still intentionally uses DeviceLensActivity for its two-photo add flow.
        assertTrue(source.contains("title = \"Add to Catalogue\""))
        assertTrue(source.contains("onClick = { context.startActivity(Intent(context, DeviceLensActivity::class.java)) }"))
    }
}
