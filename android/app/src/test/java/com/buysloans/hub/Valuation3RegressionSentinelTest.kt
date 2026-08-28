package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Test

class Valuation3RegressionSentinelTest {
    @Test fun canonicalLegacyExampleKeepsKnownFinancialOutputs() {
        val result = CompleteValuationDecision.evaluate(ValuationDecisionInput(
            marketValue = 1000.0,
            sellerAsk = 400.0,
            targetMarginPct = 0.30,
            sourceCount = 5
        ))
        assertEquals(1000.0, result.consolidated.decision.adjustedResale, 0.0001)
        assertEquals(700.0, result.consolidated.decision.maxBuyPrice, 0.0001)
        assertEquals(600.0, result.consolidated.decision.expectedProfitAtAsk, 0.0001)
        assertEquals(0.60, result.consolidated.decision.expectedMarginAtAsk, 0.0001)
        assertEquals(ValuationDecision.BUY, result.decision)
    }
}
