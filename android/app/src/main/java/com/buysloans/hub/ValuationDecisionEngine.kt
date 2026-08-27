package com.buysloans.hub

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

enum class ValuationOutcome { BUY, CAUTION, PASS }
enum class ComparableQuality { STRONG, FAIR, WEAK, UNAVAILABLE }

data class ValuationDecisionInput(
    val marketValue: Double,
    val askingPrice: Double,
    val exactComparableCount: Int = 0,
    val similarComparableCount: Int = 0,
    val comparablePriceSpread: Double = 0.0,
    val conditionMultiplier: Double = 1.0,
    val modelResolved: Boolean = true,
    val generationResolved: Boolean = true,
    val repairRiskAllowance: Double = 0.0,
    val freightCost: Double = 0.0,
    val sellingFeeRate: Double = 0.0,
    val fixedSellingFees: Double = 0.0,
    val targetMarginRate: Double,
    val originalMarketValue: Double? = null,
    val valuationAgeDays: Int = 0
)

data class ValuationDecision(
    val outcome: ValuationOutcome,
    val confidenceScore: Int,
    val comparableQuality: ComparableQuality,
    val adjustedMarketValue: Double,
    val expectedFees: Double,
    val targetProfit: Double,
    val maxBuyPrice: Double,
    val expectedProfitAtAsk: Double,
    val expectedMarginAtAsk: Double,
    val marketMovementRate: Double?,
    val reasons: List<String>
)

data class SavedDealSignalInput(
    val id: String,
    val normalizedIdentity: String,
    val askingPrice: Double?,
    val originalMarketValue: Double?,
    val currentMarketValue: Double?,
    val maxBuyPrice: Double?,
    val createdAgeDays: Int,
    val expectedMarginRate: Double?
)

data class DealIntelligence(
    val underpricedOpportunityIds: Set<String>,
    val staleValuationIds: Set<String>,
    val unusuallyHighMarginIds: Set<String>,
    val duplicateGroups: List<Set<String>>,
    val meaningfulMarketMovementIds: Set<String>
)

object ValuationDecisionEngine {
    private fun clamp01(value: Double) = value.coerceIn(0.0, 1.0)

    fun comparableQuality(input: ValuationDecisionInput): ComparableQuality = when {
        input.exactComparableCount >= 5 && input.comparablePriceSpread <= 0.20 -> ComparableQuality.STRONG
        input.exactComparableCount >= 3 && input.comparablePriceSpread <= 0.35 -> ComparableQuality.FAIR
        input.exactComparableCount >= 1 || input.similarComparableCount >= 3 -> ComparableQuality.WEAK
        else -> ComparableQuality.UNAVAILABLE
    }

    fun confidenceScore(input: ValuationDecisionInput): Int {
        var score = 100
        when (comparableQuality(input)) {
            ComparableQuality.STRONG -> Unit
            ComparableQuality.FAIR -> score -= 12
            ComparableQuality.WEAK -> score -= 28
            ComparableQuality.UNAVAILABLE -> score -= 50
        }
        if (!input.modelResolved) score -= 20
        if (!input.generationResolved) score -= 15
        if (input.comparablePriceSpread > 0.50) score -= 12
        else if (input.comparablePriceSpread > 0.35) score -= 6
        if (input.valuationAgeDays > 30) score -= 8
        if (input.valuationAgeDays > 90) score -= 8
        return score.coerceIn(0, 100)
    }

    fun decide(input: ValuationDecisionInput): ValuationDecision {
        require(input.marketValue >= 0.0) { "Market value cannot be negative." }
        require(input.askingPrice >= 0.0) { "Asking price cannot be negative." }
        require(input.targetMarginRate in 0.0..0.95) { "Target margin must be between 0% and 95%." }
        require(input.sellingFeeRate in 0.0..0.95) { "Selling fee rate must be between 0% and 95%." }
        require(input.conditionMultiplier in 0.0..1.25) { "Condition multiplier is outside the supported range." }

        val adjustedMarket = input.marketValue * input.conditionMultiplier
        val expectedFees = adjustedMarket * input.sellingFeeRate + max(0.0, input.fixedSellingFees)
        val targetProfit = adjustedMarket * input.targetMarginRate
        val maxBuy = max(
            0.0,
            adjustedMarket - expectedFees - max(0.0, input.freightCost) -
                max(0.0, input.repairRiskAllowance) - targetProfit
        )
        val expectedProfit = adjustedMarket - expectedFees - max(0.0, input.freightCost) -
            max(0.0, input.repairRiskAllowance) - input.askingPrice
        val expectedMargin = if (adjustedMarket > 0.0) expectedProfit / adjustedMarket else 0.0
        val confidence = confidenceScore(input)
        val quality = comparableQuality(input)
        val marketMovement = input.originalMarketValue
            ?.takeIf { it > 0.0 }
            ?.let { (adjustedMarket - it) / it }

        val reasons = mutableListOf<String>()
        if (!input.modelResolved || !input.generationResolved) reasons += "Model/year/generation still needs confirmation."
        if (quality == ComparableQuality.WEAK || quality == ComparableQuality.UNAVAILABLE) reasons += "Comparable-sales evidence is limited."
        if (input.repairRiskAllowance > 0.0) reasons += "Repair-risk allowance reduces the safe buy price."
        if (input.valuationAgeDays > 30) reasons += "Valuation is stale and should be refreshed."
        if (marketMovement != null && abs(marketMovement) >= 0.10) reasons += "Market value has moved materially since the original valuation."
        if (input.askingPrice > maxBuy) reasons += "Seller ask is above the target-margin max-buy price."

        val outcome = when {
            adjustedMarket <= 0.0 || input.askingPrice > maxBuy -> ValuationOutcome.PASS
            confidence < 55 || !input.modelResolved || !input.generationResolved || quality == ComparableQuality.UNAVAILABLE -> ValuationOutcome.CAUTION
            confidence < 75 || quality == ComparableQuality.WEAK || input.askingPrice >= maxBuy * 0.92 -> ValuationOutcome.CAUTION
            else -> ValuationOutcome.BUY
        }

        if (outcome == ValuationOutcome.BUY) reasons += "Ask is safely inside the target-margin buy ceiling with usable evidence."
        if (outcome == ValuationOutcome.CAUTION && reasons.isEmpty()) reasons += "Deal is inside max-buy but has limited safety buffer."

        return ValuationDecision(
            outcome = outcome,
            confidenceScore = confidence,
            comparableQuality = quality,
            adjustedMarketValue = adjustedMarket,
            expectedFees = expectedFees,
            targetProfit = targetProfit,
            maxBuyPrice = maxBuy,
            expectedProfitAtAsk = expectedProfit,
            expectedMarginAtAsk = clamp01(expectedMargin),
            marketMovementRate = marketMovement,
            reasons = reasons
        )
    }

    fun analyseDeals(
        deals: List<SavedDealSignalInput>,
        staleAfterDays: Int = 30,
        highMarginThreshold: Double = 0.55,
        marketMovementThreshold: Double = 0.10
    ): DealIntelligence {
        val underpriced = deals.filter { deal ->
            val ask = deal.askingPrice
            val maxBuy = deal.maxBuyPrice
            ask != null && maxBuy != null && maxBuy > 0.0 && ask <= maxBuy * 0.80
        }.mapTo(linkedSetOf()) { it.id }

        val stale = deals.filter { it.createdAgeDays >= staleAfterDays }.mapTo(linkedSetOf()) { it.id }
        val highMargin = deals.filter { (it.expectedMarginRate ?: 0.0) >= highMarginThreshold }.mapTo(linkedSetOf()) { it.id }

        val duplicateGroups = deals
            .filter { it.normalizedIdentity.isNotBlank() }
            .groupBy { it.normalizedIdentity.trim().lowercase() }
            .values
            .filter { it.size > 1 }
            .map { group -> group.mapTo(linkedSetOf()) { it.id } }

        val movement = deals.filter { deal ->
            val original = deal.originalMarketValue ?: return@filter false
            val current = deal.currentMarketValue ?: return@filter false
            original > 0.0 && abs(current - original) / original >= marketMovementThreshold
        }.mapTo(linkedSetOf()) { it.id }

        return DealIntelligence(underpriced, stale, highMargin, duplicateGroups, movement)
    }
}
