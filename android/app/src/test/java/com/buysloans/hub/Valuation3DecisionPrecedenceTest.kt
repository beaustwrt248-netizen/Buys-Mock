package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Test

class Valuation3DecisionPrecedenceTest {
    private fun deal() = SavedDealSignalInput(
        id = "deal",
        normalizedIdentity = "device model generation",
        askingPrice = 100.0,
        originalMarketValue = 1000.0,
        currentMarketValue = 1000.0,
        maxBuyPrice = 700.0,
        createdAgeDays = 1,
        expectedMarginRate = 0.70
    )

    @Test fun consolidatedPassCannotBeUpgradedByOpportunitySignals() {
        val result = CompleteValuationDecision.evaluate(
            ValuationDecisionInput(1000.0, 900.0, 0.30, sourceCount = 5),
            deal = deal()
        )
        assertEquals(ValuationDecision.PASS, result.decision)
    }

    @Test fun consolidatedCautionCannotBeUpgradedByOpportunitySignals() {
        val result = CompleteValuationDecision.evaluate(
            ValuationDecisionInput(1000.0, 400.0, 0.30, sourceCount = 5, identityResolved = false),
            deal = deal()
        )
        assertEquals(ValuationDecision.CAUTION, result.decision)
    }
}
