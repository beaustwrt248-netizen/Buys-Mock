package com.buysloans.admin

import org.junit.Assert.assertTrue
import org.junit.Test

class TeamInviteRoleEscalationTest {
    @Test
    fun noInvitePathCanCreateAdminRole() {
        val adminRoles = TeamInvitePolicy.allowedRoles(AdminSession("token", "admin-id", "Admin", "admin"))
        val managerRoles = TeamInvitePolicy.allowedRoles(AdminSession("token", "manager-id", "Manager", "manager"))
        assertTrue("admin" !in adminRoles)
        assertTrue("admin" !in managerRoles)
        assertTrue("manager" !in managerRoles)
    }
}
