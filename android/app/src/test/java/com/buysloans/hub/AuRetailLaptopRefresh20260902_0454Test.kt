package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuRetailLaptopRefresh20260902_0454Test {
    private fun key(it: AuRetailLaptopListing) =
        listOf(it.retailer, it.brand, it.familyModel, it.modelSku.orEmpty(), it.processor, it.ram, it.storage)
            .joinToString("|") { part -> part.lowercase() }

    @Test
    fun refresh_hasUniqueVerifiedConfigurationsAndStatus() {
        val listings = AuRetailLaptopRefresh20260902_0454.listings
        assertEquals(7, listings.size)
        assertEquals(listings.size, listings.map(::key).distinct().size)
        assertTrue(listings.all { it.checkedAtIso == AuRetailLaptopRefresh20260902_0454.CHECKED_AT })
        assertTrue(listings.all { it.priceAud > 0.0 && it.sourceUrl.startsWith("https://") })
        assertEquals(3, listings.count { it.status == RetailListingStatus.CLEARANCE })
        assertEquals(4, listings.count { it.status == RetailListingStatus.CURRENT })
    }

    @Test
    fun refresh_doesNotDuplicatePriorExactRetailerConfigurationKeys() {
        val previous = (
            AuRetailLaptopCatalog.listings +
                AuRetailLaptopRefresh20260902.listings +
                AuRetailLaptopRefresh20260902_0256.listings +
                AuRetailLaptopRefresh20260902_0400.listings
            ).map(::key).toSet()

        assertTrue(AuRetailLaptopRefresh20260902_0454.listings.none { key(it) in previous })
    }

    @Test
    fun guidedFamiliesAlreadyCoverNewChromebookAndAcerObservations() {
        val lenovoChrome = ChromebookSelectionCatalog.presets.filter { it.brand == "Lenovo" && it.year == 2026 }
        assertTrue(lenovoChrome.any { it.model.startsWith("IdeaPad Slim 3 Chromebook") && "MediaTek Kompanio 540" in it.processors })
        assertTrue(lenovoChrome.any { it.model.startsWith("IdeaPad Flex 3 Chromebook") && "Intel Processor N100" in it.processors })

        val asusChrome = ChromebookSelectionCatalog.presets.first {
            it.brand == "ASUS" && it.model == "Chromebook CX14 (2026)"
        }
        assertTrue("Intel Processor N50" in asusChrome.processors)

        val acer = LaptopSelectionCatalog.models("Acer")
        assertTrue(acer.any { it.model == "Aspire (2026)" })
        assertTrue(acer.any { it.model == "Swift (2026)" })
        assertTrue(acer.any { it.model == "Nitro (2026)" })
    }
}
