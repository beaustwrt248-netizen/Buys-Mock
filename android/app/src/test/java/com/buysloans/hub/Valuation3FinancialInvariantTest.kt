package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Valuation3FinancialInvariantTest {
    @Test fun maxBuyNeverNegativeEvenWhenCostsExceedResale() {
        val result = CompleteValuationDecision.evaluate(ValuationDecisionInput(
            marketValue = 100.0,
            sellerAsk = 10.0,
            targetMarginPct = 0.30,
            platformFeesPct = 0.50,
            freightCost = 100.0,
            repairRiskAllowance = 100.0,
            sourceCount = 5
        ))
        assertEquals(0.0, result.consolidated.decision.maxBuyPrice, 0.0001)
        assertEquals(ValuationDecision.PASS, result.decision)
    }

    @Test fun higherTargetMarginCannotIncreaseMaxBuy() {
        val low = CompleteValuationDecision.evaluate(ValuationDecisionInput(1000.0, 300.0, 0.20, sourceCount = 5))
        val high = CompleteValuationDecision.evaluate(ValuationDecisionInput(1000.0, 300.0, 0.40, sourceCount = 5))
        assertTrue(high.consolidated.decision.maxBuyPrice <= low.consolidated.decision.maxBuyPrice)
    }

    @Test fun additionalFeesFreightOrRepairCannotIncreaseMaxBuy() {
        val base = ValuationDecisionInput(1000.0, 300.0, 0.30, sourceCount = 5)
        val plain = CompleteValuationDecision.evaluate(base).consolidated.decision.maxBuyPrice
        val fees = CompleteValuationDecision.evaluate(base.copy(platformFeesPct = 0.10)).consolidated.decision.maxBuyPrice
        val freight = CompleteValuationDecision.evaluate(base.copy(freightCost = 50.0)).consolidated.decision.maxBuyPrice
        val repair = CompleteValuationDecision.evaluate(base.copy(repairRiskAllowance = 100.0)).consolidated.decision.maxBuyPrice
        assertTrue(fees < plain)
        assertTrue(freight < plain)
        assertTrue(repair < plain)
    }
}
