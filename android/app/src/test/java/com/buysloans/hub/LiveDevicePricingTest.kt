package com.buysloans.hub
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
class LiveDevicePricingTest {
 @Test fun normalizesStorageSpacingAndCase(){assertEquals("512GB",LiveDevicePricing.normalizeStorage("512 GB"));assertEquals("1TB",LiveDevicePricing.normalizeStorage("1 tb"))}
 @Test fun matchesAuthoritativePriceAcrossStorageFormatting(){val rows=listOf(LiveDevicePrice(597,"Google","Pixel 10 Pro Fold","GU0NP","512GB",1799.0,true));assertEquals(1799.0,LiveDevicePricing.find(rows,"Google","Pixel 10 Pro Fold","GU0NP","512 GB")?.priceAud?:0.0,0.0)}
 @Test fun canMatchByModelWhenModelNumberIsUnavailable(){val rows=listOf(LiveDevicePrice(597,"Google","Pixel 10 Pro Fold","GU0NP","512GB",1799.0,true));assertEquals(1799.0,LiveDevicePricing.find(rows,"Google","Pixel 10 Pro Fold",null,"512 GB")?.priceAud?:0.0,0.0)}
 @Test fun rejectsNonAuthoritativePrice(){val rows=listOf(LiveDevicePrice(597,"Google","Pixel 10 Pro Fold","GU0NP","512GB",1799.0,false));assertNull(LiveDevicePricing.find(rows,"Google","Pixel 10 Pro Fold","GU0NP","512 GB"))}
}
