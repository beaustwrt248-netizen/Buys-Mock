package com.buysloans.hub

enum class ObservedCondition {
    NEW,
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    PARTS
}

data class ConditionEvidence(
    val observedCondition: ObservedCondition,
    val majorFaultCount: Int = 0,
    val missingAccessoryCount: Int = 0
)

data class ConditionAdjustmentResult(
    val multiplier: Double,
    val reasons: List<String>
)

object ConditionAdjustment {
    fun assess(evidence: ConditionEvidence): ConditionAdjustmentResult {
        val base = when (evidence.observedCondition) {
            ObservedCondition.NEW -> 1.05
            ObservedCondition.EXCELLENT -> 1.00
            ObservedCondition.GOOD -> 0.90
            ObservedCondition.FAIR -> 0.75
            ObservedCondition.POOR -> 0.55
            ObservedCondition.PARTS -> 0.30
        }

        val faultPenalty = (evidence.majorFaultCount.coerceAtLeast(0) * 0.10).coerceAtMost(0.35)
        val accessoryPenalty = (evidence.missingAccessoryCount.coerceAtLeast(0) * 0.05).coerceAtMost(0.15)
        val multiplier = (base - faultPenalty - accessoryPenalty).coerceIn(0.20, 1.05)

        val reasons = mutableListOf<String>()
        reasons += "Observed condition: ${evidence.observedCondition.name.lowercase()}"
        if (faultPenalty > 0.0) reasons += "Major-fault condition penalty applied"
        if (accessoryPenalty > 0.0) reasons += "Missing-accessory condition penalty applied"

        return ConditionAdjustmentResult(multiplier = multiplier, reasons = reasons)
    }

    fun evaluate(
        input: ValuationDecisionInput,
        evidence: ConditionEvidence
    ): ValuationDecisionResult {
        val adjustment = assess(evidence)
        return ValuationDecisionEngine.evaluate(input.copy(conditionMultiplier = adjustment.multiplier))
    }
}
