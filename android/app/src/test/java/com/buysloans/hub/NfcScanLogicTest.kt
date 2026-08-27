package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NfcScanLogicTest {
    @Test
    fun reportsHardwareUnavailableAndDisabledStates() {
        assertEquals(NfcCapability.UNAVAILABLE, NfcScanLogic.capability(hasAdapter = false, enabled = false))
        assertEquals(NfcCapability.DISABLED, NfcScanLogic.capability(hasAdapter = true, enabled = false))
        assertEquals(NfcCapability.READY, NfcScanLogic.capability(hasAdapter = true, enabled = true))
    }

    @Test
    fun parsesNdefTextPayload() {
        val payload = byteArrayOf(2) + "enHello Morley".toByteArray(Charsets.UTF_8)
        val parsed = NfcScanLogic.parseWellKnown("T".toByteArray(Charsets.US_ASCII), payload)
        assertEquals("Text", parsed?.kind)
        assertEquals("Hello Morley", parsed?.value)
    }

    @Test
    fun parsesNdefUriPayload() {
        val payload = byteArrayOf(4) + "example.com/item/123".toByteArray(Charsets.UTF_8)
        val parsed = NfcScanLogic.parseWellKnown("U".toByteArray(Charsets.US_ASCII), payload)
        assertEquals("URI", parsed?.kind)
        assertEquals("https://example.com/item/123", parsed?.value)
    }

    @Test
    fun suppressesAccidentalDuplicateReadsInsideDebounceWindow() {
        assertFalse(NfcScanLogic.shouldAccept("A1B2", 2_000L, "A1B2", 1_200L, 1_500L))
        assertTrue(NfcScanLogic.shouldAccept("A1B2", 3_000L, "A1B2", 1_200L, 1_500L))
        assertTrue(NfcScanLogic.shouldAccept("C3D4", 2_000L, "A1B2", 1_900L, 1_500L))
    }

    @Test
    fun formatsTagIdentifierWithoutSignedByteArtifacts() {
        assertEquals("00FF10", NfcScanLogic.tagIdHex(byteArrayOf(0x00, 0xFF.toByte(), 0x10)))
    }
}
