package com.buysloans.hub

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConsolePricingCatalogTest {
    private val devices = listOf(
        LiveDeviceCatalogueRow(1, "console", "Sony", "PlayStation 5 Pro", "CFI-7002", listOf("2TB"), family = "PlayStation 5"),
        LiveDeviceCatalogueRow(2, "console", "Microsoft", "Xbox Series X", "1882", listOf("1TB"), family = "Xbox Series"),
        LiveDeviceCatalogueRow(3, "console", "Nintendo", "Nintendo Switch 2", "BEE-001", listOf("256GB"), family = "Switch"),
        LiveDeviceCatalogueRow(4, "console", "Nintendo", "Nintendo DS Lite", "USG-001", emptyList(), family = "Nintendo DS"),
        LiveDeviceCatalogueRow(5, "mobile_phone", "Samsung", "Galaxy S24", "SM-S921B", listOf("128GB")),
    )
    private val prices = listOf(
        LiveDevicePrice(1, "Sony", "PlayStation 5 Pro", "CFI-7002", "2TB", 1199.0, true),
        LiveDevicePrice(2, "Microsoft", "Xbox Series X", "1882", "1TB", 549.0, true),
        LiveDevicePrice(3, "Nintendo", "Nintendo Switch 2", "BEE-001", "256GB", 599.0, true),
    )

    @Before fun setUp() = LiveDevicePricing.replaceSnapshotsForTesting(prices, devices)
    @After fun tearDown() = LiveDevicePricing.replaceSnapshotsForTesting(emptyList(), emptyList())

    @Test fun consoleCatalogueComesOnlyFromLiveConsoleRows() {
        assertEquals(4, ConsolePricingCatalog.catalogue.size)
        assertTrue(ConsolePricingCatalog.catalogue.none { it.name.contains("Galaxy S24") })
    }

    @Test fun liveAuthoritativePricesDriveConsolePricing() {
        assertEquals(1199.0, ConsolePricingCatalog.search("PlayStation 5 Pro").single().priceSheetValue!!, 0.0)
        assertEquals(549.0, ConsolePricingCatalog.search("Series X").single().priceSheetValue!!, 0.0)
        assertEquals(599.0, ConsolePricingCatalog.search("Switch 2").single().priceSheetValue!!, 0.0)
    }

    @Test fun unpricedLiveConsolesNeverInventABuyPrice() {
        val ds = ConsolePricingCatalog.search("DS Lite").single()
        assertNull(ds.priceSheetValue)
        assertNull(ConsolePricingCatalog.buyPrice(ds, "B"))
    }

    @Test fun familiesAndSeriesAreDerivedFromCanonicalFields() {
        assertTrue("Sony" in ConsolePricingCatalog.families())
        assertTrue("PlayStation 5" in ConsolePricingCatalog.series("Sony"))
        assertTrue("Xbox Series" in ConsolePricingCatalog.series("Microsoft"))
    }

    @Test fun catalogueHasNoDuplicateVisibleRows() {
        val names = ConsolePricingCatalog.catalogue.map { "${it.family}|${it.series}|${it.name}".lowercase() }
        assertEquals(names.size, names.distinct().size)
    }
}
