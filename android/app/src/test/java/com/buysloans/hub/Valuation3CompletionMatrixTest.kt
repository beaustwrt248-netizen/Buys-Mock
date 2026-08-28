package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Valuation3CompletionMatrixTest {
    private val base = ValuationDecisionInput(
        marketValue = 1000.0,
        sellerAsk = 400.0,
        targetMarginPct = 0.30,
        sourceCount = 5
    )

    private fun strongComparables() = ComparableSalesQuality.assess(
        listOf(
            ComparableSaleEvidence(990.0, 5, true, true, true, true, true),
            ComparableSaleEvidence(1000.0, 8, true, true, true, true, true),
            ComparableSaleEvidence(1010.0, 12, true, true, true, true, true),
            ComparableSaleEvidence(995.0, 14, true, true, true, true, true),
            ComparableSaleEvidence(1005.0, 10, true, true, true, true, true)
        )
    )

    private fun deal(
        id: String = "deal-1",
        identity: String = "device model generation",
        ask: Double = 400.0,
        ageDays: Int = 1,
        currentMarket: Double = 1000.0,
        margin: Double = 0.45
    ) = SavedDealSignalInput(
        id = id,
        normalizedIdentity = identity,
        askingPrice = ask,
        originalMarketValue = 1000.0,
        currentMarketValue = currentMarket,
        maxBuyPrice = 700.0,
        createdAgeDays = ageDays,
        expectedMarginRate = margin
    )

    @Test fun completeStrongEvidenceKeepsBuyAndAuthoritativeMath() {
        val evidence = ConsolidatedValuationEvidence(
            comparables = strongComparables(),
            condition = ConditionAdjustment.assess(ConditionEvidence(ObservedCondition.EXCELLENT)),
            modelResolution = ModelResolution(ModelResolutionStatus.RESOLVED, "model-1", 1.0, listOf("resolved")),
            repairRisk = RepairRiskAssessment(0.0, 0.0, false, listOf("no repair reserve")),
            transactionCosts = TransactionCostEvidenceEngine.assess(TransactionCostEvidence()),
            targetMargin = TargetMarginAssessment(0.30, 0.30, listOf("configured margin")),
            applySuggestedTargetMargin = false
        )
        val result = CompleteValuationDecision.evaluate(base, evidence, deal = deal())
        assertEquals(ValuationDecision.BUY, result.decision)
        assertEquals(ValuationDecisionEngine.evaluate(base).maxBuyPrice, result.consolidated.decision.maxBuyPrice, 0.0001)
        assertEquals(ValuationDecisionEngine.evaluate(base).adjustedResale, result.consolidated.decision.adjustedResale, 0.0001)
    }

    @Test fun weakComparableAndAmbiguousIdentityConvergeOnCautionNotPass() {
        val weak = ComparableSalesAssessment(
            quality = 0.35,
            usableCount = 1,
            exactModelCount = 0,
            medianPrice = 1000.0,
            priceSpreadPct = 0.0,
            staleEvidence = false,
            reasons = listOf("weak comparables")
        )
        val ambiguous = ModelResolution(
            ModelResolutionStatus.AMBIGUOUS,
            null,
            0.50,
            listOf("ambiguous identity")
        )
        val result = CompleteValuationDecision.evaluate(
            base,
            ConsolidatedValuationEvidence(comparables = weak, modelResolution = ambiguous)
        )
        assertEquals(ValuationDecision.CAUTION, result.decision)
        assertTrue(result.consolidated.decision.confidence < 0.60)
        assertTrue(result.reasons.any { it.contains("identity", ignoreCase = true) || it.contains("model", ignoreCase = true) })
    }

    @Test fun conditionRepairFeesFreightAndOptInMarginReduceMaxBuyMonotonically() {
        val baseline = CompleteValuationDecision.evaluate(base)
        val condition = ConditionAdjustment.assess(ConditionEvidence(ObservedCondition.GOOD, majorFaultCount = 1, missingAccessoryCount = 1))
        val costs = TransactionCostEvidenceEngine.assess(
            TransactionCostEvidence(platformFeePct = 0.10, paymentFeePct = 0.02, outboundFreightCost = 35.0, packagingCost = 10.0)
        )
        val evidence = ConsolidatedValuationEvidence(
            condition = condition,
            repairRisk = RepairRiskAssessment(80.0, 0.50, false, listOf("repair reserve")),
            transactionCosts = costs,
            targetMargin = TargetMarginAssessment(0.30, 0.40, listOf("risk margin")),
            applySuggestedTargetMargin = true
        )
        val result = CompleteValuationDecision.evaluate(base, evidence)
        assertEquals(0.40, result.consolidated.input.targetMarginPct, 0.0001)
        assertEquals(80.0, result.consolidated.input.repairRiskAllowance, 0.0001)
        assertEquals(0.12, result.consolidated.input.platformFeesPct, 0.0001)
        assertEquals(45.0, result.consolidated.input.freightCost, 0.0001)
        assertTrue(result.consolidated.decision.maxBuyPrice < baseline.consolidated.decision.maxBuyPrice)
    }

    @Test fun informationalOpportunitySignalsDoNotRewriteBuyOrFinancials() {
        val opportunity = deal(ask = 300.0, margin = 0.70)
        val input = base.copy(sellerAsk = 300.0)
        val result = CompleteValuationDecision.evaluate(input, deal = opportunity)
        val direct = ValuationDecisionEngine.evaluate(input)
        assertEquals(ValuationDecision.BUY, result.decision)
        assertTrue(result.deal?.underpricedOpportunity == true)
        assertTrue(result.deal?.unusuallyHighMargin == true)
        assertEquals(direct.maxBuyPrice, result.consolidated.decision.maxBuyPrice, 0.0001)
        assertEquals(direct.expectedProfitAtAsk, result.consolidated.decision.expectedProfitAtAsk, 0.0001)
    }

    @Test fun staleDuplicateAndMarketMovementCanOnlyDowngradeToCaution() {
        val current = deal(ageDays = 45, currentMarket = 1200.0)
        val duplicate = deal(id = "deal-2")
        val result = CompleteValuationDecision.evaluate(base, deal = current, peerDeals = listOf(duplicate))
        assertEquals(ValuationDecision.CAUTION, result.decision)
        assertTrue(result.deal?.staleValuation == true)
        assertTrue(result.deal?.duplicateListing == true)
        assertTrue(result.deal?.meaningfulMarketMovement == true)
    }

    @Test fun passIsTerminalEvenWhenDealSignalsLookAttractive() {
        val passInput = base.copy(sellerAsk = 2000.0)
        val attractiveSavedDeal = deal(ask = 100.0, margin = 0.90)
        val result = CompleteValuationDecision.evaluate(passInput, deal = attractiveSavedDeal)
        assertEquals(ValuationDecision.PASS, result.decision)
        assertTrue(result.deal?.underpricedOpportunity == true)
        assertTrue(result.deal?.unusuallyHighMargin == true)
    }

    @Test fun a1932ResolvedIdentityPathPreservesCurrentDefaultPricing() {
        val direct = ValuationDecisionEngine.evaluate(base)
        val resolvedA1932 = ModelResolution(
            ModelResolutionStatus.RESOLVED,
            "A1932",
            1.0,
            listOf("A1932 resolved")
        )
        val result = CompleteValuationDecision.evaluate(
            base,
            ConsolidatedValuationEvidence(modelResolution = resolvedA1932)
        )
        assertEquals(direct.decision, result.decision)
        assertEquals(direct.maxBuyPrice, result.consolidated.decision.maxBuyPrice, 0.0001)
        assertEquals(direct.adjustedResale, result.consolidated.decision.adjustedResale, 0.0001)
        assertEquals(direct.expectedProfitAtAsk, result.consolidated.decision.expectedProfitAtAsk, 0.0001)
    }
}
