package com.buysloans.hub

import kotlin.math.abs

enum class ValuationDecision { BUY, CAUTION, PASS }

data class ValuationDecisionInput(
    val marketValue: Double,
    val sellerAsk: Double,
    val targetMarginPct: Double,
    val platformFeesPct: Double = 0.0,
    val freightCost: Double = 0.0,
    val repairRiskAllowance: Double = 0.0,
    val conditionMultiplier: Double = 1.0,
    val comparableQuality: Double = 1.0,
    val modelConfidence: Double = 1.0,
    val sourceCount: Int = 1,
    val staleDays: Int = 0,
    val priceChangePct: Double = 0.0
)

data class ValuationDecisionResult(
    val decision: ValuationDecision,
    val confidence: Double,
    val adjustedResale: Double,
    val maxBuyPrice: Double,
    val expectedProfitAtAsk: Double,
    val expectedMarginAtAsk: Double,
    val reasons: List<String>
)

object ValuationDecisionEngine {
    fun evaluate(input: ValuationDecisionInput): ValuationDecisionResult {
        val market = input.marketValue.coerceAtLeast(0.0)
        val ask = input.sellerAsk.coerceAtLeast(0.0)
        val condition = input.conditionMultiplier.coerceIn(0.0, 1.25)
        val fees = input.platformFeesPct.coerceIn(0.0, 0.75)
        val targetMargin = input.targetMarginPct.coerceIn(0.0, 0.95)
        val adjustedResale = market * condition
        val netBeforeBuy = (adjustedResale * (1.0 - fees) - input.freightCost.coerceAtLeast(0.0) - input.repairRiskAllowance.coerceAtLeast(0.0)).coerceAtLeast(0.0)
        val maxBuy = (netBeforeBuy * (1.0 - targetMargin)).coerceAtLeast(0.0)
        val expectedProfit = netBeforeBuy - ask
        val expectedMargin = if (netBeforeBuy > 0.0) expectedProfit / netBeforeBuy else 0.0

        val freshness = (1.0 - input.staleDays.coerceAtLeast(0) / 90.0).coerceIn(0.0, 1.0)
        val sourceStrength = (input.sourceCount.coerceAtLeast(0) / 5.0).coerceIn(0.0, 1.0)
        val confidence = (
            input.comparableQuality.coerceIn(0.0, 1.0) * 0.35 +
            input.modelConfidence.coerceIn(0.0, 1.0) * 0.35 +
            sourceStrength * 0.20 +
            freshness * 0.10
        ).coerceIn(0.0, 1.0)

        val reasons = mutableListOf<String>()
        if (confidence < 0.60) reasons += "Low valuation confidence"
        if (input.staleDays > 45) reasons += "Valuation evidence is stale"
        if (abs(input.priceChangePct) > 20.0) reasons += "Market price moved materially"
        if (input.repairRiskAllowance > 0.0) reasons += "Repair-risk allowance applied"
        if (ask > maxBuy && maxBuy > 0.0) reasons += "Seller ask exceeds max-buy guidance"
        if (netBeforeBuy <= 0.0) reasons += "Costs consume the resale value"

        val decision = when {
            netBeforeBuy <= 0.0 || maxBuy <= 0.0 -> ValuationDecision.PASS
            ask > maxBuy * 1.15 -> ValuationDecision.PASS
            confidence < 0.60 || input.staleDays > 45 || abs(input.priceChangePct) > 20.0 || ask > maxBuy -> ValuationDecision.CAUTION
            else -> ValuationDecision.BUY
        }

        if (reasons.isEmpty()) reasons += "Evidence and margin are within configured limits"
        return ValuationDecisionResult(
            decision = decision,
            confidence = confidence,
            adjustedResale = adjustedResale,
            maxBuyPrice = maxBuy,
            expectedProfitAtAsk = expectedProfit,
            expectedMarginAtAsk = expectedMargin,
            reasons = reasons
        )
    }
}
