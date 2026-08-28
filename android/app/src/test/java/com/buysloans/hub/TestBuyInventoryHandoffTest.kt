package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TestBuyInventoryHandoffTest {
    private fun passingChecks(category: DeviceCategory): List<HardwareCheck> =
        checklistFor(category).map { it.copy(result = TestResult.PASS) }

    @Test
    fun sendToInventorySessionCreatesPurchasedHandoff() {
        val session = TestBuySessionFinalizer.finalize(
            TestBuyDraft(
                itemName = "Nintendo Switch OLED",
                scanValue = "9312345678901",
                category = DeviceCategory.CONSOLE,
                askingPrice = 220.0,
                currentValuation = 380.0,
                maxBuyPrice = 250.0,
                checks = passingChecks(DeviceCategory.CONSOLE)
            ),
            evidenceSource = TestEvidenceSource.BARCODE,
            completedAt = "2026-08-27T17:30:00Z"
        )

        val handoff = TestBuyInventoryHandoff.create(session, "2026-08-27T17:31:00Z")

        assertEquals(InventoryLifecycle.PURCHASED, handoff.initialLifecycle)
        assertEquals(TestEvidenceSource.BARCODE, handoff.evidenceSource)
        assertEquals("9312345678901", handoff.scanReference)
        assertEquals(handoff.totalChecks, handoff.completedChecks)
        assertEquals(250.0, handoff.maxBuyPrice, 0.0)
    }

    @Test
    fun rejectAndBuyOutcomesCannotCreateInventoryHandoff() {
        val rejected = TestBuySessionFinalizer.finalize(
            TestBuyDraft(
                itemName = "Phone",
                askingPrice = 500.0,
                currentValuation = 400.0,
                maxBuyPrice = 300.0,
                checks = passingChecks(DeviceCategory.PHONE),
                category = DeviceCategory.PHONE
            ),
            completedAt = "2026-08-27T17:32:00Z"
        )
        assertEquals(BuyOutcome.REJECT, rejected.outcome)
        assertTrue(runCatching { TestBuyInventoryHandoff.create(rejected, "2026-08-27T17:33:00Z") }.isFailure)

        val buyOnly = TestBuySessionFinalizer.finalize(
            TestBuyDraft(
                itemName = "Laptop with cosmetic fault",
                askingPrice = 150.0,
                currentValuation = 300.0,
                maxBuyPrice = 180.0,
                faults = "Lid dent",
                checks = passingChecks(DeviceCategory.LAPTOP),
                category = DeviceCategory.LAPTOP
            ),
            completedAt = "2026-08-27T17:34:00Z"
        )
        assertEquals(BuyOutcome.BUY, buyOnly.outcome)
        assertTrue(runCatching { TestBuyInventoryHandoff.create(buyOnly, "2026-08-27T17:35:00Z") }.isFailure)
    }

    @Test
    fun androidNfcReferenceRemainsEvidenceOnly() {
        val session = TestBuySessionFinalizer.finalize(
            TestBuyDraft(
                itemName = "Android phone",
                scanValue = "04A1B2C3D4",
                askingPrice = 180.0,
                currentValuation = 320.0,
                maxBuyPrice = 200.0,
                checks = passingChecks(DeviceCategory.PHONE),
                category = DeviceCategory.PHONE
            ),
            evidenceSource = TestEvidenceSource.ANDROID_NFC_READ_ONLY,
            completedAt = "2026-08-27T17:36:00Z"
        )

        assertEquals(TestEvidenceSource.ANDROID_NFC_READ_ONLY, session.evidenceSource)
        assertEquals("04A1B2C3D4", session.scanReference)
        assertFalse(session.canOfferInventoryHandoff)
        assertTrue(runCatching {
            TestBuyInventoryHandoff.create(session, "2026-08-27T17:37:00Z")
        }.isFailure)
    }
}
