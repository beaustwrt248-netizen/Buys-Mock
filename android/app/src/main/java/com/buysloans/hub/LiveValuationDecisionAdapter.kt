package com.buysloans.hub

/**
 * Converts the live Android market-result shape into the authoritative Valuation 3.0 decision.
 * Market-value selection intentionally mirrors the existing UI so this bridge does not rewrite
 * working pricing or A1932 behaviour.
 */
data class LiveValuationDecision(
    val exactNewValue: Double,
    val exactUsedValue: Double,
    val componentValue: Double,
    val marketValue: Double,
    val exactEvidence: Boolean,
    val result: CompleteValuationResult
)

object LiveValuationDecisionAdapter {
    fun evaluate(
        market: MarketResult,
        sellerAsk: Double,
        targetMarginPct: Double,
        newToUsedEstimateRate: Double
    ): LiveValuationDecision {
        val exactNew = medianValue(market.exactGoogle.map { it.price })
        val exactUsed = medianValue(market.exactEbay.map { it.price })
        val exactEvidence = market.exactGoogle.isNotEmpty() || market.exactEbay.isNotEmpty()
        val used = when {
            exactUsed > 0.0 -> exactUsed
            exactNew > 0.0 -> exactNew * newToUsedEstimateRate
            else -> 0.0
        }
        val componentValue = market.components.sumOf { it.value }
        val primary = if (exactEvidence) used else componentValue
        val sourceCount = if (exactEvidence) {
            market.exactGoogle.size + market.exactEbay.size
        } else {
            market.components.count { it.value > 0.0 }
        }
        val input = ValuationDecisionInput(
            marketValue = primary,
            sellerAsk = sellerAsk,
            targetMarginPct = targetMarginPct,
            sourceCount = sourceCount,
            comparableQuality = when {
                sourceCount >= 3 -> 1.0
                sourceCount == 2 -> 0.85
                sourceCount == 1 -> 0.70
                else -> 0.0
            },
            modelConfidence = if (exactEvidence) 1.0 else 0.45,
            identityResolved = exactEvidence
        )
        return LiveValuationDecision(
            exactNewValue = exactNew,
            exactUsedValue = used,
            componentValue = componentValue,
            marketValue = primary,
            exactEvidence = exactEvidence,
            result = CompleteValuationDecision.evaluate(input)
        )
    }

    private fun medianValue(values: List<Double>): Double {
        val sorted = values.filter { it > 0.0 }.sorted()
        if (sorted.isEmpty()) return 0.0
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2.0
    }
}