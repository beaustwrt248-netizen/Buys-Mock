package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MarketplaceConsensusTest {
    private fun item(source: String, price: Double, exact: Boolean = true) = MarketplaceListing(
        title = "Lenovo ThinkPad X1 Carbon Gen 11",
        price = price,
        source = source,
        url = "https://example.invalid/item",
        condition = "Used",
        exact = exact,
        score = if (exact) 100 else 50,
        reasons = if (exact) "Exact model" else "Model mismatch"
    )

    @Test fun `non exact listings never enter consensus`() {
        val result = MarketplaceConsensusEngine.calculate(listOf(item("Gumtree", 800.0, false)))
        assertEquals(0.0, result.value, 0.01)
    }

    @Test fun `cross source median resists extreme marketplace source`() {
        val result = MarketplaceConsensusEngine.calculate(listOf(
            item("Gumtree", 800.0), item("Gumtree", 820.0),
            item("Facebook Marketplace", 790.0), item("Facebook Marketplace", 810.0),
            item("Bad Source", 2500.0)
        ))
        assertEquals(805.0, result.value, 0.01)
        assertFalse(result.sources.any { it.source == "Bad Source" })
    }
}