package com.buysloans.admin

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class SupportTicketHealthTest {
    private val now = Instant.parse("2026-08-28T02:30:00Z")

    @Test
    fun countsOnlyOpenOperationalRisk() {
        val tickets = JSONArray()
            .put(ticket("open", "2026-08-28T01:30:00Z", "", ""))
            .put(ticket("in_progress", "2026-08-28T03:30:00Z", "staff-1", "2026-08-28T02:00:00Z"))
            .put(ticket("resolved", "2026-08-28T01:00:00Z", "staff-2", ""))

        val result = summarizeSupportTicketHealth(tickets, now)

        assertEquals(2, result.open)
        assertEquals(1, result.overdue)
        assertEquals(1, result.dueSoon)
        assertEquals(1, result.awaitingFirstResponse)
        assertEquals(1, result.unassigned)
    }

    @Test
    fun missingOrInvalidSlaDoesNotBecomeOverdue() {
        val tickets = JSONArray()
            .put(ticket("open", "", "", ""))
            .put(ticket("waiting_on_user", "not-a-date", "staff-1", ""))

        val result = summarizeSupportTicketHealth(tickets, now)

        assertEquals(2, result.open)
        assertEquals(0, result.overdue)
        assertEquals(0, result.dueSoon)
        assertEquals(2, result.awaitingFirstResponse)
        assertEquals(1, result.unassigned)
    }

    @Test
    fun operationalDetailNeverIncludesTicketMessageContent() {
        val ticket = ticket("open", "2026-08-28T03:00:00Z", "staff-1", "2026-08-28T02:00:00Z")
            .put("description", "private user description")
            .put("message", "private conversation")

        val detail = supportTicketOperationalDetail(ticket)

        assertEquals(false, detail.contains("private user description"))
        assertEquals(false, detail.contains("private conversation"))
    }

    private fun ticket(status: String, sla: String, assignedTo: String, firstResponse: String) = JSONObject()
        .put("status", status)
        .put("priority", "high")
        .put("subject", "Example")
        .put("sla_due_at", sla)
        .put("assigned_to", assignedTo)
        .put("first_response_at", firstResponse)
}
