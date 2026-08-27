package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepairRiskEvidenceTest {
    @Test
    fun emptyEvidenceProducesNoAllowance() {
        val result = RepairRiskEvidence.assess(emptyList())
        assertEquals(0.0, result.suggestedAllowance, 0.0001)
        assertEquals(0.0, result.riskScore, 0.0001)
        assertFalse(result.requiresCaution)
    }

    @Test
    fun allowanceUsesExpectedRepairCostWithoutDiscountingForConfidence() {
        val result = RepairRiskEvidence.assess(
            listOf(
                RepairRiskItem("battery", 200.0, 0.5, 1.0),
                RepairRiskItem("display", 400.0, 0.25, 0.8)
            )
        )
        assertEquals(200.0, result.suggestedAllowance, 0.0001)
        assertFalse(result.requiresCaution)
    }

    @Test
    fun highLikelihoodRequiresCaution() {
        val result = RepairRiskEvidence.assess(
            listOf(RepairRiskItem("logic board", 500.0, 0.8, 0.9))
        )
        assertEquals(400.0, result.suggestedAllowance, 0.0001)
        assertTrue(result.requiresCaution)
        assertTrue(result.reasons.any { it.contains("likelihood", ignoreCase = true) })
    }

    @Test
    fun weakDiagnosticConfidenceRequiresCautionWithoutReducingAllowance() {
        val result = RepairRiskEvidence.assess(
            listOf(RepairRiskItem("intermittent port", 300.0, 0.4, 0.3))
        )
        assertEquals(120.0, result.suggestedAllowance, 0.0001)
        assertTrue(result.requiresCaution)
        assertTrue(result.reasons.any { it.contains("confidence", ignoreCase = true) })
    }

    @Test
    fun invalidInputsAreBounded() {
        val result = RepairRiskEvidence.assess(
            listOf(RepairRiskItem("unknown", -50.0, 5.0, -2.0))
        )
        assertEquals(0.0, result.suggestedAllowance, 0.0001)
        assertTrue(result.riskScore in 0.0..1.0)
    }
}
