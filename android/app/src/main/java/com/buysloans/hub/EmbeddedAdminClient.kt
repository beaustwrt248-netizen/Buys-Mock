package com.buysloans.hub

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

data class EmbeddedAdminTicket(
    val id: String,
    val category: String,
    val subject: String,
    val description: String,
    val status: String,
    val priority: String,
    val assignedTo: String?,
    val slaDueAt: String,
    val appVersion: String,
    val deviceModel: String,
    val createdAt: String,
)

data class EmbeddedAdminUser(
    val id: String,
    val displayName: String,
    val role: String,
    val enabled: Boolean,
)

data class EmbeddedAdminDevice(
    val id: String,
    val name: String,
    val platform: String,
    val appVersion: String,
    val lastSeenAt: String,
)

data class EmbeddedAdminEvent(
    val title: String,
    val detail: String,
    val occurredAt: String,
)

data class EmbeddedAdminSnapshot(
    val openTickets: Int,
    val enabledUsers: Int,
    val registeredDevices: Int,
    val recentErrors: Int,
    val recentAuditEvents: Int,
    val tickets: List<EmbeddedAdminTicket>,
    val users: List<EmbeddedAdminUser>,
    val devices: List<EmbeddedAdminDevice>,
    val errors: List<EmbeddedAdminEvent>,
    val auditEvents: List<EmbeddedAdminEvent>,
)

object EmbeddedAdminClient {
    val ticketStatuses = listOf("open", "in_progress", "waiting_on_user", "resolved", "closed")
    val ticketPriorities = listOf("low", "normal", "high", "urgent")

    suspend fun load(context: Context): EmbeddedAdminSnapshot = withContext(Dispatchers.IO) {
        val token = privilegedToken(context)
        val ticketsJson = getArray(
            "/rest/v1/support_tickets?select=id,category,subject,description,status,priority,assigned_to,sla_due_at,app_version,device_model,created_at&order=created_at.desc&limit=50",
            token
        )
        val profilesJson = getArray(
            "/rest/v1/profiles?select=id,display_name,role,is_enabled&order=display_name.asc&limit=100",
            token
        )
        val devicesJson = getArray(
            "/rest/v1/devices?select=id,device_name,platform,app_version,last_seen_at&order=last_seen_at.desc&limit=100",
            token
        )
        val errorsJson = getArray(
            "/rest/v1/admin_error_events?select=id,app_version,device_model,failing_screen,error_class,occurred_at&order=occurred_at.desc&limit=25",
            token
        )
        val auditJson = getArray(
            "/rest/v1/admin_audit_log?select=id,action,target_type,target_id,created_at&order=created_at.desc&limit=25",
            token
        )

        val tickets = ticketsJson.toObjects().map { row ->
            EmbeddedAdminTicket(
                id = row.optString("id"),
                category = row.optString("category").ifBlank { "support" },
                subject = row.optString("subject").ifBlank { "Support ticket" },
                description = row.optString("description"),
                status = row.optString("status").ifBlank { "open" },
                priority = row.optString("priority").ifBlank { "normal" },
                assignedTo = row.optNullableString("assigned_to"),
                slaDueAt = row.optString("sla_due_at"),
                appVersion = row.optString("app_version"),
                deviceModel = row.optString("device_model"),
                createdAt = row.optString("created_at"),
            )
        }
        val users = profilesJson.toObjects().mapNotNull { row ->
            val id = row.optString("id")
            if (id.isBlank()) null else EmbeddedAdminUser(
                id = id,
                displayName = row.optString("display_name").ifBlank { id.take(8) },
                role = row.optString("role").ifBlank { "user" },
                enabled = row.optBoolean("is_enabled", false),
            )
        }
        val devices = devicesJson.toObjects().map { row ->
            EmbeddedAdminDevice(
                id = row.optString("id"),
                name = row.optString("device_name").ifBlank { "Registered device" },
                platform = row.optString("platform").ifBlank { "Android" },
                appVersion = row.optString("app_version"),
                lastSeenAt = row.optString("last_seen_at"),
            )
        }
        val errors = errorsJson.toObjects().map { row ->
            EmbeddedAdminEvent(
                title = row.optString("error_class").ifBlank { "App error" },
                detail = listOf(row.optString("failing_screen"), row.optString("device_model"), row.optString("app_version"))
                    .filter { it.isNotBlank() }.joinToString(" · "),
                occurredAt = row.optString("occurred_at"),
            )
        }
        val audit = auditJson.toObjects().map { row ->
            EmbeddedAdminEvent(
                title = row.optString("action").ifBlank { "Admin action" },
                detail = listOf(row.optString("target_type"), row.optString("target_id"))
                    .filter { it.isNotBlank() }.joinToString(" · "),
                occurredAt = row.optString("created_at"),
            )
        }

        EmbeddedAdminSnapshot(
            openTickets = tickets.count { it.status !in setOf("resolved", "closed") },
            enabledUsers = users.count { it.enabled },
            registeredDevices = devices.size,
            recentErrors = errors.size,
            recentAuditEvents = audit.size,
            tickets = tickets,
            users = users,
            devices = devices,
            errors = errors,
            auditEvents = audit,
        )
    }

    suspend fun updateTicket(
        context: Context,
        ticketId: String,
        status: String,
        priority: String,
        assignedTo: String?,
    ) = withContext(Dispatchers.IO) {
        require(ticketId.isNotBlank()) { "A support ticket id is required." }
        require(status in ticketStatuses) { "Unsupported support-ticket status." }
        require(priority in ticketPriorities) { "Unsupported support-ticket priority." }
        val token = privilegedToken(context)
        val payload = JSONObject()
            .put("status", status)
            .put("priority", priority)
            .put("assigned_to", assignedTo?.trim()?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
        request(
            "/rest/v1/support_tickets?id=eq.${enc(ticketId.trim())}",
            "PATCH",
            token,
            payload.toString(),
            preferMinimal = true
        ).requireSuccess("Support ticket could not be updated.")
    }

    private suspend fun privilegedToken(context: Context): String {
        val token = AuthManager.validAccessToken(context)
        require(AuthManager.canUseAdminMode(context)) { "Admin mode requires an Admin or Manager account." }
        return token
    }

    private fun getArray(path: String, token: String): JSONArray {
        val response = request(path, "GET", token, null)
        response.requireSuccess("Admin data could not be loaded.")
        return JSONArray(response.second)
    }

    private fun request(
        path: String,
        method: String,
        token: String,
        body: String?,
        preferMinimal: Boolean = false,
    ): Pair<Int, String> {
        val connection = (URL("${BuildConfig.SUPABASE_URL}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 12_000
            readTimeout = 18_000
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
            if (preferMinimal) setRequestProperty("Prefer", "return=minimal")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        return try {
            if (body != null) connection.outputStream.use { it.write(body.toByteArray()) }
            val code = connection.responseCode
            val responseBody = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            code to responseBody
        } finally {
            connection.disconnect()
        }
    }

    private fun Pair<Int, String>.requireSuccess(fallback: String) {
        if (first in 200..299) return
        val detail = runCatching {
            val json = JSONObject(second)
            json.optString("message").ifBlank { json.optString("msg") }.ifBlank { json.optString("error") }
        }.getOrNull().orEmpty()
        error(detail.ifBlank { "$fallback ($first)" })
    }

    private fun JSONArray.toObjects(): List<JSONObject> =
        (0 until length()).mapNotNull { optJSONObject(it) }

    private fun JSONObject.optNullableString(key: String): String? =
        if (isNull(key)) null else optString(key).trim().takeIf { it.isNotBlank() && !it.equals("null", true) }

    private fun enc(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
}
