package com.buysloans.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private data class NotificationTarget(
    val label: String,
    val audience: String = "all",
    val userId: String? = null,
)

private data class NotificationDelivery(val sent: Int, val failed: Int)

@Composable
internal fun ManualNotificationPanel(session: AdminSession, profiles: JSONArray?, hostBusy: Boolean = false) {
    if (session.role !in setOf("admin", "manager")) return

    val targets = remember(profiles) {
        buildList {
            add(NotificationTarget("All Morley app users", audience = "all"))
            add(NotificationTarget("Admins", audience = "admin"))
            add(NotificationTarget("Managers", audience = "manager"))
            add(NotificationTarget("Staff", audience = "staff"))
            if (profiles != null) {
                for (i in 0 until profiles.length()) {
                    val p = profiles.optJSONObject(i) ?: continue
                    if (!p.optBoolean("is_enabled", true)) continue
                    val id = cleanJsonString(p, "id")
                    if (id.isBlank()) continue
                    val name = cleanJsonString(p, "display_name").ifBlank { "User ${id.take(8)}" }
                    val role = cleanJsonString(p, "role").ifBlank { "user" }
                    add(NotificationTarget("Customer/user — $name · $role", userId = id))
                }
            }
        }
    }

    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var selected by remember(targets) { mutableStateOf(targets.first()) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Send app notification", fontSize = 19.sp, fontWeight = FontWeight.Black)
            Text(
                "Send a manual push to all registered Morley apps, a staff audience, or one customer/user. Delivery remains role-gated and audited.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Box(Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { expanded = true }, enabled = !sending && !hostBusy, modifier = Modifier.fillMaxWidth()) {
                    Text("Target: ${selected.label}")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    targets.forEach { target ->
                        DropdownMenuItem(text = { Text(target.label) }, onClick = { selected = target; expanded = false })
                    }
                }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(120) },
                label = { Text("Notification title") },
                supportingText = { Text("${title.length}/120") },
                singleLine = true,
                enabled = !sending && !hostBusy,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = body,
                onValueChange = { body = it.take(1000) },
                label = { Text("Customer/app message") },
                supportingText = { Text("${body.length}/1000") },
                minLines = 3,
                enabled = !sending && !hostBusy,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    sending = true
                    failed = false
                    status = "Sending signed app notification…"
                    scope.launch {
                        runCatching { NotificationApi.send(session, selected.audience, selected.userId, title, body) }
                            .onSuccess { result ->
                                status = "Sent to ${result.sent} device${if (result.sent == 1) "" else "s"}${if (result.failed > 0) " · ${result.failed} failed" else ""}."
                                failed = result.failed > 0 || result.sent == 0
                                if (result.sent > 0) body = ""
                            }
                            .onFailure {
                                failed = true
                                status = it.message ?: "Notification could not be sent."
                            }
                        sending = false
                    }
                },
                enabled = !sending && !hostBusy && title.trim().isNotEmpty() && body.trim().isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (sending) "Sending…" else "Send notification", fontWeight = FontWeight.Black) }
            if (status.isNotBlank()) {
                Text(status, color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, fontSize = 12.sp)
            }
        }
    }
}

private object NotificationApi {
    suspend fun send(session: AdminSession, audience: String, targetUserId: String?, title: String, body: String): NotificationDelivery = withContext(Dispatchers.IO) {
        require(session.role in setOf("admin", "manager")) { "Notifications require Admin or Manager access." }
        val cleanTitle = title.trim()
        val cleanBody = body.trim()
        require(cleanTitle.length in 1..120) { "Notification title must be between 1 and 120 characters." }
        require(cleanBody.length in 1..1000) { "Notification message must be between 1 and 1000 characters." }
        require(audience in setOf("all", "admin", "manager", "staff")) { "Unsupported notification audience." }

        val jobPayload = JSONObject()
            .put("title", cleanTitle)
            .put("body", cleanBody)
            .put("audience", audience)
            .put("requested_by", session.userId)
        if (!targetUserId.isNullOrBlank()) jobPayload.put("target_user_id", targetUserId)

        val inserted = request(
            path = "/rest/v1/notification_jobs?select=id",
            method = "POST",
            token = session.accessToken,
            body = jobPayload.toString(),
            preferRepresentation = true
        )
        if (inserted.first !in 200..299) error(apiMessage(inserted.second, "Notification could not be queued."))
        val jobId = JSONArray(inserted.second).optJSONObject(0)?.optString("id").orEmpty()
        require(jobId.isNotBlank()) { "Notification queue did not return a job id." }

        val audit = JSONObject()
            .put("actor_user_id", session.userId)
            .put("action", "notification_queued")
            .put("target_type", if (targetUserId.isNullOrBlank()) "audience" else "user")
            .put("target_id", targetUserId ?: audience)
            .put("details", JSONObject().put("title", cleanTitle))
        val auditResult = request("/rest/v1/admin_audit_log", "POST", session.accessToken, audit.toString())
        if (auditResult.first !in 200..299) error(apiMessage(auditResult.second, "Notification audit entry could not be written."))

        val delivery = request(
            path = "/functions/v1/send-admin-notification",
            method = "POST",
            token = session.accessToken,
            body = JSONObject().put("job_id", jobId).toString()
        )
        if (delivery.first !in 200..299) error(apiMessage(delivery.second, "Notification delivery failed."))
        val result = JSONObject(delivery.second)
        NotificationDelivery(result.optInt("sent", 0), result.optInt("failed", 0))
    }

    private fun request(path: String, method: String, token: String, body: String?, preferRepresentation: Boolean = false): Pair<Int, String> {
        val connection = (URL("${BuildConfig.SUPABASE_URL}$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 25_000
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
            if (preferRepresentation) setRequestProperty("Prefer", "return=representation") else if (method == "POST" && path.startsWith("/rest/")) setRequestProperty("Prefer", "return=minimal")
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

    private fun apiMessage(body: String, fallback: String): String = runCatching {
        val j = JSONObject(body)
        j.optString("error").ifBlank { j.optString("message") }.ifBlank { j.optString("msg") }.ifBlank { fallback }
    }.getOrDefault(fallback)
}

private fun cleanJsonString(json: JSONObject, key: String): String {
    if (!json.has(key) || json.isNull(key)) return ""
    return json.optString(key).trim().takeUnless { it.equals("null", ignoreCase = true) } ?: ""
}
