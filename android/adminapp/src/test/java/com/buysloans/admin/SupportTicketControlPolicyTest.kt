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
    private val user = AdminSession("token", "user-1", "User", "user")

    @Test fun supportRolesCanTriageButOnlyAdminAndManagerCanUsePowerfulControls() {
        assertTrue(canUpdateSupportTicketTriage(admin))
        assertTrue(canUpdateSupportTicketTriage(manager))
        assertTrue(canUpdateSupportTicketTriage(staff))
        assertFalse(canUpdateSupportTicketTriage(user))

        assertTrue(canAssignSupportTicket(admin))
        assertTrue(canAssignSupportTicket(manager))
        assertFalse(canAssignSupportTicket(staff))
        assertFalse(canAssignSupportTicket(user))

        assertTrue(canSetSupportTicketPriority(admin))
        assertTrue(canSetSupportTicketPriority(manager))
        assertFalse(canSetSupportTicketPriority(staff))
        assertTrue(canManageSupportTicketControls(admin))
        assertTrue(canManageSupportTicketControls(manager))
        assertFalse(canManageSupportTicketControls(staff))
    }

    @Test fun privilegedPayloadContainsPriorityAssignmentAndSupportsUnassignment() {
        val payload = supportTicketUpdatePayload(
            admin,
            SupportTicketUpdateCommand("ticket-1", "in_progress", "high", null)
        )

        assertEquals(setOf("status", "priority", "assigned_to"), payload.keys().asSequence().toSet())
        assertEquals("in_progress", payload.getString("status"))
        assertEquals("high", payload.getString("priority"))
        assertTrue(payload.isNull("assigned_to"))
    }

    @Test fun staffPayloadIsStrictlyStatusOnly() {
        val payload = supportTicketUpdatePayload(
            staff,
            SupportTicketUpdateCommand("ticket-1", "waiting_on_user", "urgent", "someone-else")
        )

        assertEquals(setOf("status"), payload.keys().asSequence().toSet())
        assertEquals("waiting_on_user", payload.getString("status"))
        assertFalse(payload.has("priority"))
        assertFalse(payload.has("assigned_to"))
    }

    @Test fun staffWritableStatusesMatchDatabaseGuard() {
        assertEquals(listOf("in_progress", "waiting_on_user", "resolved"), supportWritableStatuses(staff))
        assertEquals(SUPPORT_TICKET_STATUSES, supportWritableStatuses(manager))
    }

    @Test(expected = IllegalArgumentException::class)
    fun staffCannotSetClosedStatus() {
        supportTicketUpdatePayload(
            staff,
            SupportTicketUpdateCommand("ticket-1", "closed", "normal", null)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun ordinaryUserCannotCreateTriagePayload() {
        supportTicketUpdatePayload(
            user,
            SupportTicketUpdateCommand("ticket-1", "open", "normal", null)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun privilegedPayloadRejectsUnsupportedPriority() {
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
