package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Test

class Valuation3ConditionBoundTest {
    @Test fun conditionMultiplierRemainsBoundedByDecisionEngine() {
        val base = ValuationDecisionInput(1000.0, 100.0, 0.30, sourceCount = 5)
        val over = CompleteValuationDecision.evaluate(base.copy(conditionMultiplier = 2.0))
        val cap = CompleteValuationDecision.evaluate(base.copy(conditionMultiplier = 1.25))
        assertEquals(cap.consolidated.decision.adjustedResale, over.consolidated.decision.adjustedResale, 0.0001)
        assertEquals(cap.consolidated.decision.maxBuyPrice, over.consolidated.decision.maxBuyPrice, 0.0001)
    }
}
