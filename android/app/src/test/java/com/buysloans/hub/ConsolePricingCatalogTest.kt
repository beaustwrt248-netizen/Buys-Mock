package com.buysloans.hub

import org.junit.Assert.assertEquals
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
}
