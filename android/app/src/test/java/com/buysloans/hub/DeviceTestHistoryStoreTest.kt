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
    fun structurallyValidButSemanticallyInvalidEntriesAreIgnored() {
        val invalid = listOf(
            DeviceTestHistoryEntry("", DeviceTestHistorySource.BARCODE, "123", "", "", "PASS", "", 1L),
            DeviceTestHistoryEntry("history-2", DeviceTestHistorySource.BARCODE, "", "", "", "PASS", "", 1L),
            DeviceTestHistoryEntry("history-3", DeviceTestHistorySource.TEST_BUY, "123", "Phone", "PHONE", "", "", 1L),
            DeviceTestHistoryEntry("history-4", DeviceTestHistorySource.NFC, "04A1", "", "PHONE", "PASS", "scan/read only", 0L)
        )

        invalid.forEach { entry ->
            assertTrue(DeviceTestHistoryStore.decode(DeviceTestHistoryStore.encode(listOf(entry))).isEmpty())
        }
    }

    @Test
    fun decodedFieldsAreNormalizedWithoutChangingNfcSemantics() {
        val entry = DeviceTestHistoryEntry(
            id = " history-5 ",
            source = DeviceTestHistorySource.NFC,
            reference = " 04A1B2C3 ",
            itemName = " Phone ",
            category = " PHONE ",
            result = " PASS ",
            summary = " NFC tag responded; scan/read test only. ",
            recordedAt = 5L
        )

        val decoded = DeviceTestHistoryStore.decode(DeviceTestHistoryStore.encode(listOf(entry))).single()
        assertEquals("history-5", decoded.id)
        assertEquals("04A1B2C3", decoded.reference)
        assertEquals("Phone", decoded.itemName)
        assertEquals("PHONE", decoded.category)
        assertEquals("PASS", decoded.result)
        assertEquals("NFC tag responded; scan/read test only.", decoded.summary)
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
