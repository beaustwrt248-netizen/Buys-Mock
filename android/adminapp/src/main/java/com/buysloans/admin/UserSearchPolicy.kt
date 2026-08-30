package com.buysloans.admin

internal fun filterUserAccessRows(users: List<UserAccessPresentation>, query: String): List<UserAccessPresentation> {
    val needle = query.trim().lowercase()
    if (needle.isBlank()) return users
    return users.filter { user ->
        sequenceOf(
            user.displayName,
            user.role,
            if (user.enabled) "enabled" else "disabled",
            if (user.isSelf) "signed in" else ""
        ).any { value -> value.lowercase().contains(needle) }
    }
}
