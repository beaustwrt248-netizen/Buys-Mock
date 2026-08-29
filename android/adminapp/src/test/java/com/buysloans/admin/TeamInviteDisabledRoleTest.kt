package com.buysloans.admin

import org.junit.Assert.assertFalse
import org.junit.Test

class TeamInviteDisabledRoleTest {
    @Test
    fun staffCannotManageTeamInvites() {
        assertFalse(TeamInvitePolicy.canManage(AdminSession("token", "staff-id", "Staff", "staff")))
    }
}
