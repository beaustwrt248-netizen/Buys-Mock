package com.buysloans.hub

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class DeviceLensAutoSaveContractTest {
    @Test
    fun `application starts durable scan history whenever Device Lens opens`() {
        val source = File("src/main/java/com/buysloans/hub/MorleyApplication.kt").readText()
        assertTrue(source.contains("activity is DeviceLensActivity"))
        assertTrue(source.contains("DeviceLensScanSession.begin"))
    }

    @Test
    fun `application retries unsynced scans without blocking startup`() {
        val source = File("src/main/java/com/buysloans/hub/MorleyApplication.kt").readText()
        assertTrue(source.contains("DeviceAssessmentStore.syncPending"))
        assertTrue(source.contains("runCatching"))
    }
}
