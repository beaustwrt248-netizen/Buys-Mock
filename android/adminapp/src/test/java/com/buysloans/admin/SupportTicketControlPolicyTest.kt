package com.buysloans.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class SupportTicketControlPolicyTest {
    private val admin = AdminSession("token", "admin-1", "Admin", "admin")
    private val manager = AdminSession("token", "manager-1", "Manager", "manager")
    private val staffSession = AdminSession("token", "staff-1", "Staff", "staff")
    private val staffAssignee = SupportAssigneePresentation("staff-2", "Support Staff", "staff", true)

    @Test
    fun adminCanPrepareAuditedTriageCommand() {
        val command = SupportTicketControlPolicy.prepare(
            admin,
            ticketId = "ticket-1",
            status = " In_Progress ",
            priority = " HIGH ",
            assignee = staffAssignee
        )
        assertEquals("ticket-1", command.ticketId)
        assertEquals("in_progress", command.status)
        assertEquals("high", command.priority)
        assertEquals("staff-2", command.assignedTo)
        assertTrue(command.confirmationText.contains("audited", ignoreCase = true))
    }

    @Test
    fun managerRetainsExistingSupportUpdateAuthority() {
        val decision = SupportTicketControlPolicy.canPrepare(
            manager,
            "ticket-2",
            "waiting_on_user",
            "normal",
            null
        )
        assertTrue(decision.allowed)
    }

    @Test
    fun staffCannotUseAdminControlMutationContract() {
        val decision = SupportTicketControlPolicy.canPrepare(
            staffSession,
            "ticket-3",
            "open",
            "normal",
            staffAssignee
        )
        assertFalse(decision.allowed)
        assertTrue(decision.reason.contains("Admin and Manager"))
    }

    @Test
    fun disabledOrNonSupportAssigneeIsRejected() {
        val disabled = staffAssignee.copy(enabled = false)
        assertFalse(SupportTicketControlPolicy.canPrepare(admin, "ticket-4", "open", "normal", disabled).allowed)

        val customer = SupportAssigneePresentation("user-9", "Customer", "user", true)
        assertFalse(SupportTicketControlPolicy.canPrepare(admin, "ticket-4", "open", "normal", customer).allowed)
    }

    @Test
    fun unsupportedStatusOrPriorityNeverProducesCommand() {
        assertThrows(IllegalArgumentException::class.java) {
            SupportTicketControlPolicy.prepare(admin, "ticket-5", "deleted", "normal", null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SupportTicketControlPolicy.prepare(admin, "ticket-5", "open", "critical", null)
        }
    }

    @Test
    fun unassignmentIsExplicitlyRepresentedAsNull() {
        val command = SupportTicketControlPolicy.prepare(admin, "ticket-6", "open", "low", null)
        assertEquals(null, command.assignedTo)
        assertTrue(command.confirmationText.contains("Unassigned"))
    }
}
