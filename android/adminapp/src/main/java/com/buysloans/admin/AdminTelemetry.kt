package com.buysloans.admin

import android.content.Context
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

internal data class AdminErrorEvent(
    val appVersion: String,
    val deviceModel: String,
    val failingScreen: String,
    val errorClass: String,
    val occurredAt: String
) {
    fun toJson(): JSONObject = JSONObject()
        .put("app_version", appVersion)
        .put("device_model", deviceModel)
        .put("failing_screen", failingScreen)
        .put("error_class", errorClass)
        .put("occurred_at", occurredAt)
}

internal object AdminTelemetry {
    private const val PREFS = "admin_health_telemetry"
    private const val KEY_EVENTS = "pending_events"
    private const val MAX_EVENTS = 20
    internal const val UNCAUGHT_SCREEN = "Uncaught/Admin"
    private val crashHandlerInstalled = AtomicBoolean(false)

    fun installCrashHandler(context: Context) {
        if (!crashHandlerInstalled.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { record(appContext, UNCAUGHT_SCREEN, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun record(context: Context, screen: String, throwable: Throwable) {
        val event = AdminErrorEvent(
            appVersion = clean(BuildConfig.VERSION_NAME, 40, "unknown"),
            deviceModel = clean("${Build.MANUFACTURER} ${Build.MODEL}".trim(), 80, "unknown"),
            failingScreen = clean(screen, 80, "Admin"),
            errorClass = clean(throwable.javaClass.simpleName.ifBlank { "Throwable" }, 100, "Throwable"),
            occurredAt = Instant.now().toString()
        )
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = parse(prefs.getString(KEY_EVENTS, null)).toMutableList()
        current.add(event)
        val bounded = bound(current)
        prefs.edit().putString(KEY_EVENTS, JSONArray(bounded.map { it.toJson() }).toString()).apply()
    }

    fun pending(context: Context): List<AdminErrorEvent> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_EVENTS, null)
        return parse(raw)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_EVENTS).apply()
    }

    internal fun bound(events: List<AdminErrorEvent>): List<AdminErrorEvent> = events.takeLast(MAX_EVENTS)

    internal fun parse(raw: String?): List<AdminErrorEvent> = runCatching {
        if (raw.isNullOrBlank()) return emptyList()
        val array = JSONArray(raw)
        bound(buildList {
            for (i in 0 until array.length()) {
                val j = array.optJSONObject(i) ?: continue
                val appVersion = clean(j.optString("app_version"), 40, "unknown")
                val deviceModel = clean(j.optString("device_model"), 80, "unknown")
                val screen = clean(j.optString("failing_screen"), 80, "Admin")
                val errorClass = clean(j.optString("error_class"), 100, "Throwable")
                val occurredAt = j.optString("occurred_at")
                if (runCatching { Instant.parse(occurredAt) }.isFailure) continue
                add(AdminErrorEvent(appVersion, deviceModel, screen, errorClass, occurredAt))
            }
        })
    }.getOrDefault(emptyList())

    private fun clean(value: String, max: Int, fallback: String): String {
        val sanitized = value.trim().replace(Regex("[^A-Za-z0-9 ._:/()\\-]"), "_").take(max)
        return sanitized.ifBlank { fallback }
    }
}
