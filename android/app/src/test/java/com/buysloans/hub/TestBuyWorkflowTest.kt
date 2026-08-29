package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TestBuyWorkflowTest {
    private fun completeReview(
        category: DeviceTestCategory = DeviceTestCategory.PHONE,
        faults: List<String> = emptyList()
    ) = DeviceTestChecklistReview(
        category = category,
        totalChecks = 8,
        passedChecks = 8 - faults.size,
        failedChecks = faults.size,
        notApplicableChecks = 0,
        untestedChecks = 0,
        faults = faults,
        isComplete = true
    )

    private fun session(
        review: DeviceTestChecklistReview = completeReview(),
        recordedFaults: List<String> = emptyList(),
        entryMethod: TestBuyItemEntryMethod = TestBuyItemEntryMethod.BARCODE_SCAN
    ) = TestBuySession(
        itemReference = "TEST-ITEM-001",
        entryMethod = entryMethod,
        deviceCategory = review.category,
        checklistReview = review,
        valuation = TestBuyValuationSnapshot(
            currentValuationCents = 50_000,
            maxBuyCents = 35_000,
            sourceLabel = "current valuation pipeline"
        ),
        recordedFaults = recordedFaults
    )

    @Test
    fun `barcode scan and manual entry are the only item entry methods`() {
        assertEquals(
            setOf(TestBuyItemEntryMethod.BARCODE_SCAN, TestBuyItemEntryMethod.MANUAL_ENTRY),
            TestBuyItemEntryMethod.entries.toSet()
        )
    }

    @Test
    fun `reject records faults without creating inventory state`() {
        val decision = TestBuyWorkflow.finish(
            session(
                review = completeReview(faults = listOf("Display output: flickers")),
                recordedFaults = listOf("Cosmetic crack on housing")
            ),
            TestBuyOutcome.REJECT
        )

        assertEquals(TestBuyOutcome.REJECT, decision.outcome)
        assertNull(decision.inventoryState)
        assertEquals(listOf("Display output: flickers", "Cosmetic crack on housing"), decision.faults)
    }

    @Test
    fun `buy consumes existing valuation and starts inventory as purchased`() {
        val decision = TestBuyWorkflow.finish(session(), TestBuyOutcome.BUY)

        assertEquals(TestBuyOutcome.BUY, decision.outcome)
        assertEquals(InventoryLifecycleState.PURCHASED, decision.inventoryState)
        assertEquals(50_000, decision.currentValuationCents)
        assertEquals(35_000, decision.maxBuyCents)
    }

    @Test
    fun `send to inventory hands item to testing state`() {
        val decision = TestBuyWorkflow.finish(session(), TestBuyOutcome.SEND_TO_INVENTORY)

        assertEquals(TestBuyOutcome.SEND_TO_INVENTORY, decision.outcome)
        assertEquals(InventoryLifecycleState.TESTING, decision.inventoryState)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `buy requires completed checklist`() {
        val incomplete = completeReview().copy(untestedChecks = 1, isComplete = false)
        TestBuyWorkflow.finish(session(review = incomplete), TestBuyOutcome.BUY)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `send to inventory requires completed checklist`() {
        val incomplete = completeReview().copy(untestedChecks = 2, isComplete = false)
        TestBuyWorkflow.finish(session(review = incomplete), TestBuyOutcome.SEND_TO_INVENTORY)
    }

    @Test
    fun `reject remains available when testing cannot be completed`() {
        val incomplete = completeReview().copy(untestedChecks = 3, isComplete = false)
        val decision = TestBuyWorkflow.finish(session(review = incomplete), TestBuyOutcome.REJECT)

        assertEquals(TestBuyOutcome.REJECT, decision.outcome)
        assertNull(decision.inventoryState)
    }

    @Test
    fun `faults are trimmed de-duplicated and retained as evidence`() {
        val decision = TestBuyWorkflow.finish(
            session(recordedFaults = listOf("  USB port loose  ", "USB port loose", " ")),
            TestBuyOutcome.BUY
        )

        assertEquals(listOf("USB port loose"), decision.faults)
        assertTrue(decision.maxBuyCents <= decision.currentValuationCents)
    }
}