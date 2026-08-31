package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

class UiCopyStyleTest {
    private fun sourceText(): String {
        val roots = listOf(
            File("src/main/java"),
            File("app/src/main/java"),
            File("android/app/src/main/java")
        )
        val root = roots.firstOrNull { it.exists() }
        assertNotNull("Could not locate Morley Android source for UI copy audit", root)
        return root!!.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { it.readText() }
    }

    @Test fun legacyBrandAndAuthenticationCasingDoNotReturn() {
        val source = sourceText()
        listOf(
            "Buys & Loans Hub",
            "Buys and Loans Calculator",
            "Back to Sign in",
            "Sign Up with Invite",
            "Forgot Password"
        ).forEach { legacy ->
            assertFalse("Legacy or inconsistently-cased UI copy returned: $legacy", source.contains(legacy))
        }
    }
}
