package com.buysloans.hub

import android.content.Context
import android.os.Build
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

/**
 * Persists only a minimal crash marker synchronously, then uploads it on the next
 * healthy authenticated launch. No exception messages, stack traces, user data,
 * or arbitrary payloads are collected.
 */
object MainAppCrashTelemetry {
    private const val PREFS = "main_app_crash_telemetry"
    private const val KEY_ERROR_CLASS = "error_class"
    private const val KEY_FAILING_SCREEN = "failing_screen"
    private const val KEY_OCCURRED_AT = "occurred_at"
    private const val MAX_TEXT = 160

    @Volatile private var installed = false

    data class PendingCrash(
        val errorClass: String,
        val failingScreen: String,
        val occurredAt: Long
    )

    fun install(context: Context) {
        if (installed) return
        synchronized(this) {
            if (installed) return
            val appContext = context.applicationContext
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                runCatching { persist(appContext, throwable) }
                previous?.uncaughtException(thread, throwable)
            }
            installed = true
        }
    }

    internal fun persist(context: Context, throwable: Throwable, occurredAt: Long = System.currentTimeMillis()) {
        val screen = DiagnosticContextStore.snapshot(context)?.screen.orEmpty().ifBlank { "Unknown" }
        val errorClass = throwable.javaClass.simpleName.ifBlank { throwable.javaClass.name.substringAfterLast('.') }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ERROR_CLASS, sanitize(errorClass))
            .putString(KEY_FAILING_SCREEN, sanitize(screen))
            .putLong(KEY_OCCURRED_AT, occurredAt)
            .commit()
    }

    internal fun pending(context: Context): PendingCrash? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val occurredAt = prefs.getLong(KEY_OCCURRED_AT, 0L)
        if (occurredAt <= 0L) return null
        return PendingCrash(
            errorClass = sanitize(prefs.getString(KEY_ERROR_CLASS, "UnknownError").orEmpty()).ifBlank { "UnknownError" },
            failingScreen = sanitize(prefs.getString(KEY_FAILING_SCREEN, "Unknown").orEmpty()).ifBlank { "Unknown" },
            occurredAt = occurredAt
        )
    }

    suspend fun flushPending(context: Context): Boolean {
        val crash = pending(context) ?: return true
        if (!AuthManager.isSignedIn(context)) return false
        val token = runCatching { AuthManager.validAccessToken(context) }.getOrNull() ?: return false
        val payload = JSONObject().apply {
            put("p_app_version", sanitize(BuildConfig.VERSION_NAME))
            put("p_device_model", sanitize("${Build.MANUFACTURER} ${Build.MODEL}".trim()).ifBlank { "Unknown" })
            put("p_failing_screen", crash.failingScreen)
            put("p_error_class", crash.errorClass)
            put("p_occurred_at", Instant.ofEpochMilli(crash.occurredAt).toString())
        }
        val code = runCatching {
            val connection = (URL("${BuildConfig.SUPABASE_URL}/rest/v1/rpc/report_client_crash").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5000
                readTimeout = 5000
                doOutput = true
                setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
            }
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val responseCode = connection.responseCode
            connection.disconnect()
            responseCode
        }.getOrNull() ?: return false
        if (code !in 200..299) return false

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getLong(KEY_OCCURRED_AT, 0L) == crash.occurredAt) prefs.edit().clear().apply()
        return true
    }

    internal fun sanitize(value: String): String =
        value.replace(Regex("[\\r\\n\\t]"), " ").trim().take(MAX_TEXT)
}
