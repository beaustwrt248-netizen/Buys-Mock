package com.buysloans.nova;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CaptchaPayloadTest {
    @Test public void supabasePasswordAuthUsesGoTrueSecurityEnvelope() throws Exception {
        JSONObject body = new JSONObject()
                .put("email", "user@example.com")
                .put("password", "secret")
                .put("gotrue_meta_security", new JSONObject().put("captcha_token", "token-123"));
        assertEquals("token-123", body.getJSONObject("gotrue_meta_security").getString("captcha_token"));
    }
}
