package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveDeviceCatalogueScaleTest {
    @Test
    fun wearableCategoryStillFindsRowsBeyondFirstThousand() {
        val devices = buildList {
            repeat(1000) { index ->
                add(
                    LiveDeviceCatalogueRow(
                        id = (index + 1).toLong(),
                        category = "tablet",
                        brand = "TabletBrand",
                        model = "Tablet $index",
                        modelNumber = null,
                        storageOptions = listOf("128GB"),
                    ),
                )
            }
            repeat(139) { index ->
                add(
                    LiveDeviceCatalogueRow(
                        id = (1001 + index).toLong(),
                        category = "wearable",
                        brand = "WatchBrand",
                        model = "Watch $index",
                        modelNumber = null,
                        storageOptions = emptyList(),
                    ),
                )
            }
        }

        val watches = filterCatalogueDevices(devices, "wearable")

        assertEquals(139, watches.size)
        assertTrue(watches.all { normalizeCatalogueCategory(it.category) == "wearable" })
    }

    @Test
    fun legacyWatchCategoryAliasesMapToWearable() {
        val aliases = listOf("wearable", "wearables", "smart_watch", "smartwatch", "smart watches", "watch", "watches")
        aliases.forEach { alias -> assertEquals("wearable", normalizeCatalogueCategory(alias)) }
    }

    @Test
    fun categoryFilteringDoesNotLimitFullCatalogue() {
        val devices = (1..1250).map { index ->
            LiveDeviceCatalogueRow(
                id = index.toLong(),
                category = if (index > 1100) "wearable" else "mobile_phone",
                brand = "Brand",
                model = "Model $index",
                modelNumber = null,
                storageOptions = emptyList(),
            )
        }

        assertEquals(1250, filterCatalogueDevices(devices, null).size)
        assertEquals(150, filterCatalogueDevices(devices, "wearable").size)
    }
}
