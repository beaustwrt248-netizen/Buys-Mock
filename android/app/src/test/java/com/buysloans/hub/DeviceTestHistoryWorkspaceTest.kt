package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceTestHistoryWorkspaceTest {
    private fun entry(
        id: String,
        source: DeviceTestHistorySource,
        reference: String,
        itemName: String,
        recordedAt: Long
    ) = DeviceTestHistoryEntry(
        id = id,
        source = source,
        reference = reference,
        itemName = itemName,
        category = "PHONE",
        result = "PASS",
        summary = "Checked",
        recordedAt = recordedAt
    )

    private val entries = listOf(
        entry("old-barcode", DeviceTestHistorySource.BARCODE, "111", "Laptop", 100L),
        entry("nfc", DeviceTestHistorySource.NFC, "04:A1", "Phone", 300L),
        entry("test-buy", DeviceTestHistorySource.TEST_BUY, "", "Console", 200L),
        entry("new-barcode", DeviceTestHistorySource.BARCODE, "222", "PC", 400L)
    )

    @Test
    fun `all history is newest first with source counts`() {
        val snapshot = DeviceTestHistoryWorkspace.snapshot(entries)

        assertEquals(DeviceTestHistoryFilter.ALL, snapshot.filter)
        assertEquals(4, snapshot.totalCount)
        assertEquals(4, snapshot.visibleCount)
        assertEquals(2, snapshot.barcodeCount)
        assertEquals(1, snapshot.testBuyCount)
        assertEquals(1, snapshot.androidNfcReadOnlyCount)
        assertEquals(listOf("PC", "Phone", "Console", "Laptop"), snapshot.items.map { it.itemLabel })
    }

    @Test
    fun `source filters only expose matching history`() {
        val barcode = DeviceTestHistoryWorkspace.snapshot(entries, DeviceTestHistoryFilter.BARCODE)
        val testBuy = DeviceTestHistoryWorkspace.snapshot(entries, DeviceTestHistoryFilter.TEST_BUY)
        val nfc = DeviceTestHistoryWorkspace.snapshot(entries, DeviceTestHistoryFilter.ANDROID_NFC_READ_ONLY)

        assertEquals(listOf("PC", "Laptop"), barcode.items.map { it.itemLabel })
        assertEquals(listOf("Console"), testBuy.items.map { it.itemLabel })
        assertEquals(listOf("Phone"), nfc.items.map { it.itemLabel })
        assertTrue(nfc.items.single().isAndroidNfcReadOnly)
        assertEquals("Android NFC read-only", nfc.items.single().sourceLabel)
    }

    @Test
    fun `limit is applied after filtering and cannot leak other sources`() {
        val snapshot = DeviceTestHistoryWorkspace.snapshot(
            entries,
            filter = DeviceTestHistoryFilter.BARCODE,
            limit = 1
        )

        assertEquals(1, snapshot.visibleCount)
        assertEquals("PC", snapshot.items.single().itemLabel)
        assertFalse(snapshot.items.single().isAndroidNfcReadOnly)
    }
}
