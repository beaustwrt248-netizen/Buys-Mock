package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceChecklistProfileTest {
    @Test
    fun allProfilesKeepSharedSafetyChecksAndUniqueIds() {
        DeviceCategory.entries.forEach { category ->
            val specs = DeviceChecklistProfiles.forCategory(category)
            val ids = specs.map { it.id }
            assertEquals(ids.toSet().size, ids.size)
            assertTrue(ids.containsAll(listOf("power", "display", "ports", "wifi", "bluetooth", "speakers", "storage")))
            assertEquals(specs.map { it.id to it.label }, checklistFor(category).map { it.id to it.label })
        }
    }

    @Test
    fun laptopProfileCoversBatteryInputCameraAndMicrophone() {
        val ids = DeviceChecklistProfiles.forCategory(DeviceCategory.LAPTOP).map { it.id }.toSet()
        assertTrue(ids.containsAll(setOf("battery", "keyboard", "camera", "microphone")))
    }

    @Test
    fun desktopProfileAvoidsClaimingUnsupportedStressDiagnostics() {
        val specs = DeviceChecklistProfiles.forCategory(DeviceCategory.DESKTOP_PC)
        assertTrue(specs.any { it.id == "usb" })
        assertTrue(specs.any { it.id == "ethernet" && it.optional })
        assertTrue(specs.any { it.id == "gpu_output" && it.guidance.contains("Do not infer GPU stress stability") })
    }

    @Test
    fun consoleProfileMakesDiscDriveOptionalForDigitalModels() {
        val disc = DeviceChecklistProfiles.forCategory(DeviceCategory.CONSOLE).single { it.id == "disc" }
        assertTrue(disc.optional)
        assertTrue(disc.guidance.contains("Mark N/A"))
    }

    @Test
    fun phoneNfcProfileIsExplicitlyAndroidReadOnlyAndInventoryIsolated() {
        val nfc = DeviceChecklistProfiles.forCategory(DeviceCategory.PHONE).single { it.id == "nfc" }
        assertTrue(nfc.optional)
        assertTrue(nfc.guidance.contains("Android only"))
        assertTrue(nfc.guidance.contains("read-only"))
        assertTrue(nfc.guidance.contains("must not look up, assign, link, unlink or modify inventory"))
    }

    @Test
    fun storageAndBatteryGuidanceDoNotOverclaimHealthDiagnostics() {
        val storage = DeviceChecklistProfiles.forCategory(DeviceCategory.LAPTOP).single { it.id == "storage" }
        val battery = DeviceChecklistProfiles.forCategory(DeviceCategory.PHONE).single { it.id == "battery" }
        assertTrue(storage.guidance.contains("Do not claim SMART or flash-health diagnostics when unavailable"))
        assertTrue(battery.guidance.contains("do not claim battery capacity testing without supported data"))
        assertFalse(storage.guidance.contains("guaranteed", ignoreCase = true))
        assertFalse(battery.guidance.contains("guaranteed", ignoreCase = true))
    }
}
