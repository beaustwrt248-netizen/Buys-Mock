package com.buysloans.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GuardianControlPolicyTest {
    private fun draft(
        killSwitch: Boolean = false,
        reason: String = "",
        risk: String = "low",
        mode: String = "observe",
        confidence: Double = 0.850,
        parallel: Int = 1,
    ) = GuardianControlDraft(
        enabled = true,
        autoFixEnabled = true,
        maxAutoRisk = risk,
        operatingMode = mode,
        learningEnabled = true,
        evolutionEnabled = false,
        confidenceThreshold = confidence,
        maxParallelRepairs = parallel,
        quarantineOnRepeatedFailure = true,
        killSwitch = killSwitch,
        killSwitchReason = reason,
    )

    @Test fun adminAndManagerCanUseGuardedControls() {
        assertNull(validateGuardianControlDraft(draft(), "admin", false))
        assertNull(validateGuardianControlDraft(draft(), "manager", false))
    }

    @Test fun staffCannotUseGuardianControls() {
        assertEquals("Guardian controls require Admin or Manager access.", validateGuardianControlDraft(draft(), "staff", false))
    }

    @Test fun managerCannotDisengageExistingKillSwitch() {
        assertEquals("Only an Admin can disengage the Guardian kill switch.", validateGuardianControlDraft(draft(killSwitch = false), "manager", true))
        assertNull(validateGuardianControlDraft(draft(killSwitch = false), "admin", true))
    }

    @Test fun killSwitchRequiresReasonAndForcesRuntimeOffInPayload() {
        assertEquals("Enter a reason before engaging the Guardian kill switch.", validateGuardianControlDraft(draft(killSwitch = true, reason = "x"), "admin", false))
        val payload = guardianControlPayload(draft(killSwitch = true, reason = "incident response"))
        assertEquals(false, payload.getBoolean("p_enabled"))
        assertEquals(false, payload.getBoolean("p_auto_fix_enabled"))
        assertEquals("incident response", payload.getString("p_kill_switch_reason"))
    }

    @Test fun backendBoundsAreMirroredLocally() {
        assertEquals("Maximum automatic risk must be low or medium.", validateGuardianControlDraft(draft(risk = "high"), "admin", false))
        assertEquals("Guardian operating mode is invalid.", validateGuardianControlDraft(draft(mode = "unrestricted"), "admin", false))
        assertEquals("Confidence threshold must be between 0.500 and 0.999.", validateGuardianControlDraft(draft(confidence = 1.0), "admin", false))
        assertEquals("Parallel repair limit must be between 1 and 5.", validateGuardianControlDraft(draft(parallel = 6), "admin", false))
    }
}
