package com.buysloans.hub

data class ConsolePriceEntry(
    val name: String,
    val rrp: Double
)

data class ConsoleDeviceEntry(
    val family: String,
    val series: String,
    val name: String,
    val priceSheetValue: Double? = null
) {
    val hasPrice: Boolean get() = priceSheetValue != null
}

object ConsolePricingCatalog {
    // Existing Morley price-sheet rows remain authoritative.
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

    private val priceByName = entries.associateBy { it.name.lowercase() }

    /**
     * Full console/device catalogue. New rows are deliberately unpriced; prices can be supplied later
     * without replacing existing Morley price-sheet records. Ordering is newest series first.
     */
    private val catalogueSeed = listOf(
        // PlayStation
        Triple("PlayStation", "PS5", "Sony PS5 Pro"),
        Triple("PlayStation", "PS5", "Sony PS5 Slim Disc"),
        Triple("PlayStation", "PS5", "Sony PS5 Digital Slim"),
        Triple("PlayStation", "PS5", "Sony PS5 Disc"),
        Triple("PlayStation", "PS5", "Sony PS5 Digital"),
        Triple("PlayStation", "PS4", "Sony PS4 Pro"),
        Triple("PlayStation", "PS4", "Sony PS4 Slim 1 TB"),
        Triple("PlayStation", "PS4", "Sony PS4 Slim 500 GB"),
        Triple("PlayStation", "PS4", "Sony PS4 OG 1 TB"),
        Triple("PlayStation", "PS4", "Sony PS4 OG 500 GB"),
        Triple("PlayStation", "PS3", "Sony PS3 Super Slim"),
        Triple("PlayStation", "PS3", "Sony PS3 Slim"),
        Triple("PlayStation", "PS3", "Sony PS3 Original"),
        Triple("PlayStation", "PS2", "Sony PS2 Slim"),
        Triple("PlayStation", "PS2", "Sony PlayStation 2"),
        Triple("PlayStation", "PS1", "Sony PS one"),
        Triple("PlayStation", "PS1", "Sony PlayStation"),
        Triple("PlayStation", "Portable", "Sony PlayStation Vita"),
        Triple("PlayStation", "Portable", "Sony PSP"),

        // Xbox
        Triple("Xbox", "Series", "Xbox Series X 2 TB"),
        Triple("Xbox", "Series", "Xbox Series X 1 TB"),
        Triple("Xbox", "Series", "Xbox Series X 1 TB All-Digital"),
        Triple("Xbox", "Series", "Xbox Series S 1 TB"),
        Triple("Xbox", "Series", "Xbox Series S 512 GB"),
        Triple("Xbox", "One", "Xbox One X"),
        Triple("Xbox", "One", "Xbox One S"),
        Triple("Xbox", "One", "Xbox One (brick)"),
        Triple("Xbox", "360", "Xbox 360 E"),
        Triple("Xbox", "360", "Xbox 360 S"),
        Triple("Xbox", "360", "Xbox 360"),
        Triple("Xbox", "Original", "Original Xbox"),

        // Nintendo current/home systems
        Triple("Nintendo", "Switch 2", "Nintendo Switch 2"),
        Triple("Nintendo", "Switch", "Nintendo Switch OLED"),
        Triple("Nintendo", "Switch", "Nintendo Switch"),
        Triple("Nintendo", "Switch", "Nintendo Switch Lite"),
        Triple("Nintendo", "Wii U", "Nintendo Wii U"),
        Triple("Nintendo", "Wii", "Nintendo Wii Family Edition"),
        Triple("Nintendo", "Wii", "Nintendo Wii"),
        Triple("Nintendo", "GameCube", "Nintendo GameCube"),
        Triple("Nintendo", "Nintendo 64", "Nintendo 64"),
        Triple("Nintendo", "SNES", "Super Nintendo Entertainment System"),
        Triple("Nintendo", "NES", "Nintendo Entertainment System"),

        // Nintendo 3DS family
        Triple("Nintendo", "3DS", "New Nintendo 2DS XL"),
        Triple("Nintendo", "3DS", "New Nintendo 3DS XL"),
        Triple("Nintendo", "3DS", "New Nintendo 3DS"),
        Triple("Nintendo", "3DS", "Nintendo 2DS"),
        Triple("Nintendo", "3DS", "Nintendo 3DS XL"),
        Triple("Nintendo", "3DS", "Nintendo 3DS"),

        // Nintendo DS family
        Triple("Nintendo", "DS", "Nintendo DSi XL"),
        Triple("Nintendo", "DS", "Nintendo DSi"),
        Triple("Nintendo", "DS", "Nintendo DS Lite"),
        Triple("Nintendo", "DS", "Nintendo DS"),

        // Game Boy family
        Triple("Nintendo", "Game Boy", "Game Boy Micro"),
        Triple("Nintendo", "Game Boy", "Game Boy Advance SP"),
        Triple("Nintendo", "Game Boy", "Game Boy Advance"),
        Triple("Nintendo", "Game Boy", "Game Boy Color"),
        Triple("Nintendo", "Game Boy", "Game Boy Pocket"),
        Triple("Nintendo", "Game Boy", "Game Boy"),

        // Sega
        Triple("Sega", "Dreamcast", "Sega Dreamcast"),
        Triple("Sega", "Saturn", "Sega Saturn"),
        Triple("Sega", "Mega Drive", "Sega Mega Drive / Genesis"),
        Triple("Sega", "Game Gear", "Sega Game Gear")
    )

    val catalogue: List<ConsoleDeviceEntry> = catalogueSeed.map { (family, series, name) ->
        ConsoleDeviceEntry(family, series, name, priceByName[name.lowercase()]?.rrp)
    }

    fun families(): List<String> = catalogue.map { it.family }.distinct()

    fun series(family: String): List<String> = catalogue
        .filter { it.family == family }
        .map { it.series }
        .distinct()

    fun devices(family: String, series: String): List<ConsoleDeviceEntry> = catalogue
        .filter { it.family == family && it.series == series }

    fun search(query: String): List<ConsoleDeviceEntry> {
        val tokens = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return catalogue
        return catalogue.filter { entry ->
            val haystack = "${entry.family} ${entry.series} ${entry.name}".lowercase()
            tokens.all(haystack::contains)
        }
    }

    val grades = listOf("A", "B", "C")

    /** Standard Morley grade buy percentages supplied for general pricing. */
    val gradeBuyPercent = mapOf(
        "A" to 0.70,
        "B" to 0.50,
        "C" to 0.30
    )

    fun buyPrice(entry: ConsolePriceEntry, grade: String): Double =
        entry.rrp * (gradeBuyPercent[grade] ?: error("Unsupported grade: $grade"))

    fun buyPrice(entry: ConsoleDeviceEntry, grade: String): Double? =
        entry.priceSheetValue?.times(gradeBuyPercent[grade] ?: error("Unsupported grade: $grade"))
}
