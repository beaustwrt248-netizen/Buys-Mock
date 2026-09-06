package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AllDeviceCatalogueVisualContractTest {
    private val source = File("src/main/java/com/buysloans/hub/LiveDeviceCatalogueBrowser.kt").readText()

    @Test
    fun allDeviceCatalogueUsesDeviceAwareVisuals() {
        assertTrue(source.contains("CatalogueDevicePhoto(device)"))
        assertTrue(source.contains("MobilePhonePhoto("))
        assertTrue(source.contains("PricingCategoryVisual("))
    }

    @Test
    fun allDeviceCatalogueDoesNotFallBackToBrandInitials() {
        assertFalse(source.contains("device.brand.take(2).uppercase()"))
    }
}
