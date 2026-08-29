package com.buysloans.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminUpdateManagerTest {
    @Test fun parsesReleaseMetadata() {
        val release = AdminUpdateManager.parseRelease(
            """{"versionCode":6,"versionName":"0.1.5","apkUrl":"https://example.test/admin.apk","sha256":"${"b".repeat(64)}","required":true}"""
        )
        assertEquals(6, release.versionCode)
        assertEquals("0.1.5", release.versionName)
        assertEquals("https://example.test/admin.apk", release.apkUrl)
        assertEquals("b".repeat(64), release.sha256)
        assertTrue(release.required)
    }

    @Test fun defaultsRequiredToFalse() {
        val release = AdminUpdateManager.parseRelease(
            """{"versionCode":6,"versionName":"0.1.5","apkUrl":"https://example.test/admin.apk","sha256":"${"c".repeat(64)}"}"""
        )
        assertFalse(release.required)
    }
}
