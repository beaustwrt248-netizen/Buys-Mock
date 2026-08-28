package com.buysloans.admin

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SupportQueuePanelTest {
    private val now = Instant.parse("2026-08-28T08:00:00Z")

    @Test
    fun reportsAssignmentResponseAndOverdueSla() {
        val rows = JSONArray().put(
            JSONObject()
                .put("subject", "Scanner issue")
                .put("status", "in_progress")
                .put("priority", "high")
                .put("assigned_to", "staff-1")
                .put("sla_due_at", "2026-08-28T07:00:00Z")
                .put("first_response_at", "2026-08-28T06:00:00Z")
        )
        val entry = buildSupportQueue(rows).single()
        assertEquals(SupportSlaState.OVERDUE, supportSlaState(entry, now))
        assertEquals("assigned • responded • SLA overdue", supportQueueSubtitle(entry, now))
    }

    @Test
    fun unresolvedUnassignedTicketCanBeDueSoon() {
        val rows = JSONArray().put(
            JSONObject()
                .put("subject", "Account help")
                .put("status", "open")
                .put("priority", "normal")
                .put("sla_due_at", "2026-08-28T09:00:00Z")
        )
        val entry = buildSupportQueue(rows).single()
        assertEquals(SupportSlaState.DUE_SOON, supportSlaState(entry, now))
        assertEquals("unassigned • awaiting first response • SLA due soon", supportQueueSubtitle(entry, now))
    }

    @Test
    fun closedTicketDoesNotReportOverdue() {
        val entry = SupportQueueEntry(
            subject = "Done",
            status = "closed",
            priority = "urgent",
            assignedTo = null,
            slaDueAt = "2026-08-27T08:00:00Z",
            firstResponseAt = null
        )
        assertEquals(SupportSlaState.CLOSED, supportSlaState(entry, now))
        assertTrue(supportQueueSubtitle(entry, now).endsWith("SLA closed"))
    }
}
