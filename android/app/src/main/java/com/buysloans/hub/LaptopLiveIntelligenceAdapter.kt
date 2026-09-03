package com.buysloans.hub

/**
 * Read-only bridge between live laptop marketplace evidence and LaptopBuyIntelligence.
 * This adapter is intentionally shadow-only: it never changes the authoritative offer.
 */
data class LiveLaptopEvidence(
    val id: String,
    val title: String,
    val priceAud: Double,
    val source: String,
    val condition: String = "",
    val sellerKey: String? = null,
    val sold: Boolean = false,
    val ageDays: Int = 0
)

data class LaptopShadowDecision(
    val target: LaptopFingerprint,
    val fairBuyZone: LaptopFairBuyZone,
    val liveAuthorityChanged: Boolean = false
)

object LaptopLiveIntelligenceAdapter {
    private val families = listOf(
        "macbook pro", "macbook air", "macbook", "thinkpad", "ideapad", "thinkbook", "yoga", "legion", "loq",
        "latitude", "inspiron", "xps", "precision", "vostro", "alienware", "elitebook", "probook", "pavilion",
        "envy", "spectre", "omen", "victus", "zbook", "aspire", "swift", "travelmate", "chromebook", "predator",
        "nitro", "zenbook", "vivobook", "expertbook", "proart", "rog zephyrus", "zephyrus", "tuf gaming", "tuf",
        "modern", "prestige", "stealth", "katana", "raider", "creator", "blade", "aero", "aorus", "surface laptop",
        "surface book", "galaxy book", "gram", "matebook", "tecra", "satellite", "dynabook", "lifebook", "toughbook",
        "framework laptop", "laptop 13", "omnibook"
    )

    fun targetFingerprint(
        preset: LaptopPreset,
        processor: String,
        ram: String,
        storage: String,
        modelCode: String = ""
    ): LaptopFingerprint = LaptopFingerprint(
        brand = preset.brand,
        family = familyFromText(preset.model),
        generation = preset.year.toString(),
        modelCode = modelCode.takeIf { it.isNotBlank() },
        screenInches = screenFromText(preset.model),
        cpu = canonicalCpu(processor),
        gpu = gpuFromText(processor),
        ramGb = capacityGb(ram),
        storageGb = capacityGb(storage)
    )

    fun comparable(evidence: LiveLaptopEvidence): LaptopComparable {
        val title = evidence.title
        val hardFilter = LaptopListingFilter.decision(title)
        return LaptopComparable(
            id = evidence.id.ifBlank { title },
            fingerprint = LaptopFingerprint(
                brand = brandFromText(title),
                family = familyFromText(title),
                generation = yearFromText(title),
                modelCode = modelCodeFromText(title),
                screenInches = screenFromText(title),
                cpu = cpuFromText(title),
                gpu = gpuFromText(title),
                ramGb = ramFromText(title),
                storageGb = storageFromText(title)
            ),
            priceAud = evidence.priceAud,
            sold = evidence.sold,
            ageDays = evidence.ageDays,
            source = evidence.source.ifBlank { "unknown" },
            sellerKey = evidence.sellerKey,
            wholeDevice = !hardFilter.rejected,
            suspicious = hasInternalContradiction(title)
        )
    }

    fun shadowEvaluate(
        preset: LaptopPreset,
        processor: String,
        ram: String,
        storage: String,
        modelCode: String,
        evidence: List<LiveLaptopEvidence>,
        inputs: LaptopBuyInputs = LaptopBuyInputs()
    ): LaptopShadowDecision {
        val target = targetFingerprint(preset, processor, ram, storage, modelCode)
        val zone = LaptopBuyIntelligence.evaluate(target, evidence.map(::comparable), inputs)
        return LaptopShadowDecision(target, zone, liveAuthorityChanged = false)
    }

    private fun normal(s: String): String = s.lowercase().replace(Regex("[^a-z0-9.]+"), " ").replace(Regex("\\s+"), " ").trim()

    private fun familyFromText(text: String): String {
        val x = normal(text)
        return families.firstOrNull { x.contains(Regex("\\b${Regex.escape(it)}\\b")) }.orEmpty()
    }

    private fun brandFromText(text: String): String {
        val x = normal(text)
        return when {
            Regex("\\bapple\\b").containsMatchIn(x) || x.contains("macbook") -> "Apple"
            Regex("\\b(lenovo)\\b").containsMatchIn(x) || listOf("thinkpad", "ideapad", "thinkbook", "legion", "loq").any(x::contains) -> "Lenovo"
            Regex("\\bdell\\b").containsMatchIn(x) || listOf("latitude", "inspiron", "xps", "precision", "vostro", "alienware").any(x::contains) -> "Dell"
            Regex("\\bhp\\b|hewlett packard").containsMatchIn(x) || listOf("elitebook", "probook", "pavilion", "envy", "spectre", "omen", "victus", "zbook", "omnibook").any(x::contains) -> "HP"
            Regex("\\basus\\b").containsMatchIn(x) || listOf("zenbook", "vivobook", "zephyrus", "tuf").any(x::contains) -> "ASUS"
            Regex("\\bacer\\b").containsMatchIn(x) || listOf("aspire", "swift", "predator", "nitro").any(x::contains) -> "Acer"
            Regex("\\bmicrosoft\\b").containsMatchIn(x) || x.contains("surface laptop") || x.contains("surface book") -> "Microsoft"
            Regex("\\bmsi\\b").containsMatchIn(x) -> "MSI"
            Regex("\\brazer\\b").containsMatchIn(x) || x.contains("blade") -> "Razer"
            Regex("\\bsamsung\\b").containsMatchIn(x) || x.contains("galaxy book") -> "Samsung"
            Regex("\\blg\\b").containsMatchIn(x) || Regex("\\bgram\\b").containsMatchIn(x) -> "LG"
            Regex("\\bframework\\b").containsMatchIn(x) -> "Framework"
            else -> ""
        }
    }

    private fun yearFromText(text: String): String? = Regex("\\b(20(?:0[6-9]|1\\d|2[0-6]))\\b").find(normal(text))?.value
    private fun modelCodeFromText(text: String): String? = Regex("\\bA\\d{4}\\b", RegexOption.IGNORE_CASE).find(text)?.value?.uppercase()
    private fun screenFromText(text: String): Double? = Regex("\\b(10(?:\\.\\d)?|11(?:\\.\\d)?|12(?:\\.\\d)?|13(?:\\.\\d)?|14(?:\\.\\d)?|15(?:\\.\\d)?|16(?:\\.\\d)?|17(?:\\.\\d)?|18(?:\\.\\d)?)\\s*(?:inch|inches|\")", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.toDoubleOrNull()

    private fun canonicalCpu(value: String): String? {
        val x = normal(value).removePrefix("apple ")
        return when {
            Regex("\\bm[1-5](?: (?:pro|max|ultra))?\\b").find(x) != null -> Regex("\\bm[1-5](?: (?:pro|max|ultra))?\\b").find(x)?.value
            Regex("\\bintel core (?:ultra )?i?[3579](?: [0-9a-z-]+)?\\b").find(x) != null -> Regex("\\bintel core (?:ultra )?i?[3579](?: [0-9a-z-]+)?\\b").find(x)?.value
            Regex("\\bi[3579]\\s*[- ]?\\s*\\d{4,5}[a-z]{0,3}\\b").find(x) != null -> Regex("\\bi[3579]\\s*[- ]?\\s*\\d{4,5}[a-z]{0,3}\\b").find(x)?.value?.replace(Regex("\\s+"), "")
            Regex("\\b(?:amd )?ryzen(?: ai)? [3579](?: [0-9a-z]+)?\\b").find(x) != null -> Regex("\\b(?:amd )?ryzen(?: ai)? [3579](?: [0-9a-z]+)?\\b").find(x)?.value
            Regex("\\bsnapdragon x(?:2)?(?: plus| elite)?\\b").find(x) != null -> Regex("\\bsnapdragon x(?:2)?(?: plus| elite)?\\b").find(x)?.value
            else -> x.takeIf { it.isNotBlank() }
        }
    }

    private fun cpuFromText(text: String): String? {
        val x = normal(text)
        val candidates = listOf(
            Regex("\\bm[1-5](?: (?:pro|max|ultra))?\\b"),
            Regex("\\bi[3579]\\s*[- ]?\\s*\\d{4,5}[a-z]{0,3}\\b"),
            Regex("\\b(?:amd )?ryzen(?: ai)? [3579](?: [0-9a-z]+)?\\b"),
            Regex("\\bsnapdragon x(?:2)?(?: plus| elite)?\\b")
        )
        return candidates.firstNotNullOfOrNull { it.find(x)?.value?.replace(Regex("\\s+"), " ") }
    }

    private fun gpuFromText(text: String): String? = Regex("\\b(?:rtx|gtx|rx|arc)\\s*\\d{3,4}(?:\\s*(?:ti|super|xt|xtx))?\\b", RegexOption.IGNORE_CASE).find(text)?.value

    private fun ramFromText(text: String): Int? = Regex("\\b(4|8|12|16|18|24|32|36|48|64|96|128)\\s*gb\\s*(?:ram|memory)?\\b", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.toIntOrNull()

    private fun storageFromText(text: String): Int? {
        val x = normal(text)
        val storageLabel = Regex("\\b(64|128|256|512)\\s*gb\\s*(?:ssd|storage)?\\b|\\b(1|2|4|8)\\s*tb\\s*(?:ssd|storage)?\\b").find(x) ?: return null
        return if (storageLabel.groupValues[1].isNotBlank()) storageLabel.groupValues[1].toIntOrNull()
        else storageLabel.groupValues[2].toIntOrNull()?.times(1024)
    }

    private fun capacityGb(value: String): Int? {
        val x = normal(value)
        Regex("(\\d+)\\s*gb").find(x)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
        return Regex("(\\d+(?:\\.\\d+)?)\\s*tb").find(x)?.groupValues?.get(1)?.toDoubleOrNull()?.let { (it * 1024).toInt() }
    }

    private fun hasInternalContradiction(title: String): Boolean {
        val x = normal(title)
        val years = Regex("\\b20(?:0[6-9]|1\\d|2[0-6])\\b").findAll(x).map { it.value }.distinct().toList()
        val appleChips = Regex("\\bm[1-5](?: (?:pro|max|ultra))?\\b").findAll(x).map { it.value }.distinct().toList()
        return years.size > 1 || appleChips.size > 1
    }
}
