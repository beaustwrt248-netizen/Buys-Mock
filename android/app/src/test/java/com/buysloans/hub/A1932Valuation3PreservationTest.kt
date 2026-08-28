package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A1932 preservation lock: the Valuation 3.0 orchestration must be transparent when no new
 * evidence is supplied. The underlying A1932 market/pricing source remains untouched.
 */
class A1932Valuation3PreservationTest {
    @Test fun completeDecisionIsTransparentForA1932ShapedLegacyInput() {
        val input = ValuationDecisionInput(
            marketValue = 350.0,
            sellerAsk = 150.0,
            targetMarginPct = 0.30,
            sourceCount = 5,
            identityResolved = true,
            modelConfidence = 1.0
        )
        val legacy = ValuationDecisionEngine.evaluate(input)
        val complete = CompleteValuationDecision.evaluate(input)
        assertEquals(legacy.decision, complete.decision)
        assertEquals(legacy.confidence, complete.consolidated.decision.confidence, 0.0001)
        assertEquals(legacy.adjustedResale, complete.consolidated.decision.adjustedResale, 0.0001)
        assertEquals(legacy.maxBuyPrice, complete.consolidated.decision.maxBuyPrice, 0.0001)
        assertEquals(legacy.expectedProfitAtAsk, complete.consolidated.decision.expectedProfitAtAsk, 0.0001)
        assertEquals(legacy.expectedMarginAtAsk, complete.consolidated.decision.expectedMarginAtAsk, 0.0001)
    }
}
