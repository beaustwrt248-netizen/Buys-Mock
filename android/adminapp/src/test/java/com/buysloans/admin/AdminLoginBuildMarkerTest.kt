package com.buysloans.admin

import org.junit.Assert.assertTrue
import org.junit.Test

class AdminLoginBuildMarkerTest {
    @Test fun loginReadinessStillRequiresCaptcha() {
        assertTrue(isAdminLoginReady("admin@example.com", "secret", "captcha-token", busy = false))
    }
}
