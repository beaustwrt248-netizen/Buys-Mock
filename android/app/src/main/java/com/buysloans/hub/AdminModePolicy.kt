package com.buysloans.hub

object AdminModePolicy {
    private val allowedRoles = setOf("admin", "manager")

    fun canEnter(role: String): Boolean = role.trim().lowercase() in allowedRoles
}
