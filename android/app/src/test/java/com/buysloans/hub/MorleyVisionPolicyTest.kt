package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MorleyVisionPolicyTest {
    private fun inspection(
        model: String = "iPhone 17 Pro",
        modelNumber: String = "A3523",
        storage: String = "256GB",
        confidence: Double = 0.94
    ) = DeviceInspection(
        brand = "Apple",
        family = "iPhone 17",
        model = model,
        modelNumber = modelNumber,
        colour = "Cosmic Orange",
        storage = storage,
        conditionGrade = "B",
        conditionSummary = "Minor cosmetic wear",
        confidence = confidence,
        damageFlags = emptyList(),
        damageRegions = emptyList(),
        evidence = listOf("camera layout"),
        uncertainties = emptyList(),
        nextPhotos = emptyList(),
        catalogueMatch = CatalogueMatch(
            id = "iphone-17-pro",
            category = "mobile phone",
            brand = "Apple",
            family = "iPhone 17",
            modelName = model,
            modelNumber = modelNumber,
            releaseYear = 2025,
            storageOptions = listOf("256GB", "512GB", "1TB"),
            imageReferenceUrl = null
        )
    )

    @Test
    fun nullLikeValuesAreNeverPresentedAsFacts() {
        assertEquals("", MorleyVisionPolicy.clean("null"))
        assertEquals("", MorleyVisionPolicy.clean(" undefined "))
        assertEquals("Not verified", MorleyVisionPolicy.displayOrUnverified("null"))
    }

    @Test
    fun exactDeviceListingMatchesVerifiedIdentity() {
        val subject = inspection()
        assertTrue(MorleyVisionPolicy.titleMatchesIdentity("Apple iPhone 17 Pro 256GB Cosmic Orange", subject))
        assertTrue(MorleyVisionPolicy.titleMatchesIdentity("iPhone 17 Pro A3523 unlocked", subject))
    }

    @Test
    fun olderGenerationListingCannotContaminateValuation() {
        val subject = inspection()
        assertFalse(MorleyVisionPolicy.titleMatchesIdentity("Apple iPhone 14 Pro Max 256GB", subject))
        assertFalse(MorleyVisionPolicy.titleMatchesIdentity("iPhone 16 Pro 256GB", subject))
    }

    @Test
    fun explicitStorageMismatchIsRejected() {
        val subject = inspection(storage = "256GB")
        assertFalse(MorleyVisionPolicy.titleMatchesIdentity("Apple iPhone 17 Pro 512GB", subject))
    }

    @Test
    fun uncertainIdentityBlocksPricing() {
        val subject = inspection(confidence = 0.42)
        assertFalse(subject.identityVerified)
        assertEquals("", subject.identityQuery)
        assertTrue(MorleyVisionPolicy.pricingBlockReason(subject)!!.contains("not verified", ignoreCase = true))
    }

    @Test
    fun robustComparableSetRejectsExtremePriceExtraction() {
        val listings = listOf(
            MarketListing("Facebook Marketplace", "iPhone 17 Pro", 80.0, "used"),
            MarketListing("eBay AU", "iPhone 17 Pro", 1180.0, "used"),
            MarketListing("eBay AU", "iPhone 17 Pro", 1220.0, "used"),
            MarketListing("Facebook Marketplace", "iPhone 17 Pro", 1250.0, "used")
        )
        val result = MorleyVisionPolicy.robustComparableSet(listings)
        assertEquals(3, result.size)
        assertFalse(result.any { it.price == 80.0 })
    }

    @Test
    fun categoryProfilesAddGuidedInspectionAngles() {
        val phone = MorleyVisionPolicy.inspectionProfile("mobile phone")
        assertEquals(listOf("front", "back"), phone.required)
        assertTrue(phone.recommended.contains("camera area"))
        assertTrue(phone.recommended.contains("model/storage label"))

        val laptop = MorleyVisionPolicy.inspectionProfile("laptop")
        assertTrue(laptop.recommended.contains("hinges"))
    }
}
