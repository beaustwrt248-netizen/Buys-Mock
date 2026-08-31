package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
}
