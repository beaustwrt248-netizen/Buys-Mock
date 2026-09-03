package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MobilePhoneDeviceCatalogTest {
    @Test
    fun importedDocumentContainsExpectedModelCount() {
        assertEquals(307, ImportedMobilePhoneCatalog.models.size)
    }

    @Test
    fun samsungModelNumbersResolveToFriendlySeriesNames() {
        val s24Ultra = ImportedMobilePhoneCatalog.models.firstOrNull {
            it.brand == "Samsung" && it.modelNumber == "SM-S928B"
        }
        assertNotNull(s24Ultra)
        assertEquals("Galaxy S24 Ultra", s24Ultra!!.model)
        assertTrue("1 TB" in s24Ultra.storages)

        val s25Plus = ImportedMobilePhoneCatalog.models.firstOrNull {
            it.brand == "Samsung" && it.modelNumber == "SM-S936B"
        }
        assertEquals("Galaxy S25 Plus", s25Plus?.model)
    }

    @Test
    fun existingPricedVariantWinsOverImportedDuplicate() {
        val existing = MobilePhoneDeviceCatalog.variants("Samsung", "Galaxy S24 Ultra")
            .first { it.storage == "256 GB" }
        assertEquals(999.0, existing.priceSheetValue!!, 0.0)
        assertEquals("SM-S928B", existing.modelNumber)
    }

    @Test
    fun importedMissingVariantIsAddedWithoutInventingPrice() {
        val imported = MobilePhoneDeviceCatalog.variants("Samsung", "Galaxy S24")
            .first { it.storage == "128 GB" }
        assertNull(imported.priceSheetValue)
        assertEquals("SM-S921B", imported.modelNumber)
    }

    @Test
    fun modelNumberSearchFindsFriendlyModel() {
        assertTrue(MobilePhoneDeviceCatalog.modelMatches("Samsung", "Galaxy S24 Ultra", "SM-S928B"))
    }

    @Test
    fun mergedCatalogueHasNoDuplicateBrandModelStorageKeys() {
        val keys = MobilePhoneDeviceCatalog.entries.map {
            "${it.brand.lowercase()}|${it.model.lowercase()}|${it.storage.replace(" ", "").lowercase()}"
        }
        assertEquals(keys.size, keys.distinct().size)
    }
}
