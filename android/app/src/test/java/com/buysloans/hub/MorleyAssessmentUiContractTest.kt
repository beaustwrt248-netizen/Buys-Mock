package com.buysloans.hub

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MorleyAssessmentUiContractTest {
    private fun source(): String = File("src/main/java/com/buysloans/hub/MorleyAssessmentUi.kt").readText()

    @Test
    fun `assessment review keeps unresolved identity and storage explicit`() {
        val src = source()
        assertTrue(src.contains("Identity unresolved"))
        assertTrue(src.contains("Storage unresolved"))
        assertFalse(src.contains("Unknown device"))
        assertFalse(src.contains("0 GB"))
    }

    @Test
    fun `unobserved hardware checks stay not tested instead of being guessed`() {
        val src = source()
        assertTrue(src.contains("not_tested"))
        assertTrue(src.contains("Not tested"))
        assertFalse(src.contains("assumePass"))
    }

    @Test
    fun `review surfaces condition valuation repair and risk explanations`() {
        val src = source()
        assertTrue(src.contains("Condition"))
        assertTrue(src.contains("Valuation"))
        assertTrue(src.contains("Repair or Buy"))
        assertTrue(src.contains("Risk review"))
    }

    @Test
    fun `commercial action remains locked behind explicit staff confirmation`() {
        val src = source()
        assertTrue(src.contains("explicitStaffConfirmation"))
        assertTrue(src.contains("Confirm recommendation"))
        assertTrue(src.contains("requiresStaffConfirmation"))
    }

    @Test
    fun `assessment review preserves Morley blue visual language`() {
        val src = source()
        assertTrue(src.contains("MorleyBlue"))
        assertTrue(src.contains("MaterialTheme"))
    }
}