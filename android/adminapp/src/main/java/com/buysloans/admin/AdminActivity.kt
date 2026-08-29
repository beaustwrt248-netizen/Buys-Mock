package com.buysloans.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
private val TextPrimary = Color(0xFFF4F7FB)

class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdminTelemetry.installCrashHandler(applicationContext)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Accent,
                    background = Bg,
                    surface = CardBg,
                    onBackground = TextPrimary,
                    onSurface = TextPrimary,
                    onSurfaceVariant = Muted
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = Bg, contentColor = TextPrimary) {
                    AdminRoot()
                }
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

    fun updateMaintenance(s: AdminSession, current: MaintenanceConfig, enabled: Boolean, message: String) {
        busy = true; error = ""
        scope.launch {
            runCatching { AdminApi.updateMaintenanceConfig(s, current, enabled, message) }
                .onSuccess { refresh(s) }
                .onFailure {
                    AdminTelemetry.record(context, "Controls/Maintenance", it)
                    error = it.message ?: "Maintenance configuration could not be updated."
                    busy = false
                }
        }
    }

    fun updateUserAccess(s: AdminSession, command: UserControlCommand) {
        busy = true; error = ""
        scope.launch {
            runCatching { UserControlCoordinator.execute(s, command, confirmed = true) }
                .onSuccess { refresh(s) }
                .onFailure {
                    AdminTelemetry.record(context, "Users/AccessControl", it)
                    error = it.message ?: "User access could not be updated."
                    busy = false
                }
        }
    }

    if (session == null) {
        LoginScreen(busy, error) { email, password, captchaToken ->
            busy = true; error = ""
            scope.launch {
                runCatching { AdminApi.signIn(email, password, captchaToken) }
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
        Dashboard(
            session = session!!,
            snapshot = snapshot,
            busy = busy,
            error = error,
            onRefresh = { refresh(session!!) },
            onUpdateMaintenance = { current, enabled, message -> updateMaintenance(session!!, current, enabled, message) },
            onUserControl = { command -> updateUserAccess(session!!, command) },
            onSignOut = { session = null; snapshot = null }
        )
    }
}

@Composable
private fun LoginScreen(busy: Boolean, error: String, onLogin: (String, String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var captchaToken by remember { mutableStateOf("") }
    var captchaError by remember { mutableStateOf("") }
    var captchaEpoch by remember { mutableIntStateOf(0) }

    LaunchedEffect(error, busy) {
        if (error.isNotBlank() && !busy) {
            captchaToken = ""
            captchaEpoch += 1
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("MORLEY ADMIN", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Black)
        Text("Admin Control", color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Black)
        Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", color = Muted, fontSize = 11.sp)
        Text("Authenticated operational access. Writable actions are limited to allowlisted, audited maintenance and Admin-only user-access controls.", color = Muted, modifier = Modifier.padding(vertical = 10.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Admin email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.EmailAddress }
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.Password }
        )
        Spacer(Modifier.height(12.dp))
        Text("Security check", color = TextPrimary, fontWeight = FontWeight.Bold)
        key(captchaEpoch) {
            CaptchaChallenge(
                modifier = Modifier.fillMaxWidth().height(110.dp),
                onToken = { token -> captchaToken = token; captchaError = "" },
                onFailure = { message -> captchaToken = ""; captchaError = message }
            )
        }
        if (captchaToken.isNotBlank()) Text("Security check complete.", color = Good, fontSize = 12.sp)
        if (captchaError.isNotBlank()) Text(captchaError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 10.dp))
        if (busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
            Text("Signing in securely…", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        } else if (isAdminLoginReady(email, password, captchaToken, busy = false)) {
            Text("Email, password and security check are ready.", color = Good, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        }
        Button(
            onClick = { onLogin(email.trim(), password, captchaToken) },
            enabled = isAdminLoginReady(email, password, captchaToken, busy),
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
        ) { Text(if (busy) "Signing in…" else "Sign in", fontWeight = FontWeight.Black) }
    }
}

@Composable
private fun Dashboard(
    session: AdminSession,
    snapshot: AdminSnapshot?,
    busy: Boolean,
    error: String,
    onRefresh: () -> Unit,
    onUpdateMaintenance: (MaintenanceConfig, Boolean, String) -> Unit,
    onUserControl: (UserControlCommand) -> Unit,
    onSignOut: () -> Unit
) {
    var tab by remember { mutableStateOf("Health") }
    val tabs = listOf("Health", "Tickets", "Staff alerts", "Users & devices", "Controls", "Audit", "Release")
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("MORLEY ADMIN", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Text(session.displayName, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(session.role.uppercase(), color = Muted, fontSize = 11.sp)
            }
            TextButton(onClick = onSignOut) { Text("Sign out") }
        }
        Text("CONTROLLED ADMIN MODE", color = Good, fontWeight = FontWeight.Black)
        tabs.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                if (row.size == 1) Spacer(Modifier.weight(1f))
                row.forEach { name ->
                    FilterChip(
                        selected = tab == name,
                        onClick = { tab = name },
                        label = { Text(name, fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        OutlinedButton(onClick = onRefresh, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Refresh Admin data") }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        when (tab) {
            "Health" -> HealthPanel(snapshot)
            "Tickets" -> SupportOperationsPanel(session, snapshot?.tickets)
            "Staff alerts" -> ListPanel("Staff alerts", snapshot?.announcements, ::announcementLine)
            "Users & devices" -> UsersDevicesPanel(session, snapshot, busy, onUserControl)
            "Controls" -> MaintenancePanel(snapshot?.config, busy, onUpdateMaintenance)
            "Audit" -> AuditTimelinePanel(snapshot?.auditEvents)
            "Release" -> ReleasePanel(snapshot?.config)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.Black)
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
    SectionTitle("Production health")
    Metric("Open support tickets", openTickets.toString(), if (openTickets > 0) Warn else Good)
    Metric("Devices not seen in 7 days", staleDevices.toString(), if (staleDevices > 0) Warn else Good)
    Metric("Disabled staff/user accounts", disabledUsers.toString(), Muted)
    Metric("Admin app errors in 24 hours", recentErrors.toString(), if (recentErrors > 0) Warn else Good)
    if (errors != null && errors.length() > 0) {
        Text("Recent privacy-minimal Admin errors", color = TextPrimary, fontWeight = FontWeight.Bold)
        for (i in 0 until minOf(errors.length(), 8)) {
            val j = errors.optJSONObject(i) ?: continue
            InfoCard("${j.optString("error_class")} • ${j.optString("failing_screen")}", "${j.optString("app_version")} • ${j.optString("device_model")} • ${j.optString("occurred_at")}")
        }
    }
    Text("Health remains read-only. Telemetry excludes user identifiers, emails, ticket content, tokens, stack traces and free-form error messages. Writable actions remain isolated to audited maintenance and Admin-only user-access controls.", color = Muted, fontSize = 12.sp)
}

@Composable
private fun UsersDevicesPanel(
    session: AdminSession,
    s: AdminSnapshot?,
    busy: Boolean,
    onUserControl: (UserControlCommand) -> Unit
) {
    val devices = s?.devices
    val versions = if (devices == null) emptyList() else (0 until devices.length()).map { devices.optJSONObject(it)?.optString("app_version")?.takeIf(String::isNotBlank) }
    val currentVersion = currentReleaseVersion(s?.config)
    val adoption = summarizeVersionAdoption(versions, currentVersion)

    SectionTitle("App-version adoption")
    Text("Current release ${currentVersion ?: "unknown"}. Counts are derived from registered-device app versions only.", color = Muted, fontSize = 12.sp)
    Metric("On current release", adoption.current.toString(), Good)
    Metric("Outdated", adoption.outdated.toString(), if (adoption.outdated > 0) Warn else Good)
    Metric("Ahead / test", adoption.aheadOrTest.toString(), Muted)
    Metric("Unknown version", adoption.unknown.toString(), if (adoption.unknown > 0) Warn else Muted)
    UserManagementPanel(session, s?.profiles, busy, onUserControl)
    ListPanel("Devices", devices) { j -> "${j.optString("device_name").ifBlank { "Device" }} • ${j.optString("app_version").ifBlank { "unknown version" }} • ${j.optString("last_seen_at")}" }
}

@Composable
private fun MaintenancePanel(config: JSONArray?, busy: Boolean, onUpdate: (MaintenanceConfig, Boolean, String) -> Unit) {
    val current = maintenanceConfig(config)
    SectionTitle("Safe remote configuration")
    Text("Only maintenance mode and its short notice are writable here. Pricing, scanner, valuation-history, release and minimum-version fields are preserved and rejected if changed.", color = Muted, fontSize = 12.sp)
    if (current == null) {
        Text("Feature flags are not available to this Admin session.", color = Warn)
        return
    }
    var enabled by remember(current.enabled, current.message) { mutableStateOf(current.enabled) }
    var message by remember(current.enabled, current.message) { mutableStateOf(current.message) }
    Card(colors = CardDefaults.cardColors(containerColor = CardBg, contentColor = TextPrimary), border = BorderStroke(1.dp, Accent.copy(alpha=.18f)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) { Text("Maintenance mode", fontWeight = FontWeight.Bold); Text(if (enabled) "Users may be shown the configured maintenance state by clients that consume this flag." else "Normal configured state.", color = Muted, fontSize = 11.sp) }
                Switch(checked = enabled, onCheckedChange = { enabled = it }, enabled = !busy)
            }
            OutlinedTextField(value = message, onValueChange = { message = it.take(160) }, label = { Text("Maintenance notice") }, supportingText = { Text("${message.length}/160") }, enabled = !busy, modifier = Modifier.fillMaxWidth())
            Button(onClick = { onUpdate(current, enabled, message) }, enabled = !busy && (enabled != current.enabled || message.trim() != current.message), modifier = Modifier.fillMaxWidth()) { Text("Save audited maintenance control", fontWeight = FontWeight.Black) }
        }
    }
    Text("Every successful change is written through the existing Admin/Manager RPC and recorded in the durable admin audit log. This screen cannot publish a release, change minimum versions, or modify OTA metadata.", color = Muted, fontSize = 12.sp)
}

@Composable
private fun ReleasePanel(config: JSONArray?) {
    SectionTitle("Release status")
    if (config == null || config.length() == 0) Text("No release configuration returned.", color = Muted)
    else for (i in 0 until config.length()) { val j = config.optJSONObject(i) ?: continue; if (j.optString("key") == "feature_flags") continue; val v = j.optJSONObject("value") ?: JSONObject(); InfoCard(j.optString("key"), "${v.optString("versionName")} (${v.optInt("versionCode")})${if (v.has("forceUpdate")) " • forceUpdate ${v.optBoolean("forceUpdate")}" else ""}") }
    Text("This screen does not publish releases, change minimum versions, or modify OTA metadata.", color = Muted, fontSize = 12.sp)
}

@Composable private fun ListPanel(title: String, data: JSONArray?, line: (JSONObject) -> String) { SectionTitle(title); if (data == null || data.length() == 0) Text("No records returned.", color = Muted) else for (i in 0 until minOf(data.length(), 50)) { data.optJSONObject(i)?.let { InfoCard(line(it), if (it.has("created_at")) it.optString("created_at") else "") } } }
@Composable private fun InfoCard(title: String, detail: String) { Card(colors = CardDefaults.cardColors(containerColor = CardBg, contentColor = TextPrimary), border = BorderStroke(1.dp, Accent.copy(alpha=.18f)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text(title, fontWeight = FontWeight.Bold); if (detail.isNotBlank()) Text(detail, color = Muted, fontSize = 11.sp) } } }
@Composable private fun Metric(label: String, value: String, color: Color) { Card(colors = CardDefaults.cardColors(containerColor = CardBg, contentColor = TextPrimary), modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = Muted); Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 19.sp) } } }
private fun currentReleaseVersion(config: JSONArray?): String? { if (config == null) return null; for (i in 0 until config.length()) { val row = config.optJSONObject(i) ?: continue; if (row.optString("key") == "current_release") return row.optJSONObject("value")?.optString("versionName")?.takeIf(String::isNotBlank) }; return null }
private fun ticketLine(j: JSONObject) = "${j.optString("priority").uppercase()} • ${j.optString("status")} • ${j.optString("subject")}" 
private fun announcementLine(j: JSONObject) = "${if (j.optBoolean("is_active")) "ACTIVE" else "INACTIVE"} • ${j.optString("audience")} • ${j.optString("title")}" 
private fun countWhere(a: JSONArray?, key: String, values: Set<String>): Int = if (a == null) 0 else (0 until a.length()).count { values.contains(a.optJSONObject(it)?.optString(key)) }
private fun countBoolean(a: JSONArray?, key: String, value: Boolean): Int = if (a == null) 0 else (0 until a.length()).count { a.optJSONObject(it)?.optBoolean(key) == value }
private fun countOlderThan(a: JSONArray?, key: String, ageMs: Long): Int { if (a == null) return 0; val cutoff = System.currentTimeMillis() - ageMs; return (0 until a.length()).count { val raw = a.optJSONObject(it)?.optString(key).orEmpty(); runCatching { java.time.Instant.parse(raw).toEpochMilli() < cutoff }.getOrDefault(false) } }
private fun countRecent(a: JSONArray?, key: String, ageMs: Long): Int { if (a == null) return 0; val cutoff = System.currentTimeMillis() - ageMs; return (0 until a.length()).count { val raw = a.optJSONObject(it)?.optString(key).orEmpty(); runCatching { java.time.Instant.parse(raw).toEpochMilli() >= cutoff }.getOrDefault(false) } }
