package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryLifecycleHistoryTest {
    @Test
    fun safeLifecycleSequenceReplaysToSold() {
        val events = listOf(
            InventoryLifecycleHistory.transition("stock-1", InventoryLifecycle.PURCHASED, InventoryLifecycle.TESTING, occurredAt = "2026-08-27T15:30:00Z"),
            InventoryLifecycleHistory.transition("stock-1", InventoryLifecycle.TESTING, InventoryLifecycle.READY_FOR_SALE, occurredAt = "2026-08-27T15:31:00Z"),
            InventoryLifecycleHistory.transition("stock-1", InventoryLifecycle.READY_FOR_SALE, InventoryLifecycle.LISTED, occurredAt = "2026-08-27T15:32:00Z"),
            InventoryLifecycleHistory.transition("stock-1", InventoryLifecycle.LISTED, InventoryLifecycle.SOLD, occurredAt = "2026-08-27T15:33:00Z")
        )
        assertEquals(InventoryLifecycle.SOLD, InventoryLifecycleHistory.currentState(InventoryLifecycle.PURCHASED, events))
    }

    @Test
    fun unsafeDirectSaleIsRejected() {
        assertTrue(runCatching {
            InventoryLifecycleHistory.transition("stock-2", InventoryLifecycle.PURCHASED, InventoryLifecycle.SOLD, occurredAt = "2026-08-27T15:34:00Z")
        }.isFailure)
    }

    @Test
    fun returnedRepairRequiresReasonAndCanReenterTesting() {
        assertTrue(runCatching {
            InventoryLifecycleHistory.transition("stock-3", InventoryLifecycle.READY_FOR_SALE, InventoryLifecycle.RETURNED_REPAIR, occurredAt = "2026-08-27T15:35:00Z")
        }.isFailure)

        val repair = InventoryLifecycleHistory.transition(
            "stock-3",
            InventoryLifecycle.READY_FOR_SALE,
            InventoryLifecycle.RETURNED_REPAIR,
            reason = "Charging port intermittent",
            occurredAt = "2026-08-27T15:36:00Z"
        )
        val retest = InventoryLifecycleHistory.transition(
            "stock-3",
            InventoryLifecycle.RETURNED_REPAIR,
            InventoryLifecycle.TESTING,
            occurredAt = "2026-08-27T15:37:00Z"
        )
        assertEquals(InventoryLifecycle.TESTING, InventoryLifecycleHistory.currentState(InventoryLifecycle.READY_FOR_SALE, listOf(repair, retest)))
    }

    @Test
    fun nonContiguousHistoryIsRejected() {
        val event = InventoryLifecycleHistory.transition(
            "stock-4",
            InventoryLifecycle.TESTING,
            InventoryLifecycle.READY_FOR_SALE,
            occurredAt = "2026-08-27T15:38:00Z"
        )
        assertTrue(runCatching {
            InventoryLifecycleHistory.currentState(InventoryLifecycle.PURCHASED, listOf(event))
        }.isFailure)
    }
}
