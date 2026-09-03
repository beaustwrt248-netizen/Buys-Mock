package com.buysloans.hub

import org.junit.Assert.*
import org.junit.Test

class LaptopLiveIntelligenceAdapterTest {
    private val preset = LaptopPreset(
        brand = "Apple",
        model = "MacBook Pro 14-inch (2021)",
        year = 2021,
        processors = listOf("Apple M1 Pro"),
        ramOptions = listOf("16GB"),
        storageOptions = listOf("512GB")
    )

    @Test fun targetFingerprintUsesWholeDeviceIdentity() {
        val target = LaptopLiveIntelligenceAdapter.targetFingerprint(preset, "Apple M1 Pro", "16GB", "512GB", "A2442")
        assertEquals("Apple", target.brand)
        assertEquals("macbook pro", target.family)
        assertEquals("2021", target.generation)
        assertEquals("A2442", target.modelCode)
        assertEquals("m1 pro", target.cpu)
        assertEquals(16, target.ramGb)
        assertEquals(512, target.storageGb)
    }

    @Test fun wrongYearAndAccessoryCannotContaminateShadowValue() {
        val evidence = listOf(
            LiveLaptopEvidence("good-1", "Apple MacBook Pro 14 inch 2021 A2442 M1 Pro 16GB RAM 512GB SSD", 1100.0, "eBay AU", sold = true),
            LiveLaptopEvidence("good-2", "MacBook Pro A2442 14 inch 2021 M1 Pro 16GB RAM 512GB SSD", 1120.0, "eBay AU", sold = true),
            LiveLaptopEvidence("good-3", "Apple MacBook Pro 14 inch 2021 A2442 M1 Pro 16GB RAM 512GB SSD", 1080.0, "Other sold", sold = true),
            LiveLaptopEvidence("wrong-year", "Apple MacBook Pro 14 inch 2020 M1 Pro 16GB RAM 512GB SSD", 400.0, "eBay AU", sold = true),
            LiveLaptopEvidence("screen", "Replacement screen for MacBook Pro A2442 14 inch 2021", 199.0, "eBay AU", sold = true)
        )
        val shadow = LaptopLiveIntelligenceAdapter.shadowEvaluate(preset, "Apple M1 Pro", "16GB", "512GB", "A2442", evidence)
        assertTrue(shadow.fairBuyZone.marketValue in 1050.0..1150.0)
        assertTrue(shadow.fairBuyZone.comparables.any { !it.accepted && it.comparable.id == "wrong-year" })
        assertTrue(shadow.fairBuyZone.comparables.any { !it.accepted && it.comparable.id == "screen" })
        assertFalse(shadow.liveAuthorityChanged)
    }

    @Test fun activeOnlyEvidenceRemainsManualReview() {
        val evidence = listOf(
            LiveLaptopEvidence("a", "Apple MacBook Pro 14 inch 2021 A2442 M1 Pro 16GB RAM 512GB SSD", 1100.0, "Google"),
            LiveLaptopEvidence("b", "Apple MacBook Pro 14 inch 2021 A2442 M1 Pro 16GB RAM 512GB SSD", 1110.0, "eBay"),
            LiveLaptopEvidence("c", "Apple MacBook Pro 14 inch 2021 A2442 M1 Pro 16GB RAM 512GB SSD", 1090.0, "Marketplace")
        )
        val shadow = LaptopLiveIntelligenceAdapter.shadowEvaluate(preset, "Apple M1 Pro", "16GB", "512GB", "A2442", evidence)
        assertEquals("MANUAL REVIEW", shadow.fairBuyZone.decision)
        assertTrue(shadow.fairBuyZone.comparables.all { !it.comparable.sold })
        assertFalse(shadow.liveAuthorityChanged)
    }

    @Test fun contradictoryChipListingIsRejected() {
        val comparable = LaptopLiveIntelligenceAdapter.comparable(
            LiveLaptopEvidence("contradiction", "Apple MacBook Pro 2021 M1 Pro M2 Pro 16GB RAM 512GB SSD", 1000.0, "eBay")
        )
        assertTrue(comparable.suspicious)
    }
}
