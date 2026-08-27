package com.buysloans.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class UserControlCoordinatorTest {
    private val admin = AdminSession("token", "admin-1", "Admin", "admin")
    private val manager = AdminSession("token", "manager-1", "Manager", "manager")

    private fun mutableUser(enabled: Boolean = true) = UserAccessPresentation(
        userId = "user-2",
        displayName = "Test User",
        role = "staff",
        enabled = enabled,
        isSelf = false,
        canChangeRole = true,
        canEnable = !enabled,
        canDisable = enabled,
        readOnlyReason = null
    )

    @Test
    fun preparesOnlyAllowlistedAuditedRoleCommand() {
        val command = UserControlCoordinator.prepare(
            session = admin,
            user = mutableUser(),
            action = AdminUserAction.SET_ROLE,
            requestedRole = " Manager "
        )
        assertEquals("user-2", command.targetUserId)
        assertEquals(AdminUserAction.SET_ROLE, command.action)
        assertEquals("manager", command.requestedRole)
        assertTrue(command.confirmationText.contains("audited", ignoreCase = true))
    }

    @Test
    fun refusesPresentationBlockedActionBeforeNetworkBoundary() {
        val user = mutableUser(enabled = true).copy(canDisable = false, readOnlyReason = "Read only")
        val error = assertThrows(IllegalArgumentException::class.java) {
            UserControlCoordinator.prepare(admin, user, AdminUserAction.DISABLE)
        }
        assertTrue(error.message.orEmpty().contains("Read only"))
    }

    @Test
    fun managerCannotBypassGovernanceEvenWithForgedPresentation() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            UserControlCoordinator.prepare(manager, mutableUser(), AdminUserAction.SET_ROLE, "admin")
        }
        assertTrue(error.message.orEmpty().contains("Only enabled Admin"))
    }

    @Test
    fun invalidRoleNeverProducesCommand() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            UserControlCoordinator.prepare(admin, mutableUser(), AdminUserAction.SET_ROLE, "owner")
        }
        assertTrue(error.message.orEmpty().contains("staff, manager or admin"))
    }

    @Test
    fun selfProtectionRemainsVisibleInPresentationContract() {
        val self = mutableUser().copy(
            userId = admin.userId,
            isSelf = true,
            canChangeRole = false,
            canEnable = false,
            canDisable = false,
            readOnlyReason = "Your signed-in Admin account is protected from Android access changes."
        )
        assertFalse(self.canDisable)
        val error = assertThrows(IllegalArgumentException::class.java) {
            UserControlCoordinator.prepare(admin, self, AdminUserAction.DISABLE)
        }
        assertTrue(error.message.orEmpty().contains("protected"))
    }
}
