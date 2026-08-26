package com.buysloans.hub

import org.junit.Assert.assertEquals
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
