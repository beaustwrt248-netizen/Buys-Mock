package com.buysloans.hub

/**
 * Transitional Android adapter for the older inline Device Lens repair screen.
 *
 * It deliberately does NOT calculate a commercial recommendation. MorleyAssessmentCore is the
 * canonical Repair-or-Buy rules engine; Android only carries staff-entered evidence while the
 * dedicated MorleyRepairDecisionActivity enforces explicit staff confirmation before stock.
 */
object MorleyRepairDecisionPolicy {
    @Suppress("UNUSED_PARAMETER")
    fun evaluate(
        buyCost: Double?,
        resaleAsIs: Double?,
        resaleAfterRepair: Double?,
        repairCost: Double?,
        partsRecoveryValue: Double?,
        partsProcessingCost: Double?,
        minMargin: Double,
        repairDays: Double?
    ): MorleyRepairDecisionState =
        MorleyRepairDecisionState.awaitingCanonicalRecommendation().copy(
            repairCost = repairCost?.takeIf { it.isFinite() && it >= 0.0 },
            repairDays = repairDays?.takeIf { it.isFinite() && it >= 0.0 }
        )
}
