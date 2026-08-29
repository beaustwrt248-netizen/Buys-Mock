package com.buysloans.admin

import org.junit.Assert.assertTrue
import org.junit.Test

class TeamInviteEmailNormalisationTest {
    @Test
    fun payloadLowercasesInviteEmail() {
        val payload = teamInvitePayload(
            AdminSession("token", "admin-id", "Admin", "admin"),
            "Dorothy Vaughan",
            " DOROTHY@EXAMPLE.COM ",
            "staff",
            "c".repeat(64),
            "2026-09-05T00:00:00Z"
        )
        assertTrue(payload.contains("dorothy@example.com"))
    }
}
