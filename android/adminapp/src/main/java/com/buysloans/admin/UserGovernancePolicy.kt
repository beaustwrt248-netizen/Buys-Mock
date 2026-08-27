package com.buysloans.admin

internal enum class AdminUserAction {
    SET_ROLE,
    ENABLE,
    DISABLE
}

internal data class UserGovernanceDecision(
    val allowed: Boolean,
    val reason: String
)

internal object UserGovernancePolicy {
    private val allowedRoles = setOf("staff", "manager", "admin")

    fun canMutate(
        actorRole: String,
        actorUserId: String,
        targetUserId: String,
        action: AdminUserAction,
        requestedRole: String? = null
    ): UserGovernanceDecision {
        if (actorRole != "admin") {
            return UserGovernanceDecision(false, "Only enabled Admin accounts may change user access.")
        }
        if (actorUserId.isBlank() || targetUserId.isBlank()) {
            return UserGovernanceDecision(false, "A valid actor and target account are required.")
        }
        if (actorUserId == targetUserId) {
            return UserGovernanceDecision(false, "Your own active Admin account is protected from Android user-management changes.")
        }
        if (action == AdminUserAction.SET_ROLE) {
            val role = requestedRole?.trim()?.lowercase().orEmpty()
            if (role !in allowedRoles) {
                return UserGovernanceDecision(false, "Role must be staff, manager or admin.")
            }
        }
        return UserGovernanceDecision(true, "Allowed through the audited admin-user-control boundary.")
    }

    fun androidActionName(action: AdminUserAction): String = when (action) {
        AdminUserAction.SET_ROLE -> "set_role"
        AdminUserAction.ENABLE -> "enable"
        AdminUserAction.DISABLE -> "disable"
    }
}
