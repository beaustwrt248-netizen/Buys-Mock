package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Test

class TestBuyAutoPricingTest {
    @Test
    fun autoMaxBuyUsesExistingGpRelationship() {
        assertEquals(350.0, calculatedTestBuyMaxBuy(500.0, TestBuyPricingGrade.A), 0.001)
        assertEquals(250.0, calculatedTestBuyMaxBuy(500.0, TestBuyPricingGrade.B), 0.001)
        assertEquals(150.0, calculatedTestBuyMaxBuy(500.0, TestBuyPricingGrade.C), 0.001)
        assertEquals(350.0, calculatedTestBuyMaxBuy(500.0, TestBuyPricingGrade.LUXURY), 0.001)
    }

    @Test
    fun missingPricingIsIncompleteNotRejectGuidance() {
        val draft = TestBuyDraft(
            itemName = "MacBook",
            category = DeviceCategory.LAPTOP,
            checks = checklistFor(DeviceCategory.LAPTOP)
        )
        assertEquals(TestBuyGuidanceState.COMPLETE_TEST_AND_PRICING, testBuyGuidanceState(draft))
    }

    @Test
    fun sellerAskAboveCalculatedMaxExplainsRejectReason() {
        val checks = checklistFor(DeviceCategory.LAPTOP).map { it.copy(result = TestResult.PASS) }
        val maxBuy = calculatedTestBuyMaxBuy(350.0, TestBuyPricingGrade.A)
        val draft = TestBuyDraft(
            itemName = "MacBook",
            category = DeviceCategory.LAPTOP,
            askingPrice = 800.0,
            currentValuation = 350.0,
            maxBuyPrice = maxBuy,
            checks = checks
        )
        assertEquals(TestBuyGuidanceState.REJECT_ASK_ABOVE_MAX, testBuyGuidanceState(draft))
    }

    @Test
    fun cleanDealWithinCalculatedMaxIsReady() {
        val checks = checklistFor(DeviceCategory.LAPTOP).map { it.copy(result = TestResult.PASS) }
        val maxBuy = calculatedTestBuyMaxBuy(500.0, TestBuyPricingGrade.B)
        val draft = TestBuyDraft(
            itemName = "MacBook",
            category = DeviceCategory.LAPTOP,
            askingPrice = 220.0,
            currentValuation = 500.0,
            maxBuyPrice = maxBuy,
            checks = checks
        )
        assertEquals(TestBuyGuidanceState.READY_CLEAN, testBuyGuidanceState(draft))
    }
    @Test
    fun formattedAustralianCurrencyInputsRemainReliable() {
        assertEquals(1200.0, parseMorleyCurrencyInput("1,200"), 0.001)
        assertEquals(1200.0, parseMorleyCurrencyInput("\$1,200"), 0.001)
        assertEquals(1200.0, parseMorleyCurrencyInput("A\$1,200.00"), 0.001)
        assertEquals(799.77, parseMorleyCurrencyInput(" 799.77 "), 0.001)
        assertEquals(null, parseMorleyCurrencyInput("not a price"))
    }

    @Test
    fun quickDealAndTestBuyShareOneGradePolicy() {
        assertEquals(30.0, targetGpForGradeLabel("A"), 0.001)
        assertEquals(50.0, targetGpForGradeLabel("B"), 0.001)
        assertEquals(70.0, targetGpForGradeLabel("C"), 0.001)
        assertEquals(30.0, targetGpForGradeLabel("Luxury"), 0.001)
        assertEquals(350.0, calculatedMorleyMaxBuy(500.0, "A")!!, 0.001)
        assertEquals(250.0, calculatedMorleyMaxBuy(500.0, "B")!!, 0.001)
        assertEquals(150.0, calculatedMorleyMaxBuy(500.0, "C")!!, 0.001)
        assertEquals(350.0, calculatedMorleyMaxBuy(500.0, "Luxury")!!, 0.001)
        assertEquals(600.0, calculatedMorleyMaxBuy(parseMorleyCurrencyInput("\$1,200"), "B")!!, 0.001)
    }

}
