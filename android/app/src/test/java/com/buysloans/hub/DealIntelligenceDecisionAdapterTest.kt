package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DealIntelligenceDecisionAdapterTest {
    private fun healthyInput() = ValuationDecisionInput(
        marketValue = 1000.0,
        sellerAsk = 300.0,
        targetMarginPct = 0.30,
        platformFeesPct = 0.10,
        freightCost = 20.0,
        repairRiskAllowance = 40.0,
        conditionMultiplier = 0.95,
        comparableQuality = 0.95,
        modelConfidence = 1.0,
        sourceCount = 5
    )

    @Test
    fun underpricedAndHighMarginSignalsDoNotRewritePricingMath() {
        val input = healthyInput()
        val baseline = ValuationDecisionEngine.evaluate(input)
        val deal = SavedDealSignalInput(
            id = "deal-1",
            normalizedIdentity = "apple macbook air 2020",
            askingPrice = input.sellerAsk,
            originalMarketValue = input.marketValue,
            currentMarketValue = input.marketValue,
            maxBuyPrice = baseline.maxBuyPrice,
            createdAgeDays = 2,
            expectedMarginRate = 0.60
        )
        val result = DealIntelligenceDecisionAdapter.evaluate(input, deal)

        assertTrue(result.underpricedOpportunity)
        assertTrue(result.unusuallyHighMargin)
        assertEquals(baseline.maxBuyPrice, result.base.maxBuyPrice, 0.0001)
        assertEquals(baseline.adjustedResale, result.base.adjustedResale, 0.0001)
        assertEquals(baseline.expectedProfitAtAsk, result.base.expectedProfitAtAsk, 0.0001)
        assertEquals(ValuationDecision.BUY, result.decision)
    }

    @Test
    fun staleSavedValuationDowngradesBuyToCautionWithoutChangingPrices() {
        val input = healthyInput()
        val baseline = ValuationDecisionEngine.evaluate(input)
        val deal = SavedDealSignalInput(
            id = "stale",
            normalizedIdentity = "dell xps 13 9310",
            askingPrice = input.sellerAsk,
            originalMarketValue = input.marketValue,
            currentMarketValue = input.marketValue,
            maxBuyPrice = baseline.maxBuyPrice,
            createdAgeDays = 45,
            expectedMarginRate = 0.40
        )
        val result = DealIntelligenceDecisionAdapter.evaluate(input, deal)

        assertTrue(result.staleValuation)
        assertEquals(ValuationDecision.CAUTION, result.decision)
        assertEquals(baseline.maxBuyPrice, result.base.maxBuyPrice, 0.0001)
        assertTrue(result.reasons.any { it.contains("refreshed", ignoreCase = true) })
    }

    @Test
    fun duplicateIdentityRequiresCautionButNeverChangesFinancialOutputs() {
        val input = healthyInput()
        val baseline = ValuationDecisionEngine.evaluate(input)
        val deal = SavedDealSignalInput("current", " Dell XPS 13 9310 ", 300.0, 1000.0, 1000.0, baseline.maxBuyPrice, 1, 0.40)
        val peer = SavedDealSignalInput("peer", "dell xps 13 9310", 320.0, 1000.0, 1000.0, baseline.maxBuyPrice, 1, 0.35)
        val result = DealIntelligenceDecisionAdapter.evaluate(input, deal, listOf(peer))

        assertTrue(result.duplicateListing)
        assertEquals(ValuationDecision.CAUTION, result.decision)
        assertEquals(baseline.maxBuyPrice, result.base.maxBuyPrice, 0.0001)
        assertEquals(baseline.confidence, result.base.confidence, 0.0001)
    }

    @Test
    fun marketMovementRequiresCautionAndPassCanNeverBeUpgraded() {
        val input = healthyInput()
        val baseline = ValuationDecisionEngine.evaluate(input)
        val moving = SavedDealSignalInput("moving", "console", 300.0, 800.0, 1000.0, baseline.maxBuyPrice, 2, 0.40)
        val movingResult = DealIntelligenceDecisionAdapter.evaluate(input, moving)
        assertTrue(movingResult.meaningfulMarketMovement)
        assertEquals(ValuationDecision.CAUTION, movingResult.decision)

        val passInput = input.copy(sellerAsk = 1000.0)
        val passBase = ValuationDecisionEngine.evaluate(passInput)
        val passDeal = moving.copy(id = "pass", askingPrice = 1000.0)
        val passResult = DealIntelligenceDecisionAdapter.evaluate(passInput, passDeal)
        assertEquals(ValuationDecision.PASS, passBase.decision)
        assertEquals(ValuationDecision.PASS, passResult.decision)
    }
}
