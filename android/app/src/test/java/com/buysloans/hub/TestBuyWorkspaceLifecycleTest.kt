package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TestBuyWorkspaceLifecycleTest {
    @Test
    fun inventoryHandoffMustFollowPurchasedTestingReadyPath() {
        assertTrue(canTransitionLifecycle(InventoryLifecycle.PURCHASED, InventoryLifecycle.TESTING))
        assertTrue(canTransitionLifecycle(InventoryLifecycle.TESTING, InventoryLifecycle.READY_FOR_SALE))
        assertFalse(canTransitionLifecycle(InventoryLifecycle.PURCHASED, InventoryLifecycle.READY_FOR_SALE))
    }

    @Test
    fun readyItemCannotSkipStraightToSold() {
        assertTrue(canTransitionLifecycle(InventoryLifecycle.READY_FOR_SALE, InventoryLifecycle.LISTED))
        assertTrue(canTransitionLifecycle(InventoryLifecycle.LISTED, InventoryLifecycle.SOLD))
        assertFalse(canTransitionLifecycle(InventoryLifecycle.READY_FOR_SALE, InventoryLifecycle.SOLD))
    }
}
