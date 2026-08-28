package com.buysloans.admin

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AdminAuthPayloadTest {
    @Test
    fun passwordPayloadIncludesCaptchaTokenWithoutExtraAuthFields() {
        val json = JSONObject(passwordSignInPayload(" admin@example.com ", "secret", "captcha-token"))
        assertEquals("admin@example.com", json.getString("email"))
        assertEquals("secret", json.getString("password"))
        assertEquals("captcha-token", json.getJSONObject("gotrue_meta_security").getString("captcha_token"))
        assertFalse(json.has("service_role"))
        assertFalse(json.has("role"))
    }
}
