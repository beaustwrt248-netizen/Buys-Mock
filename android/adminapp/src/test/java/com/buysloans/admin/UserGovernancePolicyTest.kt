package com.buysloans.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserGovernancePolicyTest {
    @Test
    fun managerRemainsReadOnlyForUserChanges() {
        val decision = UserGovernancePolicy.canMutate(
            actorRole = "manager",
            actorUserId = "manager-1",
            targetUserId = "staff-1",
            action = AdminUserAction.DISABLE
        )
        assertFalse(decision.allowed)
    }

    @Test
    fun adminCannotMutateOwnActiveAccount() {
        val decision = UserGovernancePolicy.canMutate(
            actorRole = "admin",
            actorUserId = "admin-1",
            targetUserId = "admin-1",
            action = AdminUserAction.SET_ROLE,
            requestedRole = "staff"
        )
        assertFalse(decision.allowed)
    }

    @Test
    fun adminCanChangeAnotherAccountRoleWithinAllowlist() {
        val decision = UserGovernancePolicy.canMutate(
            actorRole = "admin",
            actorUserId = "admin-1",
            targetUserId = "staff-1",
            action = AdminUserAction.SET_ROLE,
            requestedRole = "manager"
        )
        assertTrue(decision.allowed)
        assertEquals("set_role", UserGovernancePolicy.androidActionName(AdminUserAction.SET_ROLE))
    }

    @Test
    fun invalidRoleIsRejectedBeforeNetworkCall() {
        val decision = UserGovernancePolicy.canMutate(
            actorRole = "admin",
            actorUserId = "admin-1",
            targetUserId = "staff-1",
            action = AdminUserAction.SET_ROLE,
            requestedRole = "owner"
        )
        assertFalse(decision.allowed)
    }

    @Test
    fun androidScopeExcludesDestructiveAccountActions() {
        assertEquals(
            setOf(AdminUserAction.SET_ROLE, AdminUserAction.ENABLE, AdminUserAction.DISABLE),
            AdminUserAction.entries.toSet()
        )
    }
}
