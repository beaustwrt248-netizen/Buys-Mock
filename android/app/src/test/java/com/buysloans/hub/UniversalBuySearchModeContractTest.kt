package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalBuySearchModeContractTest {
    @Test
    fun `unknown mode fails closed to quick search`() {
        assertEquals(UniversalBuySearchMode.QUICK_SEARCH, UniversalBuySearchMode.from("nonsense"))
        assertEquals(UniversalBuySearchMode.MANUAL_SEARCH, UniversalBuySearchMode.from("manual_search"))
        assertEquals(UniversalBuySearchMode.PRICE_CHECK, UniversalBuySearchMode.from("price_check"))
    }

    @Test
    fun `manual and price modes stay on shared search engine`() {
        assertTrue(UniversalBuySearchMode.MANUAL_SEARCH.usesUniversalEngine)
        assertTrue(UniversalBuySearchMode.PRICE_CHECK.usesUniversalEngine)
        assertTrue(UniversalBuySearchMode.QUICK_SEARCH.usesUniversalEngine)
        assertFalse(UniversalBuySearchMode.AI_SCAN.usesUniversalEngine)
    }

    @Test
    fun `modes have distinct staff intent`() {
        assertEquals("Quick Search", UniversalBuySearchMode.QUICK_SEARCH.title)
        assertEquals("Manual Search", UniversalBuySearchMode.MANUAL_SEARCH.title)
        assertEquals("Price Check", UniversalBuySearchMode.PRICE_CHECK.title)
        assertEquals("AI Device Scan", UniversalBuySearchMode.AI_SCAN.title)
    }
}