package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TurnstileHttpsContractTest {
    @Test
    fun morleyTurnstileUsesBuyshubHttpsOnly() {
        val source = File("src/main/java/com/buysloans/hub/AuthActivity.kt").readText()
        assertTrue(source.contains("private const val TURNSTILE_PAGE=\"https://buyshub.me/admin/turnstile.html\""))
        assertFalse(source.contains("http://buyshub.me/admin/turnstile.html"))
    }
}
