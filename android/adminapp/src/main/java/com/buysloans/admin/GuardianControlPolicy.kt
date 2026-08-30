package com.buysloans.admin

import org.json.JSONObject

internal data class GuardianControlDraft(
    val enabled: Boolean,
    val autoFixEnabled: Boolean,
    val maxAutoRisk: String,
    val operatingMode: String,
    val learningEnabled: Boolean,
    val evolutionEnabled: Boolean,
    val confidenceThreshold: Double,
    val maxParallelRepairs: Int,
    val quarantineOnRepeatedFailure: Boolean,
    val killSwitch: Boolean,
    val killSwitchReason: String,
)

internal fun validateGuardianControlDraft(draft: GuardianControlDraft, role: String, currentKillSwitch: Boolean): String? {
    if (role !in setOf("admin", "manager")) return "Guardian controls require Admin or Manager access."
    if (draft.maxAutoRisk !in setOf("low", "medium")) return "Maximum automatic risk must be low or medium."
    if (draft.operatingMode !in setOf("observe", "assist", "guarded_auto")) return "Guardian operating mode is invalid."
    if (draft.confidenceThreshold !in 0.500..0.999) return "Confidence threshold must be between 0.500 and 0.999."
    if (draft.maxParallelRepairs !in 1..5) return "Parallel repair limit must be between 1 and 5."
    if (draft.killSwitch && draft.killSwitchReason.trim().length < 3) return "Enter a reason before engaging the Guardian kill switch."
    if (currentKillSwitch && !draft.killSwitch && role != "admin") return "Only an Admin can disengage the Guardian kill switch."
    return null
}

internal fun guardianControlPayload(draft: GuardianControlDraft): JSONObject = JSONObject()
    .put("p_enabled", if (draft.killSwitch) false else draft.enabled)
    .put("p_auto_fix_enabled", if (draft.killSwitch) false else draft.autoFixEnabled)
    .put("p_max_auto_risk", draft.maxAutoRisk)
    .put("p_operating_mode", draft.operatingMode)
    .put("p_learning_enabled", draft.learningEnabled)
    .put("p_evolution_enabled", draft.evolutionEnabled)
    .put("p_confidence_threshold", draft.confidenceThreshold)
    .put("p_max_parallel_repairs", draft.maxParallelRepairs)
    .put("p_quarantine_on_repeated_failure", draft.quarantineOnRepeatedFailure)
    .put("p_kill_switch", draft.killSwitch)
    .put("p_kill_switch_reason", draft.killSwitchReason.trim().take(500).ifBlank { JSONObject.NULL })
