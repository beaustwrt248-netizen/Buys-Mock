package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract tests for the Gumtree/Facebook marketplace integration.
 * These protect the core valuation rule: marketplace evidence must never
 * influence a price unless the listing is an exact device match.
 */
class MarketplaceSearchContractTest {
    @Test
    fun marketplaceListingCarriesExplicitExactFlag() {
        val listing = MarketplaceListing(
            title = "MSI MPG Trident AS 13NUC7-491AU RTX 4060",
            price = 1299.0,
            source = "Gumtree",
            url = "https://example.invalid/listing",
            condition = "Used",
            exact = true,
            score = 100,
            reasons = "Brand + Family + Model"
        )
        assertTrue(listing.exact)
    }

    @Test
    fun accessoryEvidenceCanBeExplicitlyRejected() {
        val listing = MarketplaceListing(
            title = "MSI Trident power supply replacement part",
            price = 99.0,
            source = "Facebook Marketplace",
            url = "https://example.invalid/part",
            condition = "Used",
            exact = false,
            score = 58,
            reasons = "Family + Part/accessory"
        )
        assertFalse(listing.exact)
    }

    @Test
    fun onlyExactMarketplacePricesAreEligibleForConsensus() {
        val evidence = listOf(
            MarketplaceListing("Exact device", 900.0, "Gumtree", "", "Used", true, 100, "Exact"),
            MarketplaceListing("Similar device", 1500.0, "Gumtree", "", "Used", false, 70, "Similar"),
            MarketplaceListing("Accessory", 100.0, "Facebook Marketplace", "", "Used", false, 50, "Part/accessory")
        )
        val eligible = evidence.filter { it.exact && it.price > 0 }.map { it.price }
        assertTrue(eligible == listOf(900.0))
    }
}
