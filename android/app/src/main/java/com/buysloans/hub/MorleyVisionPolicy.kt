package com.buysloans.hub

/**
 * Safety policy for Morley Vision. Visual inference is evidence, never a fact by default.
 * Unknown values remain explicitly unverified rather than being guessed or rendered as "null".
 */
object MorleyVisionPolicy {
    const val MIN_IDENTITY_CONFIDENCE = 0.70
    const val MIN_DAMAGE_CONFIDENCE = 0.55
    const val MIN_COMPARABLES_FOR_PRICE = 2

    private val placeholders = setOf(
        "", "null", "nil", "none", "n/a", "na", "unknown", "undefined", "not available", "not provided"
    )

    fun clean(value: String?): String {
        val cleaned = value.orEmpty().trim()
        return cleaned.takeUnless { it.lowercase() in placeholders }.orEmpty()
    }

    fun displayOrUnverified(value: String?): String = clean(value).ifBlank { "Not verified" }

    fun isIdentityVerified(inspection: DeviceInspection): Boolean {
        val match = inspection.catalogueMatch
        val brand = clean(match?.brand).ifBlank { clean(inspection.brand) }
        val model = clean(match?.modelName).ifBlank { clean(inspection.model) }
        return inspection.confidence >= MIN_IDENTITY_CONFIDENCE && brand.isNotBlank() && model.isNotBlank()
    }

    fun pricingBlockReason(inspection: DeviceInspection): String? = when {
        !isIdentityVerified(inspection) -> "Device identity is not verified strongly enough for a price recommendation."
        inspection.qualityWarnings.isNotEmpty() -> "Retake unclear photos before using a price recommendation."
        inspection.consistencyWarnings.isNotEmpty() -> "Resolve cross-photo inconsistencies before using a price recommendation."
        else -> null
    }

    fun inspectionProfile(category: String?): VisionInspectionProfile {
        val key = clean(category).lowercase()
        return when {
            key.contains("tablet") -> VisionInspectionProfile(
                category = "tablet",
                required = listOf("front", "back"),
                recommended = listOf("left edge", "right edge", "camera area", "ports")
            )
            key.contains("watch") -> VisionInspectionProfile(
                category = "watch",
                required = listOf("screen", "back/sensors"),
                recommended = listOf("crown/buttons", "case edges", "band mounts")
            )
            key.contains("laptop") || key.contains("computer") -> VisionInspectionProfile(
                category = "laptop",
                required = listOf("screen/keyboard", "lid/base"),
                recommended = listOf("left ports", "right ports", "hinges", "corners")
            )
            key.contains("console") -> VisionInspectionProfile(
                category = "console",
                required = listOf("front/top", "back/ports"),
                recommended = listOf("sides", "serial/model label", "controller/accessories")
            )
            key.contains("accessor") -> VisionInspectionProfile(
                category = "accessory",
                required = listOf("front", "back"),
                recommended = listOf("connectors", "model label")
            )
            else -> VisionInspectionProfile(
                category = "phone",
                required = listOf("front", "back"),
                recommended = listOf("left edge", "right edge", "top/bottom edges", "camera area", "model/storage label")
            )
        }
    }

    /** Returns true only when a market title is sufficiently specific to the verified device. */
    fun titleMatchesIdentity(title: String, inspection: DeviceInspection): Boolean {
        if (!isIdentityVerified(inspection)) return false
        val haystack = normalise(title)
        if (haystack.isBlank()) return false

        val match = inspection.catalogueMatch
        val brand = clean(match?.brand).ifBlank { clean(inspection.brand) }
        val model = clean(match?.modelName).ifBlank { clean(inspection.model) }
        val modelNumber = clean(match?.modelNumber).ifBlank { clean(inspection.modelNumber) }

        val modelNumberNorm = normalise(modelNumber)
        if (modelNumberNorm.isNotBlank() && containsPhrase(haystack, modelNumberNorm)) return true

        val important = tokens(model).filterNot { token ->
            token.length <= 1 || token in setOf("phone", "mobile", "smartphone", "the")
        }
        if (important.isEmpty() || important.any { !hasToken(haystack, it) }) return false

        val brandToken = tokens(brand).firstOrNull()
        if (brandToken != null && !hasToken(haystack, brandToken)) {
            // Apple listings commonly omit the word Apple but retain iPhone/iPad model identity.
            val appleProduct = brand.equals("Apple", true) && important.any { it == "iphone" || it == "ipad" || it == "watch" }
            if (!appleProduct) return false
        }

        // If a listing explicitly states storage, reject an explicit mismatch.
        val expectedStorage = canonicalStorage(clean(inspection.storage))
        val listingStorages = storageTokens(haystack)
        if (expectedStorage != null && listingStorages.isNotEmpty() && expectedStorage !in listingStorages) return false

        return true
    }

    fun robustComparableSet(listings: List<MarketListing>): List<MarketListing> {
        if (listings.size < 3) return listings
        val prices = listings.map { it.price }.filter { it > 0.0 }.sorted()
        if (prices.size < 3) return listings
        val median = median(prices)
        if (median <= 0.0) return listings
        return listings.filter { it.price in (median * 0.45)..(median * 2.20) }
    }

    fun median(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2.0 else sorted[mid]
    }

    private fun normalise(value: String): String = value.lowercase()
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()

    private fun tokens(value: String): List<String> = normalise(value).split(' ').filter { it.isNotBlank() }

    private fun hasToken(haystack: String, token: String): Boolean =
        haystack.split(' ').any { it == token }

    private fun containsPhrase(haystack: String, phrase: String): Boolean =
        " $haystack ".contains(" $phrase ")

    private fun canonicalStorage(value: String): String? {
        val compact = value.lowercase().replace(" ", "")
        val match = Regex("(\\d+)(gb|tb)").find(compact) ?: return null
        return match.groupValues[1] + match.groupValues[2]
    }

    private fun storageTokens(normalisedTitle: String): Set<String> =
        Regex("\\b(\\d+)\\s*(gb|tb)\\b").findAll(normalisedTitle)
            .map { it.groupValues[1] + it.groupValues[2] }
            .toSet()
}

data class VisionInspectionProfile(
    val category: String,
    val required: List<String>,
    val recommended: List<String>
)

enum class VisionReviewState {
    UNREVIEWED,
    CONFIRMED,
    NOT_DAMAGE
}

data class VisionDamageReview(
    val region: DamageRegion,
    val state: VisionReviewState = VisionReviewState.UNREVIEWED,
    val staffSeverity: String? = null
) {
    val effectiveSeverity: String
        get() = MorleyVisionPolicy.clean(staffSeverity).ifBlank { region.severity }
}
