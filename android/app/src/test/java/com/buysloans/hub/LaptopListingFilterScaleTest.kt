package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LaptopListingFilterScaleTest {
    private val brands = listOf(
        "Apple", "Lenovo", "Dell", "HP", "Acer", "ASUS", "MSI", "Alienware", "Gigabyte",
        "Razer", "Microsoft", "Samsung", "LG", "Huawei", "Toshiba", "Dynabook", "Fujitsu",
        "Panasonic", "Framework", "Medion", "Clevo", "Chuwi", "Gateway", "VAIO", "Honor",
        "Xiaomi", "Realme", "Infinix", "Tecno", "Google"
    )

    private val models = listOf(
        "A1932", "A2337", "T14 Gen 3 21AH", "X1 Carbon 21HM", "Latitude 5420",
        "XPS 9310", "EliteBook 840 G8", "ProBook 450 G9", "UX3402", "FA507",
        "AN515", "SF314", "GF63", "GE76", "NP950X", "16Z90R", "MateBook D14",
        "CF-54", "Lifebook U7412", "Framework 13"
    )

    private val contamination = listOf(
        "data recovery service", "computer repair service", "screen replacement service",
        "battery replacement service", "keyboard replacement service", "logic board repair",
        "diagnostic service", "virus removal service", "software installation service",
        "ssd upgrade service", "laptop rental service", "we buy laptops",
        "replacement screen", "motherboard only", "charger only", "docking station",
        "laptop bag", "parts only", "empty box", "recovery media"
    )

    @Test fun `thousands of contaminated laptop listings are hard rejected`() {
        var checked = 0
        for (brand in brands) {
            for (model in models) {
                for (bad in contamination) {
                    val result = LaptopListingFilter.decision("$brand $model $bad")
                    assertTrue("Expected reject: $brand $model $bad", result.rejected)
                    checked++
                }
            }
        }
        assertTrue("Regression matrix must stay above 10000 cases", checked >= 10_000)
    }

    @Test fun `ordinary whole laptop sale wording is not rejected`() {
        val saleSuffixes = listOf(
            "laptop used good condition", "notebook 16GB 512GB SSD", "with charger",
            "fresh Windows install", "recently serviced laptop", "battery replaced last year"
        )
        var checked = 0
        for (brand in brands.take(20)) {
            for (model in models.take(12)) {
                for (suffix in saleSuffixes) {
                    val result = LaptopListingFilter.decision("$brand $model $suffix")
                    assertFalse("False positive: $brand $model $suffix -> ${result.reason}", result.rejected)
                    checked++
                }
            }
        }
        assertTrue(checked >= 1_000)
    }

    @Test fun `known cross model identifiers are incompatible`() {
        val pairs = listOf(
            "Apple MacBook Air A1932" to "Apple MacBook Air A1466",
            "Apple MacBook Air A1932" to "Apple MacBook Air A2337 M1",
            "Lenovo ThinkPad T14 21AH" to "Lenovo ThinkPad T14 21AK",
            "ASUS Zenbook UX3402" to "ASUS Zenbook UX5401",
            "Samsung Galaxy Book NP950X" to "Samsung Galaxy Book NP960X"
        )
        pairs.forEach { (query, title) ->
            assertTrue("Expected identifier conflict: $query <> $title", LaptopListingFilter.conflictingIdentifier(query, title))
        }
    }

    @Test fun `service rows can never become exact valuation evidence`() {
        val cases = listOf(
            "Apple MacBook Air A1932 data recovery service",
            "Dell Latitude 5420 laptop repair service",
            "Lenovo ThinkPad T14 Gen 3 diagnostic service",
            "HP EliteBook 840 G8 screen replacement service",
            "ASUS Zenbook UX3402 SSD upgrade service"
        )
        cases.forEach { title ->
            val (exact, _, reasons) = classifyMarketplace(title.substringBefore(" service"), title)
            assertFalse(title, exact)
            assertTrue("$title -> $reasons", reasons.contains("Service listing") || reasons.contains("Part/accessory"))
        }
    }

    @Test fun `A1932 cannot value from wrong MacBook generations`() {
        val query = "Apple MacBook Air A1932 13 inch 2019 i5 256GB"
        val wrong = listOf(
            "Apple MacBook Air A1466 13 inch 2017 i5 256GB",
            "Apple MacBook Air A2179 13 inch 2020 i5 256GB",
            "Apple MacBook Air A2337 13 inch 2020 M1 256GB",
            "Apple MacBook Air A2681 13 inch 2022 M2 256GB",
            "Apple MacBook Air 11 inch 2015 i5 256GB"
        )
        wrong.forEach { title ->
            val (exact, _, _) = classifyMarketplace(query, title)
            assertFalse("Wrong generation became exact: $title", exact)
        }
    }
}
