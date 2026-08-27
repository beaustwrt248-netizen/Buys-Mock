package com.buysloans.admin

import org.json.JSONArray
import org.json.JSONObject

internal data class MaintenanceConfig(
    val enabled: Boolean,
    val message: String,
    val fullFeatureFlags: JSONObject
)

internal fun maintenanceConfig(config: JSONArray?): MaintenanceConfig? {
    if (config == null) return null
    for (i in 0 until config.length()) {
        val row = config.optJSONObject(i) ?: continue
        if (row.optString("key") != "feature_flags") continue
        val flags = row.optJSONObject("value") ?: return null
        return MaintenanceConfig(
            enabled = flags.optBoolean("maintenanceMode", false),
            message = flags.optString("maintenanceMessage").take(160),
            fullFeatureFlags = JSONObject(flags.toString())
        )
    }
    return null
}

internal fun maintenanceUpdatePayload(current: MaintenanceConfig, enabled: Boolean, message: String): JSONObject {
    val clean = message.trim().take(160)
    val flags = JSONObject(current.fullFeatureFlags.toString())
        .put("maintenanceMode", enabled)
        .put("maintenanceMessage", clean)
    return JSONObject()
        .put("config_key", "feature_flags")
        .put("config_value", flags)
}
