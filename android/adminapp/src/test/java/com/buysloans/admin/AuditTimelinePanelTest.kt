package com.buysloans.admin

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuditTimelinePanelTest {
    @Test
    fun timelineUsesOnlyOperationalMetadata() {
        val rows = JSONArray().put(
            JSONObject()
                .put("action", "support_ticket_changed")
                .put("target_type", "support_ticket")
                .put("target_id", "ticket-123")
                .put("created_at", "2026-08-28T01:00:00Z")
                .put("details", JSONObject().put("secret", "must-not-render"))
        )

        val entry = buildAuditTimeline(rows).single()
        assertEquals("support_ticket_changed", entry.action)
        assertEquals("support_ticket", entry.targetType)
        assertEquals("ticket-123", entry.targetId)
        assertEquals("2026-08-28T01:00:00Z", entry.createdAt)
        assertFalse(entry.toString().contains("must-not-render"))
        assertEquals("Support ticket changed • support_ticket • ticket-123", auditTimelineTitle(entry))
    }

    @Test
    fun malformedRowsAreDropped() {
        val rows = JSONArray()
            .put(JSONObject().put("action", "config_updated"))
            .put(JSONObject().put("created_at", "2026-08-28T01:00:00Z"))
            .put(JSONObject().put("action", "config_updated").put("created_at", "2026-08-28T01:00:00Z"))

        val entries = buildAuditTimeline(rows)
        assertEquals(1, entries.size)
        assertTrue(auditTimelineTitle(entries.single()).startsWith("Config updated"))
    }

    @Test
    fun searchMatchesOnlyOperationalMetadata() {
        val entries = listOf(
            AuditTimelineEntry("user_access_updated", "profile", "user-123", "2026-08-29T01:00:00Z"),
            AuditTimelineEntry("support_ticket_changed", "support_ticket", "ticket-456", "2026-08-29T02:00:00Z")
        )

        assertEquals(listOf(entries[0]), filterAuditTimeline(entries, "USER-123"))
        assertEquals(listOf(entries[1]), filterAuditTimeline(entries, "support ticket"))
        assertEquals(listOf(entries[1]), filterAuditTimeline(entries, "02:00"))
        assertEquals(entries, filterAuditTimeline(entries, "   "))
        assertTrue(filterAuditTimeline(entries, "secret-payload").isEmpty())
    }
}
