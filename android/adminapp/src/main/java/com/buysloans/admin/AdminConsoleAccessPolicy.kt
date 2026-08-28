package com.buysloans.admin

internal val ADMIN_CONSOLE_ROLES = setOf("admin", "manager", "staff")
private val PRIVILEGED_CONSOLE_ROLES = setOf("admin", "manager")

internal fun canEnterAdminConsole(role: String, enabled: Boolean): Boolean =
    enabled && role in ADMIN_CONSOLE_ROLES

internal fun hasPrivilegedAdminConsole(session: AdminSession): Boolean =
    session.role in PRIVILEGED_CONSOLE_ROLES &&
        session.userId.isNotBlank() &&
        session.accessToken.isNotBlank()

internal fun isStaffSupportConsole(session: AdminSession): Boolean =
    session.role == "staff" &&
        session.userId.isNotBlank() &&
        session.accessToken.isNotBlank()

internal fun adminConsoleTabs(session: AdminSession): List<String> =
    if (isStaffSupportConsole(session)) {
        listOf("Tickets")
    } else {
        listOf("Health", "Tickets", "Staff alerts", "Users & devices", "Controls", "Audit", "Release")
    }
