package com.buysloans.admin

internal object AdminAppAccessPolicy {
    private val operationalRoles = setOf("admin", "manager", "staff")
    private val fullSnapshotRoles = setOf("admin", "manager")

    fun canEnter(role: String, isEnabled: Boolean): Boolean =
        isEnabled && role in operationalRoles

    fun canReadFullSnapshot(session: AdminSession): Boolean =
        session.role in fullSnapshotRoles &&
            session.userId.isNotBlank() &&
            session.accessToken.isNotBlank()

    fun isSupportOnly(session: AdminSession): Boolean =
        session.role == "staff" &&
            session.userId.isNotBlank() &&
            session.accessToken.isNotBlank()
}
