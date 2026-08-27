package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComparableEvidenceDecisionAdapterTest {
    @Test
    fun strongComparableEvidenceKeepsHealthyDealAsBuy() {
        val assessment = ComparableSalesQuality.assess(
            listOf(
                ComparableSaleEvidence(1000.0, 5, true, true, true, true, true),
                ComparableSaleEvidence(1020.0, 8, true, true, true, true, true),
                ComparableSaleEvidence(980.0, 12, true, true, true, true, true),
                ComparableSaleEvidence(1010.0, 15, true, true, true, true, true),
                ComparableSaleEvidence(995.0, 10, true, true, true, true, true)
            )
        )
        val result = ComparableEvidenceDecisionAdapter.evaluate(
            ValuationDecisionInput(
                marketValue = 1000.0,
                sellerAsk = 400.0,
                targetMarginPct = 0.30,
                modelConfidence = 1.0
            ),
            assessment
        )
        assertEquals(ValuationDecision.BUY, result.decision)
        assertTrue(result.confidence >= 0.80)
    }

    @Test
    fun weakComparableEvidenceForcesCautionWithoutChangingMaxBuyMath() {
        val base = ValuationDecisionInput(
            marketValue = 1000.0,
            sellerAsk = 400.0,
            targetMarginPct = 0.30,
            platformFeesPct = 0.10,
            freightCost = 25.0,
            repairRiskAllowance = 50.0,
            conditionMultiplier = 0.95,
            modelConfidence = 1.0
        )
        val baseline = ValuationDecisionEngine.evaluate(base.copy(comparableQuality = 1.0, sourceCount = 5))
        val weakAssessment = ComparableSalesQuality.assess(
            listOf(
                ComparableSaleEvidence(1000.0, 120, false, true, false, false, false)
            )
        )
        val result = ComparableEvidenceDecisionAdapter.evaluate(base, weakAssessment)
        assertEquals(ValuationDecision.CAUTION, result.decision)
        assertEquals(baseline.maxBuyPrice, result.maxBuyPrice, 0.001)
        assertTrue(result.confidence < baseline.confidence)
        assertTrue(result.reasons.any { it.contains("confidence", ignoreCase = true) || it.contains("stale", ignoreCase = true) })
    }

    @Test
    fun staleComparableAssessmentPropagatesCautionWithoutChangingPriceMath() {
        val input = ValuationDecisionInput(
            marketValue = 800.0,
            sellerAsk = 250.0,
            targetMarginPct = 0.30,
            sourceCount = 5
        )
        val staleAssessment = ComparableSalesAssessment(
            quality = 0.95,
            usableCount = 5,
            exactModelCount = 5,
            medianPrice = 800.0,
            priceSpreadPct = 5.0,
            staleEvidence = true,
            reasons = listOf("Comparable evidence is stale")
        )
        val direct = ValuationDecisionEngine.evaluate(input.copy(comparableQuality = 0.95, sourceCount = 5))
        val result = ComparableEvidenceDecisionAdapter.evaluate(input, staleAssessment)
        assertEquals(ValuationDecision.CAUTION, result.decision)
        assertEquals(direct.maxBuyPrice, result.maxBuyPrice, 0.001)
        assertTrue(result.reasons.any { it.contains("stale", ignoreCase = true) })
    }
}
