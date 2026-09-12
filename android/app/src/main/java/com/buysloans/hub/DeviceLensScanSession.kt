package com.buysloans.hub

import android.content.Context

object DeviceLensScanSession {
    private const val PREFS = "morley_device_lens_session"
    private const val KEY_ACTIVE = "active_assessment_id"

    @Synchronized
    fun begin(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_ACTIVE, null)?.takeIf { it.isNotBlank() }
        if (existing != null) return existing
        val id = DeviceAssessmentStore.beginLocalScan(context.applicationContext)
        prefs.edit().putString(KEY_ACTIVE, id).apply()
        return id
    }

    @Synchronized
    fun resume(context: Context, assessmentId: String): String {
        require(assessmentId.isNotBlank()) { "Assessment ID is required." }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACTIVE, assessmentId)
            .apply()
        return assessmentId
    }

    fun current(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ACTIVE, null)
            ?.takeIf { it.isNotBlank() }

    @Synchronized
    fun finishLocalSession(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ACTIVE)
            .apply()
    }
}
