package com.buysloans.admin

import org.junit.Assert.assertFalse
import org.junit.Test

class TeamInviteExpiryContractTest {
    @Test
    fun freshInviteModelIsNotUsed() {
        val invite = TeamInvite("id", "user@example.com", "User Name", "staff", "2099-01-01T00:00:00Z", null, "2026-08-29T00:00:00Z")
        assertFalse(invite.isUsed)
        assertFalse(invite.isExpired)
    }
}
