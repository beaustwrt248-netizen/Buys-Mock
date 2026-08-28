package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryLifecycleMutationBoundaryTest {
    @Test
    fun `returned repair requires a nonblank reason at mutation boundary`() {
        val missing = runCatching {
            InventoryLifecycleHistory.validateTransition(
                InventoryLifecycle.READY_FOR_SALE,
                InventoryLifecycle.RETURNED_REPAIR,
                "   "
            )
        }
        assertTrue(missing.isFailure)

        assertEquals(
            "Charging port intermittent",
            InventoryLifecycleHistory.validateTransition(
                InventoryLifecycle.READY_FOR_SALE,
                InventoryLifecycle.RETURNED_REPAIR,
                "  Charging port intermittent  "
            )
        )
    }

    @Test
    fun `normal lifecycle transitions do not require a reason`() {
        assertEquals(
            "",
            InventoryLifecycleHistory.validateTransition(
                InventoryLifecycle.PURCHASED,
                InventoryLifecycle.TESTING
            )
        )
        assertEquals(
            "",
            InventoryLifecycleHistory.validateTransition(
                InventoryLifecycle.TESTING,
                InventoryLifecycle.READY_FOR_SALE
            )
        )
        assertEquals(
            "",
            InventoryLifecycleHistory.validateTransition(
                InventoryLifecycle.READY_FOR_SALE,
                InventoryLifecycle.LISTED
            )
        )
    }

    @Test
    fun `illegal lifecycle jumps are still rejected by mutation boundary`() {
        assertTrue(runCatching {
            InventoryLifecycleHistory.validateTransition(
                InventoryLifecycle.PURCHASED,
                InventoryLifecycle.LISTED
            )
        }.isFailure)
        assertTrue(runCatching {
            InventoryLifecycleHistory.validateTransition(
                InventoryLifecycle.SOLD,
                InventoryLifecycle.TESTING
            )
        }.isFailure)
    }

    @Test
    fun `repair item can reenter testing without a repair reason`() {
        assertEquals(
            "",
            InventoryLifecycleHistory.validateTransition(
                InventoryLifecycle.RETURNED_REPAIR,
                InventoryLifecycle.TESTING
            )
        )
    }
}
