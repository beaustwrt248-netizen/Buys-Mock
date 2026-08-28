package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InventorySalePolicyTest {
    @Test
    fun `only Listed inventory can be sold`() {
        InventoryLifecycle.entries.forEach { state ->
            assertTrue(
                "Unexpected sale eligibility for ${state.name}",
                InventorySalePolicy.canSell(state) == (state == InventoryLifecycle.LISTED)
            )
        }
    }

    @Test
    fun `sale boundary agrees with lifecycle transition to Sold`() {
        assertTrue(InventorySalePolicy.canSell(InventoryLifecycle.LISTED))
        assertTrue(canTransitionLifecycle(InventoryLifecycle.LISTED, InventoryLifecycle.SOLD))
        assertFalse(InventorySalePolicy.canSell(InventoryLifecycle.PURCHASED))
        assertFalse(InventorySalePolicy.canSell(InventoryLifecycle.TESTING))
        assertFalse(InventorySalePolicy.canSell(InventoryLifecycle.READY_FOR_SALE))
        assertFalse(InventorySalePolicy.canSell(InventoryLifecycle.RETURNED_REPAIR))
        assertFalse(InventorySalePolicy.canSell(InventoryLifecycle.SOLD))
    }

    @Test
    fun `require sellable rejects lifecycle bypasses`() {
        InventoryLifecycle.entries.filter { it != InventoryLifecycle.LISTED }.forEach { state ->
            assertTrue(runCatching { InventorySalePolicy.requireSellable(state) }.isFailure)
        }
        assertTrue(runCatching { InventorySalePolicy.requireSellable(InventoryLifecycle.LISTED) }.isSuccess)
    }
}
