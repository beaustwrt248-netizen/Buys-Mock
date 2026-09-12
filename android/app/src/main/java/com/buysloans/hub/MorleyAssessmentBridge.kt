package com.buysloans.hub

internal data class MorleyAssessmentSnapshot(
    val state: String,
    val identityResolved: Boolean,
    val storageResolved: Boolean,
    val evidenceSufficient: Boolean,
    val reviewRequired: Boolean,
    val valuationAllowed: Boolean,
    val valuationBlockedReason: String?,
    val confidence: Double,
    val confirmedDamageCount: Int,
    val dismissedDamageCount: Int,
    val pendingDamageCount: Int,
    val commercialAuthority: String = "advisory",
    val requiresStaffConfirmation: Boolean = true
)

internal object MorleyAssessmentBridge {
    fun from(
        inspection: DeviceInspection,
        review: MorleyVisionReviewState,
        pricing: LivePricingResult?
    ): MorleyAssessmentSnapshot {
        val identityResolved = inspection.identityVerified
        val storageResolved = review.storageVerified
        val evidenceSufficient = review.qualityWarnings.isEmpty() && review.consistencyWarnings.isEmpty()
        val reviewRequired = !review.canCompleteStaffReview
        val valuationAllowed = review.canCompleteStaffReview && pricing?.canUseSuggestedPrice == true
        val confirmedDamageCount = review.damageReviews.count { it.decision == VisionStaffDecision.CONFIRMED }
        val dismissedDamageCount = review.damageReviews.count { it.decision == VisionStaffDecision.NOT_DAMAGE }
        val pendingDamageCount = review.unresolvedDamageCount

        val valuationBlockedReason = when {
            !identityResolved -> "Verify the exact device identity before using AI valuation."
            !storageResolved -> "Confirm verified device storage before using AI valuation."
            !evidenceSufficient -> "Resolve photo quality or cross-photo consistency warnings before using AI valuation."
            pendingDamageCount > 0 -> "Confirm or dismiss every detected damage region before using AI valuation."
            pricing == null -> "Staff review is verified. Run live pricing to create an advisory valuation."
            pricing.recommendationBlockedReason != null -> pricing.recommendationBlockedReason
            !pricing.canUseSuggestedPrice -> "Not enough verified market evidence to create an advisory valuation."
            else -> null
        }

        val state = when {
            !identityResolved -> "identity_unresolved"
            !storageResolved -> "storage_unresolved"
            !evidenceSufficient -> "evidence_insufficient"
            reviewRequired -> "review_required"
            valuationAllowed -> "proposal_ready"
            else -> "identified"
        }

        return MorleyAssessmentSnapshot(
            state = state,
            identityResolved = identityResolved,
            storageResolved = storageResolved,
            evidenceSufficient = evidenceSufficient,
            reviewRequired = reviewRequired,
            valuationAllowed = valuationAllowed,
            valuationBlockedReason = valuationBlockedReason,
            confidence = inspection.confidence.coerceIn(0.0, 1.0),
            confirmedDamageCount = confirmedDamageCount,
            dismissedDamageCount = dismissedDamageCount,
            pendingDamageCount = pendingDamageCount,
            commercialAuthority = "advisory",
            requiresStaffConfirmation = true
        )
    }
}
