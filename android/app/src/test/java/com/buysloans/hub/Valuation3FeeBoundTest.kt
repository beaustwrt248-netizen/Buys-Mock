package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Test

class Valuation3FeeBoundTest {
    @Test fun feePercentageRemainsBoundedByDecisionEngine() {
        val base = ValuationDecisionInput(1000.0, 100.0, 0.30, sourceCount = 5)
        val over = CompleteValuationDecision.evaluate(base.copy(platformFeesPct = 2.0))
        val cap = CompleteValuationDecision.evaluate(base.copy(platformFeesPct = 0.75))
        assertEquals(cap.consolidated.decision.maxBuyPrice, over.consolidated.decision.maxBuyPrice, 0.0001)
        assertEquals(cap.consolidated.decision.expectedProfitAtAsk, over.consolidated.decision.expectedProfitAtAsk, 0.0001)
    }
}
