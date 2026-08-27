package com.buysloans.admin

import org.junit.Assert.assertEquals
import org.junit.Test

class VersionAdoptionTest {
    @Test
    fun classifiesCurrentOutdatedAheadAndUnknown() {
        val summary = summarizeVersionAdoption(
            listOf("2.14.6", "v2.14.6", "2.14.5", "2.15.0-test", null, "unknown"),
            "2.14.6",
        )

        assertEquals(2, summary.current)
        assertEquals(1, summary.outdated)
        assertEquals(1, summary.aheadOrTest)
        assertEquals(2, summary.unknown)
    }

    @Test
    fun missingCurrentReleaseKeepsAllDevicesUnknown() {
        val summary = summarizeVersionAdoption(listOf("2.14.6", "2.14.5"), null)
        assertEquals(0, summary.current)
        assertEquals(0, summary.outdated)
        assertEquals(0, summary.aheadOrTest)
        assertEquals(2, summary.unknown)
    }

    @Test
    fun partialVersionsCompareSafely() {
        val summary = summarizeVersionAdoption(listOf("2.14", "2.14.1", "2.13.9"), "2.14.0")
        assertEquals(1, summary.current)
        assertEquals(1, summary.outdated)
        assertEquals(1, summary.aheadOrTest)
        assertEquals(0, summary.unknown)
    }
}
