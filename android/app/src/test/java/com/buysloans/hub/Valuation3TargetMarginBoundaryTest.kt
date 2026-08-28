package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Valuation3TargetMarginBoundaryTest {
    @Test fun healthyEvidenceNeverLowersConfiguredMargin() {
        listOf(0.0, 0.20, 0.30, 0.50, 0.90).forEach { configured ->
            val assessment = TargetMarginPolicy.assess(TargetMarginEvidence(configuredMarginPct = configured))
            assertTrue(assessment.suggestedMarginPct >= assessment.configuredMarginPct)
            assertEquals(configured.coerceIn(0.0, 0.95), assessment.suggestedMarginPct, 0.0001)
        }
    }

    @Test fun riskPremiumCannotPushSuggestedMarginAboveEngineCap() {
        val assessment = TargetMarginPolicy.assess(TargetMarginEvidence(
            configuredMarginPct = 0.94,
            confidence = 0.0,
            repairRiskScore = 1.0,
            staleEvidence = true,
            identityResolved = false
        ))
        assertEquals(0.95, assessment.suggestedMarginPct, 0.0001)
    }
}
