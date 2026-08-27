package com.buysloans.hub

/**
 * Combines the existing valuation decision with additive saved-deal intelligence.
 * Financial calculations are never rewritten here: max-buy, resale, profit, fees,
 * freight, repair allowance and target-margin math remain owned by ValuationDecisionEngine.
 */
data class DealAwareValuationResult(
    val decision: ValuationDecision,
    val base: ValuationDecisionResult,
    val underpricedOpportunity: Boolean,
    val staleValuation: Boolean,
    val unusuallyHighMargin: Boolean,
    val duplicateListing: Boolean,
    val meaningfulMarketMovement: Boolean,
    val reasons: List<String>
)

object DealIntelligenceDecisionAdapter {
    fun evaluate(
        input: ValuationDecisionInput,
        deal: SavedDealSignalInput,
        peerDeals: List<SavedDealSignalInput> = emptyList()
    ): DealAwareValuationResult {
        val base = ValuationDecisionEngine.evaluate(input)
        val allDeals = (peerDeals + deal).distinctBy { it.id }
        val intelligence = DealIntelligenceEngine.analyse(allDeals)
        val id = deal.id
        val duplicate = intelligence.duplicateGroups.any { id in it }
        val stale = id in intelligence.staleValuationIds
        val movement = id in intelligence.meaningfulMarketMovementIds
        val underpriced = id in intelligence.underpricedOpportunityIds
        val highMargin = id in intelligence.unusuallyHighMarginIds

        val reasons = base.reasons.toMutableList()
        if (underpriced) reasons += "Deal is materially below max-buy guidance"
        if (highMargin) reasons += "Expected margin is unusually high"
        if (duplicate) reasons += "Duplicate saved listing identity requires review"
        if (stale) reasons += "Saved valuation is stale and should be refreshed"
        if (movement) reasons += "Market value moved materially since the saved valuation"

        val decision = when {
            base.decision == ValuationDecision.PASS -> ValuationDecision.PASS
            duplicate || stale || movement -> ValuationDecision.CAUTION
            else -> base.decision
        }

        return DealAwareValuationResult(
            decision = decision,
            base = base,
            underpricedOpportunity = underpriced,
            staleValuation = stale,
            unusuallyHighMargin = highMargin,
            duplicateListing = duplicate,
            meaningfulMarketMovement = movement,
            reasons = reasons.distinct()
        )
    }
}
