package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TemporaryPasswordGateTest {
    @Test
    fun temporaryPasswordMetadataRequiresPasswordChange() {
        assertTrue(temporaryPasswordRequired("{\"user_metadata\":{\"must_change_password\":true}}"))
    }

    @Test
    fun ordinaryAccountPassesGate() {
        assertFalse(temporaryPasswordRequired("{\"user_metadata\":{\"must_change_password\":false}}"))
        assertFalse(temporaryPasswordRequired("{\"user_metadata\":{}}"))
    }

    @Test
    fun passwordChangePayloadIncludesCurrentPassword() {
        val payload = temporaryPasswordChangePayload("Temporary123!", "NewPassword123!")

        assertEquals("Temporary123!", payload.getString("current_password"))
        assertEquals("NewPassword123!", payload.getString("password"))
        assertFalse(payload.getJSONObject("data").getBoolean("must_change_password"))
    }
}
