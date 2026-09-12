package com.buysloans.hub

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceLensRepairDecisionContractTest {
    private fun source(path: String): String = File(path).readText()

    @Test
    fun `device lens requires repair decision before stock preparation`() {
        val activity = source("src/main/java/com/buysloans/hub/DeviceLensActivity.kt")
        assertTrue(activity.contains("REPAIR_DECISION"))
        assertTrue(activity.contains("repairDecisionConfirmed"))
        assertTrue(activity.contains("MorleyRepairDecisionPanel"))
        assertTrue(activity.contains("Confirm staff decision"))
        assertTrue(activity.contains("Repair-or-Buy must be confirmed before adding stock"))
    }

    @Test
    fun `repair decision ui keeps recommendation advisory and shows commercial evidence`() {
        val ui = source("src/main/java/com/buysloans/hub/MorleyRepairDecisionUi.kt")
        assertTrue(ui.contains("AI recommendation"))
        assertTrue(ui.contains("Staff decision"))
        assertTrue(ui.contains("As-is margin"))
        assertTrue(ui.contains("Repaired margin"))
        assertTrue(ui.contains("Parts margin"))
        assertTrue(ui.contains("Repair cost"))
        assertTrue(ui.contains("Estimated repair time"))
        assertTrue(ui.contains("requiresStaffConfirmation"))
        assertFalse(ui.contains("autoConfirm"))
    }
}
