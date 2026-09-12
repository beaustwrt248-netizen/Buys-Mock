package com.buysloans.admin

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdminSessionStoreTest {
    @Before
    fun resetBefore() {
        AdminSessionStore.clear()
    }

    @After
    fun resetAfter() {
        AdminSessionStore.clear()
    }

    @Test
    fun emptyStoreDoesNotAuthorizeWorkspace() {
        assertNull(AdminSessionStore.current())
        assertFalse(AdminSessionStore.hasAuthorizedSession())
    }

    @Test
    fun authorizedSessionCanBeRead() {
        val session = session()

        AdminSessionStore.set(session)

        assertEquals(session, AdminSessionStore.current())
        assertTrue(AdminSessionStore.hasAuthorizedSession())
    }

    @Test
    fun clearRemovesTokensAndAuthorization() {
        AdminSessionStore.set(session())

        AdminSessionStore.clear()

        assertNull(AdminSessionStore.current())
        assertFalse(AdminSessionStore.hasAuthorizedSession())
    }

    @Test
    fun incompleteSessionNeverAuthorizesWorkspace() {
        AdminSessionStore.set(session(accessToken = ""))

        assertFalse(AdminSessionStore.hasAuthorizedSession())
    }

    private fun session(accessToken: String = "access-token") = AdminSession(
        accessToken = accessToken,
        userId = "admin-user",
        displayName = "Admin",
        role = "admin",
        refreshToken = "refresh-token"
    )
}
