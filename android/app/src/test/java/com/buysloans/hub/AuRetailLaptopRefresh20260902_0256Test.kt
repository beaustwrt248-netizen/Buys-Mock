package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuRetailLaptopRefresh20260902_0256Test {
    @Test
    fun `refresh records verified configurations and availability`() {
        // Keep retail observations exact and status-aware; valuation/confidence rules remain elsewhere.
        val listings = AuRetailLaptopRefresh20260902_0256.listings
        assertEquals(4, listings.size)

        assertTrue(listings.any {
            it.retailer == "ASUS Australia" &&
                it.modelSku == "CX1405CTA-S38128" &&
                it.processor == "Intel Core 3 N355" &&
                it.ram == "8GB" && it.storage == "128GB" &&
                it.priceAud == 489.0 && it.status == RetailListingStatus.CURRENT
        })
        assertTrue(listings.any {
            it.retailer == "Microsoft Australia" &&
                it.familyModel == "Surface Laptop 15-inch (8th Edition)" &&
                it.processor == "Snapdragon X2 Elite 12 Core" &&
                it.ram == "16GB" && it.storage == "512GB" && it.priceAud == 2999.0
        })
        assertTrue(listings.any {
            it.retailer == "Harvey Norman" && it.familyModel == "OmniBook Ultra 14" &&
                it.processor == "Intel Core Ultra 7 356H" && it.priceAud == 3699.0
        })
        assertTrue(listings.any {
            it.retailer == "Officeworks" && it.modelSku == "LE83K1001F" &&
                it.status == RetailListingStatus.UNAVAILABLE
        })
    }

    @Test
    fun `guided catalog supports newly verified processors without relaxing evidence rules`() {
        val asusCx14 = ChromebookSelectionCatalog.presets.first {
            it.brand == "ASUS" && it.model == "Chromebook CX14 (2026)"
        }
        assertTrue(asusCx14.processors.contains("Intel Core 3 N355"))

        val surface = LaptopSelectionCatalog.presets.first {
            it.brand == "Microsoft" && it.model == "Surface Laptop (2026)"
        }
        assertTrue(surface.processors.contains("Snapdragon X2 Plus"))
        assertTrue(surface.processors.contains("Snapdragon X2 Elite"))

        val omnibook = LaptopSelectionCatalog.presets.first {
            it.brand == "HP" && it.model == "OmniBook (2026)"
        }
        assertTrue(omnibook.processors.contains("Intel Core Ultra 7 356H"))

        val xps = LaptopSelectionCatalog.presets.first {
            it.brand == "Dell" && it.model == "XPS 13 (2026)"
        }
        assertTrue(xps.processors.contains("Intel Core 5"))
    }

    @Test
    fun `refresh does not duplicate existing retailer sku observations`() {
        val all = AuRetailLaptopCatalog.listings +
            AuRetailLaptopRefresh20260902.listings +
            AuRetailLaptopRefresh20260902_0256.listings

        val keys = all.map { listing ->
            listOf(
                listing.retailer.lowercase(),
                listing.brand.lowercase(),
                listing.familyModel.lowercase(),
                listing.modelSku.orEmpty().lowercase(),
                listing.processor.lowercase(),
                listing.ram.lowercase(),
                listing.storage.lowercase()
            ).joinToString("|")
        }
        assertEquals(keys.size, keys.distinct().size)
    }
}
