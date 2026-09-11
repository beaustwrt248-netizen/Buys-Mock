package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GoogleCameraScannerRoutingTest {
    @Test
    fun scanDeviceAndGoogleCameraUseSameSearchCamera() {
        // All user-facing scan entry points use UniversalBuySearchActivity. The
        // catalogue-enrolment tile may still open DeviceLensActivity because it is
        // an add-to-catalogue workflow, not a Scan Device entry point.
        val source = File("src/main/java/com/buysloans/hub/DashboardActivity.kt").readText()
        assertTrue(source.contains("title = \"Scan Device\""))
        assertTrue(source.contains("subtitle = \"Google camera scan\""))
        assertTrue(source.contains("BottomDestination.SCAN ->"))
        assertTrue(source.contains("Device scan\", \"Use the same Google camera scanner as Morley search."))
        assertTrue(source.split("Intent(context, UniversalBuySearchActivity::class.java)").size >= 4)

        val scanDestination = source.substringAfter("BottomDestination.SCAN ->").substringBefore("BottomDestination.TRADE ->")
        assertTrue(scanDestination.contains("Intent(context, UniversalBuySearchActivity::class.java)"))
        assertFalse(scanDestination.contains("Intent(context, DeviceLensActivity::class.java)"))

        val scanTile = source.substringAfter("title = \"Scan Device\"").substringBefore("title = \"Manual Search\"")
        assertTrue(scanTile.contains("Intent(context, UniversalBuySearchActivity::class.java)"))
        assertFalse(scanTile.contains("Intent(context, DeviceLensActivity::class.java)"))
    }
}
