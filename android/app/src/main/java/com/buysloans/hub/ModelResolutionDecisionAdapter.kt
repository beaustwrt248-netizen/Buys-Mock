package com.buysloans.hub

/**
 * Connects model/year/generation resolution evidence to the existing decision engine.
 *
 * The adapter can only preserve or reduce confidence/identity certainty. It deliberately
 * leaves market value, condition, fees, freight, repair allowance, target margin and
 * max-buy calculations to ValuationDecisionEngine unchanged.
 */
object ModelResolutionDecisionAdapter {
    fun evaluate(
        input: ValuationDecisionInput,
        resolution: ModelResolution
    ): ValuationDecisionResult {
        val resolutionIsCertain = resolution.status == ModelResolutionStatus.RESOLVED
        val evidenceAwareInput = input.copy(
            modelConfidence = minOf(
                input.modelConfidence.coerceIn(0.0, 1.0),
                resolution.confidence.coerceIn(0.0, 1.0)
            ),
            identityResolved = input.identityResolved && resolutionIsCertain
        )

        val result = ValuationDecisionEngine.evaluate(evidenceAwareInput)
        val resolutionReasons = if (resolutionIsCertain) emptyList() else resolution.reasons
        return result.copy(reasons = (result.reasons + resolutionReasons).distinct())
    }
}
