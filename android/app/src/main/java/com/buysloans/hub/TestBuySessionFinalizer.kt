package com.buysloans.hub

import java.time.Instant

enum class TestEvidenceSource {
    MANUAL_ENTRY,
    BARCODE,
    ANDROID_NFC_READ_ONLY
}

data class TestBuySessionRecord(
    val itemName: String,
    val category: DeviceCategory,
    val evidenceSource: TestEvidenceSource,
    val scanReference: String,
    val askingPrice: Double,
    val currentValuation: Double,
    val maxBuyPrice: Double,
    val faults: String,
    val checks: List<HardwareCheck>,
    val outcome: BuyOutcome,
    val completedAt: String
) {
    val failedChecks: Int get() = checks.count { it.result == TestResult.FAIL }
    val completedChecks: Int get() = checks.count { it.result != TestResult.NOT_TESTED }
    val totalChecks: Int get() = checks.size
    val canOfferInventoryHandoff: Boolean
        get() = outcome == BuyOutcome.SEND_TO_INVENTORY &&
            evidenceSource != TestEvidenceSource.ANDROID_NFC_READ_ONLY
}

object TestBuySessionFinalizer {
    fun finalize(
        draft: TestBuyDraft,
        evidenceSource: TestEvidenceSource = TestEvidenceSource.MANUAL_ENTRY,
        completedAt: String = Instant.now().toString(),
        explicitOutcome: BuyOutcome? = null
    ): TestBuySessionRecord {
        require(draft.itemName.isNotBlank()) { "An item name is required before finalizing Test & Buy." }
        require(draft.askingPrice >= 0.0) { "Seller asking price cannot be negative." }
        require(draft.currentValuation >= 0.0) { "Current valuation cannot be negative." }
        require(draft.maxBuyPrice >= 0.0) { "Max-buy guidance cannot be negative." }
        require(runCatching { Instant.parse(completedAt) }.isSuccess) { "A valid completion timestamp is required." }

        val scanReference = draft.scanValue.trim()
        if (evidenceSource == TestEvidenceSource.BARCODE) {
            require(scanReference.isNotBlank()) { "Barcode evidence requires a scan reference." }
        }
        if (evidenceSource == TestEvidenceSource.ANDROID_NFC_READ_ONLY) {
            require(scanReference.isNotBlank()) { "Android NFC read-only evidence requires a scan reference." }
        }

        val outcome = explicitOutcome ?: recommendedOutcome(draft)
        if (explicitOutcome != null) {
            val availability = TestBuyOutcomePolicy.evaluate(draft)
            require(availability.allows(explicitOutcome)) {
                when (explicitOutcome) {
                    BuyOutcome.REJECT -> "Reject requires an item or model name."
                    BuyOutcome.BUY -> availability.buyBlockers.joinToString(" ").ifBlank { "Buy is not available for this Test & Buy session." }
                    BuyOutcome.SEND_TO_INVENTORY -> availability.inventoryBlockers.joinToString(" ").ifBlank { "Send to Inventory is not available for this Test & Buy session." }
                }
            }
        }

        return TestBuySessionRecord(
            itemName = draft.itemName.trim(),
            category = draft.category,
            evidenceSource = evidenceSource,
            scanReference = scanReference,
            askingPrice = draft.askingPrice,
            currentValuation = draft.currentValuation,
            maxBuyPrice = draft.maxBuyPrice,
            faults = draft.faults.trim(),
            checks = draft.checks.map { it.copy(notes = it.notes.trim()) },
            outcome = outcome,
            completedAt = completedAt
        )
    }
}
