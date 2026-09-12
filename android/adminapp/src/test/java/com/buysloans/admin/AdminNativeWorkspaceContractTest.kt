package com.buysloans.admin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AdminNativeWorkspaceContractTest {
    private val adminActivitySource = File("src/main/java/com/buysloans/admin/AdminActivity.kt").readText()
    private val loginSource = File("src/main/java/com/buysloans/admin/AdminLoginActivity.kt").readText()

    @Test
    fun authenticatedWorkspaceIsComposeOnly() {
        listOf(
            "android.webkit.WebView",
            "WebView(",
            "evaluateJavascript",
            "loadUrl(",
            "installNativeAdminSession",
            "/admin/native-logout",
            "EXTRA_ACCESS_TOKEN",
            "EXTRA_REFRESH_TOKEN"
        ).forEach { forbidden ->
            assertFalse("AdminActivity must not contain $forbidden", adminActivitySource.contains(forbidden))
        }
        assertTrue(adminActivitySource.contains("AdminSessionStore.current()"))
    }

    @Test
    fun loginStoresNativeSessionBeforeOpeningWorkspace() {
        assertTrue(loginSource.contains("AdminSessionStore.set(session)"))
        assertTrue(loginSource.indexOf("AdminSessionStore.set(session)") < loginSource.indexOf("Intent(this, AdminActivity::class.java)"))
        assertFalse(loginSource.contains("putExtra(AdminActivity.EXTRA_ACCESS_TOKEN"))
        assertFalse(loginSource.contains("putExtra(AdminActivity.EXTRA_REFRESH_TOKEN"))
    }

    @Test
    fun onlyLoginOwnsTheTurnstileBridge() {
        assertTrue(loginSource.contains("addJavascriptInterface"))
        assertTrue(loginSource.contains("\"AndroidBridge\""))
        assertFalse(adminActivitySource.contains("addJavascriptInterface"))
    }
}
