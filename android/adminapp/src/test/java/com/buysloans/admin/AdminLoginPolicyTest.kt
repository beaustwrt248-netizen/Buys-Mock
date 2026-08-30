package com.buysloans.admin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminLoginPolicyTest {
    @Test fun validCredentialsAndCaptchaEnableSignIn() {
        assertTrue(isAdminLoginReady("admin@example.com", "secret", "captcha-token", busy = false))
    }

    @Test fun validTrimmedEmailEnablesSignIn() {
        assertTrue(isAdminLoginReady("  admin@example.com  ", "secret", "captcha-token", busy = false))
    }

    @Test fun commonValidEmailFormsEnableSignIn() {
        assertTrue(isAdminLoginReady("first.last+admin@example.com.au", "secret", "captcha-token", busy = false))
    }

    @Test fun malformedEmailKeepsSignInDisabled() {
        assertFalse(isAdminLoginReady("admin", "secret", "captcha-token", busy = false))
        assertFalse(isAdminLoginReady("admin@localhost", "secret", "captcha-token", busy = false))
        assertFalse(isAdminLoginReady("@example.com", "secret", "captcha-token", busy = false))
        assertFalse(isAdminLoginReady("admin@example", "secret", "captcha-token", busy = false))
        assertFalse(isAdminLoginReady("admin @example.com", "secret", "captcha-token", busy = false))
    }

    @Test fun emptyEmailKeepsSignInDisabled() {
        assertFalse(isAdminLoginReady("", "secret", "captcha-token", busy = false))
        assertFalse(isAdminLoginReady("   ", "secret", "captcha-token", busy = false))
    }

    @Test fun emptyPasswordKeepsSignInDisabled() {
        assertFalse(isAdminLoginReady("admin@example.com", "", "captcha-token", busy = false))
    }

    @Test fun missingCaptchaKeepsSignInDisabled() {
        assertFalse(isAdminLoginReady("admin@example.com", "secret", "", busy = false))
        assertFalse(isAdminLoginReady("admin@example.com", "secret", "   ", busy = false))
    }

    @Test fun busyStateKeepsSignInDisabled() {
        assertFalse(isAdminLoginReady("admin@example.com", "secret", "captcha-token", busy = true))
    }
}
