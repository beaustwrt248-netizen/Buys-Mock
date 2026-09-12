package com.buysloans.admin

/**
 * Process-scoped owner for the authenticated Morley Admin session.
 *
 * Tokens are intentionally not persisted to disk in the native-rebuild recovery release.
 * If Android kills the process, the next launch returns to native sign-in.
 */
internal object AdminSessionStore {
    @Volatile
    private var active: AdminSession? = null

    @Synchronized
    fun set(session: AdminSession) {
        active = session
    }

    fun current(): AdminSession? = active

    fun hasAuthorizedSession(): Boolean {
        val session = active ?: return false
        return session.accessToken.isNotBlank() &&
            session.refreshToken.isNotBlank() &&
            session.userId.isNotBlank() &&
            AdminAppAccessPolicy.canEnter(session.role, isEnabled = true)
    }

    @Synchronized
    fun clear() {
        active?.accessToken = ""
        active?.refreshToken = ""
        active = null
    }
}
