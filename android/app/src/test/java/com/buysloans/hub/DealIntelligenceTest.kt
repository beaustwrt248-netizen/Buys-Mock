package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DealIntelligenceTest {
    @Test
    fun flagsUnderpricedStaleHighMarginAndMovementSignals() {
        val deal = SavedDealSignalInput(
            id = "deal-1",
            normalizedIdentity = "apple macbook air a1932",
            askingPrice = 300.0,
            originalMarketValue = 500.0,
            currentMarketValue = 560.0,
            maxBuyPrice = 400.0,
            createdAgeDays = 45,
            expectedMarginRate = 0.60
        )
        val result = DealIntelligenceEngine.analyse(listOf(deal))
        assertTrue("deal-1" in result.underpricedOpportunityIds)
        assertTrue("deal-1" in result.staleValuationIds)
        assertTrue("deal-1" in result.unusuallyHighMarginIds)
        assertTrue("deal-1" in result.meaningfulMarketMovementIds)
    }

    @Test
    fun groupsDuplicateNormalizedIdentitiesWithoutGuessingBlankOnes() {
        val deals = listOf(
            SavedDealSignalInput("a", " Dell XPS 13 9310 ", null, null, null, null, 0, null),
            SavedDealSignalInput("b", "dell xps 13 9310", null, null, null, null, 0, null),
            SavedDealSignalInput("c", "", null, null, null, null, 0, null),
            SavedDealSignalInput("d", "", null, null, null, null, 0, null)
        )
        val groups = DealIntelligenceEngine.analyse(deals).duplicateGroups
        assertEquals(1, groups.size)
        assertEquals(setOf("a", "b"), groups.single())
    }

    @Test
    fun doesNotFlagInvalidOrInsufficientFinancialSignals() {
        val deal = SavedDealSignalInput(
            id = "safe",
            normalizedIdentity = "item",
            askingPrice = 50.0,
            originalMarketValue = 0.0,
            currentMarketValue = 90.0,
            maxBuyPrice = 0.0,
            createdAgeDays = 5,
            expectedMarginRate = 0.10
        )
        val result = DealIntelligenceEngine.analyse(listOf(deal))
        assertFalse("safe" in result.underpricedOpportunityIds)
        assertFalse("safe" in result.staleValuationIds)
        assertFalse("safe" in result.unusuallyHighMarginIds)
        assertFalse("safe" in result.meaningfulMarketMovementIds)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidThresholdConfiguration() {
        DealIntelligenceEngine.analyse(emptyList(), highMarginThreshold = 1.2)
    }
}
