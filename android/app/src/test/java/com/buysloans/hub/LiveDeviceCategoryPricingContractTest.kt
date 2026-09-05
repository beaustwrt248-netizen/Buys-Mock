package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveDeviceCategoryPricingContractTest {
    @Test
    fun authoritativeTabletAndWearableVariantsResolveByStorage() {
        val prices = listOf(
            LiveDevicePrice(10, "Apple", "iPad 3rd Gen Wi-Fi", "A1416", "32GB", 79.0, true),
            LiveDevicePrice(20, "Samsung", "Galaxy Watch Ultra LTE 47mm", "SM-L705F", "64GB", 699.0, true),
        )

        assertEquals(79.0, LiveDevicePricing.find(prices, "Apple", "iPad 3rd Gen Wi-Fi", "A1416", "32 GB")?.priceAud ?: -1.0, 0.0)
        assertEquals(699.0, LiveDevicePricing.find(prices, "Samsung", "Galaxy Watch Ultra LTE 47mm", "SM-L705F", "64GB")?.priceAud ?: -1.0, 0.0)
    }

    @Test
    fun nonAuthoritativeVariantDoesNotDriveQuickSummaryBuyPrice() {
        val prices = listOf(
            LiveDevicePrice(10, "Apple", "iPad", "A0000", "64GB", 500.0, false),
        )

        assertNull(LiveDevicePricing.find(prices, "Apple", "iPad", "A0000", "64GB"))
    }
}
