package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceTestHistoryStoreTest {
    @Test
    fun historyRoundTripPreservesSourceReferenceAndTimestamp() {
        val entry = DeviceTestHistoryEntry(
            id = "history-1",
            source = DeviceTestHistorySource.NFC,
            reference = "04A1B2C3",
            itemName = "Phone",
            category = DeviceCategory.PHONE.name,
            result = "PASS",
            summary = "NFC tag responded; scan/read test only.",
            recordedAt = 123456789L
        )
        val decoded = DeviceTestHistoryStore.decode(DeviceTestHistoryStore.encode(listOf(entry)))
        assertEquals(listOf(entry), decoded)
    }

    @Test
    fun malformedHistoryIsIgnoredSafely() {
        assertTrue(DeviceTestHistoryStore.decode("not-json").isEmpty())
        assertTrue(DeviceTestHistoryStore.decode("[{\"source\":\"UNKNOWN\"}]").isEmpty())
    }

    @Test
    fun nfcHistoryModelContainsNoInventoryIdentitySemantics() {
        val source = DeviceTestHistorySource.NFC.name.lowercase()
        assertEquals("nfc", source)
        assertFalse(source.contains("inventory"))
        assertFalse(source.contains("assign"))
        assertFalse(source.contains("link"))
    }

    @Test
    fun barcodeAndTestBuySourcesRemainExplicitlySeparated() {
        assertEquals(DeviceTestHistorySource.BARCODE, DeviceTestHistorySource.valueOf("BARCODE"))
        assertEquals(DeviceTestHistorySource.TEST_BUY, DeviceTestHistorySource.valueOf("TEST_BUY"))
    }
}
