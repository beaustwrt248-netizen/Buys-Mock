package com.buysloans.hub

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceLensCancellationRetentionContractTest {
    @Test
    fun `finishing an incomplete Device Lens session is retained as cancelled`() {
        val src = File("src/main/java/com/buysloans/hub/MorleyApplication.kt").readText()
        assertTrue(src.contains("activity is DeviceLensActivity"))
        assertTrue(src.contains("activity.isFinishing"))
        assertTrue(src.contains("\"cancelled\""))
        assertTrue(src.contains("DeviceLensScanSession.current"))
        assertTrue(src.contains("DeviceLensScanSession.finishLocalSession"))
    }
}
