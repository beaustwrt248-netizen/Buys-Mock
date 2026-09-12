package com.buysloans.hub

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MorleyRepairDecisionActivityRuntimeContractTest {
    @Test
    fun `repair decision screen uses live Compose context instead of uninitialised singleton`() {
        val src = File("src/main/java/com/buysloans/hub/MorleyRepairDecisionActivity.kt").readText()
        assertTrue(src.contains("LocalContext.current"))
        assertFalse(src.contains("private object LocalRepairContext"))
        assertFalse(src.contains("lateinit var current: Context"))
    }
}
