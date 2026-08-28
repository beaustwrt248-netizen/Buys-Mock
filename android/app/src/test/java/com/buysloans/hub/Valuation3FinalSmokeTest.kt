package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Valuation3FinalSmokeTest {
    @Test fun completeEngineProducesFiniteSaneFinancialOutputs() {
        val result = CompleteValuationDecision.evaluate(ValuationDecisionInput(
            marketValue = 1250.0,
            sellerAsk = 500.0,
            targetMarginPct = 0.35,
            platformFeesPct = 0.12,
            freightCost = 30.0,
            repairRiskAllowance = 75.0,
            conditionMultiplier = 0.90,
            comparableQuality = 0.85,
            modelConfidence = 0.90,
            sourceCount = 4,
            staleDays = 10,
            identityResolved = true
        ))
        val financials = result.consolidated.decision
        assertTrue(financials.confidence in 0.0..1.0)
        assertTrue(financials.adjustedResale >= 0.0)
        assertTrue(financials.maxBuyPrice >= 0.0)
        assertTrue(financials.adjustedResale.isFinite())
        assertTrue(financials.maxBuyPrice.isFinite())
        assertTrue(financials.expectedProfitAtAsk.isFinite())
        assertTrue(financials.expectedMarginAtAsk.isFinite())
        assertTrue(result.reasons.isNotEmpty())
        assertTrue(result.decision == ValuationDecision.BUY || result.decision == ValuationDecision.CAUTION || result.decision == ValuationDecision.PASS)
    }
}
