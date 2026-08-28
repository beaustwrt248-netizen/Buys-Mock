package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Test

class Valuation3DealSignalPricingIsolationTest {
    private val input = ValuationDecisionInput(1000.0, 400.0, 0.30, sourceCount = 5)
    private fun deal(id: String, age: Int, current: Double) = SavedDealSignalInput(
        id = id,
        normalizedIdentity = "device model generation",
        askingPrice = 400.0,
        originalMarketValue = 1000.0,
        currentMarketValue = current,
        maxBuyPrice = 700.0,
        createdAgeDays = age,
        expectedMarginRate = 0.45
    )

    @Test fun everyDealRiskSignalLeavesAuthoritativeFinancialOutputsIdentical() {
        val plain = CompleteValuationDecision.evaluate(input)
        val cases = listOf(
            CompleteValuationDecision.evaluate(input, deal = deal("stale", 60, 1000.0)),
            CompleteValuationDecision.evaluate(input, deal = deal("moved", 1, 1300.0)),
            CompleteValuationDecision.evaluate(input, deal = deal("dup", 1, 1000.0), peerDeals = listOf(deal("peer", 1, 1000.0)))
        )
        cases.forEach { result ->
            assertEquals(plain.consolidated.decision.adjustedResale, result.consolidated.decision.adjustedResale, 0.0001)
            assertEquals(plain.consolidated.decision.maxBuyPrice, result.consolidated.decision.maxBuyPrice, 0.0001)
            assertEquals(plain.consolidated.decision.expectedProfitAtAsk, result.consolidated.decision.expectedProfitAtAsk, 0.0001)
            assertEquals(plain.consolidated.decision.expectedMarginAtAsk, result.consolidated.decision.expectedMarginAtAsk, 0.0001)
        }
    }
}
