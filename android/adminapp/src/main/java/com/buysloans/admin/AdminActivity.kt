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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
                Surface(Modifier.fillMaxSize(), color = Bg, contentColor = TextPrimary) { AdminRoot() }
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
    var lastRefreshedAtMillis by remember { mutableLongStateOf(0L) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current.applicationContext

    fun refresh(s: AdminSession) {
        busy = true
        error = ""
        scope.launch {
            runCatching { AdminApi.load(s) }
                .onSuccess {
                    snapshot = it
                    lastRefreshedAtMillis = System.currentTimeMillis()
                }
                .onFailure {
                    AdminTelemetry.record(context, "Dashboard/Refresh", it)
                    error = it.message ?: "Admin data could not be loaded."
                }
            busy = false
        }
    }

    fun updateMaintenance(s: AdminSession, current: MaintenanceConfig, enabled: Boolean, message: String, otaEnabled: Boolean) {
        busy = true
        error = ""
        scope.launch {
            runCatching { AdminApi.updateMaintenanceConfig(s, current, enabled, message, otaEnabled) }
                .onSuccess { refresh(s) }
                .onFailure {
                    AdminTelemetry.record(context, "Controls/RemoteConfig", it)
                    error = it.message ?: "Remote configuration could not be updated."
                    busy = false
                }
        }
    }

    fun updateUserAccess(s: AdminSession, command: UserControlCommand) {
        busy = true
        error = ""
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

    val currentSession = session
    if (currentSession == null) {
        LoginScreen(busy, error) { email, password, captchaToken ->
            busy = true
            error = ""
            scope.launch {
                runCatching { AdminApi.signIn(email, password, captchaToken) }
                    .onSuccess { signedIn ->
                        session = signedIn
                        val pending = AdminTelemetry.pending(context)
                        if (pending.isNotEmpty()) runCatching { AdminApi.submitTelemetry(signedIn, pending) }
                            .onSuccess { AdminTelemetry.clear(context) }
                        refresh(signedIn)
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
            session = currentSession,
            snapshot = snapshot,
            busy = busy,
            error = error,
            lastRefreshedAtMillis = lastRefreshedAtMillis,
            onRefresh = { refresh(currentSession) },
            onUpdateMaintenance = { current, enabled, message, ota -> updateMaintenance(currentSession, current, enabled, message, ota) },
            onUserControl = { updateUserAccess(currentSession, it) },
            onSignOut = { session = null; snapshot = null; lastRefreshedAtMillis = 0L }
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
        if (error.isNotBlank() && !busy) { captchaToken = ""; captchaEpoch += 1 }
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("MORLEY ADMIN", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Black)
        Text("Admin Control", fontSize = 30.sp, fontWeight = FontWeight.Black)
        Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", color = Muted, fontSize = 11.sp)
        Text("Authenticated operational access with backend-enforced role and audit boundaries.", color = Muted, modifier = Modifier.padding(vertical = 10.dp))
        OutlinedTextField(
            value = email, onValueChange = { email = it }, label = { Text("Admin email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.EmailAddress }
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it }, label = { Text("Password") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.Password }
        )
        Spacer(Modifier.height(12.dp))
        Text("Security check", fontWeight = FontWeight.Bold)
        key(captchaEpoch) {
            CaptchaChallenge(
                modifier = Modifier.fillMaxWidth().height(110.dp),
                onToken = { captchaToken = it; captchaError = "" },
                onFailure = { captchaToken = ""; captchaError = it }
            )
        }
        if (captchaToken.isNotBlank()) Text("Security check complete.", color = Good, fontSize = 12.sp)
        if (captchaError.isNotBlank()) Text(captchaError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 10.dp))
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 12.dp))
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
    lastRefreshedAtMillis: Long,
    onRefresh: () -> Unit,
    onUpdateMaintenance: (MaintenanceConfig, Boolean, String, Boolean) -> Unit,
    onUserControl: (UserControlCommand) -> Unit,
    onSignOut: () -> Unit
) {
    var tab by remember { mutableStateOf("Overview") }
    var now by remember(lastRefreshedAtMillis) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lastRefreshedAtMillis) {
        while (lastRefreshedAtMillis > 0L) { delay(60_000L); now = System.currentTimeMillis() }
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp).padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, Accent.copy(alpha = .22f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("MORLEY ADMIN", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text(session.displayName, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        Text("${session.role.uppercase()} • CONTROLLED ADMIN MODE", color = Good, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onSignOut) { Text("Sign out") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (tab != "Overview") OutlinedButton(onClick = { tab = "Overview" }, modifier = Modifier.weight(1f)) { Text("Overview") }
                    Button(onClick = onRefresh, enabled = !busy, modifier = Modifier.weight(1f)) { Text(if (busy) "Refreshing…" else "Refresh") }
                }
                Freshness(lastRefreshedAtMillis, now)
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        when (tab) {
            "Overview" -> AdminOverview(session, snapshot) { tab = it }
            "Health" -> HealthPanel(snapshot)
            "Tickets" -> SupportOperationsPanel(session, snapshot?.tickets, snapshot?.profiles, busy, onRefresh)
            "Guardian" -> GuardianPanel(session)
            "Notifications" -> { SectionTitle("Notifications"); ManualNotificationPanel(session, snapshot?.profiles, busy) }
            "Staff alerts" -> ListPanel("Staff alerts", snapshot?.announcements, ::announcementLine)
            "Users & devices" -> UsersDevicesPanel(session, snapshot, busy, onUserControl)
            "Controls" -> MaintenancePanel(snapshot?.config, busy, onUpdateMaintenance)
            "Audit" -> AuditTimelinePanel(snapshot?.auditEvents)
            "Release" -> ReleasePanel(snapshot?.config)
        }
    }
}

@Composable
private fun Freshness(last: Long, now: Long) {
    if (last <= 0L) { Text("Admin data has not loaded yet.", color = Warn, fontSize = 11.sp); return }
    val freshness = supportQueueFreshness(last, now)
    val time = remember(last) { DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(last)) }
    Text(
        if (freshness.stale) "Data stale • $time • ${freshness.ageMinutes}m old — refresh before acting" else "Data current • refreshed $time",
        color = if (freshness.stale) Warn else Good,
        fontSize = 11.sp,
        fontWeight = if (freshness.stale) FontWeight.Bold else FontWeight.Normal
    )
}

@Composable
private fun AdminOverview(session: AdminSession, s: AdminSnapshot?, onOpen: (String) -> Unit) {
    val openTickets = countWhere(s?.tickets, "status", setOf("open", "in_progress", "waiting_on_user"))
    val staleDevices = countOlderThan(s?.devices, "last_seen_at", 7L * 24 * 60 * 60 * 1000)
    val disabledUsers = countBoolean(s?.profiles, "is_enabled", false)
    val recentErrors = countRecent(s?.errorEvents, "occurred_at", 24L * 60 * 60 * 1000)
    SectionTitle("Operations overview")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CompactMetric("Open tickets", openTickets.toString(), if (openTickets > 0) Warn else Good, Modifier.weight(1f))
        CompactMetric("Errors 24h", recentErrors.toString(), if (recentErrors > 0) Warn else Good, Modifier.weight(1f))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CompactMetric("Stale devices", staleDevices.toString(), if (staleDevices > 0) Warn else Good, Modifier.weight(1f))
        CompactMetric("Disabled users", disabledUsers.toString(), if (disabledUsers > 0) Warn else Good, Modifier.weight(1f))
    }
    Text("Workspaces", fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 4.dp))
    DashboardActionRow("Support", "$openTickets active", { onOpen("Tickets") }, "Guardian", if (session.role in setOf("admin", "manager")) "Control Center" else "Restricted", { onOpen("Guardian") })
    DashboardActionRow("Notifications", "Send + target", { onOpen("Notifications") }, "Users & devices", "$staleDevices stale", { onOpen("Users & devices") })
    DashboardActionRow("Staff alerts", "Announcements", { onOpen("Staff alerts") }, "Safe controls", "Maintenance + OTA", { onOpen("Controls") })
    DashboardActionRow("Audit", "Privileged activity", { onOpen("Audit") }, "Release", currentReleaseVersion(s?.config)?.let { "Morley $it" } ?: "Version status", { onOpen("Release") })
    OutlinedButton(onClick = { onOpen("Health") }, modifier = Modifier.fillMaxWidth()) { Text("Production health") }
}

@Composable
private fun DashboardActionRow(leftTitle: String, leftDetail: String, leftAction: () -> Unit, rightTitle: String, rightDetail: String, rightAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DashboardAction(leftTitle, leftDetail, leftAction, Modifier.weight(1f))
        DashboardAction(rightTitle, rightDetail, rightAction, Modifier.weight(1f))
    }
}

@Composable
private fun DashboardAction(title: String, detail: String, action: () -> Unit, modifier: Modifier) {
    OutlinedButton(onClick = action, modifier = modifier.heightIn(min = 74.dp), contentPadding = PaddingValues(10.dp)) {
        Column(Modifier.fillMaxWidth()) { Text(title, fontWeight = FontWeight.Black); Text(detail, color = Muted, fontSize = 10.sp) }
    }
}

@Composable
private fun CompactMetric(label: String, value: String, color: Color, modifier: Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBg), modifier = modifier) {
        Column(Modifier.padding(12.dp)) { Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.Black); Text(label, color = Muted, fontSize = 10.sp) }
    }
}

@Composable private fun SectionTitle(text: String) { Text(text, fontSize = 21.sp, fontWeight = FontWeight.Black) }

@Composable
private fun HealthPanel(s: AdminSnapshot?) {
    SectionTitle("Production health")
    Metric("Open support tickets", countWhere(s?.tickets, "status", setOf("open", "in_progress", "waiting_on_user")).toString(), Warn)
    Metric("Devices not seen in 7 days", countOlderThan(s?.devices, "last_seen_at", 7L * 24 * 60 * 60 * 1000).toString(), Warn)
    Metric("Disabled accounts", countBoolean(s?.profiles, "is_enabled", false).toString(), Muted)
    Metric("Admin app errors in 24 hours", countRecent(s?.errorEvents, "occurred_at", 24L * 60 * 60 * 1000).toString(), Warn)
}

@Composable
private fun UsersDevicesPanel(session: AdminSession, s: AdminSnapshot?, busy: Boolean, onUserControl: (UserControlCommand) -> Unit) {
    SectionTitle("Users & devices")
    UserManagementPanel(session, s?.profiles, busy, onUserControl)
    ListPanel("Devices", s?.devices) { j -> "${j.optString("device_name").ifBlank { "Device" }} • ${j.optString("app_version").ifBlank { "unknown version" }} • ${j.optString("last_seen_at")}" }
}

@Composable
private fun MaintenancePanel(config: JSONArray?, busy: Boolean, onUpdate: (MaintenanceConfig, Boolean, String, Boolean) -> Unit) {
    val current = maintenanceConfig(config)
    SectionTitle("Safe remote configuration")
    if (current == null) { Text("Feature flags are not available to this Admin session.", color = Warn); return }
    var enabled by remember(current.enabled, current.message, current.otaEnabled) { mutableStateOf(current.enabled) }
    var message by remember(current.enabled, current.message, current.otaEnabled) { mutableStateOf(current.message) }
    var ota by remember(current.enabled, current.message, current.otaEnabled) { mutableStateOf(current.otaEnabled) }
    Card(colors = CardDefaults.cardColors(containerColor = CardBg), border = BorderStroke(1.dp, Accent.copy(alpha = .18f)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Maintenance mode", fontWeight = FontWeight.Bold); Switch(enabled, { enabled = it }, enabled = !busy) }
            OutlinedTextField(message, { message = it.take(160) }, label = { Text("Maintenance notice") }, supportingText = { Text("${message.length}/160") }, enabled = !busy, modifier = Modifier.fillMaxWidth())
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("OTA updates", fontWeight = FontWeight.Bold); Switch(ota, { ota = it }, enabled = !busy) }
            Button(onClick = { onUpdate(current, enabled, message, ota) }, enabled = !busy && (enabled != current.enabled || message.trim() != current.message || ota != current.otaEnabled), modifier = Modifier.fillMaxWidth()) { Text("Save audited remote controls", fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun ReleasePanel(config: JSONArray?) {
    SectionTitle("Release status")
    val remote = maintenanceConfig(config)
    if (remote != null) InfoCard("OTA delivery", if (remote.otaEnabled) "ENABLED • signed update checks active" else "DISABLED • update checks paused")
    if (config == null || config.length() == 0) Text("No release configuration returned.", color = Muted)
    else for (i in 0 until config.length()) {
        val j = config.optJSONObject(i) ?: continue
        if (j.optString("key") == "feature_flags") continue
        val v = j.optJSONObject("value") ?: JSONObject()
        InfoCard(j.optString("key"), "${v.optString("versionName")} (${v.optInt("versionCode")})")
    }
}

@Composable private fun ListPanel(title: String, data: JSONArray?, line: (JSONObject) -> String) { SectionTitle(title); if (data == null || data.length() == 0) Text("No records returned.", color = Muted) else for (i in 0 until minOf(data.length(), 50)) data.optJSONObject(i)?.let { InfoCard(line(it), it.optString("created_at")) } }
@Composable private fun InfoCard(title: String, detail: String) { Card(colors = CardDefaults.cardColors(containerColor = CardBg), border = BorderStroke(1.dp, Accent.copy(alpha = .18f)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text(title, fontWeight = FontWeight.Bold); if (detail.isNotBlank()) Text(detail, color = Muted, fontSize = 11.sp) } } }
@Composable private fun Metric(label: String, value: String, color: Color) { Card(colors = CardDefaults.cardColors(containerColor = CardBg), modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = Muted); Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 19.sp) } } }

private fun currentReleaseVersion(config: JSONArray?): String? { if (config == null) return null; for (i in 0 until config.length()) { val row = config.optJSONObject(i) ?: continue; if (row.optString("key") == "current_release") return row.optJSONObject("value")?.optString("versionName")?.takeIf(String::isNotBlank) }; return null }
private fun announcementLine(j: JSONObject) = "${if (j.optBoolean("is_active")) "ACTIVE" else "INACTIVE"} • ${j.optString("audience")} • ${j.optString("title")}" 
private fun countWhere(a: JSONArray?, key: String, values: Set<String>): Int = if (a == null) 0 else (0 until a.length()).count { values.contains(a.optJSONObject(it)?.optString(key)) }
private fun countBoolean(a: JSONArray?, key: String, value: Boolean): Int = if (a == null) 0 else (0 until a.length()).count { a.optJSONObject(it)?.optBoolean(key) == value }
private fun countOlderThan(a: JSONArray?, key: String, ageMs: Long): Int { if (a == null) return 0; val cutoff = System.currentTimeMillis() - ageMs; return (0 until a.length()).count { runCatching { Instant.parse(a.optJSONObject(it)?.optString(key).orEmpty()).toEpochMilli() < cutoff }.getOrDefault(false) } }
private fun countRecent(a: JSONArray?, key: String, ageMs: Long): Int { if (a == null) return 0; val cutoff = System.currentTimeMillis() - ageMs; return (0 until a.length()).count { runCatching { Instant.parse(a.optJSONObject(it)?.optString(key).orEmpty()).toEpochMilli() >= cutoff }.getOrDefault(false) } }
