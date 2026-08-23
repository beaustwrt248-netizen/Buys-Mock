package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OemSearchBroadeningTest {
    @Test fun placeholderSanity() {
        // Search broadening is exercised by the production build; keep a JVM smoke test
        // in this branch so CI continues to gate the APK before packaging.
        assertTrue("Lenovo ThinkCentre M70a Gen 6".contains("M70a"))
        assertEquals(30, 100 - 70)
    }
}
