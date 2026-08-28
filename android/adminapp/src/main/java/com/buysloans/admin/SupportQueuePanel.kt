package com.buysloans.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import java.time.Instant

internal data class SupportQueueEntry(
    val subject: String,
    val status: String,
    val priority: String,
    val assignedTo: String?,
    val slaDueAt: String?,
    val firstResponseAt: String?
)

internal enum class SupportSlaState { CLOSED, NO_TARGET, ON_TRACK, DUE_SOON, OVERDUE }

internal fun buildSupportQueue(rows: JSONArray?): List<SupportQueueEntry> {
    if (rows == null) return emptyList()
    return (0 until rows.length()).mapNotNull { index ->
        val row = rows.optJSONObject(index) ?: return@mapNotNull null
        val subject = row.optString("subject").trim()
        val status = row.optString("status").trim()
        val priority = row.optString("priority").trim()
        if (subject.isBlank() || status.isBlank() || priority.isBlank()) return@mapNotNull null
        SupportQueueEntry(
            subject = subject,
            status = status,
            priority = priority,
            assignedTo = row.optString("assigned_to").trim().takeIf { it.isNotBlank() },
            slaDueAt = row.optString("sla_due_at").trim().takeIf { it.isNotBlank() },
            firstResponseAt = row.optString("first_response_at").trim().takeIf { it.isNotBlank() }
        )
    }
}

internal fun supportSlaState(entry: SupportQueueEntry, now: Instant): SupportSlaState {
    if (entry.status in setOf("resolved", "closed")) return SupportSlaState.CLOSED
    val due = entry.slaDueAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return SupportSlaState.NO_TARGET
    if (!due.isAfter(now)) return SupportSlaState.OVERDUE
    return if (due.minusSeconds(2 * 60 * 60).isAfter(now)) SupportSlaState.ON_TRACK else SupportSlaState.DUE_SOON
}

internal fun supportQueueSubtitle(entry: SupportQueueEntry, now: Instant): String {
    val assignment = if (entry.assignedTo == null) "unassigned" else "assigned"
    val response = if (entry.firstResponseAt == null) "awaiting first response" else "responded"
    val sla = when (supportSlaState(entry, now)) {
        SupportSlaState.CLOSED -> "SLA closed"
        SupportSlaState.NO_TARGET -> "no SLA target"
        SupportSlaState.ON_TRACK -> "SLA on track"
        SupportSlaState.DUE_SOON -> "SLA due soon"
        SupportSlaState.OVERDUE -> "SLA overdue"
    }
    return "$assignment • $response • $sla"
}

@Composable
internal fun SupportQueuePanel(rows: JSONArray?) {
    val entries = buildSupportQueue(rows)
    val now = Instant.now()
    Text("Support ticket queue", fontSize = 21.sp, fontWeight = FontWeight.Black)
    Text(
        "Read-only assignment and SLA visibility. Ticket ownership, status, priority and assignment writes remain governed by existing support RLS and staff/Admin policies.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp
    )
    if (entries.isEmpty()) {
        Text("No support tickets returned.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    entries.take(50).forEach { entry ->
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .18f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("${entry.priority.uppercase()} • ${entry.status} • ${entry.subject}", fontWeight = FontWeight.Bold)
                Text(supportQueueSubtitle(entry, now), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
    }
}
