package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TargetMarginPolicyTest {
    @Test fun healthyEvidencePreservesConfiguredMargin() {
        val result = TargetMarginPolicy.assess(TargetMarginEvidence(configuredMarginPct = 0.30))
        assertEquals(0.30, result.suggestedMarginPct, 0.0001)
    }

    @Test fun weakEvidenceCanOnlyIncreaseMarginReserve() {
        val result = TargetMarginPolicy.assess(TargetMarginEvidence(
            configuredMarginPct = 0.30,
            confidence = 0.40,
            repairRiskScore = 0.80,
            staleEvidence = true,
            identityResolved = false
        ))
        assertEquals(0.45, result.suggestedMarginPct, 0.0001)
        assertTrue(result.suggestedMarginPct >= result.configuredMarginPct)
    }

    @Test fun riskPremiumIsBounded() {
        val result = TargetMarginPolicy.assess(TargetMarginEvidence(
            configuredMarginPct = 0.90,
            confidence = 0.0,
            repairRiskScore = 1.0,
            staleEvidence = true,
            identityResolved = false
        ))
        assertEquals(0.95, result.suggestedMarginPct, 0.0001)
    }
}
