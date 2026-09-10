package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UniversalBuySearchScannerContractTest {
    @Test
    fun universalSearchReusesGooglePlayCodeScannerAndFeedsResultIntoQuery() {
        val source = File("src/main/java/com/buysloans/hub/UniversalBuySearchActivity.kt").readText()

        assertTrue(source.contains("GmsBarcodeScanning.getClient(this)"))
        assertTrue(source.contains("googleCodeScanner.startScan()"))
        assertTrue(source.contains("Scan Code with Google Camera"))
        assertTrue(source.contains("query = scannedValue"))
        assertTrue(source.contains("UniversalBuySearch.search(query, 30)"))
    }

    @Test
    fun scannerStatusDoesNotEchoPayloadOrSdkDiagnostics() {
        val source = File("src/main/java/com/buysloans/hub/UniversalBuySearchActivity.kt").readText()

        assertTrue(source.contains("Code scanned — searching Morley."))
        assertTrue(source.contains("Google scanner is unavailable right now. You can still search manually."))
        assertFalse(source.contains("Scanned \$value"))
        assertFalse(source.contains("error.message"))
    }

    @Test
    fun scannerEnabledMorleySearchIsReachableFromWorkspaceMenu() {
        val dashboard = File("src/main/java/com/buysloans/hub/DashboardActivity.kt").readText()

        assertTrue(dashboard.contains("\"Morley search\""))
        assertTrue(dashboard.contains("Intent(context, UniversalBuySearchActivity::class.java)"))
        assertTrue(dashboard.contains("scan a code with the Google camera"))
    }
}
