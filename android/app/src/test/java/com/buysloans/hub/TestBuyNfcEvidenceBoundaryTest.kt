package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TestBuyNfcEvidenceBoundaryTest {
    private fun completedPhoneDraft(scanValue: String): TestBuyDraft = TestBuyDraft(
        itemName = "Galaxy S24",
        scanValue = scanValue,
        category = DeviceCategory.PHONE,
        askingPrice = 300.0,
        currentValuation = 650.0,
        maxBuyPrice = 400.0,
        checks = checklistFor(DeviceCategory.PHONE).map { it.copy(result = TestResult.PASS) }
    )

    @Test
    fun `android NFC evidence requires a nonblank read reference`() {
        val result = runCatching {
            TestBuySessionFinalizer.finalize(
                completedPhoneDraft(scanValue = "   "),
                evidenceSource = TestEvidenceSource.ANDROID_NFC_READ_ONLY,
                completedAt = "2026-08-28T14:10:00Z"
            )
        }
        assertTrue(result.isFailure)
    }

    @Test
    fun `android NFC evidence remains session evidence without authorizing inventory handoff`() {
        val draft = completedPhoneDraft(scanValue = "04:A1:B2:C3:D4:E5:80")
        val manual = TestBuySessionFinalizer.finalize(
            draft,
            evidenceSource = TestEvidenceSource.MANUAL_ENTRY,
            completedAt = "2026-08-28T14:11:00Z"
        )
        val nfc = TestBuySessionFinalizer.finalize(
            draft,
            evidenceSource = TestEvidenceSource.ANDROID_NFC_READ_ONLY,
            completedAt = "2026-08-28T14:11:00Z"
        )

        assertEquals(manual.outcome, nfc.outcome)
        assertEquals(BuyOutcome.SEND_TO_INVENTORY, nfc.outcome)
        assertTrue(manual.canOfferInventoryHandoff)
        assertFalse(nfc.canOfferInventoryHandoff)
        assertEquals(TestEvidenceSource.ANDROID_NFC_READ_ONLY, nfc.evidenceSource)
        assertEquals("04:A1:B2:C3:D4:E5:80", nfc.scanReference)
    }

    @Test
    fun `NFC inventory handoff creation is rejected while barcode remains allowed`() {
        val draft = completedPhoneDraft(scanValue = "04:A1:B2:C3:D4:E5:80")
        val nfc = TestBuySessionFinalizer.finalize(
            draft,
            evidenceSource = TestEvidenceSource.ANDROID_NFC_READ_ONLY,
            completedAt = "2026-08-28T14:11:00Z"
        )
        val barcode = TestBuySessionFinalizer.finalize(
            draft,
            evidenceSource = TestEvidenceSource.BARCODE,
            completedAt = "2026-08-28T14:11:00Z"
        )

        assertTrue(runCatching {
            TestBuyInventoryHandoff.create(nfc, handoffCreatedAt = "2026-08-28T14:12:00Z")
        }.isFailure)
        assertEquals(
            InventoryLifecycle.PURCHASED,
            TestBuyInventoryHandoff.create(
                barcode,
                handoffCreatedAt = "2026-08-28T14:12:00Z"
            ).initialLifecycle
        )
    }

    @Test
    fun `NFC checklist contract is Android read-only and inventory isolated`() {
        val nfc = DeviceChecklistProfiles.forCategory(DeviceCategory.PHONE).single { it.id == "nfc" }
        val guidance = nfc.guidance.lowercase()

        assertTrue(guidance.contains("android only"))
        assertTrue(guidance.contains("read-only"))
        assertTrue(guidance.contains("must not look up"))
        assertTrue(guidance.contains("assign"))
        assertTrue(guidance.contains("link"))
        assertTrue(guidance.contains("unlink"))
        assertTrue(guidance.contains("modify inventory"))
        assertFalse(guidance.contains("web"))
        assertFalse(guidance.contains("pwa"))
    }

    @Test
    fun `NFC history presentation explicitly labels read-only evidence`() {
        val entry = DeviceTestHistoryEntry(
            id = "nfc-history",
            source = DeviceTestHistorySource.NFC,
            reference = "04:A1:B2:C3:D4:E5:80",
            itemName = "Galaxy S24",
            category = DeviceCategory.PHONE.name,
            result = "PASS",
            summary = "NFC scan/read test passed.",
            recordedAt = 1_777_000_000_000L
        )
        val timeline = DeviceTestHistoryTimeline.from(listOf(entry)).single()

        assertEquals("Android NFC read-only", timeline.sourceLabel)
        assertEquals("04:A1:B2:C3:D4:E5:80", timeline.evidenceLabel)
        assertTrue(timeline.isAndroidNfcReadOnly)
    }
}
