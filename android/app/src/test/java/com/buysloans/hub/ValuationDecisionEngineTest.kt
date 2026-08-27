package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValuationDecisionEngineTest {
    @Test fun strongEvidenceUnderMaxBuyReturnsBuy() {
        val result = ValuationDecisionEngine.decide(
            ValuationDecisionInput(
                marketValue = 600.0,
                askingPrice = 250.0,
                exactComparableCount = 6,
                comparablePriceSpread = 0.12,
                targetMarginRate = 0.30,
                sellingFeeRate = 0.10,
                freightCost = 20.0
            )
        )
        assertEquals(ComparableQuality.STRONG, result.comparableQuality)
        assertEquals(ValuationOutcome.BUY, result.outcome)
        assertEquals(340.0, result.maxBuyPrice, 0.01)
        assertTrue(result.confidenceScore >= 75)
    }

    @Test fun feesRepairAndConditionReduceMaxBuy() {
        val result = ValuationDecisionEngine.decide(
            ValuationDecisionInput(
                marketValue = 1000.0,
                askingPrice = 350.0,
                exactComparableCount = 5,
                comparablePriceSpread = 0.15,
                conditionMultiplier = 0.80,
                repairRiskAllowance = 100.0,
                freightCost = 30.0,
                sellingFeeRate = 0.10,
                fixedSellingFees = 10.0,
                targetMarginRate = 0.30
            )
        )
        assertEquals(800.0, result.adjustedMarketValue, 0.01)
        assertEquals(340.0, result.maxBuyPrice, 0.01)
        assertEquals(ValuationOutcome.PASS, result.outcome)
    }

    @Test fun unresolvedGenerationForcesCautionEvenWhenCheap() {
        val result = ValuationDecisionEngine.decide(
            ValuationDecisionInput(
                marketValue = 600.0,
                askingPrice = 100.0,
                exactComparableCount = 5,
                comparablePriceSpread = 0.10,
                modelResolved = true,
                generationResolved = false,
                targetMarginRate = 0.30
            )
        )
        assertEquals(ValuationOutcome.CAUTION, result.outcome)
        assertTrue(result.reasons.any { it.contains("generation", ignoreCase = true) })
    }

    @Test fun targetMarginMatchesExistingGradeGpSemantics() {
        val a = ValuationDecisionEngine.decide(ValuationDecisionInput(1000.0, 600.0, 6, 0, 0.10, targetMarginRate = 0.30))
        val b = ValuationDecisionEngine.decide(ValuationDecisionInput(1000.0, 400.0, 6, 0, 0.10, targetMarginRate = 0.50))
        val c = ValuationDecisionEngine.decide(ValuationDecisionInput(1000.0, 200.0, 6, 0, 0.10, targetMarginRate = 0.70))
        assertEquals(700.0, a.maxBuyPrice, 0.01)
        assertEquals(500.0, b.maxBuyPrice, 0.01)
        assertEquals(300.0, c.maxBuyPrice, 0.01)
    }

    @Test fun dealIntelligenceFindsOpportunityStaleDuplicateMarginAndMovement() {
        val signals = ValuationDecisionEngine.analyseDeals(
            listOf(
                SavedDealSignalInput("1", "apple a1932 2018", 200.0, 500.0, 575.0, 300.0, 45, 0.60),
                SavedDealSignalInput("2", "Apple A1932 2018", 290.0, 500.0, 505.0, 300.0, 2, 0.20),
                SavedDealSignalInput("3", "surface pro 8", 400.0, 600.0, 600.0, 420.0, 2, 0.25)
            )
        )
        assertTrue("1" in signals.underpricedOpportunityIds)
        assertTrue("1" in signals.staleValuationIds)
        assertTrue("1" in signals.unusuallyHighMarginIds)
        assertTrue(signals.duplicateGroups.any { it == setOf("1", "2") })
        assertTrue("1" in signals.meaningfulMarketMovementIds)
    }
}
