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

    private fun relativeDistance(value: Double, centre: Double): Double =
        if (value <= 0.0 || centre <= 0.0) Double.POSITIVE_INFINITY else abs(value - centre) / centre

    fun calculate(
        ebayUsed: List<Double>,
        googleNew: List<Double>,
        marketplace: MarketplaceEvidence?,
        newToUsedRate: Double
    ): IntegratedMarketValue {
        val candidates = mutableListOf<IntegratedSourceValue>()

        val ebay = median(ebayUsed)
        val ebayCount = ebayUsed.count { it > 0.0 }
        if (ebay > 0.0) candidates += IntegratedSourceValue("eBay AU", ebay, ebayCount, "used")

        val google = median(googleNew)
        val googleCount = googleNew.count { it > 0.0 }
        if (google > 0.0) candidates += IntegratedSourceValue("Google Shopping AU", google * newToUsedRate, googleCount, "new-derived")

        marketplace?.let { evidence ->
            val consensus = MarketplaceConsensusEngine.calculate(evidence.gumtree + evidence.facebook)
            consensus.sources.forEach { source ->
                candidates += IntegratedSourceValue(source.source, source.median, source.sampleSize, "used-marketplace")
            }
        }

        if (candidates.isEmpty()) {
            return IntegratedMarketValue(0.0, emptyList(), emptyList(), marketplace, "UNAVAILABLE")
        }

        val strongUsed = candidates.filter { it.kind == "used" && it.sampleSize >= 3 }
        val strongAny = candidates.filter { it.sampleSize >= 2 }
        val anchorPool = when {
            strongUsed.isNotEmpty() -> strongUsed
            strongAny.isNotEmpty() -> strongAny
            else -> candidates
        }
        val anchor = median(anchorPool.map { it.value })

        val kept = candidates.filter { source ->
            when {
                source.value <= 0.0 -> false
                source in anchorPool -> true
                source.sampleSize <= 1 && anchor > 0.0 -> relativeDistance(source.value, anchor) <= 0.25
                anchor > 0.0 -> relativeDistance(source.value, anchor) <= 0.35
                else -> true
            }
        }

        val effective = if (kept.isNotEmpty()) kept else anchorPool
        val excluded = candidates.filterNot { it in effective }.map { it.source }
        val weighted = effective.flatMap { source ->
            val weight = when {
                source.kind == "used" && source.sampleSize >= 3 -> 3
                source.sampleSize >= 2 -> 2
                else -> 1
            }
            List(weight) { source.value }
        }
        val value = median(weighted)

        val strongSourceCount = effective.count { it.sampleSize >= 2 }
        val confidence = when {
            strongSourceCount >= 3 -> "HIGH"
            strongSourceCount >= 2 -> "MEDIUM"
            strongSourceCount == 1 && effective.any { it.kind == "used" && it.sampleSize >= 3 } -> "MEDIUM"
            effective.isNotEmpty() -> "LOW"
            else -> "UNAVAILABLE"
        }

        return IntegratedMarketValue(value, effective, excluded, marketplace, confidence)
    }

    suspend fun calculateLive(
        context: Context,
        query: String,
        ebayUsed: List<Double>,
        googleNew: List<Double>,
        newToUsedRate: Double
    ): IntegratedMarketValue {
        val resolved = runCatching { LaptopModelCatalog.resolve(context, query) }.getOrNull()
        val marketQuery = resolved?.canonicalQuery?.takeIf { it.isNotBlank() } ?: query
        val marketplace = runCatching { searchMarketplaceEvidence(context, marketQuery) }.getOrNull()
        return calculate(ebayUsed, googleNew, marketplace, newToUsedRate)
    }
}
