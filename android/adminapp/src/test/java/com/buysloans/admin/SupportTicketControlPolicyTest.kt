package com.buysloans.admin

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportTicketControlPolicyTest {
    private val admin = AdminSession("token", "admin-1", "Admin", "admin")
    private val manager = AdminSession("token", "manager-1", "Manager", "manager")
    private val staff = AdminSession("token", "staff-1", "Staff", "staff")

    @Test fun adminAndManagerCanUsePrivilegedTicketControls() {
        assertTrue(canManageSupportTicketControls(admin))
        assertTrue(canManageSupportTicketControls(manager))
        assertFalse(canManageSupportTicketControls(staff))
    }

    @Test fun payloadContainsOnlyTriageFieldsAndSupportsUnassignment() {
        val payload = supportTicketUpdatePayload(
            admin,
            SupportTicketUpdateCommand("ticket-1", "in_progress", "high", null)
        )

        assertEquals(setOf("status", "priority", "assigned_to"), payload.keySet())
        assertEquals("in_progress", payload.getString("status"))
        assertEquals("high", payload.getString("priority"))
        assertTrue(payload.isNull("assigned_to"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnsupportedPriority() {
        supportTicketUpdatePayload(
            admin,
            SupportTicketUpdateCommand("ticket-1", "open", "critical", null)
        )
    }

    @Test fun assigneesIncludeEnabledSupportRolesOnly() {
        val profiles = JSONArray()
            .put(profile("admin-1", "A", "admin", true))
            .put(profile("manager-1", "M", "manager", true))
            .put(profile("staff-1", "S", "staff", true))
            .put(profile("staff-disabled", "D", "staff", false))
            .put(profile("user-1", "U", "user", true))

        val assignees = eligibleSupportAssignees(profiles)

        assertEquals(setOf("admin-1", "manager-1", "staff-1"), assignees.map { it.id }.toSet())
    }

    private fun profile(id: String, name: String, role: String, enabled: Boolean) = JSONObject()
        .put("id", id)
        .put("display_name", name)
        .put("role", role)
        .put("is_enabled", enabled)
}
