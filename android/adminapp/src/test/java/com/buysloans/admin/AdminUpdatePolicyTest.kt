package com.buysloans.admin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminUpdatePolicyTest {
    private val hash = "a".repeat(64)

    @Test fun newerValidReleaseIsOffered() {
        assertTrue(shouldOfferAdminUpdate(4, AdminUpdateRelease(5, "0.1.4", "https://example.test/admin.apk", hash)))
    }

    @Test fun sameOrOlderReleaseIsNotOffered() {
        assertFalse(shouldOfferAdminUpdate(5, AdminUpdateRelease(5, "0.1.4", "https://example.test/admin.apk", hash)))
        assertFalse(shouldOfferAdminUpdate(6, AdminUpdateRelease(5, "0.1.4", "https://example.test/admin.apk", hash)))
    }

    @Test fun insecureUrlOrInvalidDigestIsRejected() {
        assertFalse(shouldOfferAdminUpdate(4, AdminUpdateRelease(5, "0.1.4", "http://example.test/admin.apk", hash)))
        assertFalse(shouldOfferAdminUpdate(4, AdminUpdateRelease(5, "0.1.4", "https://example.test/admin.apk", "bad")))
    }
}
