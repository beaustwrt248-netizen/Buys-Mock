package com.buysloans.admin

import org.junit.Assert.assertTrue
import org.junit.Test

class TeamInviteIdentityContractTest {
    @Test
    fun validStaffIdentityPassesPolicy() {
        TeamInvitePolicy.validate(
            AdminSession("token", "admin-id", "Admin", "admin"),
            "Katherine Johnson",
            "katherine@example.com",
            "staff"
        )
        assertTrue(true)
    }
}
