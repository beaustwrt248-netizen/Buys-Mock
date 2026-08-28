package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValuationDecisionCoordinatorTest {
    private val base = ValuationDecisionInput(
        marketValue = 1000.0,
        sellerAsk = 400.0,
        targetMarginPct = 0.30,
        sourceCount = 5
    )

    @Test fun emptyEvidencePreservesExistingPricingAndDecision() {
        val direct = ValuationDecisionEngine.evaluate(base)
        val coordinated = ValuationDecisionCoordinator.evaluate(base)
        assertEquals(direct.decision, coordinated.decision.decision)
        assertEquals(direct.maxBuyPrice, coordinated.decision.maxBuyPrice, 0.0001)
        assertEquals(direct.adjustedResale, coordinated.decision.adjustedResale, 0.0001)
    }

    @Test fun evidenceIsCombinedBeforeSingleAuthoritativeDecision() {
        val comparable = ComparableSalesAssessment(0.90, 5, 5, 1000.0, 10.0, false, listOf("Strong comparable evidence"))
        val repair = RepairRiskAssessment(100.0, 0.30, false, listOf("Repair reserve"))
        val costs = TransactionCostAssessment(0.10, 50.0, listOf("Selling costs"))
        val condition = ConditionAdjustmentResult(0.90, listOf("Good condition"))

        val result = ValuationDecisionCoordinator.evaluate(
            base,
            ConsolidatedValuationEvidence(
                comparables = comparable,
                condition = condition,
                repairRisk = repair,
                transactionCosts = costs
            )
        )

        assertEquals(0.90, result.input.conditionMultiplier, 0.0001)
        assertEquals(0.10, result.input.platformFeesPct, 0.0001)
        assertEquals(50.0, result.input.freightCost, 0.0001)
        assertEquals(100.0, result.input.repairRiskAllowance, 0.0001)
        assertTrue(result.decision.maxBuyPrice < ValuationDecisionEngine.evaluate(base).maxBuyPrice)
    }

    @Test fun targetMarginSuggestionIsOptInToPreserveCurrentPricing() {
        val margin = TargetMarginAssessment(0.30, 0.40, listOf("Risk reserve"))
        val preserved = ValuationDecisionCoordinator.evaluate(base, ConsolidatedValuationEvidence(targetMargin = margin))
        val applied = ValuationDecisionCoordinator.evaluate(base, ConsolidatedValuationEvidence(targetMargin = margin, applySuggestedTargetMargin = true))

        assertEquals(0.30, preserved.input.targetMarginPct, 0.0001)
        assertEquals(ValuationDecisionEngine.evaluate(base).maxBuyPrice, preserved.decision.maxBuyPrice, 0.0001)
        assertEquals(0.40, applied.input.targetMarginPct, 0.0001)
        assertTrue(applied.decision.maxBuyPrice < preserved.decision.maxBuyPrice)
    }

    @Test fun repairUncertaintyCanDowngradeBuyButNeverUpgradePass() {
        val caution = RepairRiskAssessment(0.0, 0.70, true, listOf("Repair uncertainty"))
        val buyResult = ValuationDecisionCoordinator.evaluate(base, ConsolidatedValuationEvidence(repairRisk = caution))
        assertEquals(ValuationDecision.CAUTION, buyResult.decision.decision)

        val passInput = base.copy(sellerAsk = 2000.0)
        val passResult = ValuationDecisionCoordinator.evaluate(passInput, ConsolidatedValuationEvidence(repairRisk = caution))
        assertEquals(ValuationDecision.PASS, passResult.decision.decision)
    }
}
