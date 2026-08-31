package com.buysloans.hub

import android.content.Intent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private data class PendingUserChange(
    val user: EmbeddedAdminUser,
    val action: String,
    val requestedRole: String? = null,
)

class EmbeddedAdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!AuthManager.isSignedIn(this) || !AuthManager.canUseAdminMode(this)) {
            finish()
            return
        }
        setContent { MaterialTheme(colorScheme = MorleyColorScheme) { EmbeddedAdminScreen() } }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun EmbeddedAdminScreen() {
        val context = this@EmbeddedAdminActivity
        val scope = rememberCoroutineScope()
        var snapshot by remember { mutableStateOf<EmbeddedAdminSnapshot?>(null) }
        var busy by remember { mutableStateOf(false) }
        var savingTicket by remember { mutableStateOf("") }
        var savingUser by remember { mutableStateOf("") }
        var pendingUserChange by remember { mutableStateOf<PendingUserChange?>(null) }
        var error by remember { mutableStateOf("") }
        var feedback by remember { mutableStateOf("") }

        fun refresh() {
            if (busy) return
            busy = true
            error = ""
            feedback = ""
            scope.launch {
                runCatching { EmbeddedAdminClient.load(context) }
                    .onSuccess { snapshot = it }
                    .onFailure {
                        error = it.message ?: "Admin mode could not be loaded."
                        if (!AuthManager.canUseAdminMode(context)) finish()
                    }
                busy = false
            }
        }

        fun saveTicket(ticket: EmbeddedAdminTicket, status: String, priority: String, assignedTo: String?) {
            if (savingTicket.isNotBlank()) return
            savingTicket = ticket.id
            error = ""
            feedback = ""
            scope.launch {
                runCatching { EmbeddedAdminClient.updateTicket(context, ticket.id, status, priority, assignedTo) }
                    .onSuccess {
                        feedback = "Ticket updated successfully. Server-side access policies remain authoritative."
                        runCatching { EmbeddedAdminClient.load(context) }.onSuccess { snapshot = it }
                    }
                    .onFailure { error = it.message ?: "Support ticket could not be updated." }
                savingTicket = ""
            }
        }

        fun executeUserChange(change: PendingUserChange) {
            if (savingUser.isNotBlank()) return
            savingUser = change.user.id
            error = ""
            feedback = ""
            scope.launch {
                runCatching {
                    EmbeddedAdminClient.updateUserAccess(
                        context = context,
                        targetUserId = change.user.id,
                        action = change.action,
                        requestedRole = change.requestedRole,
                    )
                }.onSuccess {
                    feedback = when (change.action) {
                        "enable" -> "${change.user.displayName} was enabled through the audited user-control service."
                        "disable" -> "${change.user.displayName} was disabled through the audited user-control service."
                        else -> "${change.user.displayName}'s role was updated through the audited user-control service."
                    }
                    runCatching { EmbeddedAdminClient.load(context) }.onSuccess { snapshot = it }
                }.onFailure {
                    error = it.message ?: "User access could not be updated."
                }
                savingUser = ""
            }
        }

        pendingUserChange?.let { change ->
            val actionText = when (change.action) {
                "enable" -> "enable this account"
                "disable" -> "disable this account"
                "set_role" -> "change this account to ${change.requestedRole?.replaceFirstChar { it.uppercase() }}"
                else -> "change this account"
            }
            AlertDialog(
                onDismissRequest = { pendingUserChange = null },
                title = { Text("Confirm user access change") },
                text = {
                    Text("${change.user.displayName}: $actionText? This action is sent through the protected Admin user-control service and recorded by the existing governance path.")
                },
                dismissButton = {
                    TextButton(onClick = { pendingUserChange = null }) { Text("Cancel") }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            pendingUserChange = null
                            executeUserChange(change)
                        },
                        colors = if (change.action == "disable") {
                            ButtonDefaults.buttonColors(containerColor = Color(0xFF7D2B38), contentColor = Color.White)
                        } else ButtonDefaults.buttonColors()
                    ) { Text("Confirm", fontWeight = FontWeight.Black) }
                }
            )
        }

        LaunchedEffect(Unit) { refresh() }
        Scaffold(
            containerColor = MorleyBackground,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MorleyBackground,
                        titleContentColor = MorleyTextPrimary
                    ),
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Text("‹", color = MorleyAccent, fontSize = 34.sp)
                        }
                    },
                    title = { Text("Admin Mode", fontWeight = FontWeight.Black) }
                )
            }
        ) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AdminCard(
                    "PRIVILEGED SESSION",
                    "${AuthManager.accountLabel(context)} • ${AuthManager.role(context).replaceFirstChar { it.uppercase() }}"
                )
                Text("Operational overview", color = MorleyTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)
                if (busy && snapshot == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (error.isNotBlank()) AdminMessage(error, MorleyDanger)
                if (feedback.isNotBlank()) AdminMessage(feedback, MorleySuccess)

                snapshot?.let { s ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AdminMetric("Open tickets", s.openTickets.toString(), Modifier.weight(1f))
                        AdminMetric("Active users", s.enabledUsers.toString(), Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AdminMetric("Devices", s.registeredDevices.toString(), Modifier.weight(1f))
                        AdminMetric("Recent errors", s.recentErrors.toString(), Modifier.weight(1f))
                    }
                    AdminMetric("Recent audit events", s.recentAuditEvents.toString(), Modifier.fillMaxWidth())

                    AdminSectionTitle("Support operations", "Change ticket status, priority and assignment directly from Morley.")
                    if (s.tickets.isEmpty()) {
                        AdminEmptyState("No support tickets need action.")
                    } else {
                        val assignees = s.users.filter { it.enabled && it.role in setOf("admin", "manager", "staff") }
                        s.tickets.take(20).forEach { ticket ->
                            TicketControlCard(
                                ticket = ticket,
                                assignees = assignees,
                                busy = savingTicket == ticket.id,
                                onSave = { status, priority, assignedTo -> saveTicket(ticket, status, priority, assignedTo) }
                            )
                        }
                    }

                    val canManageUsers = AuthManager.role(context) == "admin"
                    AdminSectionTitle(
                        "Users",
                        if (canManageUsers) {
                            "Manage enabled state and Staff/Manager/Admin roles through the audited user-control service. Your own active Admin account is protected."
                        } else {
                            "Managers have read-only user visibility. User-access changes remain Admin-only and server-authorised."
                        }
                    )
                    if (s.users.isEmpty()) {
                        AdminEmptyState("No authorised user records returned.")
                    } else {
                        s.users.take(20).forEach { user ->
                            UserControlCard(
                                user = user,
                                isSelf = user.id == s.actorUserId,
                                canManage = canManageUsers,
                                busy = savingUser == user.id,
                                onAction = { action, requestedRole ->
                                    pendingUserChange = PendingUserChange(user, action, requestedRole)
                                }
                            )
                        }
                    }

                    AdminSectionTitle("Devices", "Recently registered Morley devices and app versions.")
                    if (s.devices.isEmpty()) AdminEmptyState("No registered devices returned.")
                    else s.devices.take(12).forEach { device ->
                        AdminListRow(
                            title = device.name,
                            detail = listOf(device.platform, device.appVersion.takeIf { it.isNotBlank() }?.let { "v$it" }, device.lastSeenAt.takeIf { it.isNotBlank() })
                                .filterNotNull().joinToString(" • "),
                            badge = "DEVICE"
                        )
                    }

                    AdminSectionTitle("Recent errors", "Latest app health events visible to this Admin session.")
                    if (s.errors.isEmpty()) AdminEmptyState("No recent error events returned.")
                    else s.errors.take(10).forEach { event ->
                        AdminListRow(event.title, listOf(event.detail, event.occurredAt).filter { it.isNotBlank() }.joinToString(" • "), "ERROR")
                    }

                    AdminSectionTitle("Audit history", "Recent privileged actions from the durable Admin audit log.")
                    if (s.auditEvents.isEmpty()) AdminEmptyState("No recent audit events returned.")
                    else s.auditEvents.take(10).forEach { event ->
                        AdminListRow(event.title, listOf(event.detail, event.occurredAt).filter { it.isNotBlank() }.joinToString(" • "), "AUDIT")
                    }
                }

                Button(
                    onClick = { refresh() },
                    enabled = !busy && savingTicket.isBlank() && savingUser.isBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text(if (busy) "Refreshing…" else "Refresh Admin Data", fontWeight = FontWeight.Black) }
                OutlinedButton(
                    onClick = { startActivity(Intent(context, DiagnosticsActivity::class.java)) },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Open System Diagnostics", fontWeight = FontWeight.Bold) }
                Text(
                    "Admin Mode only appears for Admin/Manager accounts. Supabase RLS and existing audited server policies remain the security boundary for every read and write.",
                    color = MorleyTextMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TicketControlCard(
    ticket: EmbeddedAdminTicket,
    assignees: List<EmbeddedAdminUser>,
    busy: Boolean,
    onSave: (String, String, String?) -> Unit,
) {
    var status by remember(ticket.id, ticket.status) { mutableStateOf(ticket.status) }
    var priority by remember(ticket.id, ticket.priority) { mutableStateOf(ticket.priority) }
    var assignedTo by remember(ticket.id, ticket.assignedTo) { mutableStateOf(ticket.assignedTo) }
    val assigneeOptions = listOf(null to "Unassigned") + assignees.map { it.id to "${it.displayName} • ${it.role}" }
    val changed = status != ticket.status || priority != ticket.priority || assignedTo != ticket.assignedTo

    Card(
        colors = CardDefaults.cardColors(containerColor = MorleySurface),
        border = BorderStroke(1.dp, if (ticket.priority == "urgent") MorleyDanger.copy(alpha = .7f) else MorleyBorder),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(ticket.subject, color = MorleyTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text(ticket.category.uppercase(), color = MorleyAccent, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
                Surface(color = MorleyAccentSoft, shape = RoundedCornerShape(999.dp)) {
                    Text(statusLabel(ticket.status), Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = MorleyAccent, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
            if (ticket.description.isNotBlank()) Text(ticket.description, color = MorleyTextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
            val deviceLine = listOf(ticket.appVersion.takeIf { it.isNotBlank() }?.let { "App $it" }, ticket.deviceModel.takeIf { it.isNotBlank() }, ticket.createdAt.takeIf { it.isNotBlank() })
                .filterNotNull().joinToString(" • ")
            if (deviceLine.isNotBlank()) Text(deviceLine, color = MorleyTextMuted, fontSize = 10.sp)

            AdminChoice("Status", status, EmbeddedAdminClient.ticketStatuses.map { it to statusLabel(it) }, !busy) { status = it }
            AdminChoice("Priority", priority, EmbeddedAdminClient.ticketPriorities.map { it to it.replaceFirstChar { c -> c.uppercase() } }, !busy) { priority = it }
            AdminNullableChoice("Assignee", assignedTo, assigneeOptions, !busy) { assignedTo = it }

            Button(
                onClick = { onSave(status, priority, assignedTo) },
                enabled = changed && !busy,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Save Ticket Controls", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun UserControlCard(
    user: EmbeddedAdminUser,
    isSelf: Boolean,
    canManage: Boolean,
    busy: Boolean,
    onAction: (String, String?) -> Unit,
) {
    val safeCurrentRole = user.role.takeIf { it in EmbeddedAdminClient.assignableRoles } ?: "staff"
    var requestedRole by remember(user.id, user.role) { mutableStateOf(safeCurrentRole) }
    val roleChanged = requestedRole != user.role

    Card(
        colors = CardDefaults.cardColors(containerColor = MorleySurface),
        border = BorderStroke(1.dp, MorleyBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(user.displayName, color = MorleyTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    Text(
                        "${user.role.replaceFirstChar { it.uppercase() }} • ${if (user.enabled) "Enabled" else "Disabled"}",
                        color = MorleyTextSecondary,
                        fontSize = 11.sp
                    )
                }
                Text(
                    when {
                        isSelf -> "YOU"
                        user.enabled -> "ACTIVE"
                        else -> "OFF"
                    },
                    color = if (user.enabled) MorleyAccent else MorleyTextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }

            when {
                isSelf -> Text(
                    "Your active Admin account is protected from role and enabled-state changes in the app.",
                    color = MorleyTextMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                !canManage -> Text(
                    "Read-only for Manager accounts.",
                    color = MorleyTextMuted,
                    fontSize = 11.sp
                )
                else -> {
                    AdminChoice(
                        label = "Role",
                        selected = requestedRole,
                        choices = EmbeddedAdminClient.assignableRoles.map { it to it.replaceFirstChar { c -> c.uppercase() } },
                        enabled = !busy,
                    ) { requestedRole = it }
                    OutlinedButton(
                        onClick = { onAction("set_role", requestedRole) },
                        enabled = roleChanged && !busy,
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) { Text("Save Role", fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = { onAction(if (user.enabled) "disable" else "enable", null) },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        colors = if (user.enabled) {
                            ButtonDefaults.buttonColors(containerColor = Color(0xFF57202A), contentColor = Color(0xFFFFD9DE))
                        } else ButtonDefaults.buttonColors()
                    ) {
                        if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text(if (user.enabled) "Disable Account" else "Enable Account", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminChoice(label: String, selected: String, choices: List<Pair<String, String>>, enabled: Boolean, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text("$label: ${choices.firstOrNull { it.first == selected }?.second ?: selected}", Modifier.weight(1f))
            Text("⌄")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            choices.forEach { (value, title) ->
                DropdownMenuItem(text = { Text(title) }, onClick = { onSelected(value); expanded = false })
            }
        }
    }
}

@Composable
private fun AdminNullableChoice(label: String, selected: String?, choices: List<Pair<String?, String>>, enabled: Boolean, onSelected: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text("$label: ${choices.firstOrNull { it.first == selected }?.second ?: "Unassigned"}", Modifier.weight(1f))
            Text("⌄")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            choices.forEach { (value, title) ->
                DropdownMenuItem(text = { Text(title) }, onClick = { onSelected(value); expanded = false })
            }
        }
    }
}

@Composable
private fun AdminMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MorleySurfaceRaised),
        border = BorderStroke(1.dp, MorleyBorder),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = MorleyTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(value, color = MorleyAccent, fontSize = 26.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun AdminCard(kicker: String, body: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MorleySurface),
        border = BorderStroke(1.dp, MorleyBorder),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(kicker, color = MorleyAccent, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text(body, color = MorleyTextSecondary, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun AdminSectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = MorleyTextPrimary, fontSize = 21.sp, fontWeight = FontWeight.Black)
        Text(subtitle, color = MorleyTextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun AdminListRow(title: String, detail: String, badge: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MorleySurface),
        border = BorderStroke(1.dp, MorleyBorder),
        shape = RoundedCornerShape(15.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(13.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text(title, color = MorleyTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (detail.isNotBlank()) Text(detail, color = MorleyTextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
            }
            Text(badge, color = MorleyAccent, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun AdminEmptyState(text: String) {
    Surface(color = MorleySurface, shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, MorleyBorder), modifier = Modifier.fillMaxWidth()) {
        Text(text, Modifier.padding(15.dp), color = MorleyTextSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun AdminMessage(text: String, color: Color) {
    Surface(color = MorleySurface, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, color.copy(alpha = .45f)), modifier = Modifier.fillMaxWidth()) {
        Text(text, Modifier.padding(12.dp), color = color, fontSize = 12.sp)
    }
}

private fun statusLabel(status: String): String = status.replace('_', ' ').replaceFirstChar { it.uppercase() }
