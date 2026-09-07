package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.util.Locale

class DriveRecoveryTimestampSourceContractTest {
    @Test
    fun postgresUtcTimestampRendersInPerthLocalTime() {
        val formatted = DriveBackupClient.localTimestamp(
            "2026-09-07 13:47:28.023974+00:00",
            ZoneId.of("Australia/Perth"),
            Locale.forLanguageTag("en-AU")
        )

        assertFalse(formatted.contains("+00:00"))
        assertFalse(formatted.contains("13:47"))
        assertTrue(formatted.contains("9:47") || formatted.contains("21:47"))
        assertTrue(formatted.contains("2026"))
    }

    @Test
    fun malformedTimestampFallsBackWithoutCrashing() {
        assertEquals(
            "not-a-timestamp",
            DriveBackupClient.localTimestamp(
                "not-a-timestamp",
                ZoneId.of("Australia/Perth"),
                Locale.forLanguageTag("en-AU")
            )
        )
    }
}
