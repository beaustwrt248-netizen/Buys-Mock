package com.buysloans.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val SupportMuted = Color(0xFF8EA6C4)
private val SupportGood = Color(0xFF57E389)
private val SupportWarn = Color(0xFFFFC857)

@Composable
internal fun SupportOperationsPanel(
    session: AdminSession,
    tickets: JSONArray?,
    profiles: JSONArray? = null,
    busy: Boolean = false,
    onUpdated: () -> Unit = {}
) {
    val health = summarizeSupportTicketHealth(tickets)
    val scope = rememberCoroutineScope()
    var supportProfiles by remember(profiles) { mutableStateOf(profiles) }
    var selectedTicketId by remember { mutableStateOf("") }
    var conversation by remember { mutableStateOf<ProtectedConversationState?>(null) }
    var notes by remember { mutableStateOf<JSONArray?>(null) }
    var loadingWorkspace by remember { mutableStateOf(false) }
    var controlBusy by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }
    var replyText by remember(selectedTicketId) { mutableStateOf("") }
    var noteText by remember(selectedTicketId) { mutableStateOf("") }

    LaunchedEffect(session.userId, profiles) {
        if (supportProfiles == null && canAssignSupportTicket(session)) {
            runCatching { AdminApi.loadSupportAssigneeProfiles(session) }
                .onSuccess { supportProfiles = it }
                .onFailure { errorText = it.message ?: "Support assignees could not be loaded." }
        }
    }

    fun loadWorkspace(ticketId: String, ticketSubject: String) {
        if (ticketId.isBlank() || loadingWorkspace) return
        selectedTicketId = ticketId
        conversation = null
        notes = null
        feedback = ""
        errorText = ""
        loadingWorkspace = true
        scope.launch {
            val conversationResult = runCatching {
                ProtectedMessageCoordinator.load(session, ticketId, ticketSubject, 100)
            }
            val noteResult = runCatching { AdminApi.loadSupportNotes(session, ticketId, 100) }
            if (selectedTicketId == ticketId) {
                conversationResult
                    .onSuccess { conversation = it }
                    .onFailure { errorText = it.message ?: "Conversation could not be loaded." }
                noteResult
                    .onSuccess { notes = it }
                    .onFailure { if (errorText.isBlank()) errorText = it.message ?: "Internal notes could not be loaded." }
                loadingWorkspace = false
            }
        }
    }

    fun saveControls(command: SupportTicketUpdateCommand) {
        if (controlBusy || busy) return
        controlBusy = true
        feedback = ""
        errorText = ""
        scope.launch {
            runCatching { AdminApi.updateSupportTicket(session, command) }
                .onSuccess {
                    findSupportTicket(tickets, command.ticketId)?.apply {
                        put("status", command.status)
                        put("priority", command.priority)
                        if (canAssignSupportTicket(session)) put("assigned_to", command.assignedTo ?: JSONObject.NULL)
                    }
                    feedback = "Ticket controls saved and audited."
                    onUpdated()
                }
                .onFailure { errorText = it.message ?: "Support ticket could not be updated." }
            controlBusy = false
        }
    }

    fun sendReply(ticketId: String, ticketSubject: String) {
        if (controlBusy || replyText.isBlank()) return
        controlBusy = true
        feedback = ""
        errorText = ""
        val body = replyText
        scope.launch {
            runCatching { AdminApi.sendSupportReply(session, ticketId, body) }
                .onSuccess {
                    replyText = ""
                    feedback = "Update sent to the user."
                    runCatching { ProtectedMessageCoordinator.load(session, ticketId, ticketSubject, 100) }
                        .onSuccess { conversation = it }
                    onUpdated()
                }
                .onFailure { errorText = it.message ?: "Reply could not be sent." }
            controlBusy = false
        }
    }

    fun addNote(ticketId: String) {
        if (controlBusy || noteText.isBlank()) return
        controlBusy = true
        feedback = ""
        errorText = ""
        val body = noteText
        scope.launch {
            runCatching { AdminApi.addSupportInternalNote(session, ticketId, body) }
                .onSuccess {
                    noteText = ""
                    feedback = "Internal note saved."
                    runCatching { AdminApi.loadSupportNotes(session, ticketId, 100) }
                        .onSuccess { notes = it }
                }
                .onFailure { errorText = it.message ?: "Internal note could not be saved." }
            controlBusy = false
        }
    }

    Text("Support operations", fontSize = 21.sp, fontWeight = FontWeight.Black)
    Text("Open a ticket to view the full issue, reply to the user, add private staff notes and manage triage.", color = SupportMuted, fontSize = 12.sp)

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CompactMetric("Open", health.open, if (health.open > 0) SupportWarn else SupportGood, Modifier.weight(1f))
        CompactMetric("Overdue", health.overdue, if (health.overdue > 0) MaterialTheme.colorScheme.error else SupportGood, Modifier.weight(1f))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CompactMetric("Due soon", health.dueSoon, if (health.dueSoon > 0) SupportWarn else SupportGood, Modifier.weight(1f))
        CompactMetric("Unassigned", health.unassigned, if (health.unassigned > 0) SupportWarn else SupportGood, Modifier.weight(1f))
    }

    if (tickets == null || tickets.length() == 0) {
        Text("No support tickets returned.", color = SupportMuted)
        return
    }

    Text("Ticket queue", fontWeight = FontWeight.Bold)
    for (i in 0 until minOf(tickets.length(), 50)) {
        val ticket = tickets.optJSONObject(i) ?: continue
        val ticketId = ticket.optString("id")
        val ticketSubject = ticket.optString("subject").ifBlank { "Support ticket" }
        SupportTicketCard(
            title = supportTicketOperationalLine(ticket),
            detail = supportTicketOperationalDetail(ticket),
            selected = ticketId.isNotBlank() && ticketId == selectedTicketId,
            enabled = ticketId.isNotBlank() && !loadingWorkspace && !controlBusy,
            onSelect = { loadWorkspace(ticketId, ticketSubject) }
        )
    }

    val selected = findSupportTicket(tickets, selectedTicketId)
    if (selected != null) {
        Text("Ticket workspace", fontSize = 19.sp, fontWeight = FontWeight.Black)
        TicketDetailCard(selected)
        SupportTicketControlsPanel(
            session = session,
            ticket = selected,
            profiles = supportProfiles,
            busy = busy || controlBusy,
            onSave = ::saveControls
        )

        OutlinedTextField(
            value = replyText,
            onValueChange = { replyText = it.take(5000) },
            label = { Text("Reply to user") },
            supportingText = { Text("Visible to the ticket owner · ${replyText.length}/5000") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
            enabled = !controlBusy && !busy
        )
        Button(
            onClick = { sendReply(selectedTicketId, selected.optString("subject")) },
            enabled = !controlBusy && !busy && replyText.trim().isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Send user update", fontWeight = FontWeight.Black) }

        OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it.take(5000) },
            label = { Text("Internal note") },
            supportingText = { Text("Private to Admin/Manager and assigned Staff · ${noteText.length}/5000") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
            enabled = !controlBusy && !busy
        )
        OutlinedButton(
            onClick = { addNote(selectedTicketId) },
            enabled = !controlBusy && !busy && noteText.trim().isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save internal note", fontWeight = FontWeight.Bold) }

        InternalNotesPanel(notes)
    }

    if (loadingWorkspace || controlBusy) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator()
            Text(if (controlBusy) "Saving support action…" else "Opening ticket…", color = SupportMuted)
        }
    }
    if (feedback.isNotBlank()) Text(feedback, color = SupportGood, fontSize = 12.sp)
    if (errorText.isNotBlank()) Text(errorText, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)

    conversation?.let { state -> ProtectedMessagesPanel(state.ticketSubject, state.messages) }

    Text(
        "User replies are stored in the protected support conversation. Internal notes are a separate RLS-protected record and are never exposed to ticket owners. Staff remain limited to tickets assigned to them; assignment stays Admin/Manager-only.",
        color = SupportMuted,
        fontSize = 12.sp
    )
}

@Composable
private fun TicketDetailCard(ticket: JSONObject) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .35f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(ticket.optString("subject").ifBlank { "Support ticket" }, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text(ticket.optString("category").uppercase(), color = SupportMuted, fontSize = 10.sp)
            Text(ticket.optString("description").ifBlank { "No description provided." })
            Text("Status: ${supportStatusLabel(ticket.optString("status"))} · Priority: ${ticket.optString("priority").replaceFirstChar { it.uppercase() }}", color = SupportMuted, fontSize = 11.sp)
            val app = ticket.optString("app_version")
            val device = ticket.optString("device_model")
            val android = ticket.optString("android_version")
            if (app.isNotBlank() || device.isNotBlank() || android.isNotBlank()) {
                Text("App ${app.ifBlank { "unknown" }} · ${device.ifBlank { "device unknown" }} · Android ${android.ifBlank { "unknown" }}", color = SupportMuted, fontSize = 10.sp)
            }
            Text("Created ${ticket.optString("created_at")}", color = SupportMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun InternalNotesPanel(notes: JSONArray?) {
    Text("Internal notes", fontSize = 17.sp, fontWeight = FontWeight.Black)
    if (notes == null) {
        Text("Open a ticket to load private notes.", color = SupportMuted, fontSize = 11.sp)
        return
    }
    if (notes.length() == 0) {
        Text("No internal notes yet.", color = SupportMuted, fontSize = 11.sp)
        return
    }
    for (i in 0 until minOf(notes.length(), 100)) {
        val note = notes.optJSONObject(i) ?: continue
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(note.optString("body"))
                Text(note.optString("created_at"), color = SupportMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SupportTicketControlsPanel(
    session: AdminSession,
    ticket: JSONObject,
    profiles: JSONArray?,
    busy: Boolean,
    onSave: (SupportTicketUpdateCommand) -> Unit
) {
    val ticketId = ticket.optString("id")
    val currentStatus = ticket.optString("status")
    val currentPriority = ticket.optString("priority")
    val currentAssignee = ticket.optString("assigned_to")
    val assignees = eligibleSupportAssignees(profiles)
    var status by remember(ticketId, currentStatus) { mutableStateOf(currentStatus) }
    var priority by remember(ticketId, currentPriority) { mutableStateOf(currentPriority) }
    var assignedTo by remember(ticketId, currentAssignee) { mutableStateOf(currentAssignee) }
    val canTriage = canUpdateSupportTicketTriage(session)
    val canAssign = canAssignSupportTicket(session)
    val changed = status != currentStatus || priority != currentPriority || (canAssign && assignedTo != currentAssignee)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .28f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Triage", fontWeight = FontWeight.Black)
            ChoiceMenu("Status", status, SUPPORT_TICKET_STATUSES.map { it to supportStatusLabel(it) }, canTriage && !busy) { status = it }
            ChoiceMenu("Priority", priority, SUPPORT_TICKET_PRIORITIES.map { it to it.replaceFirstChar { c -> c.uppercase() } }, canTriage && !busy) { priority = it }
            ChoiceMenu("Assigned to", assignedTo, listOf("" to "Unassigned") + assignees.map { it.id to "${it.label} · ${it.role}" }, canAssign && !busy) { assignedTo = it }
            Button(
                onClick = { onSave(SupportTicketUpdateCommand(ticketId, status, priority, assignedTo.takeIf(String::isNotBlank))) },
                enabled = canTriage && !busy && changed && ticketId.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save triage", fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun ChoiceMenu(label: String, value: String, options: List<Pair<String, String>>, enabled: Boolean, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val display = options.firstOrNull { it.first == value }?.second ?: value.ifBlank { "Unassigned" }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text("$label: $display") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option.second) }, onClick = { expanded = false; onSelect(option.first) })
            }
        }
    }
}

@Composable
private fun CompactMetric(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, color = SupportMuted, fontSize = 11.sp)
            Text(value.toString(), color = color, fontWeight = FontWeight.Black, fontSize = 19.sp)
        }
    }
}

@Composable
private fun SupportTicketCard(title: String, detail: String, selected: Boolean, enabled: Boolean, onSelect: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(if (selected) 2.dp else 1.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (selected) .55f else .18f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onSelect)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, color = SupportMuted, fontSize = 11.sp)
            Text(if (selected) "OPEN · ticket workspace below" else "Tap to open ticket", color = if (selected) MaterialTheme.colorScheme.primary else SupportMuted, fontSize = 10.sp)
        }
    }
}

private fun findSupportTicket(tickets: JSONArray?, ticketId: String): JSONObject? {
    if (tickets == null || ticketId.isBlank()) return null
    for (i in 0 until tickets.length()) {
        val ticket = tickets.optJSONObject(i) ?: continue
        if (ticket.optString("id") == ticketId) return ticket
    }
    return null
}

private fun supportStatusLabel(status: String): String = when (status) {
    "open" -> "Open"
    "in_progress" -> "In progress"
    "waiting_on_user" -> "Waiting on user"
    "resolved" -> "Resolved"
    "closed" -> "Closed"
    else -> status
}
