package com.buysloans.admin

import org.junit.Assert.assertEquals
import org.junit.Test

class TeamInviteManagerVisibilityTest {
    @Test
    fun managerInviteSurfaceContainsStaffOnly() {
        assertEquals(
            listOf("staff"),
            TeamInvitePolicy.allowedRoles(AdminSession("token", "manager-id", "Manager", "manager"))
        )
    }
}
