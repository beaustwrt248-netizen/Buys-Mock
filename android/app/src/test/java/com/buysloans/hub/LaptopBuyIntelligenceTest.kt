package com.buysloans.hub

import org.junit.Assert.*
import org.junit.Test

class LaptopBuyIntelligenceTest {
    private val target = LaptopFingerprint("Apple", "MacBook Pro", "2021", "A2442", 14.2, "M1 Pro", null, 16, 512)

    @Test fun rejectsWrongGenerationAndParts() {
        val wrong = LaptopComparable("MacBook Pro", target.copy(generation = "2020", modelCode = "A2338"), 900.0, true)
        val part = LaptopComparable("replacement screen for MacBook Pro A2442", target, 250.0, true, wholeDevice = false)
        assertFalse(LaptopBuyIntelligence.classify(target, wrong).accepted)
        assertFalse(LaptopBuyIntelligence.classify(target, part).accepted)
    }

    @Test fun soldExactComparablesDriveBuyZone() {
        val comps = listOf(
            LaptopComparable("a", target, 1100.0, true, 4, "ebay", "s1"),
            LaptopComparable("b", target, 1140.0, true, 12, "ebay", "s2"),
            LaptopComparable("c", target, 1080.0, true, 20, "other", "s3"),
            LaptopComparable("d", target, 1800.0, false, 1, "active", "s4"),
            LaptopComparable("e", target, 120.0, true, 1, "other", "s5", wholeDevice = false)
        )
        val result = LaptopBuyIntelligence.evaluate(target, comps, LaptopBuyInputs(expectedCosts = 60.0, riskReserve = 30.0, minimumProfit = 200.0))
        assertTrue(result.marketValue in 1050.0..1250.0)
        assertTrue(result.hardMaximum <= (result.marketValue * 0.70).roundToInt())
        assertEquals("BUY / NEGOTIATE", result.decision)
        assertTrue(result.confidence >= 70)
    }

    @Test fun insufficientEvidenceRequiresManualReview() {
        val result = LaptopBuyIntelligence.evaluate(target, listOf(LaptopComparable("one", target, 1000.0, true)))
        assertEquals("MANUAL REVIEW", result.decision)
        assertEquals(0, result.hardMaximum)
    }
}
