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
    // Existing Morley price-sheet rows remain authoritative. Never replace these values with
    // catalogue-only rows: the expanded catalogue is deliberately allowed to be unpriced.
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

    private data class Seed(
        val family: String,
        val series: String,
        val name: String,
        val authoritativePriceName: String = name
    )

    /** Full catalogue in explicit newest-series-first order. */
    private val catalogueSeed = listOf(
        // PlayStation 5 family. Keep this exact variant order for fast counter lookup.
        Seed("PlayStation", "PS5", "Sony PS5 Pro"),
        Seed("PlayStation", "PS5", "Sony PS5 Slim Disc"),
        Seed("PlayStation", "PS5", "Sony PS5 Slim Digital", "Sony PS5 Digital Slim"),
        Seed("PlayStation", "PS5", "Sony PS5 Disc"),
        Seed("PlayStation", "PS5", "Sony PS5 Digital"),
        Seed("PlayStation", "PS4", "Sony PS4 Pro"),
        Seed("PlayStation", "PS4", "Sony PS4 Slim 1 TB"),
        Seed("PlayStation", "PS4", "Sony PS4 Slim 500 GB"),
        Seed("PlayStation", "PS4", "Sony PS4 OG 1 TB"),
        Seed("PlayStation", "PS4", "Sony PS4 OG 500 GB"),
        Seed("PlayStation", "PS3", "Sony PS3 Super Slim"),
        Seed("PlayStation", "PS3", "Sony PS3 Slim"),
        Seed("PlayStation", "PS3", "Sony PS3 Original"),
        Seed("PlayStation", "PS2", "Sony PS2 Slim"),
        Seed("PlayStation", "PS2", "Sony PlayStation 2"),
        Seed("PlayStation", "PS1", "Sony PS one"),
        Seed("PlayStation", "PS1", "Sony PlayStation"),
        Seed("PlayStation Handheld", "Vita", "Sony PlayStation Vita Slim"),
        Seed("PlayStation Handheld", "Vita", "Sony PlayStation Vita OLED"),
        Seed("PlayStation Handheld", "PSP", "Sony PSP Go"),
        Seed("PlayStation Handheld", "PSP", "Sony PSP-3000"),
        Seed("PlayStation Handheld", "PSP", "Sony PSP-2000"),
        Seed("PlayStation Handheld", "PSP", "Sony PSP-1000"),

        // Xbox
        Seed("Xbox", "Series X|S", "Xbox Series X 2 TB"),
        Seed("Xbox", "Series X|S", "Xbox Series X 1 TB", "Xbox Series X"),
        Seed("Xbox", "Series X|S", "Xbox Series X 1 TB All-Digital"),
        Seed("Xbox", "Series X|S", "Xbox Series S 1 TB"),
        Seed("Xbox", "Series X|S", "Xbox Series S 512 GB", "Xbox Series S"),
        Seed("Xbox", "Xbox One", "Xbox One X"),
        Seed("Xbox", "Xbox One", "Xbox One S"),
        Seed("Xbox", "Xbox One", "Xbox One (brick)"),
        Seed("Xbox", "Xbox 360", "Xbox 360 E"),
        Seed("Xbox", "Xbox 360", "Xbox 360 S"),
        Seed("Xbox", "Xbox 360", "Xbox 360 Elite"),
        Seed("Xbox", "Xbox 360", "Xbox 360"),
        Seed("Xbox", "Original Xbox", "Original Xbox"),

        // Nintendo hybrid/home consoles
        Seed("Nintendo", "Switch 2", "Nintendo Switch 2"),
        Seed("Nintendo", "Switch", "Nintendo Switch OLED"),
        Seed("Nintendo", "Switch", "Nintendo Switch", "Nintendo Switch"),
        Seed("Nintendo", "Switch", "Nintendo Switch Lite"),
        Seed("Nintendo", "Wii U", "Nintendo Wii U 32 GB"),
        Seed("Nintendo", "Wii U", "Nintendo Wii U 8 GB"),
        Seed("Nintendo", "Wii", "Nintendo Wii Family Edition"),
        Seed("Nintendo", "Wii", "Nintendo Wii"),
        Seed("Nintendo", "GameCube", "Nintendo GameCube"),
        Seed("Nintendo", "Nintendo 64", "Nintendo 64"),
        Seed("Nintendo", "SNES", "Super Nintendo Entertainment System"),
        Seed("Nintendo", "NES", "Nintendo Entertainment System"),

        // Nintendo 3DS family
        Seed("Nintendo 3DS", "New 2DS", "New Nintendo 2DS XL"),
        Seed("Nintendo 3DS", "New 3DS", "New Nintendo 3DS XL"),
        Seed("Nintendo 3DS", "New 3DS", "New Nintendo 3DS"),
        Seed("Nintendo 3DS", "2DS", "Nintendo 2DS"),
        Seed("Nintendo 3DS", "3DS", "Nintendo 3DS XL"),
        Seed("Nintendo 3DS", "3DS", "Nintendo 3DS"),

        // Nintendo DS family
        Seed("Nintendo DS", "DSi", "Nintendo DSi XL"),
        Seed("Nintendo DS", "DSi", "Nintendo DSi"),
        Seed("Nintendo DS", "DS Lite", "Nintendo DS Lite"),
        Seed("Nintendo DS", "DS", "Nintendo DS"),

        // Game Boy family
        Seed("Game Boy", "Game Boy Micro", "Game Boy Micro"),
        Seed("Game Boy", "Game Boy Advance", "Game Boy Advance SP"),
        Seed("Game Boy", "Game Boy Advance", "Game Boy Advance"),
        Seed("Game Boy", "Game Boy Color", "Game Boy Color"),
        Seed("Game Boy", "Game Boy", "Game Boy Pocket"),
        Seed("Game Boy", "Game Boy", "Game Boy Light"),
        Seed("Game Boy", "Game Boy", "Game Boy"),

        // Sega
        Seed("Sega", "Dreamcast", "Sega Dreamcast"),
        Seed("Sega", "Saturn", "Sega Saturn"),
        Seed("Sega", "Mega Drive", "Sega Mega Drive / Genesis"),
        Seed("Sega", "Master System", "Sega Master System II"),
        Seed("Sega", "Master System", "Sega Master System"),
        Seed("Sega Handheld", "Game Gear", "Sega Game Gear")
    )

    val catalogue: List<ConsoleDeviceEntry> = catalogueSeed.map { seed ->
        ConsoleDeviceEntry(
            family = seed.family,
            series = seed.series,
            name = seed.name,
            priceSheetValue = priceByName[seed.authoritativePriceName.lowercase()]?.rrp
        )
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
    val gradeBuyPercent = mapOf("A" to 0.70, "B" to 0.50, "C" to 0.30)

    fun buyPrice(entry: ConsolePriceEntry, grade: String): Double =
        entry.rrp * (gradeBuyPercent[grade] ?: error("Unsupported grade: $grade"))

    /** Unpriced catalogue entries deliberately return null so they can never authorise a buy. */
    fun buyPrice(entry: ConsoleDeviceEntry, grade: String): Double? =
        entry.priceSheetValue?.times(gradeBuyPercent[grade] ?: error("Unsupported grade: $grade"))
}
