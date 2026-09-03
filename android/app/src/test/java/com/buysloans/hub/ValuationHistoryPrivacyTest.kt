package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Test

class ValuationHistoryPrivacyTest {
    @Test
    fun rawBackendErrorBodyFallsBackToSafeCopy() {
        val raw = IllegalStateException("Could not save valuation: {\"message\":\"internal detail\"}")
        assertEquals(
            "Could not save valuation. Try again.",
            privacySafeValuationError(raw, "Could not save valuation. Try again.")
        )
    }

    @Test
    fun guardianApprovedSafeLoadMessageCanPassThrough() {
        val safe = IllegalStateException("Your session has expired. Sign in again.")
        assertEquals(
            "Your session has expired. Sign in again.",
            privacySafeValuationError(safe, "Could not load history. Try again.")
        )
    }
}
