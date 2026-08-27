package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceLifecycleTest {
    @Test
    fun legacyStockDefaultsToPurchasedLifecycle() {
        val item = StockItem("1", "Laptop", "123", 200.0, 500.0, 1, 1L)
        assertEquals(InventoryLifecycle.PURCHASED, item.lifecycle)
    }

    @Test
    fun completedTestBuyPathEndsReadyForSaleBeforeListing() {
        assertTrue(canTransitionLifecycle(InventoryLifecycle.PURCHASED, InventoryLifecycle.TESTING))
        assertTrue(canTransitionLifecycle(InventoryLifecycle.TESTING, InventoryLifecycle.READY_FOR_SALE))
        assertTrue(canTransitionLifecycle(InventoryLifecycle.READY_FOR_SALE, InventoryLifecycle.LISTED))
    }

    @Test
    fun lifecycleStillBlocksUnsafeDirectSale() {
        assertFalse(canTransitionLifecycle(InventoryLifecycle.PURCHASED, InventoryLifecycle.SOLD))
        assertFalse(canTransitionLifecycle(InventoryLifecycle.READY_FOR_SALE, InventoryLifecycle.SOLD))
    }

    @Test
    fun nfcDoesNotBecomeInventoryIdentity() {
        val phoneCheck = checklistFor(DeviceCategory.PHONE).first { it.id == "nfc" }
        assertTrue(phoneCheck.label.contains("scan/read", ignoreCase = true))
        assertFalse(phoneCheck.label.contains("assign", ignoreCase = true))
        assertFalse(phoneCheck.label.contains("link", ignoreCase = true))
    }
}
