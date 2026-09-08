package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalBuySearchTest {
    @Test
    fun findsAuthoritativePricedPhoneWithoutChangingPrice() {
        val result = UniversalBuySearch.search("Galaxy S25 Ultra 256").first { it.category == UniversalBuyCategory.PHONE }
        assertEquals("Galaxy S25 Ultra", result.title)
        assertEquals(1349.0, result.priceSheetValue ?: error("missing price"), 0.0)
        assertTrue(result.canAuthoriseBuy)
    }

    @Test
    fun resolvesImportedModelNumbersButKeepsUnpricedRowsUnauthorised() {
        val imported = ImportedMobilePhoneCatalog.models.firstOrNull { it.modelNumber.isNotBlank() }
            ?: error("Expected imported phone model number")
        val result = UniversalBuySearch.search(imported.modelNumber).firstOrNull {
            it.category == UniversalBuyCategory.PHONE && it.modelNumber == imported.modelNumber
        }
        assertNotNull(result)
        if (result != null && result.priceSheetValue == null) {
            assertFalse(result.canAuthoriseBuy)
            assertTrue(result.subtitle.contains("Price to be added"))
        }
    }

    @Test
    fun findsLiveConsoleAndPreservesUnpricedBoundary() {
        LiveDevicePricing.replaceSnapshotsForTesting(
            emptyList(),
            listOf(
                LiveDeviceCatalogueRow(
                    id = 418,
                    category = "console",
                    brand = "Nintendo",
                    model = "Nintendo DSi XL",
                    modelNumber = "UTL-001",
                    storageOptions = emptyList(),
                    family = "Nintendo DSi"
                )
            )
        )
        try {
            val result = UniversalBuySearch.search("Nintendo DSi XL").first { it.category == UniversalBuyCategory.CONSOLE }
            assertEquals("Nintendo DSi XL", result.title)
            assertEquals(null, result.priceSheetValue)
            assertFalse(result.canAuthoriseBuy)
            assertTrue(result.subtitle.contains("Price to be added"))
        } finally {
            LiveDevicePricing.replaceSnapshotsForTesting(emptyList(), emptyList())
        }
    }

    @Test
    fun laptopRetailObservationIsReferenceOnly() {
        val result = UniversalBuySearch.search("DB16250").first { it.category == UniversalBuyCategory.LAPTOP }
        assertEquals("Dell 16 Plus", result.title)
        assertNotNull(result.referenceValue)
        assertEquals(null, result.priceSheetValue)
        assertFalse(result.canAuthoriseBuy)
    }

    @Test
    fun arbitraryQueryAlwaysOffersGeneralBuysWithoutInventingPrice() {
        val result = UniversalBuySearch.search("vintage camera body").last()
        assertEquals(UniversalBuyCategory.GENERAL_BUYS, result.category)
        assertEquals("vintage camera body", result.title)
        assertEquals(null, result.priceSheetValue)
        assertFalse(result.canAuthoriseBuy)
    }

    @Test
    fun blankQueryReturnsNoResults() {
        assertTrue(UniversalBuySearch.search("   ").isEmpty())
    }
}
