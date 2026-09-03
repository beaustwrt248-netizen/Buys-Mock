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
    fun expandedCatalogueAddsOlderAndAdditionalBrandModelsWithoutPrices() {
        val iphone12Mini = MobilePhoneDeviceCatalog.variants("Apple", "iPhone 12 mini")
        assertTrue(iphone12Mini.any { it.storage == "64 GB" && it.priceSheetValue == null })

        val honor = MobilePhoneDeviceCatalog.variants("HONOR", "Magic7 Pro")
        assertTrue(honor.any { it.storage == "512 GB" && it.priceSheetValue == null })

        assertTrue("ASUS" in MobilePhoneDeviceCatalog.brands())
        assertTrue("HMD" in MobilePhoneDeviceCatalog.brands())
        assertTrue("Fairphone" in MobilePhoneDeviceCatalog.brands())
    }

    @Test
    fun appleModelsAreSeriesFirstNewestGenerationFirst() {
        val models = MobilePhoneDeviceCatalog.models("Apple")
        assertTrue(models.indexOf("iPhone 17 Pro Max") < models.indexOf("iPhone 16 Pro Max"))
        assertTrue(models.indexOf("iPhone 16 Pro Max") < models.indexOf("iPhone 15 Pro Max"))
        assertTrue(models.indexOf("iPhone 15 Pro Max") < models.indexOf("iPhone 14 Pro Max"))
    }

    @Test
    fun premiumVariantsStayTogetherWithinPhoneGeneration() {
        val models = MobilePhoneDeviceCatalog.models("Apple")
        val seventeen = models.filter { it.startsWith("iPhone 17") }
        assertTrue(seventeen.first().contains("Pro Max"))
        assertTrue(seventeen.all { it.startsWith("iPhone 17") })
    }

    @Test
    fun modelNumberSearchFindsFriendlyModel() {
        assertTrue(MobilePhoneDeviceCatalog.modelMatches("Samsung", "Galaxy S24 Ultra", "SM-S928B"))
    }

    @Test
    fun globalSearchFindsAcrossBrandModelNumberAndStorage() {
        val byModelNumber = MobilePhoneDeviceCatalog.search("SM-S928B")
        assertTrue(byModelNumber.any { it.brand == "Samsung" && it.model == "Galaxy S24 Ultra" })

        val byBrand = MobilePhoneDeviceCatalog.search("HONOR")
        assertTrue(byBrand.any { it.model == "Magic7 Pro" })

        val byStorage = MobilePhoneDeviceCatalog.search("1TB")
        assertTrue(byStorage.any { "1 TB" in it.storages })
    }

    @Test
    fun globalSearchMarksExistingPricedModelsWithoutAuthorisingNewOnes() {
        val priced = MobilePhoneDeviceCatalog.search("Galaxy S24 Ultra").first()
        assertTrue(priced.hasPricedVariant)

        val unpriced = MobilePhoneDeviceCatalog.search("Magic7 Pro").first()
        assertTrue(!unpriced.hasPricedVariant)
        assertTrue(MobilePhoneDeviceCatalog.variants(unpriced.brand, unpriced.model).all { it.priceSheetValue == null })
    }

    @Test
    fun mergedCatalogueHasNoDuplicateBrandModelStorageKeys() {
        val keys = MobilePhoneDeviceCatalog.entries.map {
            "${it.brand.lowercase()}|${it.model.lowercase()}|${it.storage.replace(" ", "").lowercase()}"
        }
        assertEquals(keys.size, keys.distinct().size)
    }
}
