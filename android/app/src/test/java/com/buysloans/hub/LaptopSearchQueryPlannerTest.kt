package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LaptopSearchQueryPlannerTest {
    @Test
    fun macBookAirM5BuildsCanonicalAndRetailerFriendlyQueries() {
        val preset = LaptopPreset(
            brand = "Apple",
            model = "MacBook Air 13-inch (2026, M5)",
            year = 2026,
            processors = listOf("Apple M5"),
            ramOptions = listOf("32GB"),
            storageOptions = listOf("2TB")
        )

        val queries = LaptopSearchQueryPlanner.queries(preset, "Apple M5", "32GB", "2TB")

        assertTrue(queries.size >= 3)
        assertTrue(queries.first().contains("2026"))
        assertTrue(queries.any { it == "Apple MacBook Air 13-inch M5 32GB 2TB" })
        assertTrue(queries.any { it.contains("32 GB") && it.contains("2 TB") && it.contains("Australia") })
        assertEquals(queries.size, queries.distinctBy { it.lowercase() }.size)
    }

    @Test
    fun windowsLaptopAlsoGetsRetailerFriendlyVariants() {
        val preset = LaptopPreset(
            brand = "Dell",
            model = "XPS 13 (2026)",
            year = 2026,
            processors = listOf("Intel Core Ultra 7"),
            ramOptions = listOf("32GB"),
            storageOptions = listOf("1TB")
        )

        val queries = LaptopSearchQueryPlanner.queries(preset, "Intel Core Ultra 7", "32GB", "1TB")

        assertTrue(queries.any { it == "Dell XPS 13 Intel Core Ultra 7 32GB 1TB" })
        assertTrue(queries.any { it.contains("32 GB") && it.contains("1 TB") && it.contains("Australia") })
        assertEquals(queries.size, queries.distinctBy { it.lowercase() }.size)
    }

    @Test
    fun verifiedModelCodeGetsDedicatedLookupVariant() {
        val preset = LaptopPreset(
            brand = "ASUS",
            model = "Chromebook CX14 (2026)",
            year = 2026,
            processors = listOf("Intel Processor N50"),
            ramOptions = listOf("8GB"),
            storageOptions = listOf("128GB")
        )

        val queries = LaptopSearchQueryPlanner.queries(
            preset = preset,
            processor = "Intel Processor N50",
            ram = "8GB",
            storage = "128GB",
            versionCode = "CX1405CTA"
        )

        assertTrue(queries.any { it == "ASUS CX1405CTA Intel Processor N50 8GB 128GB" })
        assertTrue(queries.size <= 4)
    }
}
