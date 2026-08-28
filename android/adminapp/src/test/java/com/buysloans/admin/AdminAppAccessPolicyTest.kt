package com.buysloans.admin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminAppAccessPolicyTest {
    @Test
    fun enabledStaffCanEnterSupportWorkspace() {
        assertTrue(AdminAppAccessPolicy.canEnter("staff", true))
    }

    @Test
    fun disabledOrOrdinaryUsersCannotEnter() {
        assertFalse(AdminAppAccessPolicy.canEnter("staff", false))
        assertFalse(AdminAppAccessPolicy.canEnter("user", true))
    }

    @Test
    fun staffCannotReadFullAdminSnapshot() {
        val staff = AdminSession("token", "staff-user", "Staff", "staff")
        assertTrue(AdminAppAccessPolicy.isSupportOnly(staff))
        assertFalse(AdminAppAccessPolicy.canReadFullSnapshot(staff))
    }

    @Test
    fun adminAndManagerRetainFullSnapshotAccess() {
        val admin = AdminSession("token", "admin-user", "Admin", "admin")
        val manager = AdminSession("token", "manager-user", "Manager", "manager")
        assertTrue(AdminAppAccessPolicy.canReadFullSnapshot(admin))
        assertTrue(AdminAppAccessPolicy.canReadFullSnapshot(manager))
        assertFalse(AdminAppAccessPolicy.isSupportOnly(admin))
        assertFalse(AdminAppAccessPolicy.isSupportOnly(manager))
    }
}
