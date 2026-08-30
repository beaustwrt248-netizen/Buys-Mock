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

    @Test fun `macbook brand is inferred and SSD spec is not an accessory`() {
        val (exact, _, reasons) = classifyMarketplace(
            "MacBook Air 2019 i5 256GB",
            "Apple MacBook Air 2019 13-inch Intel Core i5 8GB 256GB SSD"
        )
        assertTrue(reasons, exact)
    }

    @Test fun `generic macbook cannot mix air and pro into exact value`() {
        val (exact, _, reasons) = classifyMarketplace(
            "MacBook i5 2019 256GB",
            "Apple MacBook Pro 2019 13-inch Intel Core i5 8GB 256GB SSD"
        )
        assertFalse(exact)
        assertTrue(reasons.contains("Specify MacBook Air or Pro"))
    }

    @Test fun `wrong macbook year is not exact`() {
        val (exact, _, reasons) = classifyMarketplace(
            "MacBook Air 2019 i5 256GB",
            "Apple MacBook Air 2020 Intel Core i5 8GB 256GB SSD"
        )
        assertFalse(exact)
        assertTrue(reasons.contains("Year mismatch"))
    }

    @Test fun `wrong macbook storage is not exact`() {
        val (exact, _, reasons) = classifyMarketplace(
            "MacBook Air 2019 i5 256GB",
            "Apple MacBook Air 2019 Intel Core i5 8GB 512GB SSD"
        )
        assertFalse(exact)
        assertTrue(reasons.contains("Storage mismatch"))
    }

    @Test fun `wrong macbook cpu tier is not exact`() {
        val (exact, _, reasons) = classifyMarketplace(
            "MacBook Air 2019 i5 256GB",
            "Apple MacBook Air 2019 Intel Core i7 8GB 256GB SSD"
        )
        assertFalse(exact)
        assertTrue(reasons.contains("CPU mismatch"))
    }

    @Test fun `wrong macbook ram is not exact`() {
        val (exact, _, reasons) = classifyMarketplace(
            "Apple MacBook Pro 14-inch 2025 Apple M5 32GB 512GB",
            "Apple MacBook Pro 14-inch 2025 M5 16GB RAM 512GB SSD"
        )
        assertFalse(exact)
        assertTrue(reasons.contains("RAM mismatch"))
    }

    @Test fun `matching M5 configuration can qualify as exact`() {
        val (exact, _, reasons) = classifyMarketplace(
            "Apple MacBook Pro 14-inch 2025 Apple M5 32GB 512GB",
            "Apple MacBook Pro 14-inch 2025 M5 32GB RAM 512GB SSD"
        )
        assertTrue(reasons, exact)
    }
}
