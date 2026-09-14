package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DeviceLensExperienceContractTest {
    private val lensSource = File("src/main/java/com/buysloans/hub/DeviceLensActivity.kt")

    @Test
    fun cameraScreenUsesApprovedMorleyAiScanLayout() {
        val source = lensSource.readText()

        assertTrue(source.contains("Position the device within the frame"))
        assertTrue(source.contains("Scan Device"))
        assertTrue(source.contains("Auto"))
        assertTrue(source.contains("Barcode"))
        assertTrue(source.contains("Serial Number"))
        assertTrue(source.contains("ScanFrameOverlay"))
    }

    @Test
    fun analysingScreenCommunicatesAiProgress() {
        val source = lensSource.readText()

        assertTrue(source.contains("Analysing Device"))
        assertTrue(source.contains("Detecting model"))
        assertTrue(source.contains("Checking specifications"))
        assertTrue(source.contains("Identifying condition"))
        assertTrue(source.contains("Searching market data"))
        assertTrue(source.contains("Keep the device in frame for the best results."))
    }

    @Test
    fun resultsScreenSurfacesIdentityConditionAndValueActions() {
        val source = lensSource.readText()

        assertTrue(source.contains("Scan Result"))
        assertTrue(source.contains("Full Specifications"))
        assertTrue(source.contains("Condition Assessment"))
        assertTrue(source.contains("Market Value"))
        assertTrue(source.contains("Compare Prices"))
        assertTrue(source.contains("New Scan"))
    }

    @Test
    fun redesignDoesNotMoveIntoDashboardImplementation() {
        val dashboard = File("src/main/java/com/buysloans/hub/DashboardActivity.kt").readText()

        assertFalse(dashboard.contains("ScanFrameOverlay"))
        assertFalse(dashboard.contains("Analysing Device"))
    }
}
