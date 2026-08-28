package com.buysloans.admin

import java.net.URLEncoder

internal object SupportMessageAccessPolicy {
    private val allowedRoles = setOf("admin", "manager", "staff")

    fun canReadProtectedMessages(session: AdminSession): Boolean =
        session.role in allowedRoles && session.userId.isNotBlank() && session.accessToken.isNotBlank()

    fun buildReadPath(session: AdminSession, ticketId: String, limit: Int = 100): String {
        require(canReadProtectedMessages(session)) {
            "Protected support messages require an authenticated Staff, Manager or Admin session."
        }
        require(ticketId.isNotBlank()) { "A support ticket id is required." }
        val boundedLimit = limit.coerceIn(1, 100)
        val encodedTicket = URLEncoder.encode(ticketId.trim(), Charsets.UTF_8.name())
        return "/rest/v1/support_ticket_messages?ticket_id=eq.$encodedTicket&select=id,ticket_id,author_user_id,author_role,body,created_at&order=created_at.asc&limit=$boundedLimit"
    }
}
