package com.buysloans.admin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamInvitePayloadSecurityTest {
    @Test
    fun payloadContainsHashButNoPlainInviteCodeField() {
        val payload = teamInvitePayload(
            AdminSession("token", "actor-id", "Admin", "admin"),
            "Alan Turing",
            "alan@example.com",
            "staff",
            "b".repeat(64),
            "2026-09-05T00:00:00Z"
        )
        assertTrue(payload.contains("code_hash"))
        assertFalse(payload.contains("inviteCode"))
        assertFalse(payload.contains("invite_code"))
    }
}
