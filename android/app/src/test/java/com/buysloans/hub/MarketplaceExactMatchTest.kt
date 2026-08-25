package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketplaceExactMatchTest {
    @Test fun `shortened legitimate model title can still be exact`() {
        val (exact, _, _) = classifyMarketplace(
            "Lenovo ThinkPad T14 Gen 3 21AH",
            "Lenovo ThinkPad T14 Gen 3 laptop"
        )
        assertTrue(exact)
    }

    @Test fun `different generation is rejected`() {
        val (exact, _, reasons) = classifyMarketplace(
            "Lenovo ThinkPad T14 Gen 3",
            "Lenovo ThinkPad T14 Gen 2 laptop"
        )
        assertFalse(exact)
        assertTrue(reasons.contains("Generation mismatch"))
    }

    @Test fun `accessory is never exact`() {
        val (exact, _, reasons) = classifyMarketplace(
            "Lenovo ThinkPad T14 Gen 3",
            "Lenovo ThinkPad T14 Gen 3 replacement screen"
        )
        assertFalse(exact)
        assertTrue(reasons.contains("Part/accessory"))
    }

    @Test fun `apple silicon identity can qualify`() {
        val (exact, _, _) = classifyMarketplace(
            "Apple MacBook Pro M2",
            "Apple MacBook Pro M2 13-inch"
        )
        assertTrue(exact)
    }
}