package com.buysloans.hub

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class RepairDecisionCheckpointContractTest {
    @Test
    fun `repair decision records ready before staff confirmation`() {
        val src = File("src/main/java/com/buysloans/hub/MorleyRepairDecisionActivity.kt").readText()
        assertTrue(src.contains("\"repair_decision_ready\""))
        assertTrue(src.contains("\"staff_confirmed\""))
        assertTrue(src.indexOf("\"repair_decision_ready\"") < src.indexOf("\"staff_confirmed\""))
    }
}
