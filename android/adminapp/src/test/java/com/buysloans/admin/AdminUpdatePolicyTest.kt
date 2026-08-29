package com.buysloans.admin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminUpdatePolicyTest {
    private val hash = "a".repeat(64)
    private fun trusted(version: String = "0.1.5") =
        "https://github.com/beaustwrt248-netizen/Buys-Mock/releases/download/admin-v$version/Morley-Admin-$version.apk"

    @Test fun newerValidRepositoryReleaseIsOffered() {
        assertTrue(shouldOfferAdminUpdate(5, AdminUpdateRelease(6, "0.1.5", trusted(), hash)))
    }

    @Test fun sameOrOlderReleaseIsNotOffered() {
        assertFalse(shouldOfferAdminUpdate(6, AdminUpdateRelease(6, "0.1.5", trusted(), hash)))
        assertFalse(shouldOfferAdminUpdate(7, AdminUpdateRelease(6, "0.1.5", trusted(), hash)))
    }

    @Test fun untrustedUrlOrInvalidDigestIsRejected() {
        assertFalse(shouldOfferAdminUpdate(5, AdminUpdateRelease(6, "0.1.5", "https://example.test/admin.apk", hash)))
        assertFalse(shouldOfferAdminUpdate(5, AdminUpdateRelease(6, "0.1.5", trusted(), "bad")))
        assertFalse(shouldOfferAdminUpdate(5, AdminUpdateRelease(6, "", trusted(), hash)))
    }
}
