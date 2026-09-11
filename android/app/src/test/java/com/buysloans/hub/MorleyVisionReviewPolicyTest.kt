package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MorleyVisionReviewPolicyTest {
    private fun inspection(
        identityVerified: Boolean = true,
        storage: String = "256GB",
        qualityWarnings: List<String> = emptyList(),
        consistencyWarnings: List<String> = emptyList(),
        damageRegions: List<DamageRegion> = emptyList()
    ): DeviceInspection = DeviceInspection(
        brand = if (identityVerified) "Apple" else "",
        family = if (identityVerified) "iPhone" else "",
        model = if (identityVerified) "iPhone 15" else "",
        modelNumber = if (identityVerified) "A3090" else "",
        colour = "Black",
        storage = storage,
        conditionGrade = "B",
        conditionSummary = "Good used condition",
        confidence = if (identityVerified) 0.95 else 0.35,
        damageFlags = damageRegions.map { it.label },
        damageRegions = damageRegions,
        evidence = emptyList(),
        uncertainties = emptyList(),
        nextPhotos = emptyList(),
        catalogueMatch = if (identityVerified) CatalogueMatch(
            id = "iphone-15",
            category = "phones",
            brand = "Apple",
            family = "iPhone",
            modelName = "iPhone 15",
            modelNumber = "A3090",
            releaseYear = 2023,
            storageOptions = listOf("128GB", "256GB"),
            imageReferenceUrl = null
        ) else null,
        qualityWarnings = qualityWarnings,
        consistencyWarnings = consistencyWarnings,
        componentFindings = listOf("Rear camera lenses visible")
    )

    @Test
    fun unverifiedIdentityBlocksSuggestedPricing() {
        val state = MorleyVisionReviewPolicy.from(inspection(identityVerified = false), null)

        assertFalse(state.identityVerified)
        assertFalse(state.suggestedPricingAllowed)
        assertTrue(state.pricingBlockedReason!!.contains("identity", ignoreCase = true))
    }

    @Test
    fun corePricingGateBlocksPhotoQualityWarningBeforeMarketResearch() {
        val reason = MorleyVisionPolicy.pricingBlockReason(
            inspection(qualityWarnings = listOf("Front photo is blurred"))
        )

        assertTrue(reason!!.contains("Retake", ignoreCase = true))
    }

    @Test
    fun corePricingGateBlocksCrossPhotoInconsistencyBeforeMarketResearch() {
        val reason = MorleyVisionPolicy.pricingBlockReason(
            inspection(consistencyWarnings = listOf("Photos may show different devices"))
        )

        assertTrue(reason!!.contains("cross-photo", ignoreCase = true))
    }

    @Test
    fun corePricingGateAllowsCleanVerifiedEvidence() {
        assertEquals(null, MorleyVisionPolicy.pricingBlockReason(inspection()))
    }

    @Test
    fun photoQualityWarningBlocksSuggestedPricing() {
        val state = MorleyVisionReviewPolicy.from(
            inspection(qualityWarnings = listOf("Front photo is blurred")),
            LivePricingResult(emptyList(), 700.0, 650.0)
        )

        assertFalse(state.suggestedPricingAllowed)
        assertTrue(state.hasBlockingEvidenceGap)
    }

    @Test
    fun consistencyWarningBlocksSuggestedPricing() {
        val state = MorleyVisionReviewPolicy.from(
            inspection(consistencyWarnings = listOf("Photos may show different devices")),
            LivePricingResult(emptyList(), 700.0, 650.0)
        )

        assertFalse(state.suggestedPricingAllowed)
        assertTrue(state.pricingBlockedReason!!.contains("inconsist", ignoreCase = true))
    }

    @Test
    fun verifiedPricingCanBeUsedWhenEvidenceIsClean() {
        val state = MorleyVisionReviewPolicy.from(
            inspection(),
            LivePricingResult(
                listings = listOf(
                    MarketListing("eBay AU", "Apple iPhone 15 256GB", 700.0, "used"),
                    MarketListing("Facebook Marketplace", "iPhone 15 256GB", 680.0, "used")
                ),
                usedMedian = 690.0,
                conditionAdjustedResale = 655.0
            )
        )

        assertTrue(state.suggestedPricingAllowed)
        assertEquals(null, state.pricingBlockedReason)
    }

    @Test
    fun allDamageRegionsRequireExplicitStaffDecision() {
        val damage = DamageRegion(
            photoIndex = 1,
            label = "Screen crack",
            severity = "major",
            confidence = 0.93,
            x = 0.2f,
            y = 0.2f,
            width = 0.3f,
            height = 0.2f
        )
        val initial = MorleyVisionReviewPolicy.from(inspection(damageRegions = listOf(damage)), null)

        assertEquals(1, initial.unresolvedDamageCount)
        assertFalse(initial.canCompleteStaffReview)

        val reviewed = MorleyVisionReviewPolicy.decideDamage(initial, 0, VisionStaffDecision.NOT_DAMAGE)

        assertEquals(0, reviewed.unresolvedDamageCount)
        assertTrue(reviewed.canCompleteStaffReview)
    }
}
