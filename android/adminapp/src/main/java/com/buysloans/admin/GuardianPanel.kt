package com.buysloans.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private data class GuardianViewState(
    val enabled: Boolean,
    val autoFixEnabled: Boolean,
    val killSwitch: Boolean,
    val mode: String,
    val learningEnabled: Boolean,
    val evolutionEnabled: Boolean,
    val confidenceThreshold: String,
    val maxParallelRepairs: Int,
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
    var error by remember { mutableStateOf("") }
    var reloadKey by remember { mutableStateOf(0) }

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
        "Live Guardian state and incident history. Backend RLS and human-approval boundaries remain authoritative.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp
    )

    if (loading) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() }
        return
    }
    if (error.isNotBlank()) {
        Text(error, color = MaterialTheme.colorScheme.error)
        Button(onClick = { reloadKey += 1 }, modifier = Modifier.fillMaxWidth()) { Text("Retry Guardian refresh") }
        return
    }

    val s = state ?: return
    GuardianStatusCard("Guardian", if (s.enabled) "RUNNING" else "PAUSED")
    GuardianStatusCard("Operating mode", s.mode.replace('_', ' ').uppercase())
    GuardianStatusCard("Automatic repair", if (s.autoFixEnabled) "ENABLED" else "DISABLED")
    GuardianStatusCard("Kill switch", if (s.killSwitch) "ENGAGED" else "CLEAR")
    GuardianStatusCard("Learning / evolution", "${if (s.learningEnabled) "Learning on" else "Learning off"} • ${if (s.evolutionEnabled) "Evolution proposals on" else "Evolution proposals off"}")
    GuardianStatusCard("Guard rails", "Confidence ${s.confidenceThreshold} • max ${s.maxParallelRepairs} parallel repair(s) • code changes require human approval")

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
    Button(onClick = { reloadKey += 1 }, modifier = Modifier.fillMaxWidth()) { Text("Refresh Guardian") }
    Text(
        "This Android view is intentionally read-only in this milestone. Guardian kill-switch, mode and approval mutations remain behind the existing guarded backend RPC/web Control Center until their native confirmation flow is separately validated.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp
    )
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
        "/rest/v1/guardian_settings?select=enabled,auto_fix_enabled,kill_switch,operating_mode,learning_enabled,evolution_enabled,confidence_threshold,max_parallel_repairs&singleton=eq.true&limit=1",
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
        mode = settings.optString("operating_mode").ifBlank { "observe" },
        learningEnabled = settings.optBoolean("learning_enabled"),
        evolutionEnabled = settings.optBoolean("evolution_enabled"),
        confidenceThreshold = settings.optString("confidence_threshold").ifBlank { "0.850" },
        maxParallelRepairs = settings.optInt("max_parallel_repairs", 1),
        incidents = incidents,
    )
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
