package com.buysloans.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

private val SupportMuted = Color(0xFF8EA6C4)
private val SupportGood = Color(0xFF57E389)
private val SupportWarn = Color(0xFFFFC857)

@Composable
internal fun SupportOperationsPanel(session: AdminSession, tickets: JSONArray?) {
    val health = summarizeSupportTicketHealth(tickets)
    val scope = rememberCoroutineScope()
    var selectedTicketId by remember { mutableStateOf("") }
    var conversation by remember { mutableStateOf<ProtectedConversationState?>(null) }
    var conversationError by remember { mutableStateOf("") }
    var loadingConversation by remember { mutableStateOf(false) }

    fun loadConversation(ticketId: String, ticketSubject: String) {
        if (ticketId.isBlank() || loadingConversation) return
        selectedTicketId = ticketId
        conversation = null
        conversationError = ""
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

    Text("Support operations", fontSize = 21.sp, fontWeight = FontWeight.Black)
    Text(
        "Read-only assignment and SLA health. Select a ticket to load its protected conversation through the existing Admin/Manager-only reader.",
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
            enabled = ticketId.isNotBlank() && !loadingConversation,
            onSelect = { loadConversation(ticketId, ticketSubject) }
        )
    }

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
        "Ticket selection only loads the selected ticket's existing protected messages. This view cannot assign tickets, change status or priority, write messages, modify attachments, or alter support ownership. Existing Supabase RLS remains authoritative.",
        color = SupportMuted,
        fontSize = 12.sp
    )
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
                    enabled -> "Tap to view protected conversation"
                    else -> "Conversation unavailable while another ticket is loading"
                },
                color = SupportMuted,
                fontSize = 10.sp
            )
        }
    }
}