package com.buysloans.hub

enum class TestBuyItemEntryMethod {
    BARCODE_SCAN,
    MANUAL_ENTRY
}

enum class TestBuyOutcome {
    REJECT,
    BUY,
    SEND_TO_INVENTORY
}

data class TestBuyValuationSnapshot(
    val currentValuationCents: Long,
    val maxBuyCents: Long,
    val sourceLabel: String
) {
    init {
        require(currentValuationCents >= 0) { "Current valuation must not be negative" }
        require(maxBuyCents >= 0) { "Max-buy guidance must not be negative" }
        require(sourceLabel.isNotBlank()) { "Valuation source must be identified" }
    }
}

data class TestBuySession(
    val itemReference: String,
    val entryMethod: TestBuyItemEntryMethod,
    val deviceCategory: DeviceTestCategory,
    val checklistReview: DeviceTestChecklistReview,
    val valuation: TestBuyValuationSnapshot,
    val recordedFaults: List<String> = emptyList()
) {
    init {
        require(itemReference.isNotBlank()) { "Item reference must not be blank" }
        require(checklistReview.category == deviceCategory) {
            "Checklist category does not match Test & Buy device category"
        }
    }

    val faults: List<String>
        get() = (checklistReview.faults + recordedFaults.map(String::trim))
            .filter(String::isNotBlank)
            .distinct()
}

data class TestBuyDecision(
    val outcome: TestBuyOutcome,
    val inventoryState: InventoryLifecycleState?,
    val currentValuationCents: Long,
    val maxBuyCents: Long,
    val faults: List<String>
)

/**
 * Staged Test & Buy decision contract.
 *
 * The workflow consumes valuation/max-buy values already produced by the current pricing pipeline;
 * it does not implement or alter Valuation 3.0 pricing. Hardware evidence is likewise consumed from
 * the structured checklist review and tester-entered faults rather than claiming unsupported active
 * diagnostics.
 *
 * Android NFC is intentionally absent as an item-entry method. NFC remains scan/read-only evidence
 * and cannot resolve, assign, link/unlink or mutate inventory through this workflow.
 */
object TestBuyWorkflow {
    fun finish(session: TestBuySession, outcome: TestBuyOutcome): TestBuyDecision {
        require(session.checklistReview.isComplete || outcome == TestBuyOutcome.REJECT) {
            "Complete the structured hardware checklist before buying or sending to inventory"
        }

        val inventoryState = when (outcome) {
            TestBuyOutcome.REJECT -> null
            TestBuyOutcome.BUY -> InventoryLifecycleState.PURCHASED
            TestBuyOutcome.SEND_TO_INVENTORY -> InventoryLifecycleState.TESTING
        }

        return TestBuyDecision(
            outcome = outcome,
            inventoryState = inventoryState,
            currentValuationCents = session.valuation.currentValuationCents,
            maxBuyCents = session.valuation.maxBuyCents,
            faults = session.faults
        )
    }
}