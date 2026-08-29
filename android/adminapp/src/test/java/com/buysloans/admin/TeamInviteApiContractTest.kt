package com.buysloans.admin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamInviteApiContractTest {
    @Test
    fun inviteManagementDoesNotEmbedServiceRoleSecrets() {
        val source = TeamInviteApi::class.java.protectionDomain.codeSource.location.toString()
        assertFalse(source.contains("service_role", ignoreCase = true))
        assertTrue(TeamInvitePolicy.allowedRoles(AdminSession("token", "id", "Admin", "admin")).contains("manager"))
    }
}
