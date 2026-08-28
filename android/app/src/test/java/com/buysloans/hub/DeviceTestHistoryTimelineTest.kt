package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceTestHistoryTimelineTest {
    @Test
    fun `timeline keeps what was tested result and timestamp in newest-first order`() {
        val older = DeviceTestHistoryEntry(
            id = "older",
            source = DeviceTestHistorySource.TEST_BUY,
            reference = "",
            itemName = "Laptop A",
            category = "LAPTOP",
            result = "BUY",
            summary = "11/11 checks completed; tested: battery=PASS, display=PASS; faults: hinge wear.",
            recordedAt = 100L
        )
        val newer = DeviceTestHistoryEntry(
            id = "newer",
            source = DeviceTestHistorySource.BARCODE,
            reference = "9312345678901",
            itemName = "Console B",
            category = "CONSOLE",
            result = "SEND_TO_INVENTORY",
            summary = "10/10 checks completed; tested: hdmi=PASS, controller=PASS; faults: none recorded.",
            recordedAt = 200L
        )

        val timeline = DeviceTestHistoryTimeline.from(listOf(older, newer))

        assertEquals(listOf("Console B", "Laptop A"), timeline.map { it.itemLabel })
        assertEquals("9312345678901", timeline.first().evidenceLabel)
        assertEquals("SEND_TO_INVENTORY", timeline.first().result)
        assertEquals(200L, timeline.first().recordedAt)
        assertTrue(timeline.first().summary.contains("hdmi=PASS"))
        assertEquals("Manual entry", timeline.last().evidenceLabel)
    }

    @Test
    fun `nfc timeline is explicitly Android read-only and never presented as inventory evidence`() {
        val entry = DeviceTestHistoryEntry(
            id = "nfc",
            source = DeviceTestHistorySource.NFC,
            reference = "04A1B2C3D4",
            itemName = "",
            category = "",
            result = "PASS",
            summary = "NFC tag responded; technologies: Ndef; 1 supported NDEF payload read.",
            recordedAt = 300L
        )

        val item = DeviceTestHistoryTimeline.toTimelineItem(entry)

        assertEquals("Android NFC read-only", item.sourceLabel)
        assertEquals("04A1B2C3D4", item.evidenceLabel)
        assertEquals("Unlabelled scan", item.itemLabel)
        assertTrue(item.isAndroidNfcReadOnly)
        assertFalse(item.sourceLabel.contains("inventory", ignoreCase = true))
        assertFalse(item.summary.contains("assign", ignoreCase = true))
        assertFalse(item.summary.contains("link", ignoreCase = true))
        assertFalse(item.summary.contains("stock", ignoreCase = true))
    }

    @Test
    fun `timeline limit is bounded by history query`() {
        val entries = (1L..40L).map { stamp ->
            DeviceTestHistoryEntry(
                id = stamp.toString(),
                source = DeviceTestHistorySource.TEST_BUY,
                reference = "",
                itemName = "Item $stamp",
                category = "OTHER",
                result = "REJECT",
                summary = "checked",
                recordedAt = stamp
            )
        }

        val timeline = DeviceTestHistoryTimeline.from(entries, limit = 5)

        assertEquals(5, timeline.size)
        assertEquals(listOf(40L, 39L, 38L, 37L, 36L), timeline.map { it.recordedAt })
    }
}
