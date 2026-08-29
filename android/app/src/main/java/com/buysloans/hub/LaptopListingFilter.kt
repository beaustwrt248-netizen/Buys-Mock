package com.buysloans.hub

/**
 * Central laptop-marketplace contamination filter.
 *
 * Keep hard rejects deterministic and conservative: a rejected row is never allowed to
 * contribute to an exact/similar valuation. This protects every laptop brand, not only
 * catalogued Apple models.
 */
internal object LaptopListingFilter {
    data class Decision(
        val rejected: Boolean,
        val reason: String? = null
    )

    private val servicePhrases = listOf(
        "data recovery", "data retrieval", "file recovery", "hard drive recovery",
        "ssd recovery", "computer repair", "laptop repair", "macbook repair",
        "screen repair", "screen replacement service", "battery replacement service",
        "keyboard replacement service", "logic board repair", "motherboard repair",
        "board repair", "water damage repair", "liquid damage repair", "charging port repair",
        "usb port repair", "hinge repair", "diagnostic service", "diagnostics service",
        "virus removal", "malware removal", "password removal", "password reset service",
        "software installation", "windows installation", "macos installation", "os installation",
        "upgrade service", "ram upgrade service", "ssd upgrade service", "cleaning service",
        "thermal paste service", "microsoldering", "micro soldering", "soldering service",
        "it support", "tech support", "remote support", "call out service", "callout service",
        "labour only", "labor only", "repair quote", "free quote", "repair centre",
        "repair center", "repair shop", "repair technician", "no fix no fee"
    )

    private val partPhrases = listOf(
        "replacement screen", "replacement display", "lcd replacement", "oled replacement",
        "screen assembly", "display assembly", "top case", "bottom case", "palmrest",
        "palm rest", "keyboard assembly", "trackpad", "touchpad", "logic board",
        "motherboard", "mainboard", "daughter board", "daughterboard", "io board", "i o board",
        "battery only", "charger only", "adapter only", "power adapter", "dc jack",
        "charging port", "usb board", "wifi card", "wireless card", "webcam module",
        "camera module", "speaker set", "replacement speaker", "hinge set", "hinges only",
        "fan assembly", "cooling fan", "heatsink", "heat sink", "ssd only", "ram only",
        "memory only", "display cable", "screen cable", "lvds cable", "edp cable",
        "bezel only", "lid only", "housing only", "shell only", "case only",
        "for spares", "spares only", "parts only", "parting out", "breaking for parts",
        "box only", "empty box", "manual only", "restore media", "recovery media"
    )

    private val accessoryPhrases = listOf(
        "laptop bag", "laptop sleeve", "laptop stand", "laptop dock", "docking station",
        "usb hub", "usb c hub", "usb-c hub", "port replicator", "privacy filter",
        "screen protector", "keyboard cover", "hard shell case", "hardshell case",
        "carrying case", "protective case", "replacement charger", "replacement adapter",
        "power supply only", "stylus only", "pen only", "mouse only", "keyboard only"
    )

    private val nonSalePhrases = listOf(
        "wanted to buy", "wanted laptop", "wtb laptop", "looking to buy", "cash paid for",
        "we buy laptops", "sell us your laptop", "trade in service", "trade-in service",
        "rental service", "laptop rental", "computer rental", "lease a laptop", "leasing service"
    )

    private val wholeDeviceSignals = Regex(
        "\\b(laptop|notebook|ultrabook|chromebook|macbook|thinkpad|ideapad|latitude|inspiron|xps|elitebook|probook|pavilion|envy|spectre|zenbook|vivobook|expertbook|aspire|swift|travelmate|gram|matebook|galaxy book|surface laptop|surface book|toughbook|lifebook|tecra|dynabook|framework laptop|legion|loq|omen|victus|predator|nitro|rog|zephyrus|tuf|blade|raider|stealth|katana|prestige|modern)\\b"
    )

    private fun norm(value: String): String = value.lowercase()
        .replace(Regex("[^a-z0-9+.-]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    internal fun decision(title: String): Decision {
        val x = norm(title)
        if (x.isBlank()) return Decision(true, "Empty listing")

        nonSalePhrases.firstOrNull { x.contains(it) }?.let {
            return Decision(true, "Non-sale/wanted listing")
        }
        servicePhrases.firstOrNull { x.contains(it) }?.let {
            return Decision(true, "Service listing")
        }
        partPhrases.firstOrNull { x.contains(it) }?.let {
            return Decision(true, "Part/accessory")
        }
        accessoryPhrases.firstOrNull { x.contains(it) }?.let {
            return Decision(true, "Part/accessory")
        }

        // Generic service wording is only a hard reject when it clearly describes work rather
        // than a whole-device sale, preventing phrases such as "recently serviced laptop" from
        // being discarded.
        val serviceAction = Regex("\\b(repair|repairs|recovery|recover|diagnostic|diagnostics|installation|install|upgrade|upgrades|replacement|replace|cleaning|clean|soldering|microsoldering)\\b")
            .containsMatchIn(x)
        val commercialService = Regex("\\b(service|services|quote|quotes|technician|technicians|shop|centre|center|labour|labor|fee|fees)\\b")
            .containsMatchIn(x)
        if (serviceAction && commercialService && !wholeDeviceSignals.containsMatchIn(x)) {
            return Decision(true, "Service listing")
        }

        return Decision(false)
    }

    /** High-value model identifiers used to prevent cross-model contamination. */
    internal fun strongIdentifiers(value: String): Set<String> {
        val x = norm(value).replace("-", " ")
        val patterns = listOf(
            Regex("\\ba\\d{4}\\b"),                         // Apple A1932 / A2337
            Regex("\\b(?:82|83|20|21)[a-z0-9]{2,6}\\b"),   // Lenovo MTM families
            Regex("\\b[a-z]{1,4}\\d{3,5}[a-z]{0,4}\\b"), // UX3402, XPS13-like, NP950X
            Regex("\\b\\d{3,4}[a-z]{1,3}\\b")            // 5420u-like identifiers
        )
        val ignored = setOf(
            "i3", "i5", "i7", "i9", "m1", "m2", "m3", "m4",
            "128gb", "256gb", "512gb", "1tb", "2tb", "4tb", "8gb", "16gb", "32gb", "64gb"
        )
        return patterns.flatMap { regex -> regex.findAll(x).map { it.value.replace(" ", "") }.toList() }
            .filterNot { it in ignored }
            .toSet()
    }

    internal fun conflictingIdentifier(query: String, title: String): Boolean {
        val q = strongIdentifiers(query)
        val t = strongIdentifiers(title)
        if (q.isEmpty() || t.isEmpty()) return false
        return q.intersect(t).isEmpty()
    }
}
