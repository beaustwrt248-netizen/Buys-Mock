package com.buysloans.hub

/**
 * Connects repair-risk evidence to the existing decision engine while preserving any
 * larger manually supplied allowance. Repair uncertainty may downgrade BUY to CAUTION,
 * but never upgrades an existing CAUTION or PASS outcome.
 */
object RepairRiskDecisionAdapter {
    fun evaluate(
        input: ValuationDecisionInput,
        assessment: RepairRiskAssessment
    ): ValuationDecisionResult {
        val evidenceAwareInput = input.copy(
            repairRiskAllowance = maxOf(
                input.repairRiskAllowance.coerceAtLeast(0.0),
                assessment.suggestedAllowance.coerceAtLeast(0.0)
            )
        )
        val result = ValuationDecisionEngine.evaluate(evidenceAwareInput)
        if (!assessment.requiresCaution || result.decision != ValuationDecision.BUY) {
            return result
        }

        val extraReasons = assessment.reasons.filterNot { it in result.reasons }
        return result.copy(
            decision = ValuationDecision.CAUTION,
            reasons = result.reasons + extraReasons
        )
    }
}
