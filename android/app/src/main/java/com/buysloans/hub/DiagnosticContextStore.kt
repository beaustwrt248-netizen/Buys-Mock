package com.buysloans.hub

import android.content.Context

/**
 * Keeps a deliberately small local breadcrumb for support diagnostics.
 * SupportTicketActivity is ignored so opening the report form does not
 * overwrite the screen the user was actually using when the problem occurred.
 */
object DiagnosticContextStore {
    private const val PREFS = "guardian_diagnostic_context"
    private const val KEY_LAST_SCREEN = "last_screen"
    private const val KEY_LAST_SCREEN_AT = "last_screen_at"

    data class Snapshot(
        val screen: String,
        val capturedAt: Long
    )

    fun recordActivity(context: Context, screen: String, capturedAt: Long = System.currentTimeMillis()) {
        if (!shouldRecord(screen)) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_SCREEN, screen)
            .putLong(KEY_LAST_SCREEN_AT, capturedAt)
            .apply()
    }

    fun snapshot(context: Context): Snapshot? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val screen = prefs.getString(KEY_LAST_SCREEN, null)?.trim().orEmpty()
        if (screen.isBlank()) return null
        return Snapshot(screen = screen, capturedAt = prefs.getLong(KEY_LAST_SCREEN_AT, 0L))
    }

    internal fun shouldRecord(screen: String): Boolean =
        screen.isNotBlank() && screen != SupportTicketActivity::class.java.simpleName
}
