package com.buysloans.hub

import org.junit.Assert.*
import org.junit.Test

class TestBuyLogicTest {
    @Test fun phoneChecklistIncludesNfcAsReadTest() {
        val checks = checklistFor(DeviceCategory.PHONE)
        assertTrue(checks.any { it.id == "nfc" && it.label.contains("scan/read", ignoreCase = true) })
        assertFalse(checks.any { it.label.contains("inventory", ignoreCase = true) || it.label.contains("link", ignoreCase = true) })
    }

    @Test fun laptopChecklistCoversCoreHardware() {
        val ids = checklistFor(DeviceCategory.LAPTOP).map { it.id }.toSet()
        assertTrue(ids.containsAll(setOf("power","display","ports","wifi","bluetooth","speakers","storage","battery","keyboard","camera","microphone")))
    }

    @Test fun completedGoodDealCanGoToInventory() {
        val checks = checklistFor(DeviceCategory.LAPTOP).map { it.copy(result = TestResult.PASS) }
        val draft = TestBuyDraft(
            itemName = "Laptop",
            category = DeviceCategory.LAPTOP,
            askingPrice = 250.0,
            currentValuation = 500.0,
            maxBuyPrice = 300.0,
            checks = checks
        )
        assertEquals(BuyOutcome.SEND_TO_INVENTORY, recommendedOutcome(draft))
    }

    @Test fun failedHardwareRejectsDeal() {
        val checks = checklistFor(DeviceCategory.CONSOLE).mapIndexed { index, check ->
            check.copy(result = if(index == 0) TestResult.FAIL else TestResult.PASS)
        }
        val draft = TestBuyDraft("Console", category = DeviceCategory.CONSOLE, askingPrice = 100.0, maxBuyPrice = 200.0, checks = checks)
        assertEquals(BuyOutcome.REJECT, recommendedOutcome(draft))
    }

    @Test fun untestedHardwareRejectsPrematureDecision() {
        val draft = TestBuyDraft("Phone", category = DeviceCategory.PHONE, askingPrice = 100.0, maxBuyPrice = 200.0, checks = checklistFor(DeviceCategory.PHONE))
        assertEquals(BuyOutcome.REJECT, recommendedOutcome(draft))
    }

    @Test fun askingAboveSuppliedMaxBuyRejectsWithoutChangingPricingLogic() {
        val checks = checklistFor(DeviceCategory.DESKTOP_PC).map { it.copy(result = TestResult.PASS) }
        val draft = TestBuyDraft("PC", category = DeviceCategory.DESKTOP_PC, askingPrice = 450.0, currentValuation = 600.0, maxBuyPrice = 400.0, checks = checks)
        assertEquals(BuyOutcome.REJECT, recommendedOutcome(draft))
    }

    @Test fun lifecycleAllowsExpectedSafePath() {
        assertTrue(canTransitionLifecycle(InventoryLifecycle.PURCHASED, InventoryLifecycle.TESTING))
        assertTrue(canTransitionLifecycle(InventoryLifecycle.TESTING, InventoryLifecycle.READY_FOR_SALE))
        assertTrue(canTransitionLifecycle(InventoryLifecycle.READY_FOR_SALE, InventoryLifecycle.LISTED))
        assertTrue(canTransitionLifecycle(InventoryLifecycle.LISTED, InventoryLifecycle.SOLD))
    }

    @Test fun lifecycleBlocksUnsafeJump() {
        assertFalse(canTransitionLifecycle(InventoryLifecycle.PURCHASED, InventoryLifecycle.SOLD))
        assertFalse(canTransitionLifecycle(InventoryLifecycle.SOLD, InventoryLifecycle.TESTING))
    }

    @Test fun repairCanReturnToTesting() {
        assertTrue(canTransitionLifecycle(InventoryLifecycle.RETURNED_REPAIR, InventoryLifecycle.TESTING))
    }
}
