package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuRetailLaptopRefresh20260902_0400Test {
    @Test
    fun refresh_hasUniqueExactConfigurationsAndCurrentStatus() {
        val listings = AuRetailLaptopRefresh20260902_0400.listings
        assertEquals(6, listings.size)
        assertEquals(6, listings.map { "${it.retailer}|${it.modelSku}|${it.processor}|${it.ram}|${it.storage}" }.distinct().size)
        assertTrue(listings.all { it.checkedAtIso == AuRetailLaptopRefresh20260902_0400.CHECKED_AT })
        assertTrue(listings.all { it.status == RetailListingStatus.CURRENT })
    }

    @Test
    fun guidedChromebookSupportIncludesNewVerifiedAuConfigurations() {
        val cm14 = ChromebookSelectionCatalog.presets.first {
            it.brand == "ASUS" && it.model == "Chromebook CM14 (2026)"
        }
        assertTrue(cm14.processors.contains("MediaTek Kompanio 540"))

        val hpX360 = ChromebookSelectionCatalog.presets.first {
            it.brand == "HP" && it.model == "Chromebook x360 14 (2026)"
        }
        assertTrue(hpX360.processors.contains("Intel Processor N150"))
    }
}
