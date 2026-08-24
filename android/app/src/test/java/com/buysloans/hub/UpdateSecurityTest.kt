package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateSecurityTest {
    @Test fun trustedReleaseUrlIsAccepted() {
        assertTrue(UpdateManager.isTrustedApkUrl("https://github.com/beaustwrt248-netizen/Buys-Mock/releases/download/v2.7.0-cyber/B-and-L-Morley-2.7.0-cyber.apk"))
    }

    @Test fun foreignOrInsecureReleaseUrlsAreRejected() {
        assertFalse(UpdateManager.isTrustedApkUrl("http://github.com/beaustwrt248-netizen/Buys-Mock/releases/download/v1/app.apk"))
        assertFalse(UpdateManager.isTrustedApkUrl("https://github.com/other-owner/Buys-Mock/releases/download/v1/app.apk"))
        assertFalse(UpdateManager.isTrustedApkUrl("https://example.com/B-and-L-Morley.apk"))
    }

    @Test fun sha256MustBeExactly64HexCharacters() {
        assertTrue(UpdateManager.isValidSha256("a".repeat(64)))
        assertTrue(UpdateManager.isValidSha256("ABCDEF0123456789".repeat(4)))
        assertFalse(UpdateManager.isValidSha256("a".repeat(63)))
        assertFalse(UpdateManager.isValidSha256("g".repeat(64)))
        assertFalse(UpdateManager.isValidSha256(""))
    }
}
