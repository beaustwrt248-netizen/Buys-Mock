package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaintenanceModePolicyTest {
    @Test
    fun disabledFlagAllowsNormalUse() {
        val state = MaintenanceModePolicy.state(false, "Planned maintenance")
        assertFalse(state.enabled)
        assertEquals("Planned maintenance", state.message)
    }

    @Test
    fun enabledFlagBlocksWithCustomMessage() {
        val state = MaintenanceModePolicy.state(true, "Currently under maintenance")
        assertTrue(state.enabled)
        assertEquals("Currently under maintenance", state.message)
    }

    @Test
    fun enabledFlagUsesSafeDefaultForBlankMessage() {
        val state = MaintenanceModePolicy.state(true, "   ")
        assertTrue(state.enabled)
        assertEquals(MaintenanceModePolicy.DEFAULT_MESSAGE, state.message)
    }
}
