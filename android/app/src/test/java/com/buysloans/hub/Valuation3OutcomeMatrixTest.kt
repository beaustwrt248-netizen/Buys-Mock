package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Test

class Valuation3OutcomeMatrixTest {
    @Test fun explicitBuyCautionPassOutcomesRemainReachable() {
        val common = ValuationDecisionInput(1000.0, 400.0, 0.30, sourceCount = 5)
        assertEquals(ValuationDecision.BUY, CompleteValuationDecision.evaluate(common).decision)
        assertEquals(ValuationDecision.CAUTION, CompleteValuationDecision.evaluate(common.copy(identityResolved = false)).decision)
        assertEquals(ValuationDecision.PASS, CompleteValuationDecision.evaluate(common.copy(sellerAsk = 900.0)).decision)
    }

    @Test fun zeroOrNegativeMarketCannotProduceBuy() {
        assertEquals(ValuationDecision.PASS, CompleteValuationDecision.evaluate(
            ValuationDecisionInput(0.0, 0.0, 0.30, sourceCount = 5)
        ).decision)
        assertEquals(ValuationDecision.PASS, CompleteValuationDecision.evaluate(
            ValuationDecisionInput(-100.0, 0.0, 0.30, sourceCount = 5)
        ).decision)
    }
}
