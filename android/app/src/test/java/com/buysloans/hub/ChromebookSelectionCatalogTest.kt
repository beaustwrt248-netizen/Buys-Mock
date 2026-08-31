package com.buysloans.hub

import org.junit.Assert.assertTrue
import org.junit.Test

class ChromebookSelectionCatalogTest {
    @Test
    fun guidedCatalogIncludesMajorChromebookBrandsAndModels() {
        assertTrue("Google" in LaptopSelectionCatalog.brands)
        assertTrue(LaptopSelectionCatalog.models("Acer").any { it.model.contains("Chromebook") })
        assertTrue(LaptopSelectionCatalog.models("ASUS").any { it.model.contains("Chromebook") })
        assertTrue(LaptopSelectionCatalog.models("Lenovo").any { it.model.contains("Chromebook") })
        assertTrue(LaptopSelectionCatalog.models("HP").any { it.model.contains("Chromebook") })
        assertTrue(LaptopSelectionCatalog.models("Dell").any { it.model.contains("Chromebook") })
        assertTrue(LaptopSelectionCatalog.models("Samsung").any { it.model.contains("Chromebook") })
    }

    @Test
    fun chromebookPresetsExposeChromeOsTypicalStorageAndProcessors() {
        val acer311 = LaptopSelectionCatalog.models("Acer").first { it.model.startsWith("Chromebook 311") }
        assertTrue("64GB" in acer311.storageOptions)
        assertTrue("4GB" in acer311.ramOptions)
        assertTrue(acer311.processors.any { it.contains("Celeron") || it.contains("MediaTek") })
    }
}
