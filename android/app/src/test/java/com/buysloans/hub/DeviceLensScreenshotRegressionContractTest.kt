package com.buysloans.hub

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceLensScreenshotRegressionContractTest {
    private val source = File("src/main/java/com/buysloans/hub/DeviceLensActivity.kt").readText()

    @Test
    fun camera_respects_system_insets_and_has_one_close_action() {
        assertTrue(source.contains("statusBarsPadding()"))
        assertTrue(source.contains("navigationBarsPadding()"))
        assertTrue(source.contains("fillMaxWidth(.74f).aspectRatio(.82f)"))
        val camera = source.substringAfter("private fun CameraCaptureSurface(").substringBefore("@Composable\nprivate fun ScanFrameOverlay")
        assertTrue(camera.contains("Position the device within the frame"))
        assertFalse(camera.contains("IconButton(onClick = onClose)"))
    }

    @Test
    fun failed_identity_scan_prioritises_recovery_instead_of_long_review_form() {
        assertTrue(source.contains("reviewState.selectedModel == null"))
        assertTrue(source.contains("We couldn't identify this device"))
        assertTrue(source.contains("Retake Photos"))
        assertTrue(source.contains("Why the scan failed"))
        assertTrue(source.contains("View AI details"))
    }
}
