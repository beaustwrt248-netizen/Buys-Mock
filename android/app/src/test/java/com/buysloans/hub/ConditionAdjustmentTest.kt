package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConditionAdjustmentTest {
    @Test
    fun excellentConditionPreservesResale() {
        val result = ConditionAdjustment.assess(ConditionEvidence(ObservedCondition.EXCELLENT))
        assertEquals(1.0, result.multiplier, 0.0001)
    }

    @Test
    fun faultsAndMissingAccessoriesReduceConditionMultiplier() {
        val result = ConditionAdjustment.assess(
            ConditionEvidence(
                observedCondition = ObservedCondition.GOOD,
                majorFaultCount = 2,
                missingAccessoryCount = 1
            )
        )
        assertEquals(0.65, result.multiplier, 0.0001)
        assertTrue(result.reasons.any { it.contains("Major-fault") })
        assertTrue(result.reasons.any { it.contains("Missing-accessory") })
    }

    @Test
    fun conditionEvidenceChangesOnlyConditionDrivenPriceMath() {
        val input = ValuationDecisionInput(
            marketValue = 1000.0,
            sellerAsk = 400.0,
            targetMarginPct = 0.30,
            platformFeesPct = 0.10,
            freightCost = 25.0,
            repairRiskAllowance = 50.0,
            conditionMultiplier = 1.0,
            comparableQuality = 0.90,
            modelConfidence = 0.95,
            sourceCount = 5
        )

        val excellent = ConditionAdjustment.evaluate(
            input,
            ConditionEvidence(ObservedCondition.EXCELLENT)
        )
        val fair = ConditionAdjustment.evaluate(
            input,
            ConditionEvidence(ObservedCondition.FAIR)
        )

        assertEquals(1000.0, excellent.adjustedResale, 0.0001)
        assertEquals(750.0, fair.adjustedResale, 0.0001)
        assertTrue(fair.maxBuyPrice < excellent.maxBuyPrice)
        assertEquals(excellent.confidence, fair.confidence, 0.0001)
    }

    @Test
    fun penaltiesAreBoundedAndCannotMakeMultiplierNegative() {
        val result = ConditionAdjustment.assess(
            ConditionEvidence(
                observedCondition = ObservedCondition.PARTS,
                majorFaultCount = 99,
                missingAccessoryCount = 99
            )
        )
        assertEquals(0.20, result.multiplier, 0.0001)
    }
}
