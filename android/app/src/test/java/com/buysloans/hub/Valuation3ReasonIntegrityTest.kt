package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Valuation3ReasonIntegrityTest {
    @Test fun finalReasonsAreNonEmptyAndDeduplicated() {
        val result = CompleteValuationDecision.evaluate(ValuationDecisionInput(
            marketValue = 1000.0,
            sellerAsk = 400.0,
            targetMarginPct = 0.30,
            sourceCount = 5,
            identityResolved = false,
            staleDays = 60,
            priceChangePct = 25.0
        ))
        assertTrue(result.reasons.isNotEmpty())
        assertEquals(result.reasons.distinct(), result.reasons)
    }
}
