package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LiveDeviceCatalogueUiContractTest {
    @Test
    fun catalogueUsesLazyRenderingAndDoesNotCapSearchResultsAtSixty() {
        val source = File("src/main/java/com/buysloans/hub/LiveDeviceCatalogueBrowser.kt").readText()

        assertTrue(source.contains("LazyColumn("))
        assertTrue(source.contains("items(filtered, key = { it.id })"))
        assertFalse(source.contains("CATALOGUE_PAGE_SIZE"))
        assertFalse(source.contains("filtered.take("))
        assertFalse(source.contains("Show 60 more"))
    }

    @Test
    fun multiTermSearchRequiresEveryTypedTerm() {
        val devices = listOf(
            LiveDeviceCatalogueRow(1, "mobile_phone", "Samsung", "Galaxy A55 5G", "SM-A556E", listOf("128GB", "256GB")),
            LiveDeviceCatalogueRow(2, "mobile_phone", "Samsung", "Galaxy A35 5G", "SM-A356E", listOf("128GB")),
            LiveDeviceCatalogueRow(3, "laptop", "Samsung", "Galaxy Book5 Pro", "NP940XHA", listOf("512GB")),
        )

        val results = filterCatalogueDevices(devices, null, "samsung a55 256")

        assertTrue(results.size == 1)
        assertTrue(results.single().model == "Galaxy A55 5G")
    }
}
