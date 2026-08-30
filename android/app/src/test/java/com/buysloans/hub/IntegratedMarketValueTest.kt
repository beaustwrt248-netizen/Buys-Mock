package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class IntegratedMarketValueTest {
    private fun listing(source: String, price: Double, exact: Boolean = true) = MarketplaceListing(
        title = "Lenovo ThinkPad X1 Carbon Gen 11",
        price = price,
        source = source,
        url = "https://example.invalid/item",
        condition = "Used",
        exact = exact,
        score = if (exact) 100 else 50,
        reasons = if (exact) "Exact model" else "Model mismatch"
    )

    @Test fun `four source consensus blends verified used evidence`() {
        val marketplace = MarketplaceEvidence(
            gumtree = listOf(listing("Gumtree", 800.0), listing("Gumtree", 820.0)),
            facebook = listOf(listing("Facebook Marketplace", 790.0), listing("Facebook Marketplace", 810.0))
        )
        val result = IntegratedMarketValueEngine.calculate(
            ebayUsed = listOf(795.0, 805.0),
            googleNew = listOf(1200.0),
            marketplace = marketplace,
            newToUsedRate = 0.65
        )
        assertEquals(800.0, result.usedValue, 0.01)
        assertEquals(4, result.sources.size)
    }

    @Test fun `bad source cannot inflate integrated used value`() {
        val marketplace = MarketplaceEvidence(
            gumtree = listOf(listing("Gumtree", 800.0)),
            facebook = listOf(listing("Facebook Marketplace", 2500.0))
        )
        val result = IntegratedMarketValueEngine.calculate(
            ebayUsed = listOf(790.0, 810.0),
            googleNew = listOf(1200.0),
            marketplace = marketplace,
            newToUsedRate = 0.65
        )
        assertFalse(result.sources.any { it.source == "Facebook Marketplace" })
        assertEquals("LOW", result.confidence)
        assertEquals(0.0, result.usedValue, 0.01)
    }

    @Test fun `single cheap marketplace listing cannot drag strong ebay anchor`() {
        val marketplace = MarketplaceEvidence(
            gumtree = emptyList(),
            facebook = listOf(listing("Facebook Marketplace", 20.0))
        )
        val result = IntegratedMarketValueEngine.calculate(
            ebayUsed = listOf(360.0, 399.0, 420.0, 435.0, 443.0, 449.0, 465.0, 490.0),
            googleNew = listOf(260.0),
            marketplace = marketplace,
            newToUsedRate = 0.58
        )
        assertFalse(result.sources.any { it.source == "Facebook Marketplace" })
        assertFalse(result.sources.any { it.source == "Google Shopping AU" })
        assertEquals(439.0, result.usedValue, 5.0)
    }

    @Test fun `single exact source remains reference only`() {
        val marketplace = MarketplaceEvidence(
            gumtree = emptyList(),
            facebook = listOf(listing("Facebook Marketplace", 550.0))
        )
        val result = IntegratedMarketValueEngine.calculate(
            ebayUsed = emptyList(),
            googleNew = emptyList(),
            marketplace = marketplace,
            newToUsedRate = 0.65
        )
        assertEquals("LOW", result.confidence)
        assertEquals(0.0, result.usedValue, 0.01)
        assertEquals(1, result.sources.size)
    }

    @Test fun `single direct exact result is insufficient for protected value`() {
        val marketplace = MarketplaceEvidence(
            gumtree = emptyList(),
            facebook = emptyList()
        )
        val result = IntegratedMarketValueEngine.calculate(
            ebayUsed = listOf(800.0),
            googleNew = emptyList(),
            marketplace = marketplace,
            newToUsedRate = 0.65
        )
        assertEquals("LOW", result.confidence)
        assertEquals(0.0, result.usedValue, 0.01)
    }
}
