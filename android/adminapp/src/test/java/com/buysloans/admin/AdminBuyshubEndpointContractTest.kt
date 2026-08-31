package com.buysloans.admin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AdminBuyshubEndpointContractTest {
    @Test
    fun turnstileUsesBuyshubHttpsOnly() {
        val source = File("src/main/java/com/buysloans/admin/CaptchaChallenge.kt").readText()
        assertTrue(source.contains("https://buyshub.me/admin/turnstile.html"))
        assertTrue(source.contains("private const val TURNSTILE_HOST = \"buyshub.me\""))
        assertTrue(source.contains("private const val TURNSTILE_PATH = \"/admin/turnstile.html\""))
        assertFalse(source.contains("http://buyshub.me/admin/turnstile.html"))
        assertFalse(source.contains("beaustwrt248-netizen.github.io/Buys-Mock/admin/turnstile.html"))
    }
}
