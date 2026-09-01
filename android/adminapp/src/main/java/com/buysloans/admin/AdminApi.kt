package com.buysloans.admin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

internal data class AdminSession(
    var accessToken: String,
    var refreshToken: String,
    val userId: String,
    val displayName: String,
    val role: String
)
internal data class AdminSnapshot(
    val tickets: JSONArray,
    val announcements: JSONArray,
    val profiles: JSONArray,
    val devices: JSONArray,
    val config: JSONArray,
    val errorEvents: JSONArray,
    val auditEvents: JSONArray
)

internal fun passwordSignInPayload(email: String, password: String, captchaToken: String): String =
    JSONObject()
        .put("email", email.trim())
        .put("password", password)
        .put("gotrue_meta_security", JSONObject().put("captcha_token", captchaToken))
        .toString()

internal fun refreshSessionPayload(refreshToken: String): String =
    JSONObject().put("refresh_token", refreshToken).toString()

internal fun isExpiredJwtResponse(code: Int, body: String): Boolean {
    if (code !in setOf(401, 403)) return false
    val text = runCatching {
        val json = JSONObject(body)
        listOf(json.optString("msg"), json.optString("message"), json.optString("error_description"), json.optString("code"))
            .joinToString(" ")
    }.getOrDefault(body)
    return text.contains("jwt", ignoreCase = true) && text.contains("expired", ignoreCase = true)
}

internal object AdminApi {
    suspend fun signIn(email: String, password: String, captchaToken: String): AdminSession = withContext(Dispatchers.IO) {
        require(email.isNotBlank() && password.isNotBlank()) { "Enter email and password." }
        require(captchaToken.isNotBlank()) { "Complete the security check before signing in." }
        val payload = passwordSignInPayload(email, password, captchaToken)
        val auth = request("/auth/v1/token?grant_type=password", "POST", null, payload)
        if (auth.first !in 200..299) error(message(auth.second, "Sign-in failed."))
        val authJson = JSONObject(auth.second)
        var token = authJson.optString("access_token")
        var refreshToken = authJson.optString("refresh_token")
        val userId = authJson.optJSONObject("user")?.optString("id").orEmpty()
        require(token.isNotBlank() && refreshToken.isNotBlank() && userId.isNotBlank()) { "Sign-in response was incomplete." }

        val profilePath = "/rest/v1/profiles?id=eq.${enc(userId)}&select=id,display_name,role,is_enabled&limit=1"
        var profileResponse = request(profilePath, "GET", token, null)
        if (isExpiredJwtResponse(profileResponse.first, profileResponse.second)) {
            val refreshed = refreshTokens(refreshToken)
            token = refreshed.first
            refreshToken = refreshed.second
            profileResponse = request(profilePath, "GET", token, null)
        }
        if (profileResponse.first !in 200..299) error(message(profileResponse.second, "Could not verify Admin access."))
        val profile = JSONArray(profileResponse.second).optJSONObject(0) ?: error("This account is not authorised for Admin access.")
        val role = profile.optString("role")
        require(AdminAppAccessPolicy.canEnter(role, profile.optBoolean("is_enabled"))) { "This account is not authorised for Admin access." }
        AdminSession(token, refreshToken, userId, profile.optString("display_name").ifBlank { email.substringBefore('@') }, role)
    }

    suspend fun load(session: AdminSession): AdminSnapshot = withContext(Dispatchers.IO) {
        val ticketPath = "/rest/v1/support_tickets?select=id,user_id,category,subject,description,status,priority,app_version,device_model,android_version,assigned_to,sla_due_at,first_response_at,created_at,updated_at&order=created_at.desc&limit=50"
        when {
            AdminAppAccessPolicy.canReadFullSnapshot(session) -> AdminSnapshot(
                tickets = getArray(ticketPath, session),
                announcements = getArray("/rest/v1/announcements?select=id,title,body,audience,is_active,created_at&order=created_at.desc&limit=25", session),
                profiles = getArray("/rest/v1/profiles?select=id,display_name,role,is_enabled,created_at&order=created_at.desc&limit=100", session),
                devices = getArray("/rest/v1/devices?select=id,device_name,platform,app_version,app_version_code,notifications_enabled,last_seen_at&order=last_seen_at.desc&limit=100", session),
                config = getArray("/rest/v1/app_config?select=key,value,updated_at&key=in.(feature_flags,current_release,minimum_supported_version)&order=key", session),
                errorEvents = getArray("/rest/v1/admin_error_events?select=id,app_version,device_model,failing_screen,error_class,occurred_at&order=occurred_at.desc&limit=50", session),
                auditEvents = getArray("/rest/v1/admin_audit_log?select=id,action,target_type,target_id,created_at&order=created_at.desc&limit=50", session)
            )
            AdminAppAccessPolicy.isSupportOnly(session) -> AdminSnapshot(
                tickets = getArray(ticketPath, session),
                announcements = JSONArray(),
                profiles = JSONArray(),
                devices = JSONArray(),
                config = JSONArray(),
                errorEvents = JSONArray(),
                auditEvents = JSONArray()
            )
            else -> error("This account is not authorised for Admin access.")
        }
    }

    suspend fun loadSupportMessages(session: AdminSession, ticketId: String, limit: Int = 100): JSONArray = withContext(Dispatchers.IO) {
        require(SupportMessageAccessPolicy.canReadProtectedMessages(session)) {
            "Protected support messages require an authenticated Staff, Manager or Admin session."
        }
        val path = SupportMessageAccessPolicy.buildReadPath(session, ticketId, limit)
        getArray(path, session)
    }

    suspend fun loadSupportNotes(session: AdminSession, ticketId: String, limit: Int = 100): JSONArray = withContext(Dispatchers.IO) {
        require(canUpdateSupportTicketTriage(session)) { "Internal notes require an authenticated Staff, Manager or Admin session." }
        require(ticketId.isNotBlank()) { "A support ticket id is required." }
        val boundedLimit = limit.coerceIn(1, 100)
        getArray(
            "/rest/v1/support_ticket_internal_notes?ticket_id=eq.${enc(ticketId.trim())}&select=id,body,created_at&order=created_at.asc&limit=$boundedLimit",
            session
        )
    }

    suspend fun sendSupportReply(session: AdminSession, ticketId: String, body: String) = withContext(Dispatchers.IO) {
        require(canUpdateSupportTicketTriage(session)) { "Replying requires an authenticated Staff, Manager or Admin session." }
        val cleanBody = body.trim()
        require(ticketId.isNotBlank()) { "A support ticket id is required." }
        require(cleanBody.length in 1..5000) { "Reply must be between 1 and 5000 characters." }
        val payload = JSONObject()
            .put("ticket_id", ticketId.trim())
            .put("author_user_id", session.userId)
            .put("author_role", "admin")
            .put("body", cleanBody)
            .toString()
        val response = authorizedRequest(session, "/rest/v1/support_ticket_messages", "POST", payload, preferMinimal = true)
        if (response.first !in 200..299) error(message(response.second, "Reply could not be sent."))
    }

    suspend fun addSupportInternalNote(session: AdminSession, ticketId: String, body: String) = withContext(Dispatchers.IO) {
        require(canUpdateSupportTicketTriage(session)) { "Internal notes require an authenticated Staff, Manager or Admin session." }
        val cleanBody = body.trim()
        require(ticketId.isNotBlank()) { "A support ticket id is required." }
        require(cleanBody.length in 1..5000) { "Internal note must be between 1 and 5000 characters." }
        val payload = JSONObject()
            .put("ticket_id", ticketId.trim())
            .put("author_user_id", session.userId)
            .put("body", cleanBody)
            .toString()
        val response = authorizedRequest(session, "/rest/v1/support_ticket_internal_notes", "POST", payload, preferMinimal = true)
        if (response.first !in 200..299) error(message(response.second, "Internal note could not be saved."))
    }

    suspend fun loadSupportAssigneeProfiles(session: AdminSession): JSONArray = withContext(Dispatchers.IO) {
        require(canManageSupportTicketControls(session)) {
            "Support assignees require an authenticated Admin or Manager session."
        }
        getArray(
            "/rest/v1/profiles?select=id,display_name,role,is_enabled&role=in.(admin,manager,staff)&is_enabled=eq.true&order=display_name.asc&limit=100",
            session
        )
    }

    suspend fun updateSupportTicket(session: AdminSession, command: SupportTicketUpdateCommand) = withContext(Dispatchers.IO) {
        val payload = supportTicketUpdatePayload(session, command).toString()
        val path = "/rest/v1/support_tickets?id=eq.${enc(command.ticketId.trim())}"
        val response = authorizedRequest(session, path, "PATCH", payload, preferMinimal = true)
        if (response.first !in 200..299) error(message(response.second, "Support ticket could not be updated."))
    }

    suspend fun updateMaintenanceConfig(
        session: AdminSession,
        current: MaintenanceConfig,
        enabled: Boolean,
        message: String,
        otaEnabled: Boolean = current.otaEnabled
    ) = withContext(Dispatchers.IO) {
        val payload = maintenanceUpdatePayload(current, enabled, message, otaEnabled).toString()
        val response = authorizedRequest(session, "/rest/v1/rpc/admin_set_config", "POST", payload, preferMinimal = true)
        if (response.first !in 200..299) error(message(response.second, "Remote configuration could not be updated."))
    }

    suspend fun submitTelemetry(session: AdminSession, events: List<AdminErrorEvent>) = withContext(Dispatchers.IO) {
        if (events.isEmpty()) return@withContext
        val payload = JSONArray(events.map { it.toJson() }).toString()
        val response = authorizedRequest(session, "/rest/v1/admin_error_events", "POST", payload, preferMinimal = true)
        if (response.first !in 200..299) error(message(response.second, "Admin health telemetry could not be submitted."))
    }

    private fun getArray(path: String, session: AdminSession): JSONArray {
        val response = authorizedRequest(session, path, "GET", null)
        if (response.first !in 200..299) error(message(response.second, "Read-only Admin data could not be loaded."))
        return JSONArray(response.second)
    }

    private fun authorizedRequest(
        session: AdminSession,
        path: String,
        method: String,
        body: String?,
        preferMinimal: Boolean = false
    ): Pair<Int, String> {
        var response = request(path, method, session.accessToken, body, preferMinimal)
        if (isExpiredJwtResponse(response.first, response.second) && session.refreshToken.isNotBlank()) {
            val refreshed = refreshTokens(session.refreshToken)
            session.accessToken = refreshed.first
            session.refreshToken = refreshed.second
            response = request(path, method, session.accessToken, body, preferMinimal)
        }
        return response
    }

    private fun refreshTokens(refreshToken: String): Pair<String, String> {
        require(refreshToken.isNotBlank()) { "Admin session expired. Sign in again." }
        val response = request(
            "/auth/v1/token?grant_type=refresh_token",
            "POST",
            null,
            refreshSessionPayload(refreshToken)
        )
        if (response.first !in 200..299) error("Admin session expired. Sign in again.")
        val json = JSONObject(response.second)
        val access = json.optString("access_token")
        val nextRefresh = json.optString("refresh_token").ifBlank { refreshToken }
        require(access.isNotBlank()) { "Admin session expired. Sign in again." }
        return access to nextRefresh
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
