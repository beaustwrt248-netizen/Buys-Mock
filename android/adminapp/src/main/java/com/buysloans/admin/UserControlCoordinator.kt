package com.buysloans.admin

internal data class UserControlCommand(
    val targetUserId: String,
    val action: AdminUserAction,
    val requestedRole: String?,
    val confirmationText: String
)

internal object UserControlCoordinator {
    fun prepare(
        session: AdminSession,
        user: UserAccessPresentation,
        action: AdminUserAction,
        requestedRole: String? = null
    ): UserControlCommand {
        val presentationAllows = when (action) {
            AdminUserAction.SET_ROLE -> user.canChangeRole
            AdminUserAction.ENABLE -> user.canEnable
            AdminUserAction.DISABLE -> user.canDisable
        }
        require(presentationAllows) {
            user.readOnlyReason ?: "This user access action is not available in Android Admin."
        }

        val normalizedRole = requestedRole?.trim()?.lowercase()
        val decision = UserGovernancePolicy.canMutate(
            actorRole = session.role,
            actorUserId = session.userId,
            targetUserId = user.userId,
            action = action,
            requestedRole = normalizedRole
        )
        require(decision.allowed) { decision.reason }

        return UserControlCommand(
            targetUserId = user.userId,
            action = action,
            requestedRole = normalizedRole,
            confirmationText = userActionConfirmation(user, action, normalizedRole)
        )
    }

    suspend fun execute(
        session: AdminSession,
        command: UserControlCommand,
        confirmed: Boolean
    ) {
        require(confirmed) { "User access changes require explicit confirmation." }
        UserControlApi.updateAccess(
            session = session,
            targetUserId = command.targetUserId,
            action = command.action,
            requestedRole = command.requestedRole
        )
    }
}
