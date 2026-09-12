package com.buysloans.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

private const val WORKSPACE_OVERVIEW = "Overview"
private const val WORKSPACE_SUPPORT = "Support"
private const val WORKSPACE_HEALTH = "Health"
private const val WORKSPACE_GUARDIAN = "Guardian"
private const val WORKSPACE_NOTIFICATIONS = "Notifications"
private const val WORKSPACE_USERS = "Users & devices"
private const val WORKSPACE_ALERTS = "Staff alerts"
private const val WORKSPACE_CONTROLS = "Controls"
private const val WORKSPACE_AUDIT = "Audit"
private const val WORKSPACE_RELEASE = "Release"

private val NativeGood = Color(0xFF57E389)
private val NativeWarn = Color(0xFFFFC857)

@Composable
internal fun AdminNativeDashboard(
    session: AdminSession,
    onSignOut: () -> Unit,
    onSessionExpired: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val fullAccess = AdminAppAccessPolicy.canReadFullSnapshot(session)
    var snapshot by remember { mutableStateOf<AdminSnapshot?>(null) }
    var workspace by remember(session.role) { mutableStateOf(if (fullAccess) WORKSPACE_OVERVIEW else WORKSPACE_SUPPORT) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var lastRefreshedAt by remember { mutableLongStateOf(0L) }

    fun fail(throwable: Throwable) {
        val message = throwable.message.orEmpty()
        if (message.contains("Admin session expired. Sign in again.", ignoreCase = true)) {
            onSessionExpired()
            return
        }
        error = message.ifBlank { "Admin data could not be loaded." }
    }

    fun refresh() {
        if (busy) return
        busy = true
        error = ""
        scope.launch {
            runCatching { AdminApi.load(session) }
                .onSuccess {
                    snapshot = it
                    lastRefreshedAt = System.currentTimeMillis()
                }
                .onFailure(::fail)
            busy = false
        }
    }

    fun executeUserControl(command: UserControlCommand) {
        if (busy) return
        busy = true
        error = ""
        scope.launch {
            runCatching { UserControlCoordinator.execute(session, command, confirmed = true) }
                .onSuccess {
                    runCatching { AdminApi.load(session) }
                        .onSuccess {
                            snapshot = it
                            lastRefreshedAt = System.currentTimeMillis()
                        }
                        .onFailure(::fail)
                }
                .onFailure(::fail)
            busy = false
        }
    }

    fun updateMaintenance(current: MaintenanceConfig, enabled: Boolean, message: String, otaEnabled: Boolean) {
        if (busy) return
        busy = true
        error = ""
        scope.launch {
            runCatching { AdminApi.updateMaintenanceConfig(session, current, enabled, message, otaEnabled) }
                .onSuccess {
                    runCatching { AdminApi.load(session) }
                        .onSuccess {
                            snapshot = it
                            lastRefreshedAt = System.currentTimeMillis()
                        }
                        .onFailure(::fail)
                }
                .onFailure(::fail)
            busy = false
        }
    }

    LaunchedEffect(session.userId) {
        refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AdminNativeHeader(
            session = session,
            workspace = workspace,
            busy = busy,
            lastRefreshedAt = lastRefreshedAt,
            onOverview = { workspace = if (fullAccess) WORKSPACE_OVERVIEW else WORKSPACE_SUPPORT },
            onRefresh = ::refresh,
            onSignOut = onSignOut
        )

        if (error.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = .45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Admin data error", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                    Text(error)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = ::refresh, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Retry") }
                        OutlinedButton(onClick = onSignOut, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Sign out") }
                    }
                }
            }
        }

        when (workspace) {
            WORKSPACE_OVERVIEW -> AdminNativeOverview(session, snapshot) { workspace = it }
            WORKSPACE_SUPPORT -> SupportOperationsPanel(
                session = session,
                tickets = snapshot?.tickets,
                profiles = snapshot?.profiles,
                busy = busy,
                onUpdated = ::refresh
            )
            WORKSPACE_HEALTH -> AdminNativeHealth(snapshot)
            WORKSPACE_GUARDIAN -> if (fullAccess) GuardianPanel(session) else RestrictedWorkspace()
            WORKSPACE_NOTIFICATIONS -> if (fullAccess) ManualNotificationPanel(session, snapshot?.profiles, busy) else RestrictedWorkspace()
            WORKSPACE_USERS -> if (fullAccess) {
                UserManagementPanel(session, snapshot?.profiles, busy, ::executeUserControl)
                DeviceSummaryPanel(snapshot?.devices)
            } else RestrictedWorkspace()
            WORKSPACE_ALERTS -> if (fullAccess) AnnouncementPanel(snapshot?.announcements) else RestrictedWorkspace()
            WORKSPACE_CONTROLS -> if (fullAccess) MaintenanceNativePanel(snapshot?.config, busy, ::updateMaintenance) else RestrictedWorkspace()
            WORKSPACE_AUDIT -> if (fullAccess) AuditTimelinePanel(snapshot?.auditEvents) else RestrictedWorkspace()
            WORKSPACE_RELEASE -> if (fullAccess) AdminReleasePanel(snapshot) else RestrictedWorkspace()
            else -> AdminNativeOverview(session, snapshot) { workspace = it }
        }
    }
}

@Composable
private fun AdminNativeHeader(
    session: AdminSession,
    workspace: String,
    busy: Boolean,
    lastRefreshedAt: Long,
    onOverview: () -> Unit,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .22f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("MORLEY ADMIN", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text(session.displayName, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("${session.role.uppercase()} • NATIVE CONTROL MODE", color = NativeGood, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(workspace, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                TextButton(onClick = onSignOut) { Text("Sign out") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (workspace !in setOf(WORKSPACE_OVERVIEW, WORKSPACE_SUPPORT)) {
                    OutlinedButton(onClick = onOverview, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Overview") }
                }
                Button(onClick = onRefresh, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Text(if (busy) "Refreshing…" else "Refresh")
                }
            }
            Text(
                when {
                    lastRefreshedAt <= 0L && busy -> "Loading live Admin data…"
                    lastRefreshedAt <= 0L -> "Admin data has not loaded yet."
                    else -> "Live data loaded • ${((System.currentTimeMillis() - lastRefreshedAt) / 1000L).coerceAtLeast(0L)}s ago"
                },
                color = if (lastRefreshedAt > 0L) NativeGood else NativeWarn,
                fontSize = 11.sp
            )
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun AdminNativeOverview(session: AdminSession, snapshot: AdminSnapshot?, onOpen: (String) -> Unit) {
    val fullAccess = AdminAppAccessPolicy.canReadFullSnapshot(session)
    Text("Operations overview", fontSize = 22.sp, fontWeight = FontWeight.Black)

    if (!fullAccess) {
        NativeMetric("Support tickets", snapshot?.tickets?.length()?.toString() ?: "—", NativeWarn)
        Text("Staff access is intentionally limited to the support workspace.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        WorkspaceButton("Open Support", "Support queue, protected conversation and triage controls") { onOpen(WORKSPACE_SUPPORT) }
        return
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NativeMetric("Tickets", snapshot?.tickets?.length()?.toString() ?: "—", NativeWarn, Modifier.weight(1f))
        NativeMetric("Errors", snapshot?.errorEvents?.length()?.toString() ?: "—", if ((snapshot?.errorEvents?.length() ?: 0) > 0) NativeWarn else NativeGood, Modifier.weight(1f))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NativeMetric("Users", snapshot?.profiles?.length()?.toString() ?: "—", NativeGood, Modifier.weight(1f))
        NativeMetric("Devices", snapshot?.devices?.length()?.toString() ?: "—", NativeGood, Modifier.weight(1f))
    }

    WorkspaceButton("Support", "Support queue, replies, notes and triage") { onOpen(WORKSPACE_SUPPORT) }
    WorkspaceButton("Guardian", "Guardian control centre and repair boundaries") { onOpen(WORKSPACE_GUARDIAN) }
    WorkspaceButton("Notifications", "Send audited app notifications") { onOpen(WORKSPACE_NOTIFICATIONS) }
    WorkspaceButton("Users & devices", "User access, invites, reset tools and registered devices") { onOpen(WORKSPACE_USERS) }
    WorkspaceButton("Staff alerts", "Current operational announcements") { onOpen(WORKSPACE_ALERTS) }
    WorkspaceButton("Safe controls", "Maintenance and OTA feature flags") { onOpen(WORKSPACE_CONTROLS) }
    WorkspaceButton("Audit", "Read-only privileged activity timeline") { onOpen(WORKSPACE_AUDIT) }
    WorkspaceButton("Release", "Current release and device adoption") { onOpen(WORKSPACE_RELEASE) }
    WorkspaceButton("Production health", "Support, error and device-health snapshot") { onOpen(WORKSPACE_HEALTH) }
}

@Composable
private fun WorkspaceButton(title: String, detail: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 68.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(title, fontWeight = FontWeight.Black)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}

@Composable
private fun NativeMetric(label: String, value: String, color: Color, modifier: Modifier = Modifier.fillMaxWidth()) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}

@Composable
private fun AdminNativeHealth(snapshot: AdminSnapshot?) {
    Text("Production health", fontSize = 22.sp, fontWeight = FontWeight.Black)
    val tickets = snapshot?.tickets?.length() ?: 0
    val errors = snapshot?.errorEvents?.length() ?: 0
    val devices = snapshot?.devices?.length() ?: 0
    NativeMetric("Returned support tickets", tickets.toString(), if (tickets > 0) NativeWarn else NativeGood)
    NativeMetric("Returned error events", errors.toString(), if (errors > 0) NativeWarn else NativeGood)
    NativeMetric("Registered devices", devices.toString(), NativeGood)
    Text("Health is read-only here. Use the dedicated workspaces for privileged actions.", color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun DeviceSummaryPanel(devices: JSONArray?) {
    Text("Registered devices", fontSize = 21.sp, fontWeight = FontWeight.Black)
    if (devices == null || devices.length() == 0) {
        Text("No registered devices returned.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    for (index in 0 until minOf(devices.length(), 40)) {
        val row = devices.optJSONObject(index) ?: continue
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text(row.optString("device_name").ifBlank { "Device" }, fontWeight = FontWeight.Bold)
                Text(
                    listOf(row.optString("platform"), row.optString("app_version"), row.optString("last_seen_at"))
                        .filter { it.isNotBlank() }
                        .joinToString(" • "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun AnnouncementPanel(rows: JSONArray?) {
    Text("Staff alerts", fontSize = 21.sp, fontWeight = FontWeight.Black)
    if (rows == null || rows.length() == 0) {
        Text("No staff alerts returned.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    for (index in 0 until rows.length()) {
        val row = rows.optJSONObject(index) ?: continue
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(row.optString("title").ifBlank { "Announcement" }, fontWeight = FontWeight.Bold)
                Text(row.optString("body"))
                Text(
                    listOf(row.optString("audience"), row.optString("created_at"), if (row.optBoolean("is_active")) "active" else "inactive")
                        .filter { it.isNotBlank() }
                        .joinToString(" • "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun MaintenanceNativePanel(
    config: JSONArray?,
    busy: Boolean,
    onSave: (MaintenanceConfig, Boolean, String, Boolean) -> Unit
) {
    val current = remember(config) { maintenanceConfig(config) }
    Text("Safe remote controls", fontSize = 21.sp, fontWeight = FontWeight.Black)
    if (current == null) {
        Text("Feature flag configuration is not available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    var enabled by remember(current.fullFeatureFlags.toString()) { mutableStateOf(current.enabled) }
    var message by remember(current.fullFeatureFlags.toString()) { mutableStateOf(current.message) }
    var otaEnabled by remember(current.fullFeatureFlags.toString()) { mutableStateOf(current.otaEnabled) }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Maintenance mode", fontWeight = FontWeight.Black)
                    Text("Controls the approved maintenance boundary only.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
                Switch(checked = enabled, onCheckedChange = { enabled = it }, enabled = !busy)
            }
            OutlinedTextField(
                value = message,
                onValueChange = { message = it.take(160) },
                label = { Text("Maintenance message") },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("OTA updates", fontWeight = FontWeight.Black)
                    Text("Keep signed update discovery available.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
                Switch(checked = otaEnabled, onCheckedChange = { otaEnabled = it }, enabled = !busy)
            }
            Button(
                onClick = { onSave(current, enabled, message, otaEnabled) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (busy) "Saving…" else "Save safe controls", fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun AdminReleasePanel(snapshot: AdminSnapshot?) {
    Text("Release visibility", fontSize = 21.sp, fontWeight = FontWeight.Black)
    val currentVersion = currentReleaseVersion(snapshot?.config)
    Text("Current Morley release: ${currentVersion ?: "unknown"}")
    val versions = buildList {
        val devices = snapshot?.devices
        if (devices != null) {
            for (index in 0 until devices.length()) add(devices.optJSONObject(index)?.optString("app_version"))
        }
    }
    val adoption = summarizeVersionAdoption(versions, currentVersion)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NativeMetric("Current", adoption.current.toString(), NativeGood, Modifier.weight(1f))
        NativeMetric("Outdated", adoption.outdated.toString(), if (adoption.outdated > 0) NativeWarn else NativeGood, Modifier.weight(1f))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NativeMetric("Ahead/test", adoption.aheadOrTest.toString(), NativeWarn, Modifier.weight(1f))
        NativeMetric("Unknown", adoption.unknown.toString(), NativeWarn, Modifier.weight(1f))
    }
    Text("Release visibility is read-only. Signed Admin APK publication remains controlled by the protected release workflow.", color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun currentReleaseVersion(config: JSONArray?): String? {
    if (config == null) return null
    for (index in 0 until config.length()) {
        val row = config.optJSONObject(index) ?: continue
        if (row.optString("key") != "current_release") continue
        val value = row.opt("value")
        return when (value) {
            is JSONObject -> listOf("version", "versionName", "name").firstNotNullOfOrNull { key -> value.optString(key).takeIf { it.isNotBlank() } }
            is String -> value.takeIf { it.isNotBlank() }
            else -> value?.toString()?.takeIf { it.isNotBlank() }
        }
    }
    return null
}

@Composable
private fun RestrictedWorkspace() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text("Restricted workspace", fontWeight = FontWeight.Black)
            Text("This account does not have access to this Admin workspace.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
