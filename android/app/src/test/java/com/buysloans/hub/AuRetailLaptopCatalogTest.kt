package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuRetailLaptopCatalogTest {
    @Test
    fun retailSnapshotHasUniqueRetailerModelSkuKeys() {
        val keys = AuRetailLaptopCatalog.listings.map {
            listOf(it.retailer, it.brand, it.familyModel, it.modelSku.orEmpty(), it.processor, it.ram, it.storage).joinToString("|")
        }
        assertEquals(keys.distinct().size, keys.size)
    }

    @Test
    fun unavailableListingsAreNotTreatedAsCurrent() {
        val unavailable = AuRetailLaptopCatalog.listings.first { it.status == RetailListingStatus.UNAVAILABLE }
        assertFalse(AuRetailLaptopCatalog.currentListings.contains(unavailable))
    }

    @Test
    fun currentRetailSnapshotKeepsIdentityFieldsAndSources() {
        assertTrue(AuRetailLaptopCatalog.currentListings.isNotEmpty())
        AuRetailLaptopCatalog.currentListings.forEach {
            assertTrue(it.retailer.isNotBlank())
            assertTrue(it.brand.isNotBlank())
            assertTrue(it.familyModel.isNotBlank())
            assertTrue(it.processor.isNotBlank())
            assertTrue(it.ram.isNotBlank())
            assertTrue(it.storage.isNotBlank())
            assertTrue(it.operatingSystem.isNotBlank())
            assertTrue(it.priceAud > 0.0)
            assertTrue(it.sourceUrl.startsWith("https://"))
            assertEquals(AuRetailLaptopCatalog.CHECKED_AT, it.checkedAtIso)
        }
    }

    @Test
    fun guidedChromebookSupportIncludesVerified2026RetailFamilies() {
        val asus = LaptopSelectionCatalog.models("ASUS")
        assertTrue(asus.any { it.model == "Chromebook CX14 (2026)" && "Intel Processor N50" in it.processors })

        val lenovo = LaptopSelectionCatalog.models("Lenovo")
        assertTrue(lenovo.any { it.model == "Chromebook Plus 14 OLED (2026)" && "MediaTek Kompanio Ultra 910" in it.processors })
    }
}
