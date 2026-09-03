package com.buysloans.hub

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AdaptiveDashboardInsetsContractTest {
    @Test
    fun scaffoldConsumesInsetsAfterEdgeToEdgeIsEnabled() {
        val source = File("src/main/java/com/buysloans/hub/DashboardActivity.kt").readText()
        assertTrue(source.contains("enableEdgeToEdge()"))
        assertTrue(source.contains("consumeWindowInsets(pad)"))
        assertTrue(source.contains("Modifier.padding(pad)"))
    }
}
