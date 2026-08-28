package com.buysloans.hub

/**
 * Consolidates Valuation 3.0 evidence into one authoritative decision pass.
 * Existing pricing remains the default: evidence may only change fields that were already
 * designed for that evidence, and target-margin guidance is opt-in.
 */
data class ConsolidatedValuationEvidence(
    val comparables: ComparableSalesAssessment? = null,
    val condition: ConditionAdjustmentResult? = null,
    val modelResolution: ModelResolution? = null,
    val repairRisk: RepairRiskAssessment? = null,
    val transactionCosts: TransactionCostAssessment? = null,
    val targetMargin: TargetMarginAssessment? = null,
    val applySuggestedTargetMargin: Boolean = false
)

data class ConsolidatedValuationResult(
    val input: ValuationDecisionInput,
    val decision: ValuationDecisionResult,
    val reasons: List<String>
)

object ValuationDecisionCoordinator {
    fun evaluate(
        input: ValuationDecisionInput,
        evidence: ConsolidatedValuationEvidence = ConsolidatedValuationEvidence()
    ): ConsolidatedValuationResult {
        var effective = input
        val reasons = mutableListOf<String>()

        evidence.comparables?.let { assessment ->
            effective = effective.copy(
                comparableQuality = assessment.quality.coerceIn(0.0, 1.0),
                sourceCount = assessment.usableCount.coerceAtLeast(0),
                staleDays = if (assessment.staleEvidence) maxOf(effective.staleDays, 46) else effective.staleDays
            )
            reasons += assessment.reasons
        }

        evidence.condition?.let { adjustment ->
            effective = effective.copy(conditionMultiplier = adjustment.multiplier.coerceIn(0.0, 1.25))
            reasons += adjustment.reasons
        }

        evidence.modelResolution?.let { resolution ->
            val resolved = resolution.status == ModelResolutionStatus.RESOLVED
            effective = effective.copy(
                modelConfidence = minOf(effective.modelConfidence.coerceIn(0.0, 1.0), resolution.confidence.coerceIn(0.0, 1.0)),
                identityResolved = effective.identityResolved && resolved
            )
            if (!resolved) reasons += resolution.reasons
        }

        evidence.repairRisk?.let { assessment ->
            effective = effective.copy(
                repairRiskAllowance = maxOf(effective.repairRiskAllowance.coerceAtLeast(0.0), assessment.suggestedAllowance.coerceAtLeast(0.0))
            )
            reasons += assessment.reasons
        }

        evidence.transactionCosts?.let { assessment ->
            effective = effective.copy(
                platformFeesPct = maxOf(effective.platformFeesPct.coerceIn(0.0, 0.75), assessment.percentageFeesPct.coerceIn(0.0, 0.75)),
                freightCost = maxOf(effective.freightCost.coerceAtLeast(0.0), assessment.fixedCostReserve.coerceAtLeast(0.0))
            )
            reasons += assessment.reasons
        }

        evidence.targetMargin?.let { assessment ->
            if (evidence.applySuggestedTargetMargin) {
                effective = effective.copy(targetMarginPct = maxOf(effective.targetMarginPct, assessment.suggestedMarginPct).coerceIn(0.0, 0.95))
            }
            reasons += assessment.reasons
        }

        var result = ValuationDecisionEngine.evaluate(effective)
        if (evidence.repairRisk?.requiresCaution == true && result.decision == ValuationDecision.BUY) {
            result = result.copy(decision = ValuationDecision.CAUTION)
        }

        val combinedReasons = (result.reasons + reasons).distinct()
        return ConsolidatedValuationResult(
            input = effective,
            decision = result.copy(reasons = combinedReasons),
            reasons = combinedReasons
        )
    }
}
