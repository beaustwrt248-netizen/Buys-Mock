package com.buysloans.hub

data class TestBuyOutcomeAvailability(
    val canReject: Boolean,
    val canBuy: Boolean,
    val canSendToInventory: Boolean,
    val buyBlockers: List<String>,
    val inventoryBlockers: List<String>
) {
    fun allows(outcome: BuyOutcome): Boolean = when (outcome) {
        BuyOutcome.REJECT -> canReject
        BuyOutcome.BUY -> canBuy
        BuyOutcome.SEND_TO_INVENTORY -> canSendToInventory
    }
}

object TestBuyOutcomePolicy {
    fun evaluate(draft: TestBuyDraft): TestBuyOutcomeAvailability {
        val purchaseBlockers = buildList {
            if (draft.itemName.isBlank()) add("Enter an item or model name.")
            if (draft.hasUntestedChecks) add("Complete or mark N/A for every hardware check.")
            if (draft.currentValuation <= 0.0) add("Enter or consume a current valuation.")
            if (draft.maxBuyPrice <= 0.0) add("Enter approved max-buy guidance.")
            if (draft.askingPrice < 0.0) add("Seller ask cannot be negative.")
            if (draft.maxBuyPrice > 0.0 && draft.askingPrice > draft.maxBuyPrice) {
                add("Seller ask is above the approved max-buy guidance.")
            }
        }

        val buyBlockers = purchaseBlockers.toMutableList().apply {
            if (draft.failedChecks > 0 && draft.faults.isBlank()) {
                add("Record the failed hardware faults before choosing Buy.")
            }
        }

        val inventoryBlockers = purchaseBlockers.toMutableList().apply {
            if (draft.failedChecks > 0) add("Failed hardware checks cannot be sent directly to inventory.")
            if (draft.faults.isNotBlank()) add("Items with recorded faults require Buy/review rather than direct inventory handoff.")
        }

        return TestBuyOutcomeAvailability(
            canReject = draft.itemName.isNotBlank(),
            canBuy = buyBlockers.isEmpty(),
            canSendToInventory = inventoryBlockers.isEmpty(),
            buyBlockers = buyBlockers,
            inventoryBlockers = inventoryBlockers
        )
    }
}
