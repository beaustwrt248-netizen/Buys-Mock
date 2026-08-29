package com.buysloans.hub

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

internal object OtaFeaturePolicy {
    internal fun enabledFromConfig(rows: JSONArray): Boolean {
        for (i in 0 until rows.length()) {
            val row = rows.optJSONObject(i) ?: continue
            if (row.optString("key") != "feature_flags") continue
            val flags = row.optJSONObject("value") ?: return true
            return if (flags.has("otaEnabled")) flags.optBoolean("otaEnabled", true) else true
        }
        return true
    }

    suspend fun isEnabled(): Boolean {
        val context = runCatching { MorleyApplication.instance }.getOrNull() ?: return true
        val token = runCatching { AuthManager.validAccessToken(context) }.getOrNull() ?: return true
        return runCatching { fetch(token) }.getOrDefault(true)
    }

    private suspend fun fetch(token: String): Boolean = withContext(Dispatchers.IO) {
        val url = URL("${BuildConfig.SUPABASE_URL}/rest/v1/app_config?select=key,value&key=eq.feature_flags")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Cache-Control", "no-cache")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) return@withContext true
            enabledFromConfig(JSONArray(connection.inputStream.bufferedReader().use { it.readText() }))
        } finally {
            connection.disconnect()
        }
    }
}
