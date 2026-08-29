package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceTestChecklistReviewTest {
    @Test
    fun `missing catalog results stay explicitly untested`() {
        val review = DeviceTestChecklistReviewer.review(
            DeviceTestCategory.PHONE,
            listOf(DeviceTestChecklistResult("battery", TestResult.PASS))
        )

        assertEquals(1, review.passedChecks)
        assertEquals(review.totalChecks - 1, review.untestedChecks)
        assertFalse(review.isComplete)
    }

    @Test
    fun `failed checks become human-readable fault evidence`() {
        val review = DeviceTestChecklistReviewer.review(
            DeviceTestCategory.CONSOLE,
            listOf(
                DeviceTestChecklistResult("power_boot", TestResult.PASS),
                DeviceTestChecklistResult("display", TestResult.FAIL, "HDMI output flickers")
            )
        )

        assertEquals(1, review.failedChecks)
        assertEquals(listOf("Display output: HDMI output flickers"), review.faults)
    }

    @Test
    fun `not applicable counts as completed without pretending it passed`() {
        val catalog = DeviceTestChecklistCatalog.forCategory(DeviceTestCategory.LAPTOP)
        val results = catalog.map { item ->
            DeviceTestChecklistResult(
                checklistItemId = item.id,
                result = if (item.id == "camera") TestResult.NOT_APPLICABLE else TestResult.PASS
            )
        }

        val review = DeviceTestChecklistReviewer.review(DeviceTestCategory.LAPTOP, results)

        assertTrue(review.isComplete)
        assertEquals(1, review.notApplicableChecks)
        assertEquals(catalog.size - 1, review.passedChecks)
        assertEquals(0, review.failedChecks)
        assertEquals(0, review.untestedChecks)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown checklist result is rejected`() {
        DeviceTestChecklistReviewer.review(
            DeviceTestCategory.PC,
            listOf(DeviceTestChecklistResult("unsupported_probe", TestResult.PASS))
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `duplicate checklist result is rejected`() {
        DeviceTestChecklistReviewer.review(
            DeviceTestCategory.PHONE,
            listOf(
                DeviceTestChecklistResult("battery", TestResult.PASS),
                DeviceTestChecklistResult("battery", TestResult.FAIL)
            )
        )
    }
}
