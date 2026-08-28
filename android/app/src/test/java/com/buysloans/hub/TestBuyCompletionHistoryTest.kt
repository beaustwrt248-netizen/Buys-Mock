package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TestBuyCompletionHistoryTest {
    private val passedChecks = listOf(
        HardwareCheck("power", "Power", TestResult.PASS),
        HardwareCheck("wifi", "Wi-Fi", TestResult.NOT_APPLICABLE)
    )

    @Test
    fun rejectCompletionPreservesBarcodeEvidenceFaultsAndTestedChecks() {
        val session = TestBuySessionFinalizer.finalize(
            draft = TestBuyDraft(
                itemName = "Console",
                scanValue = "9312345678901",
                category = DeviceCategory.CONSOLE,
                askingPrice = 250.0,
                currentValuation = 300.0,
                maxBuyPrice = 200.0,
                faults = "HDMI port intermittent",
                checks = passedChecks
            ),
            evidenceSource = TestEvidenceSource.BARCODE,
            completedAt = "2026-08-28T00:00:00Z"
        )
        val history = completionHistoryFor(session)

        assertEquals(BuyOutcome.REJECT.name, history.result)
        assertEquals(DeviceTestHistorySource.BARCODE, history.source)
        assertEquals("9312345678901", history.reference)
        assertEquals(1787875200000L, history.recordedAt)
        assertTrue(history.summary.contains("explicit REJECT outcome"))
        assertTrue(history.summary.contains("HDMI port intermittent"))
        assertTrue(history.summary.contains("Power=PASS"))
        assertTrue(history.summary.contains("Wi-Fi=NOT_APPLICABLE"))
    }

    @Test
    fun buyCompletionDoesNotOfferInventoryHandoff() {
        val session = TestBuySessionFinalizer.finalize(
            draft = TestBuyDraft(
                itemName = "Laptop",
                category = DeviceCategory.LAPTOP,
                askingPrice = 180.0,
                currentValuation = 320.0,
                maxBuyPrice = 200.0,
                faults = "Battery degraded",
                checks = passedChecks
            ),
            completedAt = "2026-08-28T00:00:00Z"
        )
        val history = completionHistoryFor(session)

        assertEquals(BuyOutcome.BUY.name, history.result)
        assertFalse(session.canOfferInventoryHandoff)
        assertEquals(DeviceTestHistorySource.TEST_BUY, history.source)
        assertTrue(history.summary.contains("Battery degraded"))
        assertTrue(history.summary.contains("tested: Power=PASS, Wi-Fi=NOT_APPLICABLE"))
    }

    @Test
    fun sendToInventoryCompletionKeepsNfcReadOnlyEvidenceAsHistoryOnly() {
        val session = TestBuySessionFinalizer.finalize(
            draft = TestBuyDraft(
                itemName = "Phone",
                scanValue = "04A1B2C3D4",
                category = DeviceCategory.PHONE,
                askingPrice = 150.0,
                currentValuation = 260.0,
                maxBuyPrice = 175.0,
                checks = passedChecks
            ),
            evidenceSource = TestEvidenceSource.ANDROID_NFC_READ_ONLY,
            completedAt = "2026-08-28T00:00:00Z"
        )
        val history = completionHistoryFor(session)

        assertEquals(BuyOutcome.SEND_TO_INVENTORY.name, history.result)
        assertTrue(session.canOfferInventoryHandoff)
        assertEquals(DeviceTestHistorySource.NFC, history.source)
        assertEquals("04A1B2C3D4", history.reference)
        assertTrue(history.summary.contains("Power=PASS"))
        assertFalse(history.summary.contains("assign", ignoreCase = true))
        assertFalse(history.summary.contains("link", ignoreCase = true))
        assertFalse(history.summary.contains("stock", ignoreCase = true))
    }

    @Test
    fun untestedChecksAreNotClaimedAsTestedInHistory() {
        val session = TestBuySessionFinalizer.finalize(
            draft = TestBuyDraft(
                itemName = "Desktop PC",
                category = DeviceCategory.DESKTOP_PC,
                askingPrice = 100.0,
                currentValuation = 200.0,
                maxBuyPrice = 120.0,
                checks = listOf(
                    HardwareCheck("power", "Power", TestResult.PASS),
                    HardwareCheck("camera", "Camera", TestResult.NOT_TESTED)
                )
            ),
            completedAt = "2026-08-28T00:00:00Z"
        )
        val history = completionHistoryFor(session)

        assertTrue(history.summary.contains("Power=PASS"))
        assertFalse(history.summary.contains("Camera=NOT_TESTED"))
    }
}
