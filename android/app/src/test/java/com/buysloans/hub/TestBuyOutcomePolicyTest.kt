package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TestBuyOutcomePolicyTest {
    private fun completedPhone(
        faults: String = "",
        failFirstCheck: Boolean = false,
        askingPrice: Double = 300.0,
        valuation: Double = 650.0,
        maxBuyPrice: Double = 400.0
    ): TestBuyDraft {
        val checks = checklistFor(DeviceCategory.PHONE).mapIndexed { index, check ->
            check.copy(result = if (failFirstCheck && index == 0) TestResult.FAIL else TestResult.PASS)
        }
        return TestBuyDraft(
            itemName = "Galaxy S24",
            category = DeviceCategory.PHONE,
            askingPrice = askingPrice,
            currentValuation = valuation,
            maxBuyPrice = maxBuyPrice,
            faults = faults,
            checks = checks
        )
    }

    @Test
    fun cleanCompletedAffordableItemOffersAllExplicitActions() {
        val availability = TestBuyOutcomePolicy.evaluate(completedPhone())
        assertTrue(availability.canReject)
        assertTrue(availability.canBuy)
        assertTrue(availability.canSendToInventory)
    }

    @Test
    fun failedCheckRequiresRecordedFaultBeforeBuyAndBlocksInventory() {
        val noFault = TestBuyOutcomePolicy.evaluate(completedPhone(failFirstCheck = true))
        assertFalse(noFault.canBuy)
        assertFalse(noFault.canSendToInventory)

        val faultRecorded = TestBuyOutcomePolicy.evaluate(
            completedPhone(failFirstCheck = true, faults = "Battery health test failed")
        )
        assertTrue(faultRecorded.canBuy)
        assertFalse(faultRecorded.canSendToInventory)
    }

    @Test
    fun incompleteChecklistStillAllowsRejectButBlocksPurchaseActions() {
        val draft = completedPhone().copy(checks = checklistFor(DeviceCategory.PHONE))
        val availability = TestBuyOutcomePolicy.evaluate(draft)
        assertTrue(availability.canReject)
        assertFalse(availability.canBuy)
        assertFalse(availability.canSendToInventory)
    }

    @Test
    fun valuationAndMaxBuyAreRequiredForPurchaseActions() {
        val availability = TestBuyOutcomePolicy.evaluate(completedPhone(valuation = 0.0, maxBuyPrice = 0.0))
        assertFalse(availability.canBuy)
        assertFalse(availability.canSendToInventory)
    }

    @Test
    fun aboveMaxBuyBlocksBuyAndInventoryButNeverBlocksReject() {
        val availability = TestBuyOutcomePolicy.evaluate(completedPhone(askingPrice = 450.0, maxBuyPrice = 400.0))
        assertTrue(availability.canReject)
        assertFalse(availability.canBuy)
        assertFalse(availability.canSendToInventory)
    }

    @Test
    fun explicitFinalizerCannotBypassOutcomeSafety() {
        val result = runCatching {
            TestBuySessionFinalizer.finalize(
                completedPhone(askingPrice = 450.0, maxBuyPrice = 400.0),
                completedAt = "2026-08-28T09:05:00Z",
                explicitOutcome = BuyOutcome.BUY
            )
        }
        assertTrue(result.isFailure)
    }

    @Test
    fun explicitBuyCanRecordKnownFaultWithoutCreatingInventoryEligibility() {
        val record = TestBuySessionFinalizer.finalize(
            completedPhone(failFirstCheck = true, faults = "Battery health test failed"),
            completedAt = "2026-08-28T09:06:00Z",
            explicitOutcome = BuyOutcome.BUY
        )
        assertTrue(record.outcome == BuyOutcome.BUY)
        assertFalse(record.canOfferInventoryHandoff)
    }
}
