package com.buysloans.admin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

internal data class AdminSession(val accessToken: String, val userId: String, val displayName: String, val role: String)
internal data class AdminSnapshot(
    val tickets: JSONArray,
    val announcements: JSONArray,
    val profiles: JSONArray,
    val devices: JSONArray,
    val config: JSONArray,
    val errorEvents: JSONArray,
    val auditEvents: JSONArray
)

internal object AdminApi {
    suspend fun signIn(email: String, password: String): AdminSession = withContext(Dispatchers.IO) {
        require(email.isNotBlank() && password.isNotBlank()) { "Enter email and password." }
        val payload = JSONObject().put("email", email.trim()).put("password", password).toString()
        val auth = request("/auth/v1/token?grant_type=password", "POST", null, payload)
        if (auth.first !in 200..299) error(message(auth.second, "Sign-in failed."))
        val authJson = JSONObject(auth.second)
        val token = authJson.optString("access_token")
        val userId = authJson.optJSONObject("user")?.optString("id").orEmpty()
        require(token.isNotBlank() && userId.isNotBlank()) { "Sign-in response was incomplete." }

        val profilePath = "/rest/v1/profiles?id=eq.${enc(userId)}&select=id,display_name,role,is_enabled&limit=1"
        val profileResponse = request(profilePath, "GET", token, null)
        if (profileResponse.first !in 200..299) error(message(profileResponse.second, "Could not verify Admin access."))
        val profile = JSONArray(profileResponse.second).optJSONObject(0) ?: error("This account is not authorised for Admin access.")
        val role = profile.optString("role")
        require(profile.optBoolean("is_enabled") && role in setOf("admin", "manager")) { "This account is not authorised for Admin access." }
        AdminSession(token, userId, profile.optString("display_name").ifBlank { email.substringBefore('@') }, role)
    }

    suspend fun load(session: AdminSession): AdminSnapshot = withContext(Dispatchers.IO) {
        AdminSnapshot(
            tickets = getArray("/rest/v1/support_tickets?select=id,category,subject,status,priority,app_version,device_model,created_at,updated_at&order=created_at.desc&limit=50", session.accessToken),
            announcements = getArray("/rest/v1/announcements?select=id,title,body,audience,is_active,created_at&order=created_at.desc&limit=25", session.accessToken),
            profiles = getArray("/rest/v1/profiles?select=id,display_name,role,is_enabled,created_at&order=created_at.desc&limit=100", session.accessToken),
            devices = getArray("/rest/v1/devices?select=id,device_name,platform,app_version,app_version_code,notifications_enabled,last_seen_at&order=last_seen_at.desc&limit=100", session.accessToken),
            config = getArray("/rest/v1/app_config?select=key,value,updated_at&key=in.(feature_flags,current_release,minimum_supported_version)&order=key", session.accessToken),
            errorEvents = getArray("/rest/v1/admin_error_events?select=id,app_version,device_model,failing_screen,error_class,occurred_at&order=occurred_at.desc&limit=50", session.accessToken),
            auditEvents = getArray("/rest/v1/admin_audit_log?select=id,action,target_type,target_id,created_at&order=created_at.desc&limit=50", session.accessToken)
        )
    }

    suspend fun updateMaintenanceConfig(session: AdminSession, current: MaintenanceConfig, enabled: Boolean, message: String) = withContext(Dispatchers.IO) {
        val payload = maintenanceUpdatePayload(current, enabled, message).toString()
        val response = request("/rest/v1/rpc/admin_set_config", "POST", session.accessToken, payload, preferMinimal = true)
        if (response.first !in 200..299) error(message(response.second, "Maintenance configuration could not be updated."))
    }

    suspend fun submitTelemetry(session: AdminSession, events: List<AdminErrorEvent>) = withContext(Dispatchers.IO) {
        if (events.isEmpty()) return@withContext
        val payload = JSONArray(events.map { it.toJson() }).toString()
        val response = request("/rest/v1/admin_error_events", "POST", session.accessToken, payload, preferMinimal = true)
        if (response.first !in 200..299) error(message(response.second, "Admin health telemetry could not be submitted."))
    }

    private fun getArray(path: String, token: String): JSONArray {
        val response = request(path, "GET", token, null)
        if (response.first !in 200..299) error(message(response.second, "Read-only Admin data could not be loaded."))
        return JSONArray(response.second)
    }

    private fun request(path: String, method: String, token: String?, body: String?, preferMinimal: Boolean = false): Pair<Int, String> {
        val connection = (URL("${BuildConfig.SUPABASE_URL}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Accept", "application/json")
            if (preferMinimal) setRequestProperty("Prefer", "return=minimal")
            if (!token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        return try {
            if (body != null) connection.outputStream.use { it.write(body.toByteArray()) }
            val code = connection.responseCode
            val text = (if (code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            code to text
        } finally {
            connection.disconnect()
        }
    }

    private fun message(body: String, fallback: String): String = runCatching {
        val json = JSONObject(body)
        json.optString("msg").ifBlank { json.optString("message") }.ifBlank { json.optString("error_description") }.ifBlank { fallback }
    }.getOrDefault(fallback)

    private fun enc(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
}
