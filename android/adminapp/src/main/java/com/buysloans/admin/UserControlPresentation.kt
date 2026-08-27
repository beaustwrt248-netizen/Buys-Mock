package com.buysloans.admin

import org.json.JSONArray

internal data class UserAccessPresentation(
    val userId: String,
    val displayName: String,
    val role: String,
    val enabled: Boolean,
    val isSelf: Boolean,
    val canChangeRole: Boolean,
    val canEnable: Boolean,
    val canDisable: Boolean,
    val readOnlyReason: String?
)

internal fun buildUserAccessPresentation(
    session: AdminSession,
    profiles: JSONArray?
): List<UserAccessPresentation> {
    if (profiles == null) return emptyList()
    return (0 until profiles.length()).mapNotNull { index ->
        val profile = profiles.optJSONObject(index) ?: return@mapNotNull null
        val userId = profile.optString("id")
        if (userId.isBlank()) return@mapNotNull null
        val isSelf = userId == session.userId
        val roleDecision = UserGovernancePolicy.canMutate(
            actorRole = session.role,
            actorUserId = session.userId,
            targetUserId = userId,
            action = AdminUserAction.SET_ROLE,
            requestedRole = "staff"
        )
        val enableDecision = UserGovernancePolicy.canMutate(
            actorRole = session.role,
            actorUserId = session.userId,
            targetUserId = userId,
            action = AdminUserAction.ENABLE
        )
        val disableDecision = UserGovernancePolicy.canMutate(
            actorRole = session.role,
            actorUserId = session.userId,
            targetUserId = userId,
            action = AdminUserAction.DISABLE
        )
        val enabled = profile.optBoolean("is_enabled")
        val canChangeRole = roleDecision.allowed
        val canEnable = !enabled && enableDecision.allowed
        val canDisable = enabled && disableDecision.allowed
        val reason = when {
            canChangeRole || canEnable || canDisable -> null
            isSelf -> "Your signed-in Admin account is protected from Android access changes."
            session.role != "admin" -> "Managers have read-only user visibility."
            else -> roleDecision.reason
        }
        UserAccessPresentation(
            userId = userId,
            displayName = profile.optString("display_name").ifBlank { "Unnamed" },
            role = profile.optString("role").ifBlank { "unknown" },
            enabled = enabled,
            isSelf = isSelf,
            canChangeRole = canChangeRole,
            canEnable = canEnable,
            canDisable = canDisable,
            readOnlyReason = reason
        )
    }
}

internal fun userActionConfirmation(
    user: UserAccessPresentation,
    action: AdminUserAction,
    requestedRole: String? = null
): String = when (action) {
    AdminUserAction.SET_ROLE -> "Change ${user.displayName} to ${requestedRole?.trim()?.lowercase().orEmpty()}? This action is audited."
    AdminUserAction.ENABLE -> "Enable ${user.displayName}? This action is audited."
    AdminUserAction.DISABLE -> "Disable ${user.displayName}? This action is audited."
}
