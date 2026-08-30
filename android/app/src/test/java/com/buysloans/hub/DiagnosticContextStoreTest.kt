package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticContextStoreTest {
    @Test
    fun supportScreenDoesNotOverwriteProblemOrigin() {
        assertFalse(DiagnosticContextStore.shouldRecord("SupportTicketActivity"))
    }

    @Test
    fun normalApplicationScreenIsRetainedAsProblemOrigin() {
        assertTrue(DiagnosticContextStore.shouldRecord("MenuFeatureActivity"))
    }

    @Test
    fun blankScreenIsIgnored() {
        assertFalse(DiagnosticContextStore.shouldRecord(""))
    }
}
