package com.buysloans.hub

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceLensRepairDecisionContractTest {
    private fun source(path: String): String = File(path).readText()

    @Test
    fun `device lens inventory handoff requires explicit repair decision confirmation`() {
        val workspace = source("src/main/java/com/buysloans/hub/WorkspaceStore.kt")
        val activity = source("src/main/java/com/buysloans/hub/MorleyRepairDecisionActivity.kt")
        val manifest = source("src/main/AndroidManifest.xml")

        assertTrue(workspace.contains("context is DeviceLensActivity"))
        assertTrue(workspace.contains("MorleyRepairDecisionConfirmationStore.isConfirmed"))
        assertTrue(workspace.contains("MorleyRepairDecisionActivity.createIntent"))
        assertTrue(workspace.contains("Repair-or-Buy must be confirmed before adding stock"))
        assertTrue(activity.contains("MorleyRepairDecisionPanel"))
        assertTrue(activity.contains("Confirm staff decision"))
        assertTrue(activity.contains("DeviceAssessmentStore.checkpoint"))
        assertTrue(activity.contains("\"staff_confirmed\""))
        assertTrue(manifest.contains(".MorleyRepairDecisionActivity"))
    }

    @Test
    fun `repair decision ui stays advisory and never creates a second decision engine`() {
        val ui = source("src/main/java/com/buysloans/hub/MorleyRepairDecisionUi.kt")
        assertTrue(ui.contains("AI recommendation"))
        assertTrue(ui.contains("Staff decision"))
        assertTrue(ui.contains("As-is margin"))
        assertTrue(ui.contains("Repaired margin"))
        assertTrue(ui.contains("Parts margin"))
        assertTrue(ui.contains("Repair cost"))
        assertTrue(ui.contains("Estimated repair time"))
        assertTrue(ui.contains("requiresStaffConfirmation"))
        assertTrue(ui.contains("MorleyAssessmentCore remains the canonical Repair-or-Buy rules engine"))
        assertFalse(ui.contains("autoConfirm"))
        assertFalse(ui.contains("object MorleyRepairDecisionPolicy"))
        assertFalse(ui.contains("fun evaluate("))
    }
}
