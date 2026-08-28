package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompleteValuationDecisionTest {
    private val base = ValuationDecisionInput(
        marketValue = 1000.0,
        sellerAsk = 400.0,
        targetMarginPct = 0.30,
        sourceCount = 5
    )

    private fun deal(id: String = "deal-1", age: Int = 1, current: Double = 1000.0) = SavedDealSignalInput(
        id = id,
        normalizedIdentity = "device model generation",
        askingPrice = 400.0,
        originalMarketValue = 1000.0,
        currentMarketValue = current,
        maxBuyPrice = 700.0,
        createdAgeDays = age,
        expectedMarginRate = 0.45
    )

    @Test fun emptyEvidenceAndNoDealPreserveAuthoritativePricing() {
        val direct = ValuationDecisionEngine.evaluate(base)
        val result = CompleteValuationDecision.evaluate(base)
        assertEquals(direct.decision, result.decision)
        assertEquals(direct.maxBuyPrice, result.consolidated.decision.maxBuyPrice, 0.0001)
        assertEquals(direct.adjustedResale, result.consolidated.decision.adjustedResale, 0.0001)
    }

    @Test fun staleDealCanOnlyDowngradeBuyWithoutChangingFinancials() {
        val result = CompleteValuationDecision.evaluate(base, deal = deal(age = 45))
        assertEquals(ValuationDecision.CAUTION, result.decision)
        assertEquals(ValuationDecisionEngine.evaluate(base).maxBuyPrice, result.consolidated.decision.maxBuyPrice, 0.0001)
        assertTrue(result.deal?.staleValuation == true)
    }

    @Test fun meaningfulMarketMovementDowngradesToCaution() {
        val result = CompleteValuationDecision.evaluate(base, deal = deal(current = 1200.0))
        assertEquals(ValuationDecision.CAUTION, result.decision)
        assertTrue(result.deal?.meaningfulMarketMovement == true)
    }

    @Test fun duplicateListingDowngradesButNeverOverridesPass() {
        val current = deal()
        val peer = deal(id = "deal-2")
        val caution = CompleteValuationDecision.evaluate(base, deal = current, peerDeals = listOf(peer))
        assertEquals(ValuationDecision.CAUTION, caution.decision)
        assertTrue(caution.deal?.duplicateListing == true)

        val pass = CompleteValuationDecision.evaluate(base.copy(sellerAsk = 2000.0), deal = current, peerDeals = listOf(peer))
        assertEquals(ValuationDecision.PASS, pass.decision)
    }

    @Test fun repairAndTargetMarginEvidenceRemainFinanciallyAuthoritative() {
        val repair = RepairRiskAssessment(100.0, 0.30, false, listOf("Repair reserve"))
        val margin = TargetMarginAssessment(0.30, 0.40, listOf("Margin reserve"))
        val result = CompleteValuationDecision.evaluate(
            base,
            ConsolidatedValuationEvidence(repairRisk = repair, targetMargin = margin, applySuggestedTargetMargin = true),
            deal = deal()
        )
        assertEquals(100.0, result.consolidated.input.repairRiskAllowance, 0.0001)
        assertEquals(0.40, result.consolidated.input.targetMarginPct, 0.0001)
        assertTrue(result.consolidated.decision.maxBuyPrice < ValuationDecisionEngine.evaluate(base).maxBuyPrice)
    }
}
