package com.buysloans.admin

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamInvitePolicyTest {
    private fun session(role: String) = AdminSession("token", "actor-id", "Actor", role)

    @Test
    fun adminCanInviteStaffAndManagerButNotAdmin() {
        assertEquals(listOf("staff", "manager"), TeamInvitePolicy.allowedRoles(session("admin")))
        TeamInvitePolicy.validate(session("admin"), "Ada Lovelace", "ada@example.com", "staff")
        TeamInvitePolicy.validate(session("admin"), "Ada Lovelace", "ada@example.com", "manager")
        assertFails { TeamInvitePolicy.validate(session("admin"), "Ada Lovelace", "ada@example.com", "admin") }
    }

    @Test
    fun managerCanInviteStaffOnly() {
        assertTrue(TeamInvitePolicy.canManage(session("manager")))
        assertEquals(listOf("staff"), TeamInvitePolicy.allowedRoles(session("manager")))
        TeamInvitePolicy.validate(session("manager"), "Grace Hopper", "grace@example.com", "staff")
        assertFails { TeamInvitePolicy.validate(session("manager"), "Grace Hopper", "grace@example.com", "manager") }
    }

    @Test
    fun staffCannotManageInvites() {
        assertFalse(TeamInvitePolicy.canManage(session("staff")))
        assertTrue(TeamInvitePolicy.allowedRoles(session("staff")).isEmpty())
        assertFails { TeamInvitePolicy.validate(session("staff"), "Grace Hopper", "grace@example.com", "staff") }
    }

    @Test
    fun validationRejectsWeakIdentityFields() {
        assertFails { TeamInvitePolicy.validate(session("admin"), "Ada", "ada@example.com", "staff") }
        assertFails { TeamInvitePolicy.validate(session("admin"), "Ada Lovelace", "invalid", "staff") }
    }

    @Test
    fun payloadNormalisesIdentityAndBindsCreator() {
        val payload = JSONObject(
            teamInvitePayload(
                session("manager"),
                "  Grace   Hopper  ",
                " GRACE@EXAMPLE.COM ",
                "staff",
                "a".repeat(64),
                "2026-09-05T00:00:00Z"
            )
        )
        assertEquals("Grace Hopper", payload.getString("display_name"))
        assertEquals("grace@example.com", payload.getString("email"))
        assertEquals("staff", payload.getString("role"))
        assertEquals("actor-id", payload.getString("created_by"))
    }

    private fun assertFails(block: () -> Unit) {
        var failed = false
        try {
            block()
        } catch (_: IllegalArgumentException) {
            failed = true
        }
        assertTrue(failed)
    }
}
