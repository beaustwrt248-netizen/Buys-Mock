package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveValuationDecisionAdapterTest {
    private fun listing(price: Double, tier: MatchTier = MatchTier.EXACT) = Listing(
        title = "Apple MacBook Air A1932",
        price = price,
        source = "test",
        url = "https://example.invalid/$price",
        condition = "used",
        tier = tier,
        score = 100,
        reasons = "exact"
    )

    @Test fun exactUsedMarketKeepsExistingMedianAndMarginMath() {
        val market = MarketResult(
            exactGoogle = emptyList(),
            exactEbay = listOf(listing(300.0), listing(400.0)),
            similarGoogle = emptyList(), similarEbay = emptyList(), rejected = emptyList()
        )
        val live = LiveValuationDecisionAdapter.evaluate(market, 150.0, 0.30, 0.58)
        assertEquals(350.0, live.marketValue, 0.0001)
        assertEquals(245.0, live.result.consolidated.decision.maxBuyPrice, 0.0001)
        assertEquals(ValuationDecision.BUY, live.result.decision)
    }

    @Test fun exactNewFallbackPreservesConfiguredEstimateRate() {
        val market = MarketResult(
            exactGoogle = listOf(listing(1000.0)), exactEbay = emptyList(),
            similarGoogle = emptyList(), similarEbay = emptyList(), rejected = emptyList()
        )
        val live = LiveValuationDecisionAdapter.evaluate(market, 200.0, 0.30, 0.58)
        assertEquals(580.0, live.marketValue, 0.0001)
        assertEquals(406.0, live.result.consolidated.decision.maxBuyPrice, 0.0001)
    }

    @Test fun componentFallbackCannotClaimHighConfidenceBuy() {
        val market = MarketResult(
            exactGoogle = emptyList(), exactEbay = emptyList(),
            similarGoogle = emptyList(), similarEbay = emptyList(), rejected = emptyList(),
            components = listOf(ComponentValue("GPU", "gpu", 500.0, listOf(listing(500.0))))
        )
        val live = LiveValuationDecisionAdapter.evaluate(market, 100.0, 0.30, 0.65)
        assertTrue(!live.exactEvidence)
        assertTrue(live.result.decision != ValuationDecision.BUY)
    }
}