package com.buysloans.hub

import java.time.Instant

data class TestBuyInventoryHandoffRecord(
    val itemName: String,
    val category: DeviceCategory,
    val evidenceSource: TestEvidenceSource,
    val scanReference: String,
    val askingPrice: Double,
    val currentValuation: Double,
    val maxBuyPrice: Double,
    val faults: String,
    val completedChecks: Int,
    val totalChecks: Int,
    val initialLifecycle: InventoryLifecycle,
    val testCompletedAt: String,
    val handoffCreatedAt: String
)

object TestBuyInventoryHandoff {
    fun create(
        session: TestBuySessionRecord,
        handoffCreatedAt: String = Instant.now().toString()
    ): TestBuyInventoryHandoffRecord {
        require(session.canOfferInventoryHandoff) {
            "Only a completed Send to Inventory Test & Buy session can create an inventory handoff."
        }
        require(session.outcome == BuyOutcome.SEND_TO_INVENTORY)
        require(session.failedChecks == 0) { "Inventory handoff cannot contain failed hardware checks." }
        require(session.completedChecks == session.totalChecks) {
            "Inventory handoff requires every hardware check to be completed or marked not applicable."
        }
        require(runCatching { Instant.parse(session.completedAt) }.isSuccess) {
            "A valid Test & Buy completion timestamp is required."
        }
        require(runCatching { Instant.parse(handoffCreatedAt) }.isSuccess) {
            "A valid handoff timestamp is required."
        }

        return TestBuyInventoryHandoffRecord(
            itemName = session.itemName,
            category = session.category,
            evidenceSource = session.evidenceSource,
            scanReference = session.scanReference,
            askingPrice = session.askingPrice,
            currentValuation = session.currentValuation,
            maxBuyPrice = session.maxBuyPrice,
            faults = session.faults,
            completedChecks = session.completedChecks,
            totalChecks = session.totalChecks,
            initialLifecycle = InventoryLifecycle.PURCHASED,
            testCompletedAt = session.completedAt,
            handoffCreatedAt = handoffCreatedAt
        )
    }
}
