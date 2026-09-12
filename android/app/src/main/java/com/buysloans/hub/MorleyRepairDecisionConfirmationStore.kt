package com.buysloans.hub

import android.content.Context

object MorleyRepairDecisionConfirmationStore {
    private const val PREFS = "morley_repair_decision_confirmations"

    fun isConfirmed(context: Context, assessmentId: String): Boolean =
        assessmentId.isNotBlank() && context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean("$assessmentId.confirmed", false)

    fun confirmedDecision(context: Context, assessmentId: String): MorleyRepairDisposition? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("$assessmentId.decision", null)
            ?: return null
        return runCatching { MorleyRepairDisposition.valueOf(raw) }.getOrNull()
    }

    fun confirm(context: Context, assessmentId: String, decision: MorleyRepairDisposition) {
        require(assessmentId.isNotBlank()) { "Assessment ID is required." }
        require(decision != MorleyRepairDisposition.REVIEW_REQUIRED) {
            "A final staff disposition is required before stock preparation."
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("$assessmentId.confirmed", true)
            .putString("$assessmentId.decision", decision.name)
            .apply()
    }

    fun clear(context: Context, assessmentId: String) {
        if (assessmentId.isBlank()) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove("$assessmentId.confirmed")
            .remove("$assessmentId.decision")
            .apply()
    }
}
