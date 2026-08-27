package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TestBuySessionFinalizerTest {
    private fun completedPhoneDraft(
        scanValue: String = "",
        askingPrice: Double = 300.0,
        maxBuyPrice: Double = 400.0,
        faults: String = ""
    ): TestBuyDraft {
        val checks = checklistFor(DeviceCategory.PHONE).map { it.copy(result = TestResult.PASS) }
        return TestBuyDraft(
            itemName = "Galaxy S24",
            scanValue = scanValue,
            category = DeviceCategory.PHONE,
            askingPrice = askingPrice,
            currentValuation = 650.0,
            maxBuyPrice = maxBuyPrice,
            faults = faults,
            checks = checks
        )
    }

    @Test
    fun fullyPassedAffordableSessionCanOfferInventoryHandoff() {
        val record = TestBuySessionFinalizer.finalize(
            completedPhoneDraft(),
            completedAt = "2026-08-27T14:30:00Z"
        )
        assertEquals(BuyOutcome.SEND_TO_INVENTORY, record.outcome)
        assertTrue(record.canOfferInventoryHandoff)
        assertEquals(record.totalChecks, record.completedChecks)
        assertEquals(0, record.failedChecks)
    }

    @Test
    fun faultsPreserveExplicitBuyOutcomeWithoutInventoryHandoff() {
        val record = TestBuySessionFinalizer.finalize(
            completedPhoneDraft(faults = "Small crack near camera surround"),
            completedAt = "2026-08-27T14:31:00Z"
        )
        assertEquals(BuyOutcome.BUY, record.outcome)
        assertFalse(record.canOfferInventoryHandoff)
        assertTrue(record.faults.contains("crack"))
    }

    @Test
    fun aboveMaxBuyRemainsReject() {
        val record = TestBuySessionFinalizer.finalize(
            completedPhoneDraft(askingPrice = 450.0, maxBuyPrice = 400.0),
            completedAt = "2026-08-27T14:32:00Z"
        )
        assertEquals(BuyOutcome.REJECT, record.outcome)
        assertFalse(record.canOfferInventoryHandoff)
    }

    @Test
    fun barcodeEvidenceRequiresAReference() {
        val result = runCatching {
            TestBuySessionFinalizer.finalize(
                completedPhoneDraft(),
                evidenceSource = TestEvidenceSource.BARCODE,
                completedAt = "2026-08-27T14:33:00Z"
            )
        }
        assertTrue(result.isFailure)
    }

    @Test
    fun androidNfcEvidenceIsReadOnlySessionEvidenceOnly() {
        val record = TestBuySessionFinalizer.finalize(
            completedPhoneDraft(scanValue = "04:A1:B2:C3:D4:E5:80"),
            evidenceSource = TestEvidenceSource.ANDROID_NFC_READ_ONLY,
            completedAt = "2026-08-27T14:34:00Z"
        )
        assertEquals(TestEvidenceSource.ANDROID_NFC_READ_ONLY, record.evidenceSource)
        assertEquals("04:A1:B2:C3:D4:E5:80", record.scanReference)
        assertEquals(BuyOutcome.SEND_TO_INVENTORY, record.outcome)
    }

    @Test
    fun unfinishedChecklistCannotBecomeInventoryReady() {
        val draft = completedPhoneDraft().copy(
            checks = checklistFor(DeviceCategory.PHONE)
        )
        val record = TestBuySessionFinalizer.finalize(
            draft,
            completedAt = "2026-08-27T14:35:00Z"
        )
        assertEquals(BuyOutcome.REJECT, record.outcome)
        assertFalse(record.canOfferInventoryHandoff)
    }
}
