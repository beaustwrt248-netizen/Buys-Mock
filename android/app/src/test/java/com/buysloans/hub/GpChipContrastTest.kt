package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GpChipContrastTest {
    private fun gpSource(): String {
        val roots = listOf(
            File("src/main/java"),
            File("app/src/main/java"),
            File("android/app/src/main/java")
        )
        val root = roots.firstOrNull { it.exists() }
        assertNotNull("Could not locate Morley Android source", root)
        val file = root!!.walkTopDown().firstOrNull { it.isFile && it.name == "GPFix.kt" }
        assertNotNull("Could not locate GPFix.kt", file)
        return file!!.readText()
    }

    // FVP-014: selected GP chips must remain readable on strong emerald.
    @Test fun selectedGpChipUsesWhiteLabelOnStrongEmerald() {
        val gp = gpSource()
        assertFalse(
            "Low-contrast selected GP chip label returned",
            gp.contains("selectedLabelColor = MorleyTextPrimary")
        )
        assertTrue(
            "Selected GP chip must use white on strong emerald",
            gp.contains("selectedLabelColor = androidx.compose.ui.graphics.Color.White")
        )
        assertTrue(
            "Selected GP chip strong emerald container must remain unchanged",
            gp.contains("selectedContainerColor = MorleyAccentStrong")
        )
    }
}
