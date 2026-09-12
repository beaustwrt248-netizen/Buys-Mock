package com.buysloans.hub

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepairDecisionCanonicalAuthorityContractTest {
    @Test
    fun `legacy Android adapter never calculates a second repair recommendation`() {
        val src = File("src/main/java/com/buysloans/hub/MorleyRepairDecisionPolicy.kt").readText()
        assertTrue(src.contains("awaitingCanonicalRecommendation"))
        assertTrue(src.contains("MorleyAssessmentCore remains canonical"))
        assertFalse(src.contains("repairedMargin"))
        assertFalse(src.contains("partsMargin"))
        assertFalse(src.contains("maxOf"))
        assertFalse(src.contains("when ("))
    }
}
