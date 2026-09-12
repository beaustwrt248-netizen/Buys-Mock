package com.buysloans.hub

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiScanHistoryContractTest {
    @Test
    fun `scan history activity lists durable Device Lens sessions without delete controls`() {
        val src = File("src/main/java/com/buysloans/hub/AiScanHistoryActivity.kt").readText()
        assertTrue(src.contains("AI Scan History"))
        assertTrue(src.contains("DeviceScanHistoryClient.list"))
        assertTrue(src.contains("Resume Scan"))
        assertTrue(src.contains("Retry Scan"))
        assertFalse(src.contains("Delete Scan"))
    }

    @Test
    fun `history client reads only Device Lens assessments newest first`() {
        val src = File("src/main/java/com/buysloans/hub/DeviceScanHistoryClient.kt").readText()
        assertTrue(src.contains("source=eq.device_lens"))
        assertTrue(src.contains("order=created_at.desc"))
        assertTrue(src.contains("BuildConfig.SUPABASE_PUBLISHABLE_KEY"))
        assertTrue(src.contains("AuthManager.validAccessToken(context)"))
        assertFalse(src.contains("SERVICE_ROLE"))
    }

    @Test
    fun `history resume binds the existing assessment before reopening Device Lens`() {
        val history = File("src/main/java/com/buysloans/hub/AiScanHistoryActivity.kt").readText()
        val session = File("src/main/java/com/buysloans/hub/DeviceLensScanSession.kt").readText()
        assertTrue(history.contains("DeviceLensScanSession.resume"))
        assertTrue(session.contains("fun resume"))
    }
}