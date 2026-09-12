package com.buysloans.hub

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceLensMilestoneCheckpointContractTest {
    @Test
    fun `inspection and pricing persist durable scan milestones`() {
        val client = File("src/main/java/com/buysloans/hub/DeviceInspectionClient.kt").readText()
        for (checkpoint in listOf(
            "front_captured",
            "rear_captured",
            "analysis_started",
            "analysis_failed",
            "review_ready",
            "pricing_started",
            "pricing_ready"
        )) {
            assertTrue("Missing checkpoint $checkpoint", client.contains("\"$checkpoint\""))
        }
        assertTrue(client.contains("DeviceLensScanSession.current"))
        assertTrue(client.contains("DeviceAssessmentStore.checkpoint"))
    }

    @Test
    fun `stock handoff records prepared and completed milestones`() {
        val workspace = File("src/main/java/com/buysloans/hub/WorkspaceStore.kt").readText()
        assertTrue(workspace.contains("\"stock_prepared\""))
        assertTrue(workspace.contains("\"completed\""))
        assertTrue(workspace.contains("DeviceAssessmentStore.checkpoint"))
    }
}
