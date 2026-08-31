package com.buysloans.hub

data class ConsolePriceEntry(
    val name: String,
    val rrp: Double
)

object ConsolePricingCatalog {
    val entries = listOf(
        ConsolePriceEntry("Sony PS4 OG 500 GB", 149.0),
        ConsolePriceEntry("Sony PS4 OG 1 TB", 189.0),
        ConsolePriceEntry("Sony PS4 Slim 500 GB", 229.0),
        ConsolePriceEntry("Sony PS4 Slim 1 TB", 269.0),
        ConsolePriceEntry("Sony PS4 Pro", 249.0),
        ConsolePriceEntry("Sony PS5 Digital", 649.0),
        ConsolePriceEntry("Sony PS5 Disc", 699.0),
        ConsolePriceEntry("Sony PS5 Digital Slim", 799.0),
        ConsolePriceEntry("Sony PS5 Slim Disc", 849.0),
        ConsolePriceEntry("Sony PS5 Pro", 1199.0),
        ConsolePriceEntry("Xbox One (brick)", 99.0),
        ConsolePriceEntry("Xbox One S", 179.0),
        ConsolePriceEntry("Xbox One X", 249.0),
        ConsolePriceEntry("Xbox Series S", 329.0),
        ConsolePriceEntry("Xbox Series X", 549.0),
        ConsolePriceEntry("Nintendo Switch Lite", 149.0),
        ConsolePriceEntry("Nintendo Switch", 249.0),
        ConsolePriceEntry("Nintendo Switch OLED", 349.0),
        ConsolePriceEntry("Nintendo Switch 2", 599.0)
    )

    val grades = listOf("A", "B", "C")

    /** Standard Morley grade buy percentages supplied for general pricing. */
    val gradeBuyPercent = mapOf(
        "A" to 0.70,
        "B" to 0.50,
        "C" to 0.30
    )

    fun buyPrice(entry: ConsolePriceEntry, grade: String): Double =
        entry.rrp * (gradeBuyPercent[grade] ?: error("Unsupported grade: $grade"))
}
