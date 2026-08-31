package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UiCopyStyleTest {
    private fun sourceRoot(): File {
        val roots = listOf(
            File("src/main/java"),
            File("app/src/main/java"),
            File("android/app/src/main/java")
        )
        val root = roots.firstOrNull { it.exists() }
        assertNotNull("Could not locate Morley Android source for UI copy audit", root)
        return root!!
    }

    private fun sourceFile(name: String): String {
        val file = sourceRoot().walkTopDown().firstOrNull { it.isFile && it.name == name }
        assertNotNull("Could not locate $name for UI copy audit", file)
        return file!!.readText()
    }

    @Test fun legacyBrandCopyDoesNotReturnToDashboard() {
        val dashboard = sourceFile("DashboardActivity.kt")
        listOf("Buys & Loans Hub", "Buys and Loans Calculator").forEach { legacy ->
            assertFalse("Legacy dashboard copy returned: $legacy", dashboard.contains(legacy))
        }
    }

    @Test fun authenticationActionsRemainSentenceCase() {
        val auth = sourceFile("AuthActivity.kt")
        listOf("Back to Sign in", "Sign Up with Invite", "Forgot Password").forEach { legacy ->
            assertFalse("Inconsistent authentication copy returned: $legacy", auth.contains(legacy))
        }
    }

    @Test fun consoleCapacityAndProductNamesRemainPolished() {
        val catalog = sourceFile("ConsolePricingCatalog.kt")
        listOf("500gb", "1tb", "Switch Oled").forEach { legacy ->
            assertFalse("Unpolished console label returned: $legacy", catalog.contains(legacy))
        }
        listOf("500 GB", "1 TB", "Switch OLED").forEach { expected ->
            assertTrue("Expected polished console label is missing: $expected", catalog.contains(expected))
        }
    }

    @Test fun polishedPricingScreensDoNotUseEmojiHeadings() {
        val pricing = sourceFile("ComputerConsolePricingScreens.kt")
        listOf("💻", "🖥", "🎮").forEach { emoji ->
            assertFalse("Pricing screen emoji returned: $emoji", pricing.contains(emoji))
        }
    }

    @Test fun gpLuxuryControlIsSingleLineResponsive() {
        val gp = sourceFile("GPFix.kt")
        assertTrue("Luxury grade needs a wider responsive weight", gp.contains("if (option == \"Luxury\") 1.35f else 1f"))
        assertTrue("Luxury grade label must remain single line", gp.contains("maxLines = 1"))
    }
}
