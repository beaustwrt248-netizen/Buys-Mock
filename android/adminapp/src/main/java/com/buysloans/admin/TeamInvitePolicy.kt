package com.buysloans.admin

import org.json.JSONObject

internal object TeamInvitePolicy {
    fun canManage(session: AdminSession): Boolean = session.role == "admin" || session.role == "manager"

    fun allowedRoles(session: AdminSession): List<String> = when (session.role) {
        "admin" -> listOf("staff", "manager")
        "manager" -> listOf("staff")
        else -> emptyList()
    }

    fun validate(session: AdminSession, displayName: String, email: String, role: String) {
        require(canManage(session)) { "Team invites require an Admin or Manager session." }
        val name = displayName.trim().replace(Regex("\\s+"), " ")
        require(name.length in 3..100 && name.contains(' ')) { "Enter the staff member's first and last name." }
        val cleanEmail = email.trim().lowercase()
        require(cleanEmail.length in 3..254 && cleanEmail.contains('@') && cleanEmail.substringAfter('@').contains('.')) {
            "Enter a valid email address."
        }
        require(role in allowedRoles(session)) { "You are not allowed to invite that role." }
    }
}

internal fun teamInvitePayload(
    session: AdminSession,
    displayName: String,
    email: String,
    role: String,
    codeHash: String,
    expiresAt: String
): String {
    TeamInvitePolicy.validate(session, displayName, email, role)
    require(codeHash.matches(Regex("^[0-9a-f]{64}$"))) { "Invite code hash is invalid." }
    return JSONObject()
        .put("email", email.trim().lowercase())
        .put("display_name", displayName.trim().replace(Regex("\\s+"), " "))
        .put("role", role)
        .put("code_hash", codeHash)
        .put("expires_at", expiresAt)
        .put("created_by", session.userId)
        .toString()
}
