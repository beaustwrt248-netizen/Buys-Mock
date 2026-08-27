package com.buysloans.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val Bg = Color(0xFF030712)
private val CardBg = Color(0xFF0B1528)
private val Accent = Color(0xFF16C7FF)
private val Muted = Color(0xFF8EA6C4)
private val Good = Color(0xFF57E389)
private val Warn = Color(0xFFFFC857)

class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdminTelemetry.installCrashHandler(applicationContext)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = Bg, surface = CardBg)) {
                AdminRoot()
            }
        }
    }
}

@Composable
private fun AdminRoot() {
    var session by remember { mutableStateOf<AdminSession?>(null) }
    var snapshot by remember { mutableStateOf<AdminSnapshot?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current.applicationContext

    fun refresh(s: AdminSession) {
        busy = true; error = ""
        scope.launch {
            runCatching { AdminApi.load(s) }
                .onSuccess { snapshot = it }
                .onFailure {
                    AdminTelemetry.record(context, "Dashboard/Refresh", it)
                    error = it.message ?: "Admin data could not be loaded."
                }
            busy = false
        }
    }

    if (session == null) {
        LoginScreen(busy, error) { email, password ->
            busy = true; error = ""
            scope.launch {
                runCatching { AdminApi.signIn(email, password) }
                    .onSuccess { s ->
                        session = s
                        val pending = AdminTelemetry.pending(context)
                        if (pending.isNotEmpty()) {
                            runCatching { AdminApi.submitTelemetry(s, pending) }
                                .onSuccess { AdminTelemetry.clear(context) }
                        }
                        refresh(s)
                    }
                    .onFailure {
                        AdminTelemetry.record(context, "Login", it)
                        error = it.message ?: "Sign-in failed."
                        busy = false
                    }
            }
        }
    } else {
        Dashboard(session!!, snapshot, busy, error, onRefresh = { refresh(session!!) }, onSignOut = { session = null; snapshot = null })
    }
}

@Composable
private fun LoginScreen(busy: Boolean, error: String, onLogin: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center) {
        Text("MORLEY ADMIN", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Black)
        Text("Admin Control", fontSize = 30.sp, fontWeight = FontWeight.Black)
        Text("Authenticated read-only operational access. Remote actions are intentionally disabled in this stage.", color = Muted, modifier = Modifier.padding(vertical = 10.dp))
        OutlinedTextField(email, { email = it }, label = { Text("Admin email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 10.dp))
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
        Button(onClick = { onLogin(email, password) }, enabled = !busy, modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) { Text("Sign in", fontWeight = FontWeight.Black) }
    }
}

@Composable
private fun Dashboard(session: AdminSession, snapshot: AdminSnapshot?, busy: Boolean, error: String, onRefresh: () -> Unit, onSignOut: () -> Unit) {
    var tab by remember { mutableStateOf("Health") }
    val tabs = listOf("Health", "Tickets", "Staff alerts", "Users & devices", "Release")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("MORLEY ADMIN", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Black); Text(session.displayName, fontSize = 22.sp, fontWeight = FontWeight.Black); Text(session.role.uppercase(), color = Muted, fontSize = 11.sp) }
            TextButton(onClick = onSignOut) { Text("Sign out") }
        }
        Text("READ-ONLY MODE", color = Good, fontWeight = FontWeight.Black)
        tabs.chunked(3).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) { row.forEach { name -> FilterChip(selected = tab == name, onClick = { tab = name }, label = { Text(name, fontSize = 10.sp) }, modifier = Modifier.weight(1f)) }; repeat(3-row.size) { Spacer(Modifier.weight(1f)) } } }
        OutlinedButton(onClick = onRefresh, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Refresh read-only data") }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        when (tab) {
            "Health" -> HealthPanel(snapshot)
            "Tickets" -> ListPanel("Support ticket queue", snapshot?.tickets, ::ticketLine)
            "Staff alerts" -> ListPanel("Staff alerts", snapshot?.announcements, ::announcementLine)
            "Users & devices" -> UsersDevicesPanel(snapshot)
            "Release" -> ReleasePanel(snapshot?.config)
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun HealthPanel(s: AdminSnapshot?) {
    val devices = s?.devices
    val tickets = s?.tickets
    val errors = s?.errorEvents
    val openTickets = countWhere(tickets, "status", setOf("open", "in_progress", "waiting_on_user"))
    val staleDevices = countOlderThan(devices, "last_seen_at", 7L * 24 * 60 * 60 * 1000)
    val disabledUsers = countBoolean(s?.profiles, "is_enabled", false)
    val recentErrors = countRecent(errors, "occurred_at", 24L * 60 * 60 * 1000)
    Text("Production health", fontSize = 21.sp, fontWeight = FontWeight.Black)
    Metric("Open support tickets", openTickets.toString(), if (openTickets > 0) Warn else Good)
    Metric("Devices not seen in 7 days", staleDevices.toString(), if (staleDevices > 0) Warn else Good)
    Metric("Disabled staff/user accounts", disabledUsers.toString(), Muted)
    Metric("Admin app errors in 24 hours", recentErrors.toString(), if (recentErrors > 0) Warn else Good)
    if (errors != null && errors.length() > 0) {
        Text("Recent privacy-minimal Admin errors", fontWeight = FontWeight.Bold)
        for (i in 0 until minOf(errors.length(), 8)) {
            val j = errors.optJSONObject(i) ?: continue
            InfoCard("${j.optString("error_class")} • ${j.optString("failing_screen")}", "${j.optString("app_version")} • ${j.optString("device_model")} • ${j.optString("occurred_at")}")
        }
    }
    Text("Health is intentionally read-only. Telemetry excludes user identifiers, emails, ticket content, tokens, stack traces and free-form error messages. No restart, disable, force-update or account-control actions are available in this Admin app stage.", color = Muted, fontSize = 12.sp)
}

@Composable
private fun UsersDevicesPanel(s: AdminSnapshot?) {
    val devices = s?.devices
    val versions = if (devices == null) emptyList() else (0 until devices.length()).map { devices.optJSONObject(it)?.optString("app_version")?.takeIf(String::isNotBlank) }
    val currentVersion = currentReleaseVersion(s?.config)
    val adoption = summarizeVersionAdoption(versions, currentVersion)

    Text("App-version adoption", fontSize = 21.sp, fontWeight = FontWeight.Black)
    Text("Current release ${currentVersion ?: "unknown"}. Counts are derived from registered-device app versions only.", color = Muted, fontSize = 12.sp)
    Metric("On current release", adoption.current.toString(), Good)
    Metric("Outdated", adoption.outdated.toString(), if (adoption.outdated > 0) Warn else Good)
    Metric("Ahead / test", adoption.aheadOrTest.toString(), Muted)
    Metric("Unknown version", adoption.unknown.toString(), if (adoption.unknown > 0) Warn else Muted)
    ListPanel("Users", s?.profiles) { j -> "${j.optString("display_name").ifBlank { "Unnamed" }} • ${j.optString("role")} • ${if (j.optBoolean("is_enabled")) "enabled" else "disabled"}" }
    ListPanel("Devices", devices) { j -> "${j.optString("device_name").ifBlank { "Device" }} • ${j.optString("app_version").ifBlank { "unknown version" }} • ${j.optString("last_seen_at")}" }
}

@Composable
private fun ReleasePanel(config: JSONArray?) {
    Text("Release status", fontSize = 21.sp, fontWeight = FontWeight.Black)
    if (config == null || config.length() == 0) Text("No release configuration returned.", color = Muted)
    else for (i in 0 until config.length()) { val j = config.optJSONObject(i) ?: continue; val v = j.optJSONObject("value") ?: JSONObject(); InfoCard(j.optString("key"), "${v.optString("versionName")} (${v.optInt("versionCode")})${if (v.has("forceUpdate")) " • forceUpdate ${v.optBoolean("forceUpdate")}" else ""}") }
    Text("This screen does not publish releases, change minimum versions, or modify OTA metadata.", color = Muted, fontSize = 12.sp)
}

@Composable private fun ListPanel(title: String, data: JSONArray?, line: (JSONObject) -> String) { Text(title, fontSize = 21.sp, fontWeight = FontWeight.Black); if (data == null || data.length() == 0) Text("No records returned.", color = Muted) else for (i in 0 until minOf(data.length(), 50)) { data.optJSONObject(i)?.let { InfoCard(line(it), if (it.has("created_at")) it.optString("created_at") else "") } } }
@Composable private fun InfoCard(title: String, detail: String) { Card(colors = CardDefaults.cardColors(containerColor = CardBg), border = BorderStroke(1.dp, Accent.copy(alpha=.18f)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text(title, fontWeight = FontWeight.Bold); if (detail.isNotBlank()) Text(detail, color = Muted, fontSize = 11.sp) } } }
@Composable private fun Metric(label: String, value: String, color: Color) { Card(colors = CardDefaults.cardColors(containerColor = CardBg), modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = Muted); Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 19.sp) } } }
private fun currentReleaseVersion(config: JSONArray?): String? { if (config == null) return null; for (i in 0 until config.length()) { val row = config.optJSONObject(i) ?: continue; if (row.optString("key") == "current_release") return row.optJSONObject("value")?.optString("versionName")?.takeIf(String::isNotBlank) }; return null }
private fun ticketLine(j: JSONObject) = "${j.optString("priority").uppercase()} • ${j.optString("status")} • ${j.optString("subject")}" 
private fun announcementLine(j: JSONObject) = "${if (j.optBoolean("is_active")) "ACTIVE" else "INACTIVE"} • ${j.optString("audience")} • ${j.optString("title")}" 
private fun countWhere(a: JSONArray?, key: String, values: Set<String>): Int = if (a == null) 0 else (0 until a.length()).count { values.contains(a.optJSONObject(it)?.optString(key)) }
private fun countBoolean(a: JSONArray?, key: String, value: Boolean): Int = if (a == null) 0 else (0 until a.length()).count { a.optJSONObject(it)?.optBoolean(key) == value }
private fun countOlderThan(a: JSONArray?, key: String, ageMs: Long): Int { if (a == null) return 0; val cutoff = System.currentTimeMillis() - ageMs; return (0 until a.length()).count { val raw = a.optJSONObject(it)?.optString(key).orEmpty(); runCatching { java.time.Instant.parse(raw).toEpochMilli() < cutoff }.getOrDefault(false) } }
private fun countRecent(a: JSONArray?, key: String, ageMs: Long): Int { if (a == null) return 0; val cutoff = System.currentTimeMillis() - ageMs; return (0 until a.length()).count { val raw = a.optJSONObject(it)?.optString(key).orEmpty(); runCatching { java.time.Instant.parse(raw).toEpochMilli() >= cutoff }.getOrDefault(false) } }
