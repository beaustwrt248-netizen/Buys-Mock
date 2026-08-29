package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceTestChecklistCatalogTest {
    @Test
    fun `all supported categories expose non-empty unique checklist ids`() {
        DeviceTestCategory.values().forEach { category ->
            val items = DeviceTestChecklistCatalog.forCategory(category)
            assertTrue("$category should have checklist items", items.isNotEmpty())
            assertEquals(items.size, items.map { it.id }.toSet().size)
        }
    }

    @Test
    fun `phone checklist covers requested practical hardware areas`() {
        val ids = DeviceTestChecklistCatalog.forCategory(DeviceTestCategory.PHONE).map { it.id }.toSet()

        assertTrue(ids.containsAll(setOf(
            "battery",
            "display_touch",
            "wifi",
            "bluetooth",
            "speakers",
            "cameras",
            "charging_port",
            "storage"
        )))
    }

    @Test
    fun `computer and console checklists cover storage ports display and connectivity`() {
        listOf(DeviceTestCategory.LAPTOP, DeviceTestCategory.PC, DeviceTestCategory.CONSOLE).forEach { category ->
            val ids = DeviceTestChecklistCatalog.forCategory(category).map { it.id }.toSet()
            assertTrue("$category storage", ids.contains("storage"))
            assertTrue("$category ports", ids.contains("ports") || ids.contains("usb_ports"))
            assertTrue("$category display", ids.contains("display") || ids.contains("display_output"))
            assertTrue("$category wifi", ids.contains("wifi"))
            assertTrue("$category bluetooth", ids.contains("bluetooth"))
        }
    }

    @Test
    fun `catalog describes observation or reported values and never claims automated diagnostics`() {
        DeviceTestCategory.values()
            .flatMap(DeviceTestChecklistCatalog::forCategory)
            .forEach { item ->
                assertTrue(item.guidance.isNotBlank())
                assertFalse(item.guidance.contains("automatically diagnose", ignoreCase = true))
                assertFalse(item.guidance.contains("guarantee", ignoreCase = true))
                assertTrue(item.method in DeviceTestMethod.values())
            }
    }
}
