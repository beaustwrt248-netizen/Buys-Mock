package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValuationDecisionEngineTest {
    @Test
    fun strongEvidenceAndHealthyMarginReturnsBuy() {
        val result = ValuationDecisionEngine.evaluate(
            ValuationDecisionInput(
                marketValue = 1000.0,
                sellerAsk = 450.0,
                targetMarginPct = 0.30,
                platformFeesPct = 0.10,
                freightCost = 25.0,
                conditionMultiplier = 0.95,
                comparableQuality = 0.95,
                modelConfidence = 1.0,
                sourceCount = 5,
                staleDays = 2
            )
        )
        assertEquals(ValuationDecision.BUY, result.decision)
        assertTrue(result.confidence >= 0.90)
        assertTrue(result.maxBuyPrice > result.expectedProfitAtAsk)
    }

    @Test
    fun askSlightlyAboveMaxBuyReturnsCaution() {
        val baseline = ValuationDecisionEngine.evaluate(
            ValuationDecisionInput(
                marketValue = 700.0,
                sellerAsk = 0.0,
                targetMarginPct = 0.35,
                platformFeesPct = 0.08,
                sourceCount = 5
            )
        )
        val result = ValuationDecisionEngine.evaluate(
            ValuationDecisionInput(
                marketValue = 700.0,
                sellerAsk = baseline.maxBuyPrice * 1.05,
                targetMarginPct = 0.35,
                platformFeesPct = 0.08,
                sourceCount = 5
            )
        )
        assertEquals(ValuationDecision.CAUTION, result.decision)
        assertTrue(result.reasons.any { it.contains("max-buy", ignoreCase = true) })
    }

    @Test
    fun askFarAboveMaxBuyReturnsPass() {
        val result = ValuationDecisionEngine.evaluate(
            ValuationDecisionInput(
                marketValue = 500.0,
                sellerAsk = 450.0,
                targetMarginPct = 0.40,
                platformFeesPct = 0.10,
                freightCost = 20.0,
                sourceCount = 5
            )
        )
        assertEquals(ValuationDecision.PASS, result.decision)
    }

    @Test
    fun weakComparablesAndModelConfidenceReduceConfidence() {
        val result = ValuationDecisionEngine.evaluate(
            ValuationDecisionInput(
                marketValue = 900.0,
                sellerAsk = 300.0,
                targetMarginPct = 0.30,
                comparableQuality = 0.30,
                modelConfidence = 0.40,
                sourceCount = 1,
                staleDays = 50
            )
        )
        assertEquals(ValuationDecision.CAUTION, result.decision)
        assertTrue(result.confidence < 0.60)
        assertTrue(result.reasons.any { it.contains("confidence", ignoreCase = true) })
    }

    @Test
    fun repairRiskAndFeesReduceMaxBuy() {
        val clean = ValuationDecisionEngine.evaluate(
            ValuationDecisionInput(
                marketValue = 800.0,
                sellerAsk = 250.0,
                targetMarginPct = 0.30,
                sourceCount = 5
            )
        )
        val risky = ValuationDecisionEngine.evaluate(
            ValuationDecisionInput(
                marketValue = 800.0,
                sellerAsk = 250.0,
                targetMarginPct = 0.30,
                platformFeesPct = 0.12,
                freightCost = 30.0,
                repairRiskAllowance = 100.0,
                sourceCount = 5
            )
        )
        assertTrue(risky.maxBuyPrice < clean.maxBuyPrice)
        assertTrue(risky.reasons.any { it.contains("Repair-risk", ignoreCase = true) })
    }

    @Test
    fun staleOrRapidlyMovingMarketReturnsCaution() {
        val result = ValuationDecisionEngine.evaluate(
            ValuationDecisionInput(
                marketValue = 1200.0,
                sellerAsk = 400.0,
                targetMarginPct = 0.30,
                comparableQuality = 1.0,
                modelConfidence = 1.0,
                sourceCount = 5,
                staleDays = 50,
                priceChangePct = 25.0
            )
        )
        assertEquals(ValuationDecision.CAUTION, result.decision)
        assertTrue(result.reasons.any { it.contains("stale", ignoreCase = true) })
        assertTrue(result.reasons.any { it.contains("moved", ignoreCase = true) })
    }

    @Test
    fun unresolvedModelIdentityForcesCautionWithoutChangingPriceMath() {
        val resolved = ValuationDecisionEngine.evaluate(
            ValuationDecisionInput(
                marketValue = 1000.0,
                sellerAsk = 400.0,
                targetMarginPct = 0.30,
                sourceCount = 5
            )
        )
        val unresolved = ValuationDecisionEngine.evaluate(
            ValuationDecisionInput(
                marketValue = 1000.0,
                sellerAsk = 400.0,
                targetMarginPct = 0.30,
                sourceCount = 5,
                identityResolved = false
            )
        )
        assertEquals(ValuationDecision.BUY, resolved.decision)
        assertEquals(ValuationDecision.CAUTION, unresolved.decision)
        assertEquals(resolved.maxBuyPrice, unresolved.maxBuyPrice, 0.001)
        assertTrue(unresolved.reasons.any { it.contains("generation", ignoreCase = true) })
    }
}
