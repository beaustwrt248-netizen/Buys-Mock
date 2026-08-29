package com.buysloans.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TurnstileBridgeTest {
    private val immediatePoster: ((() -> Unit) -> Unit) = { action -> action() }

    @Test
    fun tokenCallbackIsDeliveredExactlyOnce() {
        val tokens = mutableListOf<String>()
        val failures = mutableListOf<String>()
        val bridge = TurnstileBridge(tokens::add, failures::add, immediatePoster)

        bridge.onToken("captcha-token")

        assertEquals(listOf("captcha-token"), tokens)
        assertTrue(failures.isEmpty())
    }

    @Test
    fun blankTokenIsIgnored() {
        val tokens = mutableListOf<String>()
        val bridge = TurnstileBridge(tokens::add, {}, immediatePoster)

        bridge.onToken("   ")

        assertTrue(tokens.isEmpty())
    }

    @Test
    fun expiredChallengeIsForwardedAsFailure() {
        val failures = mutableListOf<String>()
        val bridge = TurnstileBridge({}, failures::add, immediatePoster)

        bridge.onExpired("")

        assertEquals(listOf("Security check expired. Complete it again."), failures)
    }

    @Test
    fun turnstileErrorCodeIsForwardedWithoutExposingToken() {
        val failures = mutableListOf<String>()
        val bridge = TurnstileBridge({}, failures::add, immediatePoster)

        bridge.onError("110200")

        assertEquals(listOf("Security check failed (110200). Retry the challenge."), failures)
    }
}
