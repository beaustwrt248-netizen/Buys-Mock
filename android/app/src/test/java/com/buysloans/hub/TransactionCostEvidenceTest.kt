package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionCostEvidenceTest {
    private val healthy = ValuationDecisionInput(
        marketValue = 1000.0,
        sellerAsk = 450.0,
        targetMarginPct = 0.30,
        platformFeesPct = 0.10,
        freightCost = 20.0,
        repairRiskAllowance = 30.0,
        conditionMultiplier = 0.95,
        comparableQuality = 0.95,
        modelConfidence = 0.95,
        sourceCount = 5,
        staleDays = 5
    )

    @Test
    fun emptyEvidencePreservesExistingDecisionMath() {
        val baseline = ValuationDecisionEngine.evaluate(healthy)
        val assessment = TransactionCostEvidenceEngine.assess(TransactionCostEvidence())
        val result = TransactionCostDecisionAdapter.evaluate(healthy, assessment)

        assertEquals(baseline.decision, result.decision)
        assertEquals(baseline.adjustedResale, result.adjustedResale, 0.0001)
        assertEquals(baseline.maxBuyPrice, result.maxBuyPrice, 0.0001)
        assertEquals(baseline.expectedProfitAtAsk, result.expectedProfitAtAsk, 0.0001)
    }

    @Test
    fun percentageFeeEvidenceReducesMaxBuyWithoutReplacingCoreMath() {
        val assessment = TransactionCostEvidenceEngine.assess(
            TransactionCostEvidence(platformFeePct = 0.12, paymentFeePct = 0.03)
        )
        val baseline = ValuationDecisionEngine.evaluate(healthy)
        val result = TransactionCostDecisionAdapter.evaluate(healthy, assessment)

        assertEquals(0.15, assessment.percentageFeesPct, 0.0001)
        assertTrue(result.maxBuyPrice < baseline.maxBuyPrice)
        assertEquals(baseline.adjustedResale, result.adjustedResale, 0.0001)
    }

    @Test
    fun fixedSellingCostsAreReservedConservatively() {
        val assessment = TransactionCostEvidenceEngine.assess(
            TransactionCostEvidence(
                fixedMarketplaceFee = 5.0,
                outboundFreightCost = 40.0,
                packagingCost = 10.0
            )
        )
        val baseline = ValuationDecisionEngine.evaluate(healthy)
        val result = TransactionCostDecisionAdapter.evaluate(healthy, assessment)

        assertEquals(55.0, assessment.fixedCostReserve, 0.0001)
        assertEquals(baseline.maxBuyPrice - (55.0 - 20.0) * (1.0 - healthy.targetMarginPct), result.maxBuyPrice, 0.0001)
        assertTrue(result.reasons.any { it.contains("freight", ignoreCase = true) })
    }

    @Test
    fun existingLargerManualReservesAreNeverReduced() {
        val conservative = healthy.copy(platformFeesPct = 0.20, freightCost = 90.0)
        val assessment = TransactionCostEvidenceEngine.assess(
            TransactionCostEvidence(
                platformFeePct = 0.10,
                paymentFeePct = 0.02,
                outboundFreightCost = 30.0,
                packagingCost = 5.0
            )
        )
        val baseline = ValuationDecisionEngine.evaluate(conservative)
        val result = TransactionCostDecisionAdapter.evaluate(conservative, assessment)

        assertEquals(baseline.maxBuyPrice, result.maxBuyPrice, 0.0001)
        assertEquals(baseline.expectedProfitAtAsk, result.expectedProfitAtAsk, 0.0001)
    }

    @Test
    fun invalidNegativeAndExcessiveInputsAreBounded() {
        val assessment = TransactionCostEvidenceEngine.assess(
            TransactionCostEvidence(
                platformFeePct = 1.5,
                paymentFeePct = -0.5,
                fixedMarketplaceFee = -10.0,
                outboundFreightCost = -20.0,
                packagingCost = -5.0
            )
        )

        assertEquals(0.75, assessment.percentageFeesPct, 0.0001)
        assertEquals(0.0, assessment.fixedCostReserve, 0.0001)
    }
}
