package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GoogleCameraScannerRoutingTest {
    @Test
    fun deviceScanUsesMorleyVisionWhileCodeSearchKeepsGoogleCamera() {
        val source = File("src/main/java/com/buysloans/hub/DashboardActivity.kt").readText()

        // Device scanning remains the Morley Vision two-photo path.
        assertTrue(source.contains("title = \"Scan Device\""))
        assertTrue(source.contains("subtitle = \"AI device scan\""))
        assertTrue(source.contains("BottomDestination.SCAN ->"))
        assertTrue(source.contains("Intent(context, DeviceLensActivity::class.java)"))
        assertTrue(source.contains("\"Device scan\""))
        assertTrue(source.contains("Take front and back photos for model, damage and condition analysis."))

        // Manual/code search remains a purpose-specific Universal Buy Search route.
        assertTrue(source.contains("title = \"Manual Search\""))
        assertTrue(source.contains("openSearch(UniversalBuySearchMode.MANUAL_SEARCH)"))
        assertTrue(source.contains("Intent(context, UniversalBuySearchActivity::class.java)"))
        assertTrue(source.contains("Google code scanning available inside the search flow."))

        // The retired unified Google-camera dashboard route must not return.
        assertFalse(source.contains("title = \"Google camera search\""))
        assertFalse(source.contains("subtitle = \"Google camera scan\""))
        assertFalse(source.contains("Device scan\", \"Use the same Google camera scanner as Morley search."))
        assertFalse(source.contains("Scan Device and Google camera search now use the same Google Play services camera scanner"))

        // Add to Catalogue continues to use Device Lens for its two-photo intake flow.
        assertTrue(source.contains("title = \"Add to Catalogue\""))
        assertTrue(source.contains("onClick = { context.startActivity(Intent(context, DeviceLensActivity::class.java)) }"))
    }
}
