package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCatalogueImageResolutionTest {
    @Test
    fun modelNumberCandidatesSplitKnownVariantSeparators() {
        assertEquals(
            listOf("I006D", "ZS590KS", "AI2401"),
            catalogueModelNumberCandidates("I006D / ZS590KS; AI2401"),
        )
        assertEquals(
            listOf("MacBookPro18,3", "MacBookPro18,4"),
            catalogueModelNumberCandidates("MacBookPro18,3 | MacBookPro18,4"),
        )
    }

    @Test
    fun modelNumberCandidatesDiscardWeakTokensAndDuplicates() {
        assertEquals(
            listOf("CFI-2102A", "CFI-2102B"),
            catalogueModelNumberCandidates("x / CFI-2102A / cfi-2102a / CFI-2102B"),
        )
    }

    @Test
    fun pageTitleCanMatchAnyExactHardwareIdentifier() {
        val html = """
            <html><head><title>ASUS Zenfone 8 (ZS590KS) Support</title></head></html>
        """.trimIndent()

        assertTrue(cataloguePageTitleMatchesDevice(html, "Zenfone 8", "I006D / ZS590KS"))
        assertFalse(cataloguePageTitleMatchesDevice(html, "ROG Phone 8", "AI2401 / AI2401_A"))
    }

    @Test
    fun nearbyImageCanResolveUsingOneCandidateFromMultiIdentifierField() {
        val html = """
            <section>
                <h2>Zenfone 8</h2>
                <img src="/images/zenfone8-front.webp" alt="Zenfone 8" />
                <p>Hardware model ZS590KS</p>
            </section>
        """.trimIndent()

        assertEquals(
            "/images/zenfone8-front.webp",
            catalogueNearbyImageForExactModel(html, "Zenfone 8", "I006D / ZS590KS"),
        )
    }

    @Test
    fun nearbyImageRejectsPlaceholderAndLoadingArtwork() {
        val html = """
            <section>
                <h2>ROG Phone 8</h2>
                <img src="/assets/loading-placeholder.png" />
                <p>AI2401</p>
            </section>
        """.trimIndent()

        assertEquals(null, catalogueNearbyImageForExactModel(html, "ROG Phone 8", "AI2401"))
    }
}
