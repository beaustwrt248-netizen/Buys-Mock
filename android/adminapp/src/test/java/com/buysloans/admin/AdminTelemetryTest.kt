package com.buysloans.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminTelemetryTest {
    @Test
    fun parsePreservesOnlyOperationalFields() {
        val raw = """[{"app_version":"0.1.0","device_model":"Samsung SM-S928B","failing_screen":"Health","error_class":"IOException","occurred_at":"2026-08-27T14:30:00Z","email":"should-not-survive@example.com","message":"private details"}]"""
        val events = AdminTelemetry.parse(raw)
        assertEquals(1, events.size)
        val json = events.single().toJson().toString()
        assertTrue(json.contains("0.1.0"))
        assertTrue(json.contains("Samsung SM-S928B"))
        assertTrue(json.contains("Health"))
        assertTrue(json.contains("IOException"))
        assertTrue(!json.contains("should-not-survive"))
        assertTrue(!json.contains("private details"))
    }

    @Test
    fun malformedOrUnparseableEventsAreDropped() {
        assertTrue(AdminTelemetry.parse("not json").isEmpty())
        val raw = """[{"app_version":"0.1.0","device_model":"Phone","failing_screen":"Health","error_class":"IOException","occurred_at":"not-a-time"}]"""
        assertTrue(AdminTelemetry.parse(raw).isEmpty())
    }

    @Test
    fun pendingQueueIsBoundedToTwentyEvents() {
        val raw = (0 until 25).joinToString(prefix = "[", postfix = "]") {
            "{\"app_version\":\"0.1.0\",\"device_model\":\"Phone\",\"failing_screen\":\"Health\",\"error_class\":\"IOException\",\"occurred_at\":\"2026-08-27T14:${it.toString().padStart(2, '0')}:00Z\"}"
        }
        assertEquals(20, AdminTelemetry.parse(raw).size)
    }
}
