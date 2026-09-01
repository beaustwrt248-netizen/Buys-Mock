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

    // Price corrections remain keyed to the full verified retailer configuration.
    @Test
    fun refreshedLivePricesStayBoundToExactConfigurations() {
        val surface = AuRetailLaptopCatalog.listings.first {
            it.retailer == "Microsoft Australia" &&
                it.familyModel == "Surface Laptop 13.8-inch (8th Edition)" &&
                it.processor == "Snapdragon X2 Plus 10 Core" &&
                it.ram == "16GB" && it.storage == "512GB"
        }
        assertEquals(2799.0, surface.priceAud, 0.0)
        assertTrue(surface.sourceUrl.contains("microsoft.com/en-au"))

        val macBookPro = AuRetailLaptopCatalog.listings.first {
            it.retailer == "Apple Australia" &&
                it.familyModel == "MacBook Pro 14-inch (2026)" &&
                it.processor == "Apple M5" &&
                it.ram == "16GB" && it.storage == "1TB"
        }
        assertEquals(3199.0, macBookPro.priceAud, 0.0)
        assertTrue(macBookPro.sourceUrl.contains("apple.com/au/shop/buy-mac/macbook-pro"))
    }

    @Test
    fun guidedChromebookSupportIncludesVerified2026RetailFamilies() {
        val asus = LaptopSelectionCatalog.models("ASUS")
        assertTrue(asus.any { it.model == "Chromebook CX14 (2026)" && "Intel Processor N50" in it.processors })

        val lenovo = LaptopSelectionCatalog.models("Lenovo")
        assertTrue(lenovo.any { it.model == "Chromebook Plus 14 OLED (2026)" && "MediaTek Kompanio Ultra 910" in it.processors })
    }
}
