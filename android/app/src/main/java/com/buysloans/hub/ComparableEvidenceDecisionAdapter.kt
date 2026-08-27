package com.buysloans.hub

/**
 * Connects comparable-sales evidence quality to the existing decision engine without
 * changing market value, condition, fee, freight, repair-risk, target-margin, or max-buy math.
 */
object ComparableEvidenceDecisionAdapter {
    fun evaluate(
        input: ValuationDecisionInput,
        assessment: ComparableSalesAssessment
    ): ValuationDecisionResult {
        val evidenceAwareInput = input.copy(
            comparableQuality = assessment.quality.coerceIn(0.0, 1.0),
            sourceCount = assessment.usableCount.coerceAtLeast(0),
            staleDays = if (assessment.staleEvidence) maxOf(input.staleDays, 46) else input.staleDays
        )
        return ValuationDecisionEngine.evaluate(evidenceAwareInput)
    }
}
