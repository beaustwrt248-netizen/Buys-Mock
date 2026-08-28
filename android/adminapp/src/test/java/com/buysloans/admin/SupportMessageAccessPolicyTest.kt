package com.buysloans.admin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportMessageAccessPolicyTest {
    private fun session(role: String, token: String = "token", userId: String = "user-1") =
        AdminSession(accessToken = token, userId = userId, displayName = "Test", role = role)

    @Test
    fun adminAndManagerMayBuildTicketScopedMessageReads() {
        assertTrue(SupportMessageAccessPolicy.canReadProtectedMessages(session("admin")))
        assertTrue(SupportMessageAccessPolicy.canReadProtectedMessages(session("manager")))

        val path = SupportMessageAccessPolicy.buildReadPath(session("admin"), "ticket-123", 250)
        assertTrue(path.contains("ticket_id=eq.ticket-123"))
        assertTrue(path.contains("select=id,ticket_id,author_user_id,author_role,body,created_at"))
        assertTrue(path.endsWith("limit=100"))
    }

    @Test
    fun staffAndIncompleteSessionsAreRejected() {
        assertFalse(SupportMessageAccessPolicy.canReadProtectedMessages(session("staff")))
        assertFalse(SupportMessageAccessPolicy.canReadProtectedMessages(session("admin", token = "")))
        assertFalse(SupportMessageAccessPolicy.canReadProtectedMessages(session("manager", userId = "")))

        runCatching { SupportMessageAccessPolicy.buildReadPath(session("staff"), "ticket-123") }
            .onSuccess { error("Staff message access must be rejected") }
        runCatching { SupportMessageAccessPolicy.buildReadPath(session("admin"), "") }
            .onSuccess { error("Unscoped message access must be rejected") }
    }

    @Test
    fun queryNeverRequestsTicketDescriptionDiagnosticsOrAttachments() {
        val path = SupportMessageAccessPolicy.buildReadPath(session("manager"), "ticket/with space")
        assertTrue(path.contains("ticket_id=eq.ticket%2Fwith+space"))
        assertFalse(path.contains("description"))
        assertFalse(path.contains("diagnostics"))
        assertFalse(path.contains("attachment"))
        assertFalse(path.contains("support_tickets?"))
    }
}
