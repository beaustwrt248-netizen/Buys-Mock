package com.buysloans.admin

import org.json.JSONArray
import org.json.JSONObject

internal val SUPPORT_TICKET_STATUSES = listOf("open", "in_progress", "waiting_on_user", "resolved", "closed")
internal val SUPPORT_TICKET_PRIORITIES = listOf("low", "normal", "high", "urgent")

internal data class SupportTicketUpdateCommand(
    val ticketId: String,
    val status: String,
    val priority: String,
    val assignedTo: String?
)

internal data class SupportAssignee(
    val id: String,
    val label: String,
    val role: String
)

private fun hasSupportSession(session: AdminSession): Boolean =
    session.role in setOf("admin", "manager", "staff") &&
        session.userId.isNotBlank() &&
        session.accessToken.isNotBlank()

internal fun canUpdateSupportTicketTriage(session: AdminSession): Boolean = hasSupportSession(session)

internal fun canAssignSupportTicket(session: AdminSession): Boolean =
    session.role in setOf("admin", "manager") &&
        session.userId.isNotBlank() &&
        session.accessToken.isNotBlank()

internal fun canManageSupportTicketControls(session: AdminSession): Boolean = canAssignSupportTicket(session)

internal fun supportTicketUpdatePayload(
    session: AdminSession,
    command: SupportTicketUpdateCommand
): JSONObject {
    require(canUpdateSupportTicketTriage(session)) {
        "Support-ticket triage requires an authenticated Staff, Manager or Admin session."
    }
    require(command.ticketId.isNotBlank()) { "A support ticket id is required." }
    require(command.status in SUPPORT_TICKET_STATUSES) { "Unsupported support-ticket status." }
    require(command.priority in SUPPORT_TICKET_PRIORITIES) { "Unsupported support-ticket priority." }

    val payload = JSONObject()
        .put("status", command.status)
        .put("priority", command.priority)

    if (canAssignSupportTicket(session)) {
        val assignee = command.assignedTo
            ?.trim()
            ?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
        payload.put("assigned_to", assignee ?: JSONObject.NULL)
    }
    return payload
}

internal fun eligibleSupportAssignees(profiles: JSONArray?): List<SupportAssignee> {
    if (profiles == null) return emptyList()
    val roles = setOf("admin", "manager", "staff")
    return (0 until profiles.length()).mapNotNull { index ->
        val profile = profiles.optJSONObject(index) ?: return@mapNotNull null
        val id = profile.optString("id").trim().takeUnless { it.equals("null", ignoreCase = true) }.orEmpty()
        val role = profile.optString("role")
        if (id.isBlank() || role !in roles || !profile.optBoolean("is_enabled")) return@mapNotNull null
        SupportAssignee(
            id = id,
            label = profile.optString("display_name").ifBlank { id },
            role = role
        )
    }.sortedWith(compareBy<SupportAssignee> { it.role }.thenBy { it.label.lowercase() })
}
