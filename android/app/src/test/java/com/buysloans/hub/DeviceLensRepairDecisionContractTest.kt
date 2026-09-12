package com.buysloans.hub

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceLensRepairDecisionContractTest {
    private fun source(path: String): String = File(path).readText()

    @Test
    fun `device lens inventory handoff requires one explicit repair decision confirmation`() {
        val lens = source("src/main/java/com/buysloans/hub/DeviceLensActivity.kt")
        val workspace = source("src/main/java/com/buysloans/hub/WorkspaceStore.kt")
        val ui = source("src/main/java/com/buysloans/hub/MorleyRepairDecisionUi.kt")

        val guard = lens.indexOf("if (!requireRepairDecision()) return@AddStockScreen")
        val handoff = lens.indexOf("WorkspaceStore.addInventory(")
        assertTrue(guard >= 0)
        assertTrue(handoff > guard)
        assertTrue(lens.contains("Repair-or-Buy must be confirmed before adding stock"))
        assertTrue(ui.contains("Confirm staff decision"))
        assertTrue(workspace.contains("\"staff_confirmed\""))
        assertTrue(workspace.contains("\"stock_prepared\""))
        assertTrue(workspace.contains("\"completed\""))
        assertFalse(workspace.contains("MorleyRepairDecisionActivity.createIntent"))
    }

    @Test
    fun `repair decision screen uses live Compose context rather than uninitialised global context`() {
        val activity = source("src/main/java/com/buysloans/hub/MorleyRepairDecisionActivity.kt")
        assertTrue(activity.contains("LocalContext.current"))
        assertFalse(activity.contains("LocalRepairContext"))
        assertFalse(activity.contains("lateinit var current: Context"))
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
