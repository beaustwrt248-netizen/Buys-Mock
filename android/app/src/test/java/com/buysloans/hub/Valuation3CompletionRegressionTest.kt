package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** End-to-end completion locks for Valuation 3.0. Production pricing code is intentionally untouched. */
class Valuation3CompletionRegressionTest {
    private val baseline = ValuationDecisionInput(
        marketValue = 1000.0,
        sellerAsk = 400.0,
        targetMarginPct = 0.30,
        sourceCount = 5
    )

    private fun savedDeal(
        id: String = "deal-1",
        identity: String = "device model generation",
        ask: Double = 400.0,
        original: Double = 1000.0,
        current: Double = 1000.0,
        age: Int = 1,
        margin: Double = 0.45
    ) = SavedDealSignalInput(
        id = id,
        normalizedIdentity = identity,
        askingPrice = ask,
        originalMarketValue = original,
        currentMarketValue = current,
        maxBuyPrice = 700.0,
        createdAgeDays = age,
        expectedMarginRate = margin
    )

    @Test fun noEvidencePathExactlyPreservesLegacyDecisionMath() {
        listOf(250.0, 400.0, 700.0, 750.0, 900.0).forEach { ask ->
            val input = baseline.copy(sellerAsk = ask)
            val legacy = ValuationDecisionEngine.evaluate(input)
            val complete = CompleteValuationDecision.evaluate(input)
            assertEquals(legacy.decision, complete.decision)
            assertEquals(legacy.adjustedResale, complete.consolidated.decision.adjustedResale, 0.0001)
            assertEquals(legacy.maxBuyPrice, complete.consolidated.decision.maxBuyPrice, 0.0001)
            assertEquals(legacy.expectedProfitAtAsk, complete.consolidated.decision.expectedProfitAtAsk, 0.0001)
        }
    }

    @Test fun maxBuyBoundaryRemainsBuyThenCautionThenPass() {
        val maxBuy = ValuationDecisionEngine.evaluate(baseline).maxBuyPrice
        assertEquals(ValuationDecision.BUY, CompleteValuationDecision.evaluate(baseline.copy(sellerAsk = maxBuy)).decision)
        assertEquals(ValuationDecision.CAUTION, CompleteValuationDecision.evaluate(baseline.copy(sellerAsk = maxBuy + 1.0)).decision)
        assertEquals(ValuationDecision.PASS, CompleteValuationDecision.evaluate(baseline.copy(sellerAsk = maxBuy * 1.16)).decision)
    }

    @Test fun weakComparableAndUnresolvedIdentityCannotProduceBuy() {
        val weak = CompleteValuationDecision.evaluate(baseline.copy(comparableQuality = 0.20, sourceCount = 1))
        assertTrue(weak.decision != ValuationDecision.BUY)
        val unresolved = CompleteValuationDecision.evaluate(baseline.copy(identityResolved = false, modelConfidence = 0.30))
        assertEquals(ValuationDecision.CAUTION, unresolved.decision)
    }

    @Test fun conditionRepairFeesAndFreightAllReduceMaxBuyWithoutChangingMarketInput() {
        val plain = CompleteValuationDecision.evaluate(baseline)
        val stressed = CompleteValuationDecision.evaluate(baseline.copy(
            conditionMultiplier = 0.80,
            repairRiskAllowance = 100.0,
            platformFeesPct = 0.10,
            freightCost = 50.0
        ))
        assertTrue(stressed.consolidated.decision.maxBuyPrice < plain.consolidated.decision.maxBuyPrice)
        assertEquals(baseline.marketValue, stressed.consolidated.input.marketValue, 0.0001)
    }

    @Test fun staleMovementAndDuplicateSignalsOnlyDowngradeFinanciallyValidBuy() {
        val plain = CompleteValuationDecision.evaluate(baseline)
        assertEquals(ValuationDecision.BUY, plain.decision)

        val stale = CompleteValuationDecision.evaluate(baseline, deal = savedDeal(age = 45))
        val moved = CompleteValuationDecision.evaluate(baseline, deal = savedDeal(current = 1250.0))
        val duplicate = CompleteValuationDecision.evaluate(
            baseline,
            deal = savedDeal(),
            peerDeals = listOf(savedDeal(id = "deal-2"))
        )
        listOf(stale, moved, duplicate).forEach { result ->
            assertEquals(ValuationDecision.CAUTION, result.decision)
            assertEquals(plain.consolidated.decision.maxBuyPrice, result.consolidated.decision.maxBuyPrice, 0.0001)
        }
    }

    @Test fun underpricedAndHighMarginSignalsRemainInformational() {
        val result = CompleteValuationDecision.evaluate(
            baseline,
            deal = savedDeal(ask = 200.0, margin = 0.70)
        )
        assertEquals(ValuationDecision.BUY, result.decision)
        assertTrue(result.deal?.underpricedOpportunity == true)
        assertTrue(result.deal?.unusuallyHighMargin == true)
    }

    @Test fun passIsTerminalAcrossDealIntelligence() {
        val passInput = baseline.copy(sellerAsk = 2000.0)
        val result = CompleteValuationDecision.evaluate(
            passInput,
            deal = savedDeal(age = 90, current = 1500.0),
            peerDeals = listOf(savedDeal(id = "deal-2"))
        )
        assertEquals(ValuationDecision.PASS, result.decision)
    }

    @Test fun suggestedTargetMarginIsOptInAndNeverSilentlyChangesPricing() {
        val assessment = TargetMarginAssessment(0.30, 0.40, listOf("Evidence reserve"))
        val notApplied = CompleteValuationDecision.evaluate(
            baseline,
            ConsolidatedValuationEvidence(targetMargin = assessment, applySuggestedTargetMargin = false)
        )
        val applied = CompleteValuationDecision.evaluate(
            baseline,
            ConsolidatedValuationEvidence(targetMargin = assessment, applySuggestedTargetMargin = true)
        )
        assertEquals(0.30, notApplied.consolidated.input.targetMarginPct, 0.0001)
        assertEquals(0.40, applied.consolidated.input.targetMarginPct, 0.0001)
        assertTrue(applied.consolidated.decision.maxBuyPrice < notApplied.consolidated.decision.maxBuyPrice)
    }
}
