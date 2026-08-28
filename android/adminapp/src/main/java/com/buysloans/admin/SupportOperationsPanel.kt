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
    var conversationError by remember { mutableStateOf("") }
    var loadingConversation by remember { mutableStateOf(false) }
    var controlBusy by remember { mutableStateOf(false) }
    var controlMessage by remember { mutableStateOf("") }
    var controlError by remember { mutableStateOf("") }

    LaunchedEffect(session.userId, profiles) {
        if (supportProfiles == null && canAssignSupportTicket(session)) {
            runCatching { AdminApi.loadSupportAssigneeProfiles(session) }
                .onSuccess { supportProfiles = it }
                .onFailure { controlError = it.message ?: "Support assignees could not be loaded." }
        }
    }

    fun loadConversation(ticketId: String, ticketSubject: String) {
        if (ticketId.isBlank() || loadingConversation) return
        selectedTicketId = ticketId
        conversation = null
        conversationError = ""
        controlMessage = ""
        controlError = ""
        loadingConversation = true
        scope.launch {
            runCatching {
                ProtectedMessageCoordinator.load(
                    session = session,
                    ticketId = ticketId,
                    ticketSubject = ticketSubject,
                    limit = 100
                )
            }.onSuccess {
                if (selectedTicketId == ticketId) conversation = it
            }.onFailure {
                if (selectedTicketId == ticketId) {
                    conversationError = it.message ?: "Protected conversation could not be loaded."
                }
            }
            if (selectedTicketId == ticketId) loadingConversation = false
        }
    }

    fun saveControls(command: SupportTicketUpdateCommand) {
        if (controlBusy || busy) return
        controlBusy = true
        controlMessage = ""
        controlError = ""
        scope.launch {
            runCatching { AdminApi.updateSupportTicket(session, command) }
                .onSuccess {
                    findSupportTicket(tickets, command.ticketId)?.apply {
                        put("status", command.status)
                        put("priority", command.priority)
                        if (canAssignSupportTicket(session)) {
                            put("assigned_to", command.assignedTo ?: JSONObject.NULL)
                        }
                    }
                    controlMessage = "Ticket triage updated. Existing database triggers recorded the change and recalculated SLA when required."
                    onUpdated()
                }
                .onFailure {
                    controlError = it.message ?: "Support ticket could not be updated."
                }
            controlBusy = false
        }
    }

    Text("Support operations", fontSize = 21.sp, fontWeight = FontWeight.Black)
    Text(
        "Admin/Manager can triage and assign. Staff can triage only tickets returned by the existing assigned-ticket Supabase RLS; assignment remains unavailable to Staff.",
        color = SupportMuted,
        fontSize = 12.sp
    )
    SupportMetric("Open", health.open, if (health.open > 0) SupportWarn else SupportGood)
    SupportMetric("Overdue SLA", health.overdue, if (health.overdue > 0) MaterialTheme.colorScheme.error else SupportGood)
    SupportMetric("Due within 2 hours", health.dueSoon, if (health.dueSoon > 0) SupportWarn else SupportGood)
    SupportMetric("Awaiting first response", health.awaitingFirstResponse, if (health.awaitingFirstResponse > 0) SupportWarn else SupportGood)
    SupportMetric("Unassigned", health.unassigned, if (health.unassigned > 0) SupportWarn else SupportGood)

    if (tickets == null || tickets.length() == 0) {
        Text("No support tickets returned.", color = SupportMuted)
        return
    }

    Text("Operational queue", fontWeight = FontWeight.Bold)
    for (i in 0 until minOf(tickets.length(), 50)) {
        val ticket = tickets.optJSONObject(i) ?: continue
        val ticketId = ticket.optString("id")
        val ticketSubject = ticket.optString("subject").ifBlank { "Support ticket" }
        SupportTicketCard(
            title = supportTicketOperationalLine(ticket),
            detail = supportTicketOperationalDetail(ticket),
            selected = ticketId.isNotBlank() && ticketId == selectedTicketId,
            enabled = ticketId.isNotBlank() && !loadingConversation && !controlBusy,
            onSelect = { loadConversation(ticketId, ticketSubject) }
        )
    }

    findSupportTicket(tickets, selectedTicketId)?.let { selected ->
        SupportTicketControlsPanel(
            session = session,
            ticket = selected,
            profiles = supportProfiles,
            busy = busy || controlBusy,
            onSave = ::saveControls
        )
    }

    if (controlBusy) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator()
            Text("Saving audited triage change…", color = SupportMuted)
        }
    }
    if (controlMessage.isNotBlank()) Text(controlMessage, color = SupportGood, fontSize = 12.sp)
    if (controlError.isNotBlank()) Text(controlError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)

    if (loadingConversation) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator()
            Text("Loading protected conversation…", color = SupportMuted)
        }
    }
    if (conversationError.isNotBlank()) {
        Text(conversationError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
    }
    conversation?.let { state ->
        ProtectedMessagesPanel(
            ticketSubject = state.ticketSubject,
            messages = state.messages
        )
    }

    Text(
        "Assignment is exposed only to authenticated Admin/Manager sessions. Staff status/priority changes are field-limited and remain restricted to tickets visible through assigned-only Supabase RLS. Priority changes recalculate the existing SLA target server-side. Protected messages remain selected-ticket-only; attachments are not modified here.",
        color = SupportMuted,
        fontSize = 12.sp
    )
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
            Text("Ticket controls", fontWeight = FontWeight.Black)
            Text(ticket.optString("subject").ifBlank { "Support ticket" }, fontWeight = FontWeight.Bold)
            ChoiceMenu(
                label = "Status",
                value = status,
                options = SUPPORT_TICKET_STATUSES.map { it to supportStatusLabel(it) },
                enabled = canTriage && !busy,
                onSelect = { status = it }
            )
            ChoiceMenu(
                label = "Priority",
                value = priority,
                options = SUPPORT_TICKET_PRIORITIES.map { value -> value to value.replaceFirstChar { it.uppercase() } },
                enabled = canTriage && !busy,
                onSelect = { priority = it }
            )
            ChoiceMenu(
                label = "Assigned to",
                value = assignedTo,
                options = listOf("" to "Unassigned") + assignees.map { it.id to "${it.label} · ${it.role}" },
                enabled = canAssign && !busy,
                onSelect = { assignedTo = it }
            )
            if (canTriage && !canAssign) {
                Text("Staff triage is limited to status and priority. Assignment remains Admin/Manager-only.", color = SupportMuted, fontSize = 11.sp)
            }
            Text(
                "SLA target: ${ticket.optString("sla_due_at").ifBlank { "not set" }}. Changing priority recalculates this target through the existing database trigger.",
                color = SupportMuted,
                fontSize = 11.sp
            )
            Button(
                onClick = {
                    onSave(
                        SupportTicketUpdateCommand(
                            ticketId = ticketId,
                            status = status,
                            priority = priority,
                            assignedTo = assignedTo.takeIf(String::isNotBlank)
                        )
                    )
                },
                enabled = canTriage && !busy && changed && ticketId.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save audited triage change", fontWeight = FontWeight.Black)
            }
            if (!canTriage) {
                Text("Ticket triage requires an authenticated Staff, Manager or Admin session.", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ChoiceMenu(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    enabled: Boolean,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val display = options.firstOrNull { it.first == value }?.second ?: value.ifBlank { "Unassigned" }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("$label: $display")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.second) },
                    onClick = {
                        expanded = false
                        onSelect(option.first)
                    }
                )
            }
        }
    }
}

@Composable
private fun SupportMetric(label: String, value: Int, color: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = SupportMuted)
            Text(value.toString(), color = color, fontWeight = FontWeight.Black, fontSize = 19.sp)
        }
    }
}

@Composable
private fun SupportTicketCard(
    title: String,
    detail: String,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = if (selected) .55f else .18f)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onSelect)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, color = SupportMuted, fontSize = 11.sp)
            Text(
                when {
                    selected && enabled -> "Selected • tap another ticket to switch"
                    selected -> "Selected"
                    enabled -> "Tap to manage ticket and view protected conversation"
                    else -> "Ticket unavailable while another operation is running"
                },
                color = SupportMuted,
                fontSize = 10.sp
            )
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
