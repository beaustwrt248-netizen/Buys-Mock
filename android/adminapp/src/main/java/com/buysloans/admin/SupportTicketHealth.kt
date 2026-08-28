package com.buysloans.admin

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

internal data class SupportTicketHealth(
    val open: Int,
    val overdue: Int,
    val dueSoon: Int,
    val awaitingFirstResponse: Int,
    val unassigned: Int
)

internal fun summarizeSupportTicketHealth(
    tickets: JSONArray?,
    now: Instant = Instant.now(),
    dueSoonWindowMs: Long = 2L * 60 * 60 * 1000
): SupportTicketHealth {
    if (tickets == null) return SupportTicketHealth(0, 0, 0, 0, 0)
    var open = 0
    var overdue = 0
    var dueSoon = 0
    var awaitingFirstResponse = 0
    var unassigned = 0

    for (i in 0 until tickets.length()) {
        val ticket = tickets.optJSONObject(i) ?: continue
        val status = ticket.optString("status")
        if (status in setOf("resolved", "closed")) continue
        open += 1
        if (ticket.optString("assigned_to").isBlank()) unassigned += 1
        if (ticket.optString("first_response_at").isBlank()) awaitingFirstResponse += 1

        val due = ticket.optString("sla_due_at")
            .takeIf(String::isNotBlank)
            ?.let { runCatching { Instant.parse(it) }.getOrNull() }
            ?: continue
        val delta = due.toEpochMilli() - now.toEpochMilli()
        if (delta < 0) overdue += 1
        else if (delta <= dueSoonWindowMs) dueSoon += 1
    }

    return SupportTicketHealth(open, overdue, dueSoon, awaitingFirstResponse, unassigned)
}

internal fun supportTicketOperationalLine(ticket: JSONObject): String {
    val priority = ticket.optString("priority").uppercase().ifBlank { "NORMAL" }
    val status = ticket.optString("status").ifBlank { "unknown" }
    val subject = ticket.optString("subject").ifBlank { "Support ticket" }
    return "$priority • $status • $subject"
}

internal fun supportTicketOperationalDetail(ticket: JSONObject): String {
    val assignee = ticket.optString("assigned_to").ifBlank { "unassigned" }
    val firstResponse = ticket.optString("first_response_at").ifBlank { "awaiting first response" }
    val slaDue = ticket.optString("sla_due_at").ifBlank { "no SLA target" }
    return "Assignee $assignee • First response $firstResponse • SLA $slaDue"
}
