package com.buysloans.admin

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserControlPresentationTest {
    private val profiles = JSONArray(
        """[
          {"id":"admin-1","display_name":"Current Admin","role":"admin","is_enabled":true},
          {"id":"staff-1","display_name":"Alex","role":"staff","is_enabled":true},
          {"id":"manager-1","display_name":"Morgan","role":"manager","is_enabled":false}
        ]"""
    )

    @Test
    fun adminGetsOnlyNarrowActionsOnOtherAccounts() {
        val rows = buildUserAccessPresentation(
            AdminSession("token", "admin-1", "Admin", "admin"),
            profiles
        )
        val staff = rows.first { it.userId == "staff-1" }
        assertTrue(staff.canChangeRole)
        assertTrue(staff.canDisable)
        assertFalse(staff.canEnable)

        val disabledManager = rows.first { it.userId == "manager-1" }
        assertTrue(disabledManager.canChangeRole)
        assertTrue(disabledManager.canEnable)
        assertFalse(disabledManager.canDisable)
    }

    @Test
    fun signedInAdminRemainsProtected() {
        val self = buildUserAccessPresentation(
            AdminSession("token", "admin-1", "Admin", "admin"),
            profiles
        ).first { it.userId == "admin-1" }
        assertTrue(self.isSelf)
        assertFalse(self.canChangeRole)
        assertFalse(self.canEnable)
        assertFalse(self.canDisable)
        assertTrue(self.readOnlyReason.orEmpty().contains("protected"))
    }

    @Test
    fun managerPresentationIsReadOnly() {
        val rows = buildUserAccessPresentation(
            AdminSession("token", "manager-session", "Manager", "manager"),
            profiles
        )
        assertTrue(rows.all { !it.canChangeRole && !it.canEnable && !it.canDisable })
        assertTrue(rows.all { it.readOnlyReason == "Managers have read-only user visibility." })
    }

    @Test
    fun confirmationNeverOffersDeleteOrForceSignout() {
        val user = buildUserAccessPresentation(
            AdminSession("token", "admin-1", "Admin", "admin"),
            profiles
        ).first { it.userId == "staff-1" }
        val confirmations = listOf(
            userActionConfirmation(user, AdminUserAction.SET_ROLE, "manager"),
            userActionConfirmation(user, AdminUserAction.ENABLE),
            userActionConfirmation(user, AdminUserAction.DISABLE)
        )
        assertEquals(3, confirmations.size)
        assertTrue(confirmations.all { it.contains("audited") })
        assertTrue(confirmations.none { it.contains("delete", ignoreCase = true) || it.contains("signout", ignoreCase = true) })
    }
}
