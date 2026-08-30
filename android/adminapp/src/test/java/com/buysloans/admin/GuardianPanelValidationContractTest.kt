package com.buysloans.admin

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuardianPanelValidationContractTest {
    @Test
    fun validDraftDoesNotFallThroughToConfidenceParseError() {
        val source = File("src/main/java/com/buysloans/admin/GuardianPanel.kt").readText()

        assertTrue(source.contains("val validation = if (draft == null)"))
        assertTrue(source.contains("validateGuardianControlDraft(draft, session.role, current.killSwitch).orEmpty()"))
        assertFalse(source.contains("draft?.let { validateGuardianControlDraft(it, session.role, current.killSwitch) }\n        ?: \"Enter a valid confidence threshold.\""))
    }
}
