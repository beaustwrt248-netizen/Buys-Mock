package com.buysloans.admin

import org.junit.Assert.assertFalse
import org.junit.Test

class TeamInviteNoAdminRoleTest {
    @Test
    fun adminRoleIsNotInvitable() {
        assertFalse("admin" in TeamInvitePolicy.allowedRoles(AdminSession("token", "admin-id", "Admin", "admin")))
    }
}
