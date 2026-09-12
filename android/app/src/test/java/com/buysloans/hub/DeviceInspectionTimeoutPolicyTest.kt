package com.buysloans.hub

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DeviceInspectionTimeoutPolicyTest {
    @Test
    fun inspectionRequestIsSizedForInteractiveVisionLatency() {
        val source = File("src/main/java/com/buysloans/hub/DeviceInspectionClient.kt").readText()

        assertTrue(source.contains("private const val MAX_SINGLE_ENCODED_BYTES = 1_250_000"))
        assertTrue(source.contains("private const val LONG_EDGE = 1920"))
        assertTrue(source.contains("private const val FALLBACK_LONG_EDGE = 1600"))
        assertTrue(source.contains("private const val INSPECTION_READ_TIMEOUT_MS = 75_000"))
        assertTrue(source.contains("readTimeoutMs = INSPECTION_READ_TIMEOUT_MS"))
    }

    @Test
    fun inspectionProviderHasItsOwnDeadlineAndLatencySensitiveReasoning() {
        val source = File("../../supabase/functions/device-inspection/index.ts").readText()

        assertTrue(source.contains("const PROVIDER_TIMEOUT_MS = 60_000;"))
        assertTrue(source.contains("reasoning: { effort: \"low\" }"))
        assertTrue(source.contains("max_output_tokens: 1600"))
        assertTrue(source.contains("const providerController = new AbortController();"))
        assertTrue(source.contains("signal: providerController.signal"))
        assertTrue(source.contains("Device inspection took too long. Please retry."))
    }
}
