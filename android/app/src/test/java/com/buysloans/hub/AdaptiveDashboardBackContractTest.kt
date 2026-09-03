package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AdaptiveDashboardBackContractTest {
    @Test
    fun rootBackIsLeftToTheSystemAndInAppStateIsHandled() {
        val source = File("src/main/java/com/buysloans/hub/DashboardActivity.kt").readText()
        assertTrue(source.contains("enabled = showMenu || page != Page.Home"))
        assertTrue(source.contains("if (showMenu) closeMenu() else page = Page.Home"))
        assertFalse(source.contains("override fun onBackPressed"))
        assertFalse(source.contains("KEYCODE_BACK"))
    }
}
