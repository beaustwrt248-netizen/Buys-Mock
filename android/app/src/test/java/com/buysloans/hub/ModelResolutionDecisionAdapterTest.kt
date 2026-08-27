package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelResolutionDecisionAdapterTest {
    private val healthy = ValuationDecisionInput(
        marketValue = 1000.0,
        sellerAsk = 450.0,
        targetMarginPct = 0.30,
        platformFeesPct = 0.10,
        freightCost = 20.0,
        repairRiskAllowance = 30.0,
        conditionMultiplier = 0.95,
        comparableQuality = 0.95,
        modelConfidence = 1.0,
        sourceCount = 5,
        staleDays = 5
    )

    @Test
    fun resolvedHighConfidenceEvidencePreservesHealthyBuy() {
        val baseline = ValuationDecisionEngine.evaluate(healthy)
        val result = ModelResolutionDecisionAdapter.evaluate(
            healthy,
            ModelResolution(
                status = ModelResolutionStatus.RESOLVED,
                candidateId = "model-1",
                confidence = 1.0,
                reasons = listOf("Model identity is uniquely supported by the supplied evidence")
            )
        )

        assertEquals(ValuationDecision.BUY, baseline.decision)
        assertEquals(ValuationDecision.BUY, result.decision)
        assertEquals(baseline.adjustedResale, result.adjustedResale, 0.0001)
        assertEquals(baseline.maxBuyPrice, result.maxBuyPrice, 0.0001)
        assertEquals(baseline.expectedProfitAtAsk, result.expectedProfitAtAsk, 0.0001)
    }

    @Test
    fun ambiguousIdentityForcesCautionWithoutChangingPriceMath() {
        val baseline = ValuationDecisionEngine.evaluate(healthy)
        val result = ModelResolutionDecisionAdapter.evaluate(
            healthy,
            ModelResolution(
                status = ModelResolutionStatus.AMBIGUOUS,
                candidateId = "model-1",
                confidence = 0.55,
                reasons = listOf("Multiple model, year, or generation candidates remain plausible")
            )
        )

        assertEquals(ValuationDecision.CAUTION, result.decision)
        assertEquals(baseline.adjustedResale, result.adjustedResale, 0.0001)
        assertEquals(baseline.maxBuyPrice, result.maxBuyPrice, 0.0001)
        assertTrue(result.reasons.any { it.contains("requires confirmation", ignoreCase = true) })
        assertTrue(result.reasons.any { it.contains("remain plausible", ignoreCase = true) })
    }

    @Test
    fun weakUnresolvedEvidenceReducesConfidenceAndDoesNotGuess() {
        val baseline = ValuationDecisionEngine.evaluate(healthy)
        val result = ModelResolutionDecisionAdapter.evaluate(
            healthy,
            ModelResolution(
                status = ModelResolutionStatus.UNRESOLVED,
                candidateId = null,
                confidence = 0.20,
                reasons = listOf("Evidence is insufficient to resolve model identity")
            )
        )

        assertEquals(ValuationDecision.CAUTION, result.decision)
        assertTrue(result.confidence < baseline.confidence)
        assertEquals(baseline.maxBuyPrice, result.maxBuyPrice, 0.0001)
        assertTrue(result.reasons.any { it.contains("insufficient", ignoreCase = true) })
    }

    @Test
    fun resolvedEvidenceCannotUpgradePreviouslyUnresolvedIdentity() {
        val alreadyUnresolved = healthy.copy(identityResolved = false)
        val result = ModelResolutionDecisionAdapter.evaluate(
            alreadyUnresolved,
            ModelResolution(
                status = ModelResolutionStatus.RESOLVED,
                candidateId = "model-1",
                confidence = 1.0,
                reasons = listOf("Model identity is uniquely supported by the supplied evidence")
            )
        )

        assertEquals(ValuationDecision.CAUTION, result.decision)
        assertTrue(result.reasons.any { it.contains("requires confirmation", ignoreCase = true) })
    }
}
