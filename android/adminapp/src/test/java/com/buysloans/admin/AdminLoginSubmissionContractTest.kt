package com.buysloans.admin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminLoginSubmissionContractTest {
    @Test fun readyInputsEnableBeforeSubmission() {
        assertTrue(isAdminLoginReady("admin@example.com", "secret", "captcha-token", busy = false))
    }

    @Test fun activeSubmissionDisablesDuplicateSubmit() {
        assertFalse(isAdminLoginReady("admin@example.com", "secret", "captcha-token", busy = true))
    }

    @Test fun captchaStillRequiredAfterAnyFailureReset() {
        assertFalse(isAdminLoginReady("admin@example.com", "secret", "", busy = false))
    }
}
