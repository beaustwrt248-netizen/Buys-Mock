package com.buysloans.hub

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
}
