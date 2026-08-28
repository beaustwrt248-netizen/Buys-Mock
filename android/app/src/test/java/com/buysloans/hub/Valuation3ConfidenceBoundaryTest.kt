package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Test

class Valuation3ConfidenceBoundaryTest {
    private val base = ValuationDecisionInput(
        marketValue = 1000.0,
        sellerAsk = 300.0,
        targetMarginPct = 0.30,
        sourceCount = 5
    )

    @Test fun staleThresholdOnlyDowngradesAfterFortyFiveDays() {
        assertEquals(ValuationDecision.BUY, CompleteValuationDecision.evaluate(base.copy(staleDays = 45)).decision)
        assertEquals(ValuationDecision.CAUTION, CompleteValuationDecision.evaluate(base.copy(staleDays = 46)).decision)
    }

    @Test fun marketMovementThresholdOnlyDowngradesBeyondTwentyPercent() {
        assertEquals(ValuationDecision.BUY, CompleteValuationDecision.evaluate(base.copy(priceChangePct = 20.0)).decision)
        assertEquals(ValuationDecision.CAUTION, CompleteValuationDecision.evaluate(base.copy(priceChangePct = 20.01)).decision)
        assertEquals(ValuationDecision.CAUTION, CompleteValuationDecision.evaluate(base.copy(priceChangePct = -20.01)).decision)
    }

    @Test fun resolvedStrongEvidenceRetainsBuy() {
        val result = CompleteValuationDecision.evaluate(base.copy(
            comparableQuality = 1.0,
            modelConfidence = 1.0,
            identityResolved = true,
            staleDays = 0
        ))
        assertEquals(ValuationDecision.BUY, result.decision)
    }
}
