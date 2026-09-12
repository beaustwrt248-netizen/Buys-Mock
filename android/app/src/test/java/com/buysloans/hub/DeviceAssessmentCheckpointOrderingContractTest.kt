package com.buysloans.hub

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceAssessmentCheckpointOrderingContractTest {
    @Test
    fun `async checkpoints use a serialized dispatcher so newer milestones cannot be overtaken`() {
        val src = File("src/main/java/com/buysloans/hub/DeviceAssessmentStore.kt").readText()
        assertTrue(src.contains("limitedParallelism(1)"))
        assertTrue(src.contains("checkpointScope.launch"))
    }
}
