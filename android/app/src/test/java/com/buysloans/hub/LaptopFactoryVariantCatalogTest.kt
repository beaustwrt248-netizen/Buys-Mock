package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LaptopFactoryVariantCatalogTest {
    @Test
    fun cx1RejectsImpossibleCoreI5AndUnsupportedRam() {
        val cx1 = LaptopSelectionCatalog.models("ASUS").first { it.model.startsWith("Chromebook CX1") }
        val versions = LaptopFactoryVariantCatalog.versionCodes(cx1)
        assertTrue("CX1400" in versions)
        assertFalse("Intel Core i5" in LaptopFactoryVariantCatalog.processors(cx1, "CX1400"))
        assertTrue(LaptopFactoryVariantCatalog.ramOptions(cx1, "CX1400", "Intel Celeron N4500") == listOf("4GB", "8GB"))
        assertFalse("16GB" in LaptopFactoryVariantCatalog.ramOptions(cx1, "CX1400", "Intel Celeron N4500"))
    }

    @Test
    fun cx14ExactVariantNarrowsRamAndStorageByProcessor() {
        val cx14 = LaptopSelectionCatalog.models("ASUS").first { it.model.startsWith("Chromebook CX14") }
        assertTrue("CX1405CTA" in LaptopFactoryVariantCatalog.versionCodes(cx14))
        val ram = LaptopFactoryVariantCatalog.ramOptions(cx14, "CX1405CTA", "Intel Core 3-N355")
        assertTrue(ram == listOf("8GB"))
        val storage = LaptopFactoryVariantCatalog.storageOptions(cx14, "CX1405CTA", "Intel Core 3-N355", "8GB")
        assertTrue(storage == listOf("128GB"))
        assertFalse(LaptopFactoryVariantCatalog.configurationVerified(cx14, "CX1405CTA", "Intel Core 3-N355", "4GB", "128GB"))
        assertTrue(LaptopFactoryVariantCatalog.configurationVerified(cx14, "CX1405CTA", "Intel Core 3-N355", "8GB", "128GB"))
    }

    @Test
    fun canonicalQueryCarriesVerifiedModelCode() {
        val cx14 = LaptopSelectionCatalog.models("ASUS").first { it.model.startsWith("Chromebook CX14") }
        val query = LaptopFactoryVariantCatalog.canonicalQuery(cx14, "CX1405CTA", "Intel Processor N50", "8GB", "128GB")
        assertTrue(query.contains("CX1405CTA"))
        assertTrue(query.contains("Intel Processor N50"))
        assertTrue(query.contains("8GB"))
        assertTrue(query.contains("128GB"))
    }
}
