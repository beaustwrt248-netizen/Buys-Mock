package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class MainAdminModeRemovalContractTest {
    @Test
    fun mainMorleyAppDoesNotExposeEmbeddedAdminMode() {
        val dashboard = File("src/main/java/com/buysloans/hub/DashboardActivity.kt").readText()
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertFalse(dashboard.contains("Admin mode"))
        assertFalse(dashboard.contains("EmbeddedAdminActivity"))
        assertFalse(manifest.contains("EmbeddedAdminActivity"))
    }
}
