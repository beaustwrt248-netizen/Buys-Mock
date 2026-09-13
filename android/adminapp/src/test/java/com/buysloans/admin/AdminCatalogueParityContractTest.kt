package com.buysloans.admin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AdminCatalogueParityContractTest {
    private val dashboardSource = File("src/main/java/com/buysloans/admin/AdminNativeDashboard.kt").readText()
    private val apiSource = File("src/main/java/com/buysloans/admin/AdminApi.kt").readText()

    @Test
    fun nativeAdminExposesReadOnlyCatalogueWorkspace() {
        assertTrue(dashboardSource.contains("WORKSPACE_CATALOGUE"))
        assertTrue(dashboardSource.contains("Catalogue"))
        assertTrue(dashboardSource.contains("CatalogueNativePanel"))
        assertTrue(apiSource.contains("loadCatalogue"))
        assertTrue(apiSource.contains("/rest/v1/device_catalog?"))
        assertTrue(apiSource.contains("active=eq.true"))
        assertFalse(apiSource.contains("updateCatalogue"))
        assertFalse(apiSource.contains("deleteCatalogue"))
    }
}
