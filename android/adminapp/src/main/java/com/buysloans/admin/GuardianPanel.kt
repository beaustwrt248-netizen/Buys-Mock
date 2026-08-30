package com.buysloans.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private data class GuardianViewState(
    val enabled: Boolean,
    val autoFixEnabled: Boolean,
    val killSwitch: Boolean,
    val killSwitchReason: String,
    val mode: String,
    val maxAutoRisk: String,
    val learningEnabled: Boolean,
    val evolutionEnabled: Boolean,
    val confidenceThreshold: Double,
    val maxParallelRepairs: Int,
    val quarantineOnRepeatedFailure: Boolean,
    val incidents: JSONArray,
)

@Composable
internal fun GuardianPanel(session: AdminSession) {
    if (session.role !in setOf("admin", "manager")) {
        Text("Guardian requires Admin or Manager access.", color = MaterialTheme.colorScheme.error)
        return
    }

    var state by remember { mutableStateOf<GuardianViewState?>(null) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var success by remember { mutableStateOf("") }
    var reloadKey by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(session.accessToken, reloadKey) {
        loading = true
        error = ""
        runCatching { loadGuardianState(session) }
            .onSuccess { state = it }
            .onFailure { error = it.message ?: "Guardian data could not be loaded." }
        loading = false
    }

    Text("Guardian Control Center", fontSize = 22.sp, fontWeight = FontWeight.Black)
    Text(
        "Live Guardian controls and incident history. Backend RLS, role checks, durable auditing and human approval for code changes remain authoritative.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp
    )

    if (loading) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
        return
    }
    if (error.isNotBlank() && state == null) {
        Text(error, color = MaterialTheme.colorScheme.error)
        Button(onClick = { reloadKey += 1 }, modifier = Modifier.fillMaxWidth()) { Text("Retry Guardian refresh") }
        return
    }

    val s = state ?: return
    GuardianControlsCard(
        session = session,
        current = s,
        saving = saving,
        onSave = { draft ->
            val validation = validateGuardianControlDraft(draft, session.role, s.killSwitch)
            if (validation != null) {
                error = validation
                return@GuardianControlsCard
            }
            saving = true
            error = ""
            success = ""
            scope.launch {
                runCatching { updateGuardianControls(session, draft) }
                    .onSuccess {
                        success = "Guardian controls updated and audited."
                        reloadKey += 1
                    }
                    .onFailure { error = it.message ?: "Guardian controls could not be updated." }
                saving = false
            }
        }
    )

    if (success.isNotBlank()) Text(success, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
    if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)

    val unresolved = (0 until s.incidents.length()).count {
        s.incidents.optJSONObject(it)?.optString("state") !in setOf("resolved", "ignored")
    }
    val awaiting = (0 until s.incidents.length()).count {
        s.incidents.optJSONObject(it)?.optString("state") in setOf("proposed", "awaiting_approval")
    }
    Text("Incidents • $unresolved unresolved • $awaiting awaiting approval", fontWeight = FontWeight.Black)
    if (s.incidents.length() == 0) {
        Text("No Guardian incidents returned.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        for (i in 0 until minOf(s.incidents.length(), 30)) {
            val item = s.incidents.optJSONObject(i) ?: continue
            GuardianStatusCard(
                "${item.optString("risk_level").uppercase()} • ${item.optString("state").replace('_', ' ')}",
                item.optString("classification").ifBlank { "Unclassified" } +
                    item.optString("diagnosis_summary").takeIf { it.isNotBlank() }?.let { "\n$it" }.orEmpty()
            )
        }
    }
    Button(onClick = { reloadKey += 1 }, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text("Refresh Guardian") }
    Text(
        "Guardian control changes go only through the existing guardian_set_controls security-definer RPC. Code-changing repairs still require human approval; this screen cannot weaken authentication, RLS, CAPTCHA, release signing or role boundaries.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp
    )
}

@Composable
private fun GuardianControlsCard(
    session: AdminSession,
    current: GuardianViewState,
    saving: Boolean,
    onSave: (GuardianControlDraft) -> Unit,
) {
    var enabled by remember(current) { mutableStateOf(current.enabled) }
    var autoFixEnabled by remember(current) { mutableStateOf(current.autoFixEnabled) }
    var maxAutoRisk by remember(current) { mutableStateOf(current.maxAutoRisk) }
    var operatingMode by remember(current) { mutableStateOf(current.mode) }
    var learningEnabled by remember(current) { mutableStateOf(current.learningEnabled) }
    var evolutionEnabled by remember(current) { mutableStateOf(current.evolutionEnabled) }
    var confidenceText by remember(current) { mutableStateOf("%.3f".format(current.confidenceThreshold)) }
    var maxParallelRepairs by remember(current) { mutableStateOf(current.maxParallelRepairs) }
    var quarantine by remember(current) { mutableStateOf(current.quarantineOnRepeatedFailure) }
    var killSwitch by remember(current) { mutableStateOf(current.killSwitch) }
    var killSwitchReason by remember(current) { mutableStateOf(current.killSwitchReason) }
    var showConfirmation by remember { mutableStateOf(false) }

    val confidence = confidenceText.toDoubleOrNull()
    val draft = confidence?.let {
        GuardianControlDraft(
            enabled = enabled,
            autoFixEnabled = autoFixEnabled,
            maxAutoRisk = maxAutoRisk,
            operatingMode = operatingMode,
            learningEnabled = learningEnabled,
            evolutionEnabled = evolutionEnabled,
            confidenceThreshold = it,
            maxParallelRepairs = maxParallelRepairs,
            quarantineOnRepeatedFailure = quarantine,
            killSwitch = killSwitch,
            killSwitchReason = killSwitchReason,
        )
    }
    val validation = if (draft == null) {
        "Enter a valid confidence threshold."
    } else {
        validateGuardianControlDraft(draft, session.role, current.killSwitch).orEmpty()
    }
    val changed = draft != null && (
        enabled != current.enabled ||
            autoFixEnabled != current.autoFixEnabled ||
            maxAutoRisk != current.maxAutoRisk ||
            operatingMode != current.mode ||
            learningEnabled != current.learningEnabled ||
            evolutionEnabled != current.evolutionEnabled ||
            confidence != current.confidenceThreshold ||
            maxParallelRepairs != current.maxParallelRepairs ||
            quarantine != current.quarantineOnRepeatedFailure ||
            killSwitch != current.killSwitch ||
            (killSwitch && killSwitchReason.trim() != current.killSwitchReason.trim())
        )

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .24f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Guardian controls", fontSize = 19.sp, fontWeight = FontWeight.Black)
            Text("Admin/Manager changes are backend-validated and durably audited.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)

            GuardianSwitchRow("Guardian running", "Pause or resume Guardian intake and processing.", enabled, !saving && !killSwitch) { enabled = it }

            Text("Operating mode", fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("observe" to "Observe", "assist" to "Assist", "guarded_auto" to "Guarded auto").forEach { (value, label) ->
                    FilterChip(
                        selected = operatingMode == value,
                        onClick = { operatingMode = value },
                        enabled = !saving && !killSwitch,
                        label = { Text(label, fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            GuardianSwitchRow("Automatic repair", "Only within the configured risk ceiling and backend guard rails.", autoFixEnabled, !saving && !killSwitch) { autoFixEnabled = it }
            Text("Maximum automatic risk", fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("low" to "Low", "medium" to "Medium").forEach { (value, label) ->
                    FilterChip(
                        selected = maxAutoRisk == value,
                        onClick = { maxAutoRisk = value },
                        enabled = !saving && !killSwitch,
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider()
            GuardianSwitchRow("Learning", "Allow Guardian to learn from verified incident outcomes.", learningEnabled, !saving && !killSwitch) { learningEnabled = it }
            GuardianSwitchRow("Evolution proposals", "Allow proposals only; code changes still require human approval.", evolutionEnabled, !saving && !killSwitch) { evolutionEnabled = it }
            GuardianSwitchRow("Quarantine repeated failures", "Automatically quarantine repair paths that fail repeatedly.", quarantine, !saving && !killSwitch) { quarantine = it }

            OutlinedTextField(
                value = confidenceText,
                onValueChange = { confidenceText = it.take(5) },
                label = { Text("Confidence threshold (0.500–0.999)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                enabled = !saving && !killSwitch,
                modifier = Modifier.fillMaxWidth()
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Parallel repairs", fontWeight = FontWeight.Bold)
                    Text("Maximum simultaneous guarded repairs: $maxParallelRepairs", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
                OutlinedButton(onClick = { if (maxParallelRepairs > 1) maxParallelRepairs -= 1 }, enabled = !saving && !killSwitch && maxParallelRepairs > 1) { Text("−") }
                Spacer(Modifier.height(1.dp))
                OutlinedButton(onClick = { if (maxParallelRepairs < 5) maxParallelRepairs += 1 }, enabled = !saving && !killSwitch && maxParallelRepairs < 5) { Text("+") }
            }

            HorizontalDivider()
            GuardianSwitchRow(
                "Emergency kill switch",
                if (current.killSwitch && session.role != "admin") "ENGAGED. Only an Admin can disengage it." else "Immediately disables Guardian and automatic repair.",
                killSwitch,
                !saving && !(current.killSwitch && session.role != "admin")
            ) { killSwitch = it }
            if (killSwitch) {
                OutlinedTextField(
                    value = killSwitchReason,
                    onValueChange = { killSwitchReason = it.take(500) },
                    label = { Text("Kill switch reason") },
                    supportingText = { Text("Required and stored in the durable audit trail.") },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (validation.isNotBlank() && changed) Text(validation, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
            Button(
                onClick = { showConfirmation = true },
                enabled = !saving && changed && validation.isBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (saving) "Saving…" else "Review & save Guardian controls", fontWeight = FontWeight.Black) }
        }
    }

    if (showConfirmation && draft != null) {
        AlertDialog(
            onDismissRequest = { if (!saving) showConfirmation = false },
            title = { Text(if (draft.killSwitch) "Confirm Guardian emergency stop" else "Confirm Guardian control changes") },
            text = {
                Text(
                    if (draft.killSwitch) "This will stop Guardian and automatic repair immediately. The reason will be recorded in the Admin audit log."
                    else "Apply these Guardian settings through the protected backend RPC? Code changes will still require human approval."
                )
            },
            confirmButton = {
                Button(onClick = { showConfirmation = false; onSave(draft) }, enabled = !saving) {
                    Text(if (draft.killSwitch) "Engage kill switch" else "Apply audited changes")
                }
            },
            dismissButton = { TextButton(onClick = { showConfirmation = false }, enabled = !saving) { Text("Cancel") } }
        )
    }
}

@Composable
private fun GuardianSwitchRow(title: String, detail: String, checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f).padding(end = 10.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun GuardianStatusCard(title: String, detail: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .18f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}

private suspend fun loadGuardianState(session: AdminSession): GuardianViewState = withContext(Dispatchers.IO) {
    val settings = guardianGet(
        "/rest/v1/guardian_settings?select=enabled,auto_fix_enabled,max_auto_risk,kill_switch,kill_switch_reason,operating_mode,learning_enabled,evolution_enabled,confidence_threshold,max_parallel_repairs,quarantine_on_repeated_failure&singleton=eq.true&limit=1",
        session.accessToken
    ).optJSONObject(0) ?: error("Guardian settings are not available to this Admin session.")
    val incidents = guardianGet(
        "/rest/v1/guardian_incidents?select=id,state,risk_level,classification,confidence,diagnosis_summary,requires_approval,attempt_count,created_at,updated_at&order=created_at.desc&limit=30",
        session.accessToken
    )
    GuardianViewState(
        enabled = settings.optBoolean("enabled"),
        autoFixEnabled = settings.optBoolean("auto_fix_enabled"),
        killSwitch = settings.optBoolean("kill_switch"),
        killSwitchReason = settings.optString("kill_switch_reason"),
        mode = settings.optString("operating_mode").ifBlank { "observe" },
        maxAutoRisk = settings.optString("max_auto_risk").ifBlank { "low" },
        learningEnabled = settings.optBoolean("learning_enabled"),
        evolutionEnabled = settings.optBoolean("evolution_enabled"),
        confidenceThreshold = settings.optDouble("confidence_threshold", 0.850),
        maxParallelRepairs = settings.optInt("max_parallel_repairs", 1),
        quarantineOnRepeatedFailure = settings.optBoolean("quarantine_on_repeated_failure", true),
        incidents = incidents,
    )
}

private suspend fun updateGuardianControls(session: AdminSession, draft: GuardianControlDraft) = withContext(Dispatchers.IO) {
    val connection = (URL("${BuildConfig.SUPABASE_URL}/rest/v1/rpc/guardian_set_controls").openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 15_000
        readTimeout = 20_000
        doOutput = true
        setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
        setRequestProperty("Authorization", "Bearer ${session.accessToken}")
        setRequestProperty("Accept", "application/json")
        setRequestProperty("Content-Type", "application/json")
    }
    try {
        connection.outputStream.use { it.write(guardianControlPayload(draft).toString().toByteArray()) }
        val code = connection.responseCode
        val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) {
            val message = runCatching {
                val json = JSONObject(body)
                json.optString("message").ifBlank { json.optString("hint") }.ifBlank { json.optString("details") }
            }.getOrNull().orEmpty()
            error(message.ifBlank { "Guardian update failed ($code)." })
        }
    } finally {
        connection.disconnect()
    }
}

private fun guardianGet(path: String, token: String): JSONArray {
    val connection = (URL("${BuildConfig.SUPABASE_URL}$path").openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 15_000
        readTimeout = 20_000
        setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
        setRequestProperty("Authorization", "Bearer $token")
        setRequestProperty("Accept", "application/json")
    }
    return try {
        val code = connection.responseCode
        val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (code !in 200..299) {
            val message = runCatching { JSONObject(body).optString("message") }.getOrNull().orEmpty()
            error(message.ifBlank { "Guardian read failed ($code)." })
        }
        JSONArray(body)
    } finally {
        connection.disconnect()
    }
}
