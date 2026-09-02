package com.buysloans.admin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminWebParityPolicyTest {
    @Test fun canonicalAdminPagesStayInsideApp() {
        assertTrue(AdminWebParityPolicy.isTrustedAdminUrl("https://buyshub.me/admin/"))
        assertTrue(AdminWebParityPolicy.isTrustedAdminUrl("https://buyshub.me/admin/guardian.html"))
        assertTrue(AdminWebParityPolicy.isTrustedAdminUrl("https://buyshub.me/admin/index.html?tab=tickets"))
    }

    @Test fun lookalikeHostsAndNonAdminPathsAreRejected() {
        assertFalse(AdminWebParityPolicy.isTrustedAdminUrl("https://evil.example/admin/"))
        assertFalse(AdminWebParityPolicy.isTrustedAdminUrl("https://buyshub.me/"))
        assertFalse(AdminWebParityPolicy.isTrustedAdminUrl("http://buyshub.me/admin/"))
        assertFalse(AdminWebParityPolicy.isTrustedAdminUrl("https://buyshub.me/admin-evil/"))
    }

    @Test fun externalSchemesAreExplicitlyBounded() {
        assertTrue(AdminWebParityPolicy.isExternallyRoutableScheme("https"))
        assertTrue(AdminWebParityPolicy.isExternallyRoutableScheme("sms"))
        assertFalse(AdminWebParityPolicy.isExternallyRoutableScheme("javascript"))
        assertFalse(AdminWebParityPolicy.isExternallyRoutableScheme("file"))
    }
}
