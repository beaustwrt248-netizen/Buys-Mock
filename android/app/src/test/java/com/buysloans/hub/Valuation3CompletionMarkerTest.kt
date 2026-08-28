package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Test

class Valuation3CompletionMarkerTest {
    @Test fun completeEngineUsesConsolidatedDecisionWhenNoDealExists() {
        val input = ValuationDecisionInput(800.0, 250.0, 0.30, sourceCount = 5)
        val consolidated = ValuationDecisionCoordinator.evaluate(input)
        val complete = CompleteValuationDecision.evaluate(input)
        assertEquals(consolidated.decision.decision, complete.decision)
        assertEquals(consolidated.decision.maxBuyPrice, complete.consolidated.decision.maxBuyPrice, 0.0001)
        assertEquals(consolidated.reasons, complete.reasons)
    }
}
