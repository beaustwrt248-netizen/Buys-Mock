package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComparableSalesQualityTest {
    @Test fun strongExactSoldComparablesScoreHighly() {
        val assessment = ComparableSalesQuality.assess(listOf(1000.0, 980.0, 1020.0, 990.0, 1010.0).mapIndexed { index, price -> ComparableSaleEvidence(price, 5 + index, true, true, true, true, true) })
        assertTrue(assessment.quality >= 0.90); assertEquals(5, assessment.usableCount); assertEquals(5, assessment.exactModelCount); assertEquals(1000.0, assessment.medianPrice, 0.001); assertFalse(assessment.staleEvidence)
    }
    @Test fun sparseWeakEvidenceScoresLowAndExplainsWhy() {
        val assessment = ComparableSalesQuality.assess(listOf(ComparableSaleEvidence(800.0, 80, false, true, false, false, false)))
        assertTrue(assessment.quality < 0.30); assertTrue(assessment.reasons.contains("Limited comparable sample")); assertTrue(assessment.reasons.contains("No exact-model sold comparables")); assertTrue(assessment.reasons.contains("Some evidence is not verified sold data"))
    }
    @Test fun oldOnlyEvidenceIsMarkedStale() {
        val assessment = ComparableSalesQuality.assess(listOf(700.0, 710.0, 690.0).map { ComparableSaleEvidence(it, 70, true, true, true, true, true) })
        assertTrue(assessment.staleEvidence); assertTrue(assessment.reasons.contains("Comparable evidence is stale"))
    }
    @Test fun emptyEvidenceReturnsSafeZeroAssessment() {
        val assessment = ComparableSalesQuality.assess(emptyList()); assertEquals(0.0, assessment.quality, 0.001); assertEquals(0, assessment.usableCount); assertTrue(assessment.staleEvidence); assertEquals(listOf("No usable sold comparables"), assessment.reasons)
    }
}
