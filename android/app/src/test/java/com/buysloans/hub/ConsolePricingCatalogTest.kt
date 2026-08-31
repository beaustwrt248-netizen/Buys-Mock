package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsolePricingCatalogTest {
    @Test
    fun suppliedPricingSheetIsFullyRepresented() {
        assertEquals(19, ConsolePricingCatalog.entries.size)
        assertEquals(129.0, ConsolePricingCatalog.entries.first { it.name == "Sony PS4 OG 500gb" }.rrp, 0.01)
        assertEquals(899.0, ConsolePricingCatalog.entries.first { it.name == "Sony PS5 Pro" }.rrp, 0.01)
        assertEquals(549.0, ConsolePricingCatalog.entries.first { it.name == "Xbox Series X" }.rrp, 0.01)
        assertEquals(599.0, ConsolePricingCatalog.entries.first { it.name == "Nintendo Switch 2" }.rrp, 0.01)
    }

    @Test
    fun consoleGradesAreRestrictedToABC() {
        assertEquals(listOf("A", "B", "C"), ConsolePricingCatalog.grades)
        assertTrue(ConsolePricingCatalog.entries.none { it.rrp <= 0.0 })
    }
}
