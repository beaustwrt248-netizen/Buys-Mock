package com.buysloans.hub

import android.content.Context
import kotlin.math.abs

data class IntegratedSourceValue(
    val source: String,
    val value: Double,
    val sampleSize: Int,
    val kind: String
)

data class IntegratedMarketValue(
    val usedValue: Double,
    val sources: List<IntegratedSourceValue>,
    val excludedSources: List<String>,
    val marketplaceEvidence: MarketplaceEvidence?,
    val confidence: String
)

object IntegratedMarketValueEngine {
    private fun median(values: List<Double>): Double {
        val sorted = values.filter { it > 0.0 }.sorted()
        if (sorted.isEmpty()) return 0.0
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2.0
    }

    fun calculate(
        ebayUsed: List<Double>,
        googleNew: List<Double>,
        marketplace: MarketplaceEvidence?,
        newToUsedRate: Double
    ): IntegratedMarketValue {
        val candidates = mutableListOf<IntegratedSourceValue>()

        val ebay = median(ebayUsed)
        if (ebay > 0.0) candidates += IntegratedSourceValue("eBay AU", ebay, ebayUsed.count { it > 0.0 }, "used")

        val google = median(googleNew)
        if (google > 0.0) candidates += IntegratedSourceValue("Google Shopping AU", google * newToUsedRate, googleNew.count { it > 0.0 }, "new-derived")

        marketplace?.let { evidence ->
            val consensus = MarketplaceConsensusEngine.calculate(evidence.gumtree + evidence.facebook)
            consensus.sources.forEach { source ->
                candidates += IntegratedSourceValue(source.source, source.median, source.sampleSize, "used-marketplace")
            }
        }

        if (candidates.isEmpty()) {
            return IntegratedMarketValue(0.0, emptyList(), emptyList(), marketplace, "UNAVAILABLE")
        }
        if (candidates.size == 1) {
            return IntegratedMarketValue(candidates.first().value, candidates, emptyList(), marketplace, "LOW")
        }

        val centre = median(candidates.map { it.value })
        val kept = candidates.filter { centre <= 0.0 || abs(it.value - centre) / centre <= 0.35 }
        val excluded = candidates.filterNot { it in kept }.map { it.source }
        val value = median(kept.map { it.value })
        val confidence = when {
            kept.size >= 4 -> "HIGH"
            kept.size >= 2 -> "MEDIUM"
            kept.size == 1 -> "LOW"
            else -> "UNAVAILABLE"
        }
        return IntegratedMarketValue(value, kept, excluded, marketplace, confidence)
    }

    suspend fun calculateLive(
        context: Context,
        query: String,
        ebayUsed: List<Double>,
        googleNew: List<Double>,
        newToUsedRate: Double
    ): IntegratedMarketValue {
        val marketplace = runCatching { searchMarketplaceEvidence(context, query) }.getOrNull()
        return calculate(ebayUsed, googleNew, marketplace, newToUsedRate)
    }
}