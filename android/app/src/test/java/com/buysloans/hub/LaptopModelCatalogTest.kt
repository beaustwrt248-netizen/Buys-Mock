package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LaptopModelCatalogTest {
    @Test fun `A1932 uses canonical Apple MacBook Air query`() {
        val r = LaptopCatalogResolution(
            originalQuery = "A1932",
            canonicalQuery = "Apple MacBook Air A1932",
            brand = "Apple",
            family = "MacBook Air",
            modelName = "MacBook Air 13-inch 2018-2019",
            modelNumber = "A1932",
            score = 1.0
        )
        assertEquals("Apple MacBook Air A1932", LaptopModelCatalog.preferredQuery("A1932", r))
    }

    @Test fun `A1932 fallback preserves both valid years`() {
        val r = LaptopModelCatalog.knownIdentifierResolution(" A1932 ")
        assertNotNull(r)
        assertEquals("Apple", r?.brand)
        assertEquals("MacBook Air", r?.family)
        assertEquals("A1932", r?.modelNumber)
        assertEquals("Apple MacBook Air 13-inch A1932", r?.canonicalQuery)
        assertEquals(listOf(2018, 2019), r?.candidateYears)
        assertTrue(r?.requiresYearSelection == true)
    }

    @Test fun `A1932 2018 produces exact year query`() {
        val r = LaptopModelCatalog.knownIdentifierResolution("A1932")!!
        assertEquals(
            "Apple MacBook Air 13-inch 2018 A1932",
            LaptopModelCatalog.queryForYear(r, 2018)
        )
    }

    @Test fun `A1932 2019 produces exact year query`() {
        val r = LaptopModelCatalog.knownIdentifierResolution("A1932")!!
        assertEquals(
            "Apple MacBook Air 13-inch 2019 A1932",
            LaptopModelCatalog.queryForYear(r, 2019)
        )
    }

    @Test fun `single year candidate does not require disambiguation`() {
        val r = LaptopCatalogResolution(
            "A2337",
            "Apple MacBook Air M1 2020 A2337",
            "Apple",
            "MacBook Air",
            "MacBook Air M1",
            "A2337",
            1.0,
            listOf(LaptopCatalogCandidate("Apple", "MacBook Air", "MacBook Air M1", "A2337", 2020, 2020, 1.0))
        )
        assertEquals(listOf(2020), r.candidateYears)
        assertFalse(r.requiresYearSelection)
    }

    @Test fun `unknown model code is not invented locally`() {
        assertEquals(null, LaptopModelCatalog.knownIdentifierResolution("A9999"))
    }

    @Test fun `Apple silicon A-number uses resolved identity`() {
        val r = LaptopCatalogResolution("A2337", "Apple MacBook Air A2337", "Apple", "MacBook Air", "MacBook Air M1", "A2337", 1.0)
        assertEquals("Apple MacBook Air A2337", LaptopModelCatalog.preferredQuery("A2337", r))
    }

    @Test fun `Dell model number uses canonical family`() {
        val r = LaptopCatalogResolution("9310", "Dell XPS 13 9310", "Dell", "XPS", "XPS 13 9310", "9310", 0.95)
        assertEquals("Dell XPS 13 9310", LaptopModelCatalog.preferredQuery("9310", r))
    }

    @Test fun `ThinkPad generation code uses canonical family`() {
        val r = LaptopCatalogResolution("T14 Gen 3", "Lenovo ThinkPad T14 Gen 3", "Lenovo", "ThinkPad T14", "ThinkPad T14 Gen 3", "T14 Gen 3", 0.90)
        assertEquals("Lenovo ThinkPad T14 Gen 3", LaptopModelCatalog.preferredQuery("T14 Gen 3", r))
    }

    @Test fun `weak catalogue match never overrides user query`() {
        val r = LaptopCatalogResolution("16", "HP Omen 16", "HP", "Omen", "Omen 16", "16", 0.20)
        assertEquals("16", LaptopModelCatalog.preferredQuery("16", r))
    }
}
