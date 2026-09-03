package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsolePricingCatalogTest {
    @Test
    fun suppliedPricingSheetIsFullyRepresented() {
        assertEquals(19, ConsolePricingCatalog.entries.size)
        assertEquals(149.0, ConsolePricingCatalog.entries.first { it.name == "Sony PS4 OG 500 GB" }.rrp, 0.01)
        assertEquals(189.0, ConsolePricingCatalog.entries.first { it.name == "Sony PS4 OG 1 TB" }.rrp, 0.01)
        assertEquals(229.0, ConsolePricingCatalog.entries.first { it.name == "Sony PS4 Slim 500 GB" }.rrp, 0.01)
        assertEquals(269.0, ConsolePricingCatalog.entries.first { it.name == "Sony PS4 Slim 1 TB" }.rrp, 0.01)
        assertEquals(649.0, ConsolePricingCatalog.entries.first { it.name == "Sony PS5 Digital" }.rrp, 0.01)
        assertEquals(699.0, ConsolePricingCatalog.entries.first { it.name == "Sony PS5 Disc" }.rrp, 0.01)
        assertEquals(799.0, ConsolePricingCatalog.entries.first { it.name == "Sony PS5 Digital Slim" }.rrp, 0.01)
        assertEquals(849.0, ConsolePricingCatalog.entries.first { it.name == "Sony PS5 Slim Disc" }.rrp, 0.01)
        assertEquals(1199.0, ConsolePricingCatalog.entries.first { it.name == "Sony PS5 Pro" }.rrp, 0.01)
        assertEquals(549.0, ConsolePricingCatalog.entries.first { it.name == "Xbox Series X" }.rrp, 0.01)
        assertEquals(599.0, ConsolePricingCatalog.entries.first { it.name == "Nintendo Switch 2" }.rrp, 0.01)
    }

    @Test
    fun consoleGradesAreRestrictedToABC() {
        assertEquals(listOf("A", "B", "C"), ConsolePricingCatalog.grades)
        assertTrue(ConsolePricingCatalog.entries.none { it.rrp <= 0.0 })
    }

    @Test
    fun ps5FamilyUsesRequestedNewestVariantOrder() {
        assertEquals(
            listOf("Sony PS5 Pro", "Sony PS5 Slim Disc", "Sony PS5 Slim Digital", "Sony PS5 Disc", "Sony PS5 Digital"),
            ConsolePricingCatalog.devices("PlayStation", "PS5").map { it.name }
        )
    }

    @Test
    fun existingPriceSheetValuesRemainAuthoritativeAcrossCanonicalNames() {
        assertEquals(799.0, ConsolePricingCatalog.search("PS5 Slim Digital").single().priceSheetValue!!, 0.0)
        assertEquals(549.0, ConsolePricingCatalog.search("Series X 1 TB").first { "All-Digital" !in it.name }.priceSheetValue!!, 0.0)
        assertEquals(329.0, ConsolePricingCatalog.search("Series S 512 GB").single().priceSheetValue!!, 0.0)
    }

    @Test
    fun newlyAddedRetroModelsRemainUnpricedAndCannotCalculateBuyPrice() {
        val gameBoy = ConsolePricingCatalog.search("Game Boy Color").single()
        assertNull(gameBoy.priceSheetValue)
        assertNull(ConsolePricingCatalog.buyPrice(gameBoy, "A"))
        val ds = ConsolePricingCatalog.search("Nintendo DS Lite").single()
        assertNull(ds.priceSheetValue)
        assertNull(ConsolePricingCatalog.buyPrice(ds, "B"))
    }

    @Test
    fun searchCoversSeriesAndFamilies() {
        assertTrue(ConsolePricingCatalog.search("PS5 Slim").size >= 2)
        assertTrue(ConsolePricingCatalog.search("Nintendo DS").any { it.name == "Nintendo DSi XL" })
        assertTrue(ConsolePricingCatalog.search("Game Boy").any { it.name == "Game Boy Advance SP" })
        assertTrue(ConsolePricingCatalog.search("Xbox Series X").any { it.name == "Xbox Series X 2 TB" })
    }

    @Test
    fun familiesAndSeriesStayInExplicitNewestFirstOrder() {
        val playStationSeries = ConsolePricingCatalog.series("PlayStation")
        assertTrue(playStationSeries.indexOf("PS5") < playStationSeries.indexOf("PS4"))
        assertTrue(playStationSeries.indexOf("PS4") < playStationSeries.indexOf("PS3"))
        val nintendoSeries = ConsolePricingCatalog.series("Nintendo")
        assertTrue(nintendoSeries.indexOf("Switch 2") < nintendoSeries.indexOf("Switch"))
        assertTrue(nintendoSeries.indexOf("Switch") < nintendoSeries.indexOf("Wii U"))
    }

    @Test
    fun expandedCatalogueHasNoDuplicateDeviceNames() {
        val names = ConsolePricingCatalog.catalogue.map { it.name.lowercase() }
        assertEquals(names.size, names.distinct().size)
    }
}
