package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveDevicePricingTest {
    @Test fun normalizesStorageSpacingAndCase() {
        assertEquals("512GB", LiveDevicePricing.normalizeStorage("512 GB"))
        assertEquals("1TB", LiveDevicePricing.normalizeStorage("1 tb"))
    }

    @Test fun matchesAuthoritativePriceAcrossStorageFormatting() {
        val rows = listOf(LiveDevicePrice(42, "512GB", 1799.0, true))
        assertEquals(1799.0, LiveDevicePricing.find(rows, 42, "512 GB")?.priceAud ?: 0.0, 0.0)
    }

    @Test fun rejectsNonAuthoritativeAndWrongDevicePrices() {
        val rows = listOf(
            LiveDevicePrice(42, "512GB", 1799.0, false),
            LiveDevicePrice(43, "512GB", 999.0, true)
        )
        assertNull(LiveDevicePricing.find(rows, 42, "512 GB"))
    }
}
