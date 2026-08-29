package com.buysloans.admin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class SupportQueueFreshnessTest {
    @Test
    fun staysCurrentBeforeFiveMinutes() {
        val result = supportQueueFreshness(1_000L, 1_000L + 4L * 60L * 1000L)

        assertFalse(result.stale)
        assertEquals(4L, result.ageMinutes)
    }

    @Test
    fun becomesStaleAtFiveMinutes() {
        val result = supportQueueFreshness(1_000L, 1_000L + SUPPORT_QUEUE_STALE_AFTER_MS)

        assertTrue(result.stale)
        assertEquals(5L, result.ageMinutes)
    }

    @Test
    fun clockSkewNeverProducesNegativeAge() {
        val result = supportQueueFreshness(10_000L, 5_000L)

        assertFalse(result.stale)
        assertEquals(0L, result.ageMinutes)
    }
}
