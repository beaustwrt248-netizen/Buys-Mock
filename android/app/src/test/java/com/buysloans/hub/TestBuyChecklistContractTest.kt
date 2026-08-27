package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TestBuyChecklistContractTest {
    private val sharedIds = setOf("power", "display", "ports", "wifi", "bluetooth", "speakers", "storage")

    @Test
    fun everyCategoryKeepsSharedChecksAndUniqueIds() {
        DeviceCategory.entries.forEach { category ->
            val checks = checklistFor(category)
            val ids = checks.map { it.id }

            assertTrue("$category lost a shared hardware check", ids.containsAll(sharedIds))
            assertEquals("$category contains duplicate checklist IDs", ids.size, ids.toSet().size)
            assertTrue("$category checklist must not be empty", checks.isNotEmpty())
            assertTrue("$category checklist labels must be usable", checks.all { it.label.isNotBlank() })
        }
    }

    @Test
    fun laptopChecklistCoversPortableComputerBasics() {
        val ids = checklistFor(DeviceCategory.LAPTOP).map { it.id }.toSet()
        assertTrue(ids.containsAll(setOf("battery", "keyboard", "camera", "microphone")))
    }

    @Test
    fun desktopChecklistCoversPcSpecificConnections() {
        val ids = checklistFor(DeviceCategory.DESKTOP_PC).map { it.id }.toSet()
        assertTrue(ids.containsAll(setOf("usb", "ethernet", "gpu_output")))
    }

    @Test
    fun consoleChecklistCoversControllerDiscAndHdmi() {
        val ids = checklistFor(DeviceCategory.CONSOLE).map { it.id }.toSet()
        assertTrue(ids.containsAll(setOf("controller", "disc", "hdmi")))
    }

    @Test
    fun phoneChecklistCoversPortablePhoneHardwareAndReadOnlyNfc() {
        val checks = checklistFor(DeviceCategory.PHONE)
        val ids = checks.map { it.id }.toSet()
        assertTrue(ids.containsAll(setOf("battery", "touch", "camera", "microphone", "cellular", "nfc")))

        val nfc = checks.single { it.id == "nfc" }
        assertTrue(nfc.label.contains("scan/read", ignoreCase = true))
        assertFalse(nfc.label.contains("assign", ignoreCase = true))
        assertFalse(nfc.label.contains("link", ignoreCase = true))
        assertFalse(nfc.label.contains("write", ignoreCase = true))
    }

    @Test
    fun checklistStartsNeutralAndDoesNotClaimAutomatedDiagnostics() {
        val bannedClaims = listOf("automatic repair", "certified", "guaranteed health", "diagnosed automatically")
        DeviceCategory.entries.flatMap(::checklistFor).forEach { check ->
            assertEquals(TestResult.NOT_TESTED, check.result)
            assertEquals("", check.notes)
            bannedClaims.forEach { claim ->
                assertFalse(check.label.contains(claim, ignoreCase = true))
            }
        }
    }
}
