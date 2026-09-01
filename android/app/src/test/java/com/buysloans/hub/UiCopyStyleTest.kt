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

    @Test fun pricingSellerAskCopyRemainsCanonical() {
        val pricing = sourceFile("MainActivity.kt")
        assertFalse("Legacy Seller Ask wording returned", pricing.contains("Seller asking price"))
        assertTrue("Canonical Seller Ask wording is missing", pricing.contains("Field(\"Seller Ask\""))
    }

    @Test fun gpGradeButtonsRemainReadableInBothStates() {
        val pricing = sourceFile("MainActivity.kt")
        assertFalse(
            "Legacy GP grade contrast returned",
            pricing.contains("contentColor=if(grade==g)Color(0xFF06251B) else Color.White")
        )
        assertTrue(
            "Accessible GP grade contrast mapping is missing",
            pricing.contains("contentColor=if(grade==g)Color.White else MorleyTextPrimary")
        )
    }

    @Test fun gpLuxuryControlIsSingleLineResponsive() {
        val gp = sourceFile("GPFix.kt")
        assertTrue("Luxury grade needs a wider responsive weight", gp.contains("if (option == \"Luxury\") 1.35f else 1f"))
        assertTrue("Luxury grade label must remain single line", gp.contains("maxLines = 1"))
    }

    @Test fun smartWorkspacePrimaryActionsRemainReadableAndSellerAskCanonical() {
        val workspace = sourceFile("SmartWorkspaceSection.kt")
        val legacyContrast = "ButtonDefaults.buttonColors(containerColor = SWStrong, contentColor = MorleyTextPrimary)"
        val accessibleContrast = "ButtonDefaults.buttonColors(containerColor = SWStrong, contentColor = androidx.compose.ui.graphics.Color.White)"
        assertFalse("Low-contrast Smart Workspace primary action returned", workspace.contains(legacyContrast))
        assertTrue(
            "Both Smart Workspace primary actions must use white on strong emerald",
            workspace.split(accessibleContrast).size - 1 == 2
        )
        assertFalse("Legacy Quick Deal Seller Ask wording returned", workspace.contains("Seller asking price"))
        assertTrue("Canonical Quick Deal Seller Ask label is missing", workspace.contains("label = { Text(\"Seller Ask\") }"))
    }

    @Test fun testBuyChecklistChipLabelsRemainReadableInBothStates() {
        val testBuy = sourceFile("TestBuyActivity.kt")
        assertFalse(
            "Low-contrast Test & Buy unselected chip label returned",
            testBuy.contains("labelColor = Color.White.copy(alpha = .86f)")
        )
        assertFalse(
            "Low-contrast Test & Buy selected chip label returned",
            testBuy.contains("selectedLabelColor = Color.White")
        )
        assertTrue(
            "Test & Buy unselected chip label must use canonical dark text",
            testBuy.contains("labelColor = Color(0xFF1C2B26)")
        )
        assertTrue(
            "Test & Buy selected chip label must use canonical dark text",
            testBuy.contains("selectedLabelColor = Color(0xFF1C2B26)")
        )
    }

    @Test fun menuPrimaryActionsRemainReadableOnStrongEmerald() {
        val menu = sourceFile("MenuFeatureActivity.kt")
        val lowContrast = "ButtonDefaults.buttonColors(containerColor = MorleyAccentStrong, contentColor = MorleyTextPrimary)"
        val accessibleContrast = "ButtonDefaults.buttonColors(containerColor = MorleyAccentStrong, contentColor = Color.White)"
        assertFalse("Low-contrast Menu primary action returned", menu.contains(lowContrast))
        assertTrue(
            "Inventory and scanner primary actions must use white on strong emerald",
            menu.split(accessibleContrast).size - 1 >= 2
        )
    }

    @Test fun notificationBodiesRemainReadableOnLightCards() {
        val notifications = sourceFile("NotificationCentreActivity.kt")
        assertFalse(
            "Low-contrast white Notification Centre body text returned",
            notifications.contains("if (item.body.isNotBlank()) Text(item.body, color = Color.White)")
        )
        assertTrue(
            "Notification Centre body text must use canonical dark text",
            notifications.contains("if (item.body.isNotBlank()) Text(item.body, color = MorleyTextPrimary)")
        )
    }

    @Test fun updateVersionLabelRemainsReadableOnLightBackground() {
        val update = sourceFile("UpdateActivity.kt")
        assertFalse(
            "Legacy cyan Update Centre version label returned",
            update.contains("color=Color(0xFF70DFFF)")
        )
        assertTrue(
            "Update Centre version label must use canonical strong emerald",
            update.contains("color=MorleyAccentStrong")
        )
    }
}
