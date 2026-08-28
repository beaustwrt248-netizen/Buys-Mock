package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceTestHistoryQueryTest {
    private val entries = listOf(
        DeviceTestHistoryEntry("a", DeviceTestHistorySource.BARCODE, "123", "Phone", "PHONE", "PASS", "barcode", 10L),
        DeviceTestHistoryEntry("b", DeviceTestHistorySource.NFC, "04A1", "Phone", "PHONE", "PASS", "scan/read test only", 30L),
        DeviceTestHistoryEntry("c", DeviceTestHistorySource.TEST_BUY, "123", "Phone", "PHONE", "BUY", "completed", 20L),
        DeviceTestHistoryEntry("d", DeviceTestHistorySource.NFC, "04A1", "Phone", "PHONE", "PASS", "scan/read test only", 40L)
    )

    @Test
    fun recentReturnsNewestFirstAndHonorsLimit() {
        assertEquals(listOf("d", "b"), DeviceTestHistoryQuery.recent(entries, limit = 2).map { it.id })
    }

    @Test
    fun recentClampsUnsafeLimitsWithoutChangingEvidence() {
        assertEquals(listOf("d"), DeviceTestHistoryQuery.recent(entries, limit = 0).map { it.id })

        val manyEntries = (1..125).map { index ->
            DeviceTestHistoryEntry(
                id = "history-$index",
                source = DeviceTestHistorySource.BARCODE,
                reference = index.toString(),
                itemName = "Item $index",
                category = "OTHER",
                result = "PASS",
                summary = "barcode scan",
                recordedAt = index.toLong()
            )
        }
        val bounded = DeviceTestHistoryQuery.recent(manyEntries, limit = Int.MAX_VALUE)
        assertEquals(100, bounded.size)
        assertEquals("history-125", bounded.first().id)
        assertEquals("history-26", bounded.last().id)
    }

    @Test
    fun equalTimestampsHaveStableOrdering() {
        val tied = listOf(
            DeviceTestHistoryEntry("z", DeviceTestHistorySource.BARCODE, "1", "A", "OTHER", "PASS", "barcode", 50L),
            DeviceTestHistoryEntry("a", DeviceTestHistorySource.BARCODE, "2", "B", "OTHER", "PASS", "barcode", 50L)
        )
        assertEquals(listOf("a", "z"), DeviceTestHistoryQuery.recent(tied).map { it.id })
    }

    @Test
    fun filtersKeepBarcodeNfcAndTestBuySourcesSeparate() {
        assertEquals(listOf("d", "b"), DeviceTestHistoryQuery.recent(entries, source = DeviceTestHistorySource.NFC).map { it.id })
        assertEquals(listOf("a"), DeviceTestHistoryQuery.recent(entries, source = DeviceTestHistorySource.BARCODE).map { it.id })
        assertEquals(listOf("c"), DeviceTestHistoryQuery.recent(entries, source = DeviceTestHistorySource.TEST_BUY).map { it.id })
    }

    @Test
    fun referenceAndItemFiltersAreNormalized() {
        assertEquals(listOf("c", "a"), DeviceTestHistoryQuery.recent(entries, reference = " 123 ").map { it.id })
        assertEquals(listOf("d", "b", "c", "a"), DeviceTestHistoryQuery.recent(entries, itemName = " phone ").map { it.id })
    }

    @Test
    fun combinedFiltersRequireTheSameEvidenceEntryToMatch() {
        assertEquals(
            listOf("d", "b"),
            DeviceTestHistoryQuery.recent(
                entries,
                source = DeviceTestHistorySource.NFC,
                reference = " 04a1 ",
                itemName = " PHONE "
            ).map { it.id }
        )
        assertTrue(
            DeviceTestHistoryQuery.recent(
                entries,
                source = DeviceTestHistorySource.NFC,
                reference = "123",
                itemName = "Phone"
            ).isEmpty()
        )
    }

    @Test
    fun latestEvidenceReturnsNewestMatchingScanWithoutInventorySemantics() {
        val latest = DeviceTestHistoryQuery.latestForEvidence(entries, DeviceTestHistorySource.NFC, "04A1")
        assertEquals("d", latest?.id)
        assertEquals("scan/read test only", latest?.summary)
        assertTrue(latest?.source == DeviceTestHistorySource.NFC)
    }

    @Test
    fun latestEvidenceReturnsNullWhenThereIsNoMatchingScan() {
        assertNull(DeviceTestHistoryQuery.latestForEvidence(entries, DeviceTestHistorySource.NFC, "missing"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun latestEvidenceRejectsBlankReference() {
        DeviceTestHistoryQuery.latestForEvidence(entries, DeviceTestHistorySource.NFC, "   ")
    }
}
