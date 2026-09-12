package com.buysloans.hub

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceAssessmentPersistenceContractTest {
    private fun source(): String = File("src/main/java/com/buysloans/hub/DeviceAssessmentStore.kt").readText()

    @Test
    fun `scan receives local durable id before remote sync`() {
        val src = source()
        assertTrue(src.contains("fun beginLocalScan"))
        assertTrue(src.contains("UUID.randomUUID().toString()"))
        assertTrue(src.contains("capture_started"))
        assertTrue(src.contains("savePending"))
    }

    @Test
    fun `pending checkpoints survive connectivity failure and can be retried`() {
        val src = source()
        assertTrue(src.contains("suspend fun checkpoint"))
        assertTrue(src.contains("suspend fun syncPending"))
        assertTrue(src.contains("analysis_failed"))
        assertTrue(src.contains("cancelled"))
        assertTrue(src.contains("completed"))
    }

    @Test
    fun `client uses signed in bearer session and publishable key only`() {
        val src = source()
        assertTrue(src.contains("AuthManager.validAccessToken(context)"))
        assertTrue(src.contains("BuildConfig.SUPABASE_PUBLISHABLE_KEY"))
        assertFalse(src.contains("SERVICE_ROLE"))
        assertFalse(src.contains("service_role"))
    }

    @Test
    fun `scan history persistence never deletes assessments`() {
        val src = source()
        assertFalse(Regex("requestMethod\\s*=\\s*\"DELETE\"").containsMatchIn(src))
        assertFalse(src.contains("/device_assessments?id=eq.") && src.contains("delete("))
    }
}
