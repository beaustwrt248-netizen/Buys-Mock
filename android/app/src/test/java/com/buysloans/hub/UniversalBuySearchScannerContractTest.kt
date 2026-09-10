package com.buysloans.hub

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
}
