package com.buysloans.hub

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class EmbeddedAdminSnapshot(
    val openTickets: Int,
    val enabledUsers: Int,
    val registeredDevices: Int,
    val recentErrors: Int,
    val recentAuditEvents: Int,
)

object EmbeddedAdminClient {
    suspend fun load(context: Context): EmbeddedAdminSnapshot = withContext(Dispatchers.IO) {
        val token = AuthManager.validAccessToken(context)
        require(AuthManager.canUseAdminMode(context)) { "Admin mode requires an Admin or Manager account." }

        val tickets = getArray("/rest/v1/support_tickets?select=id,status&order=created_at.desc&limit=100", token)
        val profiles = getArray("/rest/v1/profiles?select=id,is_enabled,role&limit=200", token)
        val devices = getArray("/rest/v1/devices?select=id&limit=200", token)
        val errors = getArray("/rest/v1/admin_error_events?select=id&order=occurred_at.desc&limit=50", token)
        val audit = getArray("/rest/v1/admin_audit_log?select=id&order=created_at.desc&limit=50", token)

        EmbeddedAdminSnapshot(
            openTickets = (0 until tickets.length()).count { tickets.optJSONObject(it)?.optString("status") !in setOf("resolved", "closed") },
            enabledUsers = (0 until profiles.length()).count { profiles.optJSONObject(it)?.optBoolean("is_enabled", false) == true },
            registeredDevices = devices.length(),
            recentErrors = errors.length(),
            recentAuditEvents = audit.length(),
        )
    }

    private fun getArray(path: String, token: String): JSONArray {
        val connection = (URL("${BuildConfig.SUPABASE_URL}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 15_000
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            val code = connection.responseCode
            val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) error("Admin data request failed ($code).")
            JSONArray(body)
        } finally {
            connection.disconnect()
        }
    }
}
