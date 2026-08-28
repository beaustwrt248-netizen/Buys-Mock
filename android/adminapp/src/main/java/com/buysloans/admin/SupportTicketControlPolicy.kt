package com.buysloans.admin

internal data class SupportAssigneePresentation(
    val userId: String,
    val displayName: String,
    val role: String,
    val enabled: Boolean
)

internal data class SupportTicketControlCommand(
    val ticketId: String,
    val status: String,
    val priority: String,
    val assignedTo: String?,
    val confirmationText: String
)

internal data class SupportTicketControlDecision(
    val allowed: Boolean,
    val reason: String
)

internal object SupportTicketControlPolicy {
    val allowedStatuses = setOf("open", "in_progress", "waiting_on_user", "resolved", "closed")
    val allowedPriorities = setOf("low", "normal", "high", "urgent")
    private val assignableRoles = setOf("staff", "manager", "admin")

    fun canPrepare(
        session: AdminSession,
        ticketId: String,
        status: String,
        priority: String,
        assignee: SupportAssigneePresentation?
    ): SupportTicketControlDecision {
        if (session.accessToken.isBlank() || session.userId.isBlank()) {
            return SupportTicketControlDecision(false, "An authenticated Admin or Manager session is required.")
        }
        if (session.role !in setOf("admin", "manager")) {
            return SupportTicketControlDecision(false, "Support-ticket control is limited to Admin and Manager accounts.")
        }
        if (ticketId.isBlank()) {
            return SupportTicketControlDecision(false, "A support ticket id is required.")
        }
        if (status.trim().lowercase() !in allowedStatuses) {
            return SupportTicketControlDecision(false, "Unsupported support-ticket status.")
        }
        if (priority.trim().lowercase() !in allowedPriorities) {
            return SupportTicketControlDecision(false, "Unsupported support-ticket priority.")
        }
        if (assignee != null) {
            if (assignee.userId.isBlank() || !assignee.enabled) {
                return SupportTicketControlDecision(false, "Tickets may only be assigned to an enabled support account.")
            }
            if (assignee.role.trim().lowercase() !in assignableRoles) {
                return SupportTicketControlDecision(false, "Tickets may only be assigned to enabled Staff, Manager or Admin accounts.")
            }
        }
        return SupportTicketControlDecision(true, "Allowed by the Android Admin support-ticket control contract.")
    }

    fun prepare(
        session: AdminSession,
        ticketId: String,
        status: String,
        priority: String,
        assignee: SupportAssigneePresentation?
    ): SupportTicketControlCommand {
        val normalizedStatus = status.trim().lowercase()
        val normalizedPriority = priority.trim().lowercase()
        val decision = canPrepare(session, ticketId, normalizedStatus, normalizedPriority, assignee)
        require(decision.allowed) { decision.reason }

        val assigneeLabel = assignee?.displayName?.trim().orEmpty().ifBlank { assignee?.userId ?: "Unassigned" }
        return SupportTicketControlCommand(
            ticketId = ticketId.trim(),
            status = normalizedStatus,
            priority = normalizedPriority,
            assignedTo = assignee?.userId,
            confirmationText = "Apply audited support-ticket triage changes? Status $normalizedStatus, priority $normalizedPriority, assignee $assigneeLabel."
        )
    }
}
