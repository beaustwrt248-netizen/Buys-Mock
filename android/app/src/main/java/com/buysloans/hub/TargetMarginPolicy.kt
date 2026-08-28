package com.buysloans.hub

/**
 * Evidence-aware target-margin guidance for Valuation 3.0.
 *
 * This policy is deliberately opt-in: callers keep the existing configured target margin
 * unless they explicitly supply evidence and choose the suggested value. That preserves
 * current pricing (including A1932) while giving the consolidated decision layer a bounded,
 * explainable margin recommendation.
 */
data class TargetMarginEvidence(
    val configuredMarginPct: Double,
    val confidence: Double = 1.0,
    val repairRiskScore: Double = 0.0,
    val staleEvidence: Boolean = false,
    val identityResolved: Boolean = true
)

data class TargetMarginAssessment(
    val configuredMarginPct: Double,
    val suggestedMarginPct: Double,
    val reasons: List<String>
)

object TargetMarginPolicy {
    fun assess(evidence: TargetMarginEvidence): TargetMarginAssessment {
        val configured = evidence.configuredMarginPct.coerceIn(0.0, 0.95)
        var riskPremium = 0.0
        val reasons = mutableListOf<String>()

        val confidence = evidence.confidence.coerceIn(0.0, 1.0)
        if (confidence < 0.60) {
            riskPremium += 0.05
            reasons += "Low confidence supports a higher margin reserve"
        }
        val repairRisk = evidence.repairRiskScore.coerceIn(0.0, 1.0)
        if (repairRisk >= 0.60) {
            riskPremium += 0.05
            reasons += "Elevated repair risk supports a higher margin reserve"
        }
        if (evidence.staleEvidence) {
            riskPremium += 0.025
            reasons += "Stale evidence supports a higher margin reserve"
        }
        if (!evidence.identityResolved) {
            riskPremium += 0.05
            reasons += "Unresolved identity supports a higher margin reserve"
        }

        // Never recommend lowering the existing configured margin, and cap the additive
        // evidence premium so one weak signal cannot make pricing unusably conservative.
        val suggested = (configured + riskPremium.coerceAtMost(0.15)).coerceAtMost(0.95)
        if (reasons.isEmpty()) reasons += "Configured target margin remains appropriate"

        return TargetMarginAssessment(configured, suggested, reasons)
    }
}
