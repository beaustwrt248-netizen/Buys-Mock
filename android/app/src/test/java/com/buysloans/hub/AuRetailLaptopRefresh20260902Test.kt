package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuRetailLaptopRefresh20260902Test {
    private fun key(it: AuRetailLaptopListing) =
        listOf(it.retailer, it.brand, it.familyModel, it.modelSku.orEmpty(), it.processor, it.ram, it.storage).joinToString("|")

    @Test
    fun refreshHasUniqueVerifiedConfigurations() {
        val keys = AuRetailLaptopRefresh20260902.listings.map(::key)
        assertEquals(keys.distinct().size, keys.size)
        AuRetailLaptopRefresh20260902.listings.forEach {
            assertEquals(AuRetailLaptopRefresh20260902.CHECKED_AT, it.checkedAtIso)
            assertEquals(RetailListingStatus.CURRENT, it.status)
            assertTrue(it.priceAud > 0.0)
            assertTrue(it.sourceUrl.startsWith("https://"))
        }
    }

    @Test
    fun refreshDoesNotDuplicatePriorSnapshotConfigurations() {
        val previous = AuRetailLaptopCatalog.listings.map(::key).toSet()
        assertTrue(AuRetailLaptopRefresh20260902.listings.none { key(it) in previous })
    }

    @Test
    fun refreshedFamiliesRemainGuidedWithoutWeakeningMatching() {
        val dell = LaptopSelectionCatalog.models("Dell")
        assertTrue(dell.any { it.model == "XPS 13 (2026)" })

        val microsoft = LaptopSelectionCatalog.models("Microsoft")
        assertTrue(microsoft.any { it.model == "Surface Laptop (2026)" })

        val apple = LaptopSelectionCatalog.models("Apple")
        assertTrue(apple.any { it.model == "MacBook Neo 13-inch (2026)" && "Apple A18 Pro" in it.processors })
    }
}
