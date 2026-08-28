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
internal fun SupportOperationsPanel(session: AdminSession, tickets: JSONArray?) {
    val health = summarizeSupportTicketHealth(tickets)
    val scope = rememberCoroutineScope()
    var selectedTicketId by remember { mutableStateOf("") }
    var conversation by remember { mutableStateOf<ProtectedConversationState?>(null) }
    var conversationBusy by remember { mutableStateOf(false) }
    var conversationError by remember { mutableStateOf("") }
    val canReadProtectedMessages = SupportMessageAccessPolicy.canReadProtectedMessages(session)

    fun openProtectedConversation(ticket: JSONObject) {
        val ticketId = ticket.optString("id").trim()
        val subject = ticket.optString("subject").trim()
        if (!canReadProtectedMessages || ticketId.isBlank()) return
        selectedTicketId = ticketId
        conversation = null
        conversationError = ""
        conversationBusy = true
        scope.launch {
            runCatching {
                ProtectedMessageCoordinator.load(
                    session = session,
                    ticketId = ticketId,
                    ticketSubject = subject,
                    limit = 100
                )
            }.onSuccess {
                if (selectedTicketId == ticketId) conversation = it
            }.onFailure {
                if (selectedTicketId == ticketId) {
                    conversationError = it.message ?: "Protected conversation could not be loaded."
                }
            }
            if (selectedTicketId == ticketId) conversationBusy = false
        }
    }

    Text("Support operations", fontSize = 21.sp, fontWeight = FontWeight.Black)
    Text(
        "Read-only assignment and SLA health. Protected conversations can be opened only for one explicitly selected ticket by an authenticated Admin or Manager.",
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
        SupportTicketCard(
            title = supportTicketOperationalLine(ticket),
            detail = supportTicketOperationalDetail(ticket),
            canOpenProtectedConversation = canReadProtectedMessages && ticket.optString("id").isNotBlank(),
            isLoading = conversationBusy && selectedTicketId == ticket.optString("id"),
            onOpenProtectedConversation = { openProtectedConversation(ticket) }
        )
    }

    if (conversationBusy) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator()
            Text("Loading selected protected conversation…", color = SupportMuted)
        }
    }
    if (conversationError.isNotBlank()) {
        Text(conversationError, color = MaterialTheme.colorScheme.error)
    }
    conversation?.let { state ->
        ProtectedMessagesPanel(
            ticketSubject = state.ticketSubject,
            messages = state.messages
        )
    }

    Text(
        "This view cannot change assignment, status or priority, modify support ownership, or read messages outside the explicitly selected ticket. Staff remains excluded; existing Supabase RLS remains authoritative.",
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
    canOpenProtectedConversation: Boolean,
    isLoading: Boolean,
    onOpenProtectedConversation: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .18f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, color = SupportMuted, fontSize = 11.sp)
            Button(
                onClick = onOpenProtectedConversation,
                enabled = canOpenProtectedConversation && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoading) "Loading protected conversation…" else "Open protected conversation")
            }
        }
    }
}
