package com.buysloans.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserControlApiTest {
    @Test
    fun rolePayloadUsesAllowlistedBackendAction() {
        val payload = UserControlApi.requestPayload("user-2", AdminUserAction.SET_ROLE, " Manager ")
        assertEquals("set_role", payload.getString("action"))
        assertEquals("user-2", payload.getString("target_user_id"))
        assertEquals("manager", payload.getString("role"))
    }

    @Test
    fun enablePayloadDoesNotCarryRoleOrDestructiveAction() {
        val payload = UserControlApi.requestPayload("user-2", AdminUserAction.ENABLE)
        assertEquals("enable", payload.getString("action"))
        assertFalse(payload.has("role"))
        assertFalse(payload.toString().contains("delete"))
        assertFalse(payload.toString().contains("force_signout"))
    }

    @Test
    fun disablePayloadIsNarrowAndExplicit() {
        val payload = UserControlApi.requestPayload("user-2", AdminUserAction.DISABLE)
        assertEquals(setOf("action", "target_user_id"), payload.keys().asSequence().toSet())
        assertEquals("disable", payload.getString("action"))
        assertTrue(payload.getString("target_user_id").isNotBlank())
    }
}
