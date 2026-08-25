package com.buysloans.hub

import kotlin.math.abs

data class MarketplaceSourceValue(
    val source: String,
    val median: Double,
    val sampleSize: Int
)

data class MarketplaceConsensus(
    val value: Double,
    val sources: List<MarketplaceSourceValue>,
    val excludedSources: List<String>,
    val confidence: String
)

object MarketplaceConsensusEngine {
    private fun median(values: List<Double>): Double {
        val s = values.filter { it > 0.0 }.sorted()
        if (s.isEmpty()) return 0.0
        val m = s.size / 2
        return if (s.size % 2 == 1) s[m] else (s[m - 1] + s[m]) / 2.0
    }

    fun calculate(exactListings: List<MarketplaceListing>): MarketplaceConsensus {
        val grouped = exactListings
            .filter { it.exact && it.price > 0.0 }
            .groupBy { it.source }
            .mapValues { (_, items) -> items.map { it.price } }

        val raw = grouped.map { (source, prices) ->
            MarketplaceSourceValue(source, median(prices), prices.size)
        }.filter { it.median > 0.0 }

        if (raw.isEmpty()) return MarketplaceConsensus(0.0, emptyList(), emptyList(), "UNAVAILABLE")
        if (raw.size == 1) return MarketplaceConsensus(raw.first().median, raw, emptyList(), "LOW")

        val centre = median(raw.map { it.median })
        // Source-level guard: a marketplace source must sit within 35% of the cross-source centre.
        // This prevents one bad indexed listing/source from dragging the buying target.
        val kept = raw.filter { centre <= 0.0 || abs(it.median - centre) / centre <= 0.35 }
        val excluded = raw.filterNot { it in kept }.map { it.source }
        val value = median(kept.map { it.median })
        val confidence = when {
            kept.size >= 3 -> "HIGH"
            kept.size == 2 -> "MEDIUM"
            kept.size == 1 -> "LOW"
            else -> "UNAVAILABLE"
        }
        return MarketplaceConsensus(value, kept, excluded, confidence)
    }
}