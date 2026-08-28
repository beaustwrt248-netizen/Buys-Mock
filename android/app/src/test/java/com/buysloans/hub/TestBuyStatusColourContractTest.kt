package com.buysloans.hub

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TestBuyStatusColourContractTest {
    @Test
    fun checklistStatusesKeepExplicitGreenRedYellowColourMapping() {
        val source = File("src/main/java/com/buysloans/hub/TestBuyActivity.kt").readText()

        assertTrue(source.contains("TestResult.PASS -> TBGood"))
        assertTrue(source.contains("TestResult.FAIL -> TBBad"))
        assertTrue(source.contains("TestResult.NOT_APPLICABLE -> TBWarn"))
        assertTrue(source.contains("selectedContainerColor = resultColor.copy(alpha = .34f)"))
        assertTrue(source.contains("containerColor = if (isCompleted) statusColor.copy(alpha = .09f) else TBCard"))
    }
}
