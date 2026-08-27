package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TestBuyUiContractTest {
    @Test
    fun phoneChecklistKeepsNfcReadOnlyWording() {
        val nfc = checklistFor(DeviceCategory.PHONE).first { it.id == "nfc" }
        assertTrue(nfc.label.contains("scan/read", ignoreCase = true))
        assertTrue(!nfc.label.contains("link", ignoreCase = true))
        assertTrue(!nfc.label.contains("assign", ignoreCase = true))
    }

    @Test
    fun passingItemWithinMaxBuyCanProceedToInventoryRecommendation() {
        val checks = checklistFor(DeviceCategory.LAPTOP).map { it.copy(result = TestResult.PASS) }
        val draft = TestBuyDraft(
            itemName = "Test laptop",
            category = DeviceCategory.LAPTOP,
            askingPrice = 300.0,
            currentValuation = 600.0,
            maxBuyPrice = 400.0,
            checks = checks
        )
        assertEquals(BuyOutcome.SEND_TO_INVENTORY, recommendedOutcome(draft))
    }
}
