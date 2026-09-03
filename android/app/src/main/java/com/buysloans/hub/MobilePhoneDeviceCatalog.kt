package com.buysloans.hub

data class MobilePhoneDeviceEntry(
    val brand: String,
    val model: String,
    val modelNumber: String?,
    val storage: String,
    val priceSheetValue: Double?
) {
    val hasPrice: Boolean get() = priceSheetValue != null
}

data class MobilePhoneSearchResult(
    val brand: String,
    val model: String,
    val modelNumber: String?,
    val storages: List<String>,
    val hasPricedVariant: Boolean
)

object MobilePhoneDeviceCatalog {
    private fun key(brand: String, model: String, storage: String): String =
        listOf(brand.trim().lowercase(), model.trim().lowercase(), storage.replace(" ", "").lowercase()).joinToString("|")

    val entries: List<MobilePhoneDeviceEntry> by lazy {
        val imported = ImportedMobilePhoneCatalog.models.flatMap { model ->
            model.storages.map { storage ->
                MobilePhoneDeviceEntry(model.brand, model.model, model.modelNumber, storage, null)
            }
        }
        val expanded = ExpandedMobilePhoneCatalog.models.flatMap { model ->
            model.storages.map { storage ->
                MobilePhoneDeviceEntry(model.brand, model.model, model.modelNumber, storage, null)
            }
        }
        val additive = (imported + expanded).distinctBy { key(it.brand, it.model, it.storage) }
        val additiveByKey = additive.associateBy { key(it.brand, it.model, it.storage) }
        val existingKeys = mutableSetOf<String>()
        val existing = MobilePhonePricingCatalog.entries.map { priced ->
            val entryKey = key(priced.brand, priced.model, priced.storage)
            existingKeys += entryKey
            val source = additiveByKey[entryKey]
            MobilePhoneDeviceEntry(priced.brand, priced.model, source?.modelNumber, priced.storage, priced.priceSheetValue)
        }

        // Existing Morley price-sheet records are always authoritative. Imported and expanded
        // rows only extend the catalogue when that exact brand/model/storage key does not exist.
        existing + additive.filter { key(it.brand, it.model, it.storage) !in existingKeys }
    }

    val brandOrder = listOf(
        "Apple", "Samsung", "Google", "OnePlus", "Xiaomi", "OPPO", "Nothing",
        "Motorola", "Vivo", "Realme", "Huawei", "HONOR", "ASUS", "HMD", "Nokia",
        "Fairphone", "Meizu", "Lenovo", "Sony", "ZTE / nubia", "TCL"
    )

    fun brands(): List<String> {
        val available = entries.map { it.brand }.toSet()
        return brandOrder.filter { it in available } + available.filter { it !in brandOrder }.sorted()
    }

    private fun generation(model: String): Int {
        val patterns = listOf(
            Regex("(?i)iphone\\s+(\\d+)"),
            Regex("(?i)galaxy\\s+s(\\d+)"),
            Regex("(?i)galaxy\\s+z\\s+(?:fold|flip)\\s*(\\d+)"),
            Regex("(?i)galaxy\\s+a(\\d+)"),
            Regex("(?i)pixel\\s+(\\d+)"),
            Regex("(?i)oneplus\\s+(\\d+)"),
            Regex("(?i)find\\s+x(\\d+)"),
            Regex("(?i)reno\\s*(\\d+)"),
            Regex("(?i)xiaomi\\s+(\\d+)"),
            Regex("(?i)fairphone\\s+(\\d+)")
        )
        return patterns.firstNotNullOfOrNull { it.find(model)?.groupValues?.getOrNull(1)?.toIntOrNull() }
            ?: Regex("\\d+").find(model)?.value?.toIntOrNull()
            ?: 0
    }

    private fun variantRank(model: String): Int {
        val value = model.lowercase()
        return when {
            "pro max" in value || "ultra" in value -> 0
            "pro" in value -> 1
            "plus" in value -> 2
            "air" in value -> 3
            "edge" in value -> 4
            "fe" in value -> 5
            "mini" in value -> 6
            else -> 7
        }
    }

    /** Series/generation-first ordering: newest generation first, then premium-to-base variants. */
    private val modelComparator = compareByDescending<String> { generation(it) }
        .thenBy { variantRank(it) }
        .thenBy { it.lowercase() }

    fun models(brand: String): List<String> =
        entries.asSequence()
            .filter { it.brand == brand }
            .map { it.model }
            .distinct()
            .toList()
            .sortedWith(modelComparator)

    fun variants(brand: String, model: String): List<MobilePhoneDeviceEntry> =
        entries.filter { it.brand == brand && it.model == model }

    fun modelMatches(brand: String, model: String, query: String): Boolean {
        if (query.isBlank()) return true
        if (brand.contains(query, ignoreCase = true)) return true
        if (model.contains(query, ignoreCase = true)) return true
        return variants(brand, model).any {
            it.modelNumber?.contains(query, ignoreCase = true) == true ||
                it.storage.contains(query, ignoreCase = true) ||
                it.storage.replace(" ", "").contains(query.replace(" ", ""), ignoreCase = true)
        }
    }

    /** Search every phone category by brand, friendly model, manufacturer model number or storage. */
    fun search(query: String, limit: Int = 80): List<MobilePhoneSearchResult> {
        val needle = query.trim()
        if (needle.isBlank()) return emptyList()
        return brands().asSequence()
            .flatMap { brand -> models(brand).asSequence().map { model -> brand to model } }
            .filter { (brand, model) -> modelMatches(brand, model, needle) }
            .map { (brand, model) ->
                val variants = variants(brand, model)
                MobilePhoneSearchResult(
                    brand,
                    model,
                    variants.mapNotNull { it.modelNumber }.firstOrNull(),
                    variants.map { it.storage }.distinct(),
                    variants.any { it.hasPrice }
                )
            }
            .take(limit)
            .toList()
    }

    fun pricedEntry(entry: MobilePhoneDeviceEntry): MobilePhonePriceEntry? =
        MobilePhonePricingCatalog.entries.firstOrNull {
            it.brand == entry.brand && it.model == entry.model && it.storage == entry.storage
        }
}
