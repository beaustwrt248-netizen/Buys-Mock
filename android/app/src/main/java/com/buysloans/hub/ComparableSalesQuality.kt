package com.buysloans.hub

import kotlin.math.abs

data class ComparableSaleEvidence(
    val salePrice: Double,
    val ageDays: Int,
    val exactModelMatch: Boolean,
    val sameGeneration: Boolean,
    val sameConditionBand: Boolean,
    val verifiedSold: Boolean,
    val trustedSource: Boolean
)

data class ComparableSalesAssessment(
    val quality: Double,
    val usableCount: Int,
    val exactModelCount: Int,
    val medianPrice: Double,
    val priceSpreadPct: Double,
    val staleEvidence: Boolean,
    val reasons: List<String>
)

object ComparableSalesQuality {
    fun assess(comparables: List<ComparableSaleEvidence>): ComparableSalesAssessment {
        val usable = comparables.filter { it.salePrice > 0.0 && it.ageDays >= 0 }
        if (usable.isEmpty()) {
            return ComparableSalesAssessment(
                quality = 0.0,
                usableCount = 0,
                exactModelCount = 0,
                medianPrice = 0.0,
                priceSpreadPct = 0.0,
                staleEvidence = true,
                reasons = listOf("No usable sold comparables")
            )
        }

        val sortedPrices = usable.map { it.salePrice }.sorted()
        val median = if (sortedPrices.size % 2 == 1) {
            sortedPrices[sortedPrices.size / 2]
        } else {
            val upper = sortedPrices.size / 2
            (sortedPrices[upper - 1] + sortedPrices[upper]) / 2.0
        }

        val spread = if (median > 0.0) {
            (usable.maxOf { it.salePrice } - usable.minOf { it.salePrice }) / median
        } else 0.0

        val evidenceScores = usable.map { comparable ->
            var score = 0.0
            if (comparable.verifiedSold) score += 0.25
            if (comparable.exactModelMatch) score += 0.25
            else if (comparable.sameGeneration) score += 0.12
            if (comparable.sameConditionBand) score += 0.15
            if (comparable.trustedSource) score += 0.15
            score += when {
                comparable.ageDays <= 14 -> 0.20
                comparable.ageDays <= 30 -> 0.16
                comparable.ageDays <= 60 -> 0.10
                comparable.ageDays <= 90 -> 0.05
                else -> 0.0
            }
            score.coerceIn(0.0, 1.0)
        }

        var quality = evidenceScores.average()
        val countFactor = (usable.size / 5.0).coerceIn(0.35, 1.0)
        quality *= countFactor
        if (spread > 0.50) quality *= 0.80
        if (spread > 1.00) quality *= 0.75
        quality = quality.coerceIn(0.0, 1.0)

        val exactCount = usable.count { it.exactModelMatch }
        val stale = usable.none { it.ageDays <= 45 }
        val reasons = mutableListOf<String>()
        if (usable.size < 3) reasons += "Limited comparable sample"
        if (exactCount == 0) reasons += "No exact-model sold comparables"
        if (stale) reasons += "Comparable evidence is stale"
        if (spread > 0.50) reasons += "Comparable prices have a wide spread"
        if (usable.count { it.verifiedSold } < usable.size) reasons += "Some evidence is not verified sold data"
        if (reasons.isEmpty()) reasons += "Comparable evidence is consistent and relevant"

        return ComparableSalesAssessment(
            quality = quality,
            usableCount = usable.size,
            exactModelCount = exactCount,
            medianPrice = median,
            priceSpreadPct = abs(spread) * 100.0,
            staleEvidence = stale,
            reasons = reasons
        )
    }
}
