package com.buysloans.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AdminTelemetryTest {
    @Test
    fun eventJsonContainsOnlyApprovedOperationalFields() {
        val event = AdminErrorEvent("0.1.0", "Phone", "Health", "IOException", "2026-08-27T14:30:00Z")
        val json = event.toJson()
        val keys = json.keys().asSequence().toSet()

        assertEquals(
            setOf("app_version", "device_model", "failing_screen", "error_class", "occurred_at"),
            keys
        )
        assertEquals("0.1.0", json.getString("app_version"))
        assertEquals("Phone", json.getString("device_model"))
        assertEquals("Health", json.getString("failing_screen"))
        assertEquals("IOException", json.getString("error_class"))
        assertEquals("2026-08-27T14:30:00Z", json.getString("occurred_at"))

        val forbiddenKeys = setOf(
            "email",
            "user",
            "user_id",
            "token",
            "access_token",
            "refresh_token",
            "message",
            "stack",
            "stack_trace",
            "ticket",
            "ticket_id",
            "body"
        )
        assertFalse(keys.any { key -> forbiddenKeys.any { forbidden -> key.equals(forbidden, ignoreCase = true) } })
    }

    @Test
    fun uncaughtCrashLabelDoesNotAddThreadOrIdentityData() {
        val label = AdminTelemetry.UNCAUGHT_SCREEN
        assertEquals("Uncaught/Admin", label)
        assertFalse(label.contains("thread", ignoreCase = true))
        assertFalse(label.contains("user", ignoreCase = true))
        assertFalse(label.contains("email", ignoreCase = true))
    }

    @Test
    fun pendingQueueIsBoundedToTwentyNewestEvents() {
        val events = (0 until 25).map {
            AdminErrorEvent("0.1.0", "Phone", "Health", "IOException", "2026-08-27T14:${it.toString().padStart(2, '0')}:00Z")
        }
        val bounded = AdminTelemetry.bound(events)
        assertEquals(20, bounded.size)
        assertEquals(events[5], bounded.first())
        assertEquals(events[24], bounded.last())
    }

    @Test
    fun boundedQueueDoesNotMutateApprovedFields() {
        val event = AdminErrorEvent("0.1.0", "Samsung SM-S928B", "Dashboard/Refresh", "IOException", "2026-08-27T14:30:00Z")
        assertEquals(listOf(event), AdminTelemetry.bound(listOf(event)))
    }
}
