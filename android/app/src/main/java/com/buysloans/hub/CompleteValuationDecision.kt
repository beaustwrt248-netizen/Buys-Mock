package com.buysloans.hub

/** Final Valuation 3.0 orchestration layer. Financial math remains owned by the consolidated pass. */
data class CompleteValuationResult(
    val consolidated: ConsolidatedValuationResult,
    val deal: DealAwareValuationResult?,
    val decision: ValuationDecision,
    val reasons: List<String>
)

object CompleteValuationDecision {
    fun evaluate(
        input: ValuationDecisionInput,
        evidence: ConsolidatedValuationEvidence = ConsolidatedValuationEvidence(),
        deal: SavedDealSignalInput? = null,
        peerDeals: List<SavedDealSignalInput> = emptyList()
    ): CompleteValuationResult {
        val consolidated = ValuationDecisionCoordinator.evaluate(input, evidence)
        val dealResult = deal?.let {
            DealIntelligenceDecisionAdapter.evaluate(consolidated.input, it, peerDeals)
        }

        val finalDecision = when {
            consolidated.decision.decision == ValuationDecision.PASS || dealResult?.decision == ValuationDecision.PASS -> ValuationDecision.PASS
            consolidated.decision.decision == ValuationDecision.CAUTION || dealResult?.decision == ValuationDecision.CAUTION -> ValuationDecision.CAUTION
            else -> ValuationDecision.BUY
        }

        val reasons = (consolidated.reasons + (dealResult?.reasons ?: emptyList())).distinct()
        return CompleteValuationResult(consolidated, dealResult, finalDecision, reasons)
    }
}
