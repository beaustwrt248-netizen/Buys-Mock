package com.buysloans.hub

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MobilePhoneDeviceCatalogTest {
    private val devices = listOf(
        LiveDeviceCatalogueRow(1, "mobile_phone", "Samsung", "Galaxy S24 Ultra", "SM-S928B", listOf("256 GB", "1 TB")),
        LiveDeviceCatalogueRow(2, "mobile_phone", "Samsung", "Galaxy S24", "SM-S921B", listOf("128 GB", "256 GB")),
        LiveDeviceCatalogueRow(3, "mobile_phone", "Apple", "iPhone 17 Pro Max", null, listOf("256 GB", "512 GB", "1 TB")),
        LiveDeviceCatalogueRow(4, "mobile_phone", "Apple", "iPhone 17 Pro", null, listOf("256 GB", "512 GB")),
        LiveDeviceCatalogueRow(5, "mobile_phone", "Apple", "iPhone 17", null, listOf("128 GB", "256 GB")),
        LiveDeviceCatalogueRow(6, "mobile_phone", "Apple", "iPhone 16 Pro Max", null, listOf("256 GB")),
        LiveDeviceCatalogueRow(7, "mobile_phone", "Apple", "iPhone 15 Pro Max", null, listOf("256 GB")),
        LiveDeviceCatalogueRow(8, "mobile_phone", "Apple", "iPhone 14 Pro Max", null, listOf("128 GB")),
        LiveDeviceCatalogueRow(9, "mobile_phone", "Apple", "iPhone 12 mini", null, listOf("64 GB")),
        LiveDeviceCatalogueRow(10, "mobile_phone", "HONOR", "Magic7 Pro", null, listOf("512 GB")),
        LiveDeviceCatalogueRow(11, "mobile_phone", "ASUS", "ROG Phone 9", null, listOf("512 GB")),
        LiveDeviceCatalogueRow(12, "mobile_phone", "HMD", "Skyline", null, listOf("256 GB")),
        LiveDeviceCatalogueRow(13, "mobile_phone", "Fairphone", "Fairphone 6", null, listOf("256 GB")),
        LiveDeviceCatalogueRow(14, "tablet", "Samsung", "Galaxy Tab S10", "SM-X920", listOf("256 GB")),
    )

    private val prices = listOf(
        LiveDevicePrice(1, "Samsung", "Galaxy S24 Ultra", "SM-S928B", "256 GB", 999.0, true),
        LiveDevicePrice(10, "HONOR", "Magic7 Pro", null, "512 GB", 777.0, false),
    )

    @Before
    fun setUp() {
        LiveDevicePricing.replaceSnapshotsForTesting(prices, devices)
    }

    @After
    fun tearDown() {
        LiveDevicePricing.replaceSnapshotsForTesting(emptyList(), emptyList())
    }

    @Test
    fun liveCatalogueOnlyIncludesMobilePhonesAndExpandsStorageVariants() {
        val entries = MobilePhoneDeviceCatalog.entries
        assertTrue(entries.none { it.model.startsWith("Galaxy Tab") })
        assertTrue(entries.any { it.brand == "Samsung" && it.model == "Galaxy S24" && it.storage == "128 GB" })
        assertTrue(entries.any { it.brand == "Samsung" && it.model == "Galaxy S24 Ultra" && it.storage == "1 TB" })
    }

    @Test
    fun authoritativeLivePriceWinsAndUnpricedVariantsStayUnpriced() {
        val priced = MobilePhoneDeviceCatalog.variants("Samsung", "Galaxy S24 Ultra")
            .first { it.storage == "256 GB" }
        assertEquals(999.0, priced.priceSheetValue!!, 0.0)
        assertEquals("SM-S928B", priced.modelNumber)

        val unpriced = MobilePhoneDeviceCatalog.variants("Samsung", "Galaxy S24")
            .first { it.storage == "128 GB" }
        assertNull(unpriced.priceSheetValue)
        assertEquals("SM-S921B", unpriced.modelNumber)
    }

    @Test
    fun nonAuthoritativePricesNeverAuthoriseCatalogueEntries() {
        val honor = MobilePhoneDeviceCatalog.variants("HONOR", "Magic7 Pro").single()
        assertNull(honor.priceSheetValue)
        assertTrue(!honor.hasPrice)
    }

    @Test
    fun liveCatalogueAddsOlderAndAdditionalBrandModelsWithoutInventingPrices() {
        assertTrue(MobilePhoneDeviceCatalog.variants("Apple", "iPhone 12 mini").any {
            it.storage == "64 GB" && it.priceSheetValue == null
        })
        assertTrue(MobilePhoneDeviceCatalog.variants("HONOR", "Magic7 Pro").all { it.priceSheetValue == null })
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
        val seventeen = MobilePhoneDeviceCatalog.models("Apple").filter { it.startsWith("iPhone 17") }
        assertTrue(seventeen.isNotEmpty())
        assertEquals("iPhone 17 Pro Max", seventeen.first())
        assertTrue(seventeen.all { it.startsWith("iPhone 17") })
    }

    @Test
    fun modelNumberSearchFindsFriendlyModel() {
        assertTrue(MobilePhoneDeviceCatalog.modelMatches("Samsung", "Galaxy S24 Ultra", "SM-S928B"))
        val results = MobilePhoneDeviceCatalog.search("SM-S928B")
        assertTrue(results.any { it.brand == "Samsung" && it.model == "Galaxy S24 Ultra" })
    }

    @Test
    fun globalSearchFindsAcrossBrandModelNumberAndStorage() {
        assertTrue(MobilePhoneDeviceCatalog.search("HONOR").any { it.model == "Magic7 Pro" })
        assertTrue(MobilePhoneDeviceCatalog.search("1TB").any { "1 TB" in it.storages })
    }

    @Test
    fun globalSearchMarksOnlyAuthoritativelyPricedModels() {
        val priced = MobilePhoneDeviceCatalog.search("Galaxy S24 Ultra").first()
        assertTrue(priced.hasPricedVariant)

        val unpriced = MobilePhoneDeviceCatalog.search("Magic7 Pro").first()
        assertTrue(!unpriced.hasPricedVariant)
        assertTrue(MobilePhoneDeviceCatalog.variants(unpriced.brand, unpriced.model).all { it.priceSheetValue == null })
    }

    @Test
    fun catalogueHasNoDuplicateBrandModelStorageKeys() {
        val keys = MobilePhoneDeviceCatalog.entries.map {
            "${it.brand.lowercase()}|${it.model.lowercase()}|${it.storage.replace(" ", "").lowercase()}"
        }
        assertEquals(keys.size, keys.distinct().size)
    }
}
