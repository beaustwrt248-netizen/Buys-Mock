package com.buysloans.admin

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun refreshPayloadContainsOnlyRefreshToken() {
        val json = JSONObject(refreshSessionPayload("refresh-value"))
        assertEquals("refresh-value", json.getString("refresh_token"))
        assertEquals(1, json.length())
    }

    @Test
    fun expiredJwtResponseIsRecognisedWithoutTreatingOtherAuthErrorsAsExpired() {
        assertTrue(isExpiredJwtResponse(401, "{\"message\":\"JWT expired\"}"))
        assertFalse(isExpiredJwtResponse(400, "{\"message\":\"JWT expired\"}"))
        assertFalse(isExpiredJwtResponse(401, "{\"message\":\"Invalid login credentials\"}"))
    }

    @Test
    fun temporaryUserPayloadExplicitlyUsesVerifiedTemporaryPasswordMode() {
        val json = JSONObject(temporaryUserPayload(" Beau Staff ", " USER@EXAMPLE.COM ", "staff", "TempPass123!"))
        assertEquals("create_user", json.getString("action"))
        assertEquals("user@example.com", json.getString("email"))
        assertEquals("Beau Staff", json.getString("display_name"))
        assertEquals("staff", json.getString("role"))
        assertEquals("TempPass123!", json.getString("temporary_password"))
        assertTrue(json.getBoolean("skip_email_verification"))
    }
}
