package com.buysloans.admin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AdminNativeLoginNavigationContractTest {
    private val source = File("src/main/java/com/buysloans/admin/AdminLoginActivity.kt").readText()

    @Test
    fun authorizedSessionIsOwnedByAndroidBeforeNavigation() {
        val storeIndex = source.indexOf("AdminSessionStore.set(session)")
        val navigationIndex = source.indexOf("Intent(this, AdminActivity::class.java)")
        assertTrue("login must store the authorized session", storeIndex >= 0)
        assertTrue("session must be stored before workspace navigation", navigationIndex > storeIndex)
    }

    @Test
    fun tokensAreNotTransferredThroughIntentExtras() {
        assertFalse(source.contains("EXTRA_ACCESS_TOKEN"))
        assertFalse(source.contains("EXTRA_REFRESH_TOKEN"))
        assertFalse(source.contains("session.accessToken)"))
        assertFalse(source.contains("session.refreshToken)"))
    }
}
