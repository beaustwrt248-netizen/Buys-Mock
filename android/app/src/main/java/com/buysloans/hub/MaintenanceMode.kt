package com.buysloans.hub

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

internal data class MaintenanceModeState(
    val enabled: Boolean,
    val message: String
)

internal object MaintenanceModePolicy {
    const val DEFAULT_MESSAGE = "B&L Morley is temporarily unavailable while maintenance is completed. Please try again shortly."

    fun state(enabled: Boolean, message: String?): MaintenanceModeState = MaintenanceModeState(
        enabled = enabled,
        message = message?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_MESSAGE
    )
}

internal object MaintenanceModeClient {
    suspend fun fetch(context: Context): MaintenanceModeState? = withContext(Dispatchers.IO) {
        val token = AuthManager.validAccessToken(context)
        val url = URL("${BuildConfig.SUPABASE_URL}/rest/v1/app_config?select=value&key=eq.feature_flags")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $token")
        }
        try {
            if (connection.responseCode !in 200..299) return@withContext null
            val rows = JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
            if (rows.length() == 0) return@withContext null
            val flags = rows.getJSONObject(0).optJSONObject("value") ?: return@withContext null
            MaintenanceModePolicy.state(
                enabled = flags.optBoolean("maintenanceMode", false),
                message = flags.optString("maintenanceMessage", "")
            )
        } finally {
            connection.disconnect()
        }
    }
}
