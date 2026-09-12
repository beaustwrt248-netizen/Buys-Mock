package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GoogleCameraScannerRoutingTest {
    @Test
    fun deviceScanUsesMorleyVisionWhileCodeSearchKeepsGoogleCamera() {
        val source = File("src/main/java/com/buysloans/hub/DashboardActivity.kt").readText()

        // Device scanning is the Morley Vision two-photo path.
        assertTrue(source.contains("title = \"Scan Device\""))
        assertTrue(source.contains("subtitle = \"AI device scan\""))
        assertTrue(source.contains("BottomDestination.SCAN ->"))
        assertTrue(source.contains("context.startActivity(Intent(context, DeviceLensActivity::class.java))"))
        assertTrue(source.contains("Device scan\", \"Take front and back photos for model, damage and condition analysis."))

        // Google Play services remains available for barcode / QR / encoded search only.
        assertTrue(source.contains("title = \"Manual Search\""))
        assertTrue(source.contains("Google camera search"))
        assertTrue(source.contains("Scan a barcode, QR code or encoded model/stock label"))
        assertTrue(source.contains("Intent(context, UniversalBuySearchActivity::class.java)"))
        assertTrue(source.contains("Morley search\", \"Search by name/model or scan a code with the Google camera."))

        // The old unified routing contract must not return.
        assertFalse(source.contains("subtitle = \"Google camera scan\""))
        assertFalse(source.contains("Device scan\", \"Use the same Google camera scanner as Morley search."))
        assertFalse(source.contains("Scan Device and Google camera search now use the same Google Play services camera scanner"))

        // Add to Catalogue continues to use Device Lens for its two-photo intake flow.
        assertTrue(source.contains("title = \"Add to Catalogue\""))
        assertTrue(source.contains("onClick = { context.startActivity(Intent(context, DeviceLensActivity::class.java)) }"))
    }
}
