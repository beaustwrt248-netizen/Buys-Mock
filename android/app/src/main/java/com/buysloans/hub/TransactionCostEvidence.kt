package com.buysloans.hub

/**
 * Evidence-backed selling-cost estimate for Valuation 3.0.
 *
 * Percentage costs flow through the existing platformFeesPct input. Fixed selling costs
 * (freight, packaging and fixed marketplace/payment charges) flow through the existing
 * freightCost reserve so the core max-buy formula is reused rather than replaced.
 */
data class TransactionCostEvidence(
    val platformFeePct: Double = 0.0,
    val paymentFeePct: Double = 0.0,
    val fixedMarketplaceFee: Double = 0.0,
    val outboundFreightCost: Double = 0.0,
    val packagingCost: Double = 0.0
)

data class TransactionCostAssessment(
    val percentageFeesPct: Double,
    val fixedCostReserve: Double,
    val reasons: List<String>
)

object TransactionCostEvidenceEngine {
    fun assess(evidence: TransactionCostEvidence): TransactionCostAssessment {
        val platform = evidence.platformFeePct.coerceIn(0.0, 0.75)
        val payment = evidence.paymentFeePct.coerceIn(0.0, 0.75)
        val percentageFees = (platform + payment).coerceIn(0.0, 0.75)
        val fixedMarketplace = evidence.fixedMarketplaceFee.coerceAtLeast(0.0)
        val freight = evidence.outboundFreightCost.coerceAtLeast(0.0)
        val packaging = evidence.packagingCost.coerceAtLeast(0.0)
        val fixedReserve = fixedMarketplace + freight + packaging

        val reasons = mutableListOf<String>()
        if (percentageFees > 0.0) reasons += "Selling and payment percentage fees reserved"
        if (freight > 0.0) reasons += "Outbound freight reserved"
        if (packaging > 0.0) reasons += "Packaging cost reserved"
        if (fixedMarketplace > 0.0) reasons += "Fixed marketplace fee reserved"
        if (reasons.isEmpty()) reasons += "No transaction-cost evidence supplied"

        return TransactionCostAssessment(
            percentageFeesPct = percentageFees,
            fixedCostReserve = fixedReserve,
            reasons = reasons
        )
    }
}

object TransactionCostDecisionAdapter {
    fun evaluate(
        input: ValuationDecisionInput,
        assessment: TransactionCostAssessment
    ): ValuationDecisionResult {
        val evidenceAwareInput = input.copy(
            platformFeesPct = maxOf(
                input.platformFeesPct.coerceIn(0.0, 0.75),
                assessment.percentageFeesPct.coerceIn(0.0, 0.75)
            ),
            freightCost = maxOf(
                input.freightCost.coerceAtLeast(0.0),
                assessment.fixedCostReserve.coerceAtLeast(0.0)
            )
        )
        val result = ValuationDecisionEngine.evaluate(evidenceAwareInput)
        return result.copy(reasons = (result.reasons + assessment.reasons).distinct())
    }
}
