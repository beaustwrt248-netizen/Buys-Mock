package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepairRiskDecisionAdapterTest {
    @Test
    fun evidenceAllowanceReducesMaxBuyByReservedRisk() {
        val input = ValuationDecisionInput(
            marketValue = 1000.0,
            sellerAsk = 300.0,
            targetMarginPct = 0.30,
            sourceCount = 5,
            modelConfidence = 1.0
        )
        val assessment = RepairRiskEvidence.assess(
            listOf(RepairRiskItem("battery", 200.0, 0.50, 1.0))
        )
        val baseline = ValuationDecisionEngine.evaluate(input)
        val result = RepairRiskDecisionAdapter.evaluate(input, assessment)

        assertEquals(100.0, assessment.suggestedAllowance, 0.0001)
        assertEquals(baseline.maxBuyPrice - 70.0, result.maxBuyPrice, 0.0001)
        assertTrue(result.reasons.any { it.contains("Repair-risk allowance", ignoreCase = true) })
    }

    @Test
    fun largerManualAllowanceIsNeverReducedByEvidence() {
        val input = ValuationDecisionInput(
            marketValue = 1000.0,
            sellerAsk = 300.0,
            targetMarginPct = 0.30,
            repairRiskAllowance = 180.0,
            sourceCount = 5,
            modelConfidence = 1.0
        )
        val assessment = RepairRiskEvidence.assess(
            listOf(RepairRiskItem("battery", 200.0, 0.25, 1.0))
        )
        val direct = ValuationDecisionEngine.evaluate(input)
        val result = RepairRiskDecisionAdapter.evaluate(input, assessment)

        assertEquals(direct.maxBuyPrice, result.maxBuyPrice, 0.0001)
        assertEquals(direct.expectedProfitAtAsk, result.expectedProfitAtAsk, 0.0001)
    }

    @Test
    fun uncertainRepairEvidenceDowngradesHealthyBuyToCaution() {
        val input = ValuationDecisionInput(
            marketValue = 1000.0,
            sellerAsk = 250.0,
            targetMarginPct = 0.30,
            sourceCount = 5,
            modelConfidence = 1.0
        )
        val assessment = RepairRiskEvidence.assess(
            listOf(RepairRiskItem("intermittent port", 100.0, 0.20, 0.30))
        )
        val result = RepairRiskDecisionAdapter.evaluate(input, assessment)

        assertTrue(assessment.requiresCaution)
        assertEquals(ValuationDecision.CAUTION, result.decision)
        assertTrue(result.reasons.any { it.contains("confidence", ignoreCase = true) })
    }

    @Test
    fun repairCautionNeverUpgradesPassOutcome() {
        val input = ValuationDecisionInput(
            marketValue = 500.0,
            sellerAsk = 600.0,
            targetMarginPct = 0.30,
            sourceCount = 5,
            modelConfidence = 1.0
        )
        val assessment = RepairRiskEvidence.assess(
            listOf(RepairRiskItem("logic board", 400.0, 0.80, 0.50))
        )
        val result = RepairRiskDecisionAdapter.evaluate(input, assessment)

        assertEquals(ValuationDecision.PASS, result.decision)
    }
}
