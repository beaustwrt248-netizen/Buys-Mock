package com.buysloans.hub

data class RepairRiskItem(
    val label: String,
    val estimatedRepairCost: Double,
    val likelihood: Double,
    val diagnosticConfidence: Double = 1.0
)

data class RepairRiskAssessment(
    val suggestedAllowance: Double,
    val riskScore: Double,
    val requiresCaution: Boolean,
    val reasons: List<String>
)

object RepairRiskEvidence {
    fun assess(items: List<RepairRiskItem>): RepairRiskAssessment {
        if (items.isEmpty()) {
            return RepairRiskAssessment(
                suggestedAllowance = 0.0,
                riskScore = 0.0,
                requiresCaution = false,
                reasons = listOf("No repair-risk evidence recorded")
            )
        }

        val normalized = items.map { item ->
            val cost = item.estimatedRepairCost.coerceAtLeast(0.0)
            val likelihood = item.likelihood.coerceIn(0.0, 1.0)
            val confidence = item.diagnosticConfidence.coerceIn(0.0, 1.0)
            Triple(cost, likelihood, confidence)
        }

        val expectedAllowance = normalized.sumOf { (cost, likelihood, _) -> cost * likelihood }
        val totalCost = normalized.sumOf { (cost, _, _) -> cost }
        val weightedLikelihood = if (totalCost > 0.0) {
            normalized.sumOf { (cost, likelihood, _) -> cost * likelihood } / totalCost
        } else 0.0
        val weightedConfidence = if (totalCost > 0.0) {
            normalized.sumOf { (cost, _, confidence) -> cost * confidence } / totalCost
        } else 1.0

        val riskScore = (weightedLikelihood * 0.7 + (1.0 - weightedConfidence) * 0.3).coerceIn(0.0, 1.0)
        val reasons = mutableListOf<String>()
        if (expectedAllowance > 0.0) reasons += "Expected repair allowance derived from observed risks"
        if (weightedLikelihood >= 0.60) reasons += "Repair likelihood is elevated"
        if (weightedConfidence < 0.60) reasons += "Repair diagnosis confidence is limited"
        if (reasons.isEmpty()) reasons += "Recorded repair risks are low"

        return RepairRiskAssessment(
            suggestedAllowance = expectedAllowance,
            riskScore = riskScore,
            requiresCaution = weightedLikelihood >= 0.60 || weightedConfidence < 0.60,
            reasons = reasons
        )
    }
}
