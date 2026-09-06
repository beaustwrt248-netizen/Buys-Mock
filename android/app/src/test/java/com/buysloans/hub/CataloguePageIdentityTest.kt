package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CataloguePageIdentityTest {
    @Test
    fun exactProductTitleIsAccepted() {
        val html = """<html><head><title>Apple iPhone 17 Pro Max - Apple (AU)</title><meta property="og:image" content="hero.jpg"></head></html>"""
        assertTrue(cataloguePageTitleMatchesDevice(html, "iPhone 17 Pro Max", "A3526"))
    }

    @Test
    fun exactModelNumberInTitleIsAccepted() {
        val html = """<html><head><meta property="og:title" content="Galaxy Book5 Pro NP940XHA-KG1AU | Samsung Australia"></head></html>"""
        assertTrue(cataloguePageTitleMatchesDevice(html, "Galaxy Book5 Pro", "NP940XHA-KG1AU"))
    }

    @Test
    fun genericSupportPageIsRejectedEvenWhenBodyMentionsDevice() {
        val html = """<html><head><title>Identify your iPhone model - Apple Support</title><meta property="og:image" content="support.jpg"></head><body>iPhone 17 Pro Max A3526</body></html>"""
        assertFalse(cataloguePageTitleMatchesDevice(html, "iPhone 17 Pro Max", "A3526"))
    }

    @Test
    fun differentModelPageIsRejected() {
        val html = """<html><head><title>iPhone 16 Pro - Apple (AU)</title></head></html>"""
        assertFalse(cataloguePageTitleMatchesDevice(html, "iPhone 17 Pro Max", "A3526"))
    }
}
