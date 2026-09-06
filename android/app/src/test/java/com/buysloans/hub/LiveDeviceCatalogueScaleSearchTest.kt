package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Test

class LiveDeviceCatalogueScaleSearchTest {
    @Test
    fun brandSearchReturnsEveryMatchingDeviceBeyondOldPageLimit() {
        val devices = buildList {
            repeat(221) { index ->
                add(
                    LiveDeviceCatalogueRow(
                        id = (index + 1).toLong(),
                        category = if (index % 3 == 0) "laptop" else "mobile_phone",
                        brand = "Samsung",
                        model = "Galaxy Test $index",
                        modelNumber = "SM-TEST-$index",
                        storageOptions = listOf("128GB"),
                    ),
                )
            }
            add(LiveDeviceCatalogueRow(999, "mobile_phone", "Apple", "iPhone Test", "A9999", listOf("128GB")))
        }

        assertEquals(221, filterCatalogueDevices(devices, null, "Samsung").size)
    }
}
