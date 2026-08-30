package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LaptopSelectionCatalogTest {
    @Test
    fun catalogCoversRequestedTwentyYearWindow() {
        assertTrue(LaptopSelectionCatalog.presets.any { it.year == 2006 })
        assertTrue(LaptopSelectionCatalog.presets.any { it.year == 2026 })
        assertTrue(LaptopSelectionCatalog.presets.all { it.year in 2006..2026 })
    }

    @Test
    fun majorBrandsAreAvailable() {
        listOf("Apple", "Dell", "HP", "Lenovo", "ASUS", "Acer", "Microsoft", "MSI", "Samsung")
            .forEach { assertTrue("Missing $it", LaptopSelectionCatalog.brands.contains(it)) }
    }

    @Test
    fun selectingBrandRestrictsModels() {
        val apple = LaptopSelectionCatalog.models("Apple")
        assertFalse(apple.isEmpty())
        assertTrue(apple.all { it.brand == "Apple" })
    }

    @Test
    fun canonicalQueryContainsAllSelectedIdentityFields() {
        val preset = LaptopSelectionCatalog.models("Apple").first { it.model.contains("2020, M1") }
        val query = LaptopSelectionCatalog.canonicalQuery(preset, "Apple M1", "16GB", "512GB")
        assertTrue(query.contains("Apple"))
        assertTrue(query.contains("2020"))
        assertTrue(query.contains("Apple M1"))
        assertTrue(query.contains("16GB"))
        assertTrue(query.contains("512GB"))
    }
}
