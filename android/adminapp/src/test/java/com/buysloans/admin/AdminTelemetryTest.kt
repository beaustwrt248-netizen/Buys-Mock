package com.buysloans.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminTelemetryTest {
    @Test
    fun eventSchemaContainsOnlyApprovedOperationalFields() {
        val names = AdminErrorEvent::class.java.declaredFields.map { it.name }.toSet()
        assertTrue(names.containsAll(setOf("appVersion", "deviceModel", "failingScreen", "errorClass", "occurredAt")))
        assertFalse(names.any { it.contains("email", ignoreCase = true) })
        assertFalse(names.any { it.contains("token", ignoreCase = true) })
        assertFalse(names.any { it.contains("message", ignoreCase = true) })
        assertFalse(names.any { it.contains("stack", ignoreCase = true) })
        assertFalse(names.any { it.contains("user", ignoreCase = true) })
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
