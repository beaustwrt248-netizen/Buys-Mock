package com.buysloans.hub

import org.junit.Assert.assertEquals
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
    fun genericSupportPageTitleCannotAuthorizeGenericArtwork() {
        val html = """<html><head><title>Identify your iPhone model - Apple Support</title><meta property="og:image" content="support.jpg"></head><body>iPhone 17 Pro Max A3526</body></html>"""
        assertFalse(cataloguePageTitleMatchesDevice(html, "iPhone 17 Pro Max", "A3526"))
        assertEquals(null, catalogueNearbyImageForExactModel(html, "iPhone 17 Pro Max", "A3526"))
    }

    @Test
    fun exactModelImageCanBeRecoveredFromMultiDeviceSupportPage() {
        val html = """
            <html><head><title>Identify your iPhone model - Apple Support</title></head><body>
            <section><h2>iPhone 17 Pro Max</h2><img src="/assets/iphone-17-pro-max.png"><p>Model number: A3526</p></section>
            <section><h2>iPhone 17 Pro</h2><img src="/assets/iphone-17-pro.png"><p>Model number: A3523</p></section>
            </body></html>
        """.trimIndent()
        assertEquals(
            "/assets/iphone-17-pro-max.png",
            catalogueNearbyImageForExactModel(html, "iPhone 17 Pro Max", "A3526"),
        )
    }

    @Test
    fun nearbyImageMustMatchBothModelAndModelNumberWindow() {
        val html = """<section><h2>iPhone 16 Pro</h2><img src="iphone16.png"><p>A3293</p></section>"""
        assertEquals(null, catalogueNearbyImageForExactModel(html, "iPhone 17 Pro Max", "A3526"))
    }

    @Test
    fun differentModelPageIsRejected() {
        val html = """<html><head><title>iPhone 16 Pro - Apple (AU)</title></head></html>"""
        assertFalse(cataloguePageTitleMatchesDevice(html, "iPhone 17 Pro Max", "A3526"))
    }
}
