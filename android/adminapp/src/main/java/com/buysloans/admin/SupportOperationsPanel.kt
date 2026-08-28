package com.buysloans.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray

private val SupportMuted = Color(0xFF8EA6C4)
private val SupportGood = Color(0xFF57E389)
private val SupportWarn = Color(0xFFFFC857)

@Composable
internal fun SupportOperationsPanel(tickets: JSONArray?) {
    val health = summarizeSupportTicketHealth(tickets)
    Text("Support operations", fontSize = 21.sp, fontWeight = FontWeight.Black)
    Text(
        "Read-only assignment and SLA health. Ticket descriptions and conversation messages are not loaded into this panel.",
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
            detail = supportTicketOperationalDetail(ticket)
        )
    }
    Text(
        "This view cannot assign tickets, change status or priority, read protected message bodies, or modify support ownership. Existing RLS remains authoritative.",
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
private fun SupportTicketCard(title: String, detail: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .18f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, color = SupportMuted, fontSize = 11.sp)
        }
    }
}
