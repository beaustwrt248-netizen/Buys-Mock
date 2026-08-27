package com.buysloans.hub

import java.nio.charset.Charset

internal enum class NfcCapability {
    UNAVAILABLE,
    DISABLED,
    READY
}

internal data class NfcPayload(
    val kind: String,
    val value: String
)

internal object NfcScanLogic {
    fun capability(hasAdapter: Boolean, enabled: Boolean): NfcCapability = when {
        !hasAdapter -> NfcCapability.UNAVAILABLE
        !enabled -> NfcCapability.DISABLED
        else -> NfcCapability.READY
    }

    fun tagIdHex(bytes: ByteArray?): String = bytes
        ?.joinToString(separator = "") { "%02X".format(it.toInt() and 0xFF) }
        .orEmpty()

    fun shouldAccept(tagId: String, nowMs: Long, lastTagId: String?, lastReadMs: Long, debounceMs: Long = 1500L): Boolean {
        if (tagId.isBlank()) return false
        return tagId != lastTagId || nowMs - lastReadMs >= debounceMs
    }

    fun parseWellKnown(type: ByteArray, payload: ByteArray): NfcPayload? {
        if (payload.isEmpty()) return null
        val typeText = type.toString(Charsets.US_ASCII)
        return when (typeText) {
            "T" -> parseText(payload)
            "U" -> parseUri(payload)
            else -> null
        }
    }

    private fun parseText(payload: ByteArray): NfcPayload? {
        if (payload.isEmpty()) return null
        val status = payload[0].toInt() and 0xFF
        val utf16 = status and 0x80 != 0
        val languageLength = status and 0x3F
        val textStart = 1 + languageLength
        if (textStart > payload.size) return null
        val charset = if (utf16) Charsets.UTF_16 else Charsets.UTF_8
        val text = payload.copyOfRange(textStart, payload.size).toString(charset).trim()
        return text.takeIf { it.isNotEmpty() }?.let { NfcPayload("Text", it) }
    }

    private fun parseUri(payload: ByteArray): NfcPayload? {
        if (payload.isEmpty()) return null
        val prefixes = arrayOf(
            "", "http://www.", "https://www.", "http://", "https://", "tel:", "mailto:",
            "ftp://anonymous:anonymous@", "ftp://ftp.", "ftps://", "sftp://", "smb://", "nfs://",
            "ftp://", "dav://", "news:", "telnet://", "imap:", "rtsp://", "urn:", "pop:",
            "sip:", "sips:", "tftp:", "btspp://", "btl2cap://", "btgoep://", "tcpobex://",
            "irdaobex://", "file://", "urn:epc:id:", "urn:epc:tag:", "urn:epc:pat:",
            "urn:epc:raw:", "urn:epc:", "urn:nfc:"
        )
        val prefixIndex = payload[0].toInt() and 0xFF
        val prefix = prefixes.getOrElse(prefixIndex) { "" }
        val suffix = payload.copyOfRange(1, payload.size).toString(Charsets.UTF_8).trim()
        val uri = (prefix + suffix).trim()
        return uri.takeIf { it.isNotEmpty() }?.let { NfcPayload("URI", it) }
    }
}
