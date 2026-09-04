package com.buysloans.hub

data class MobilePhoneDeviceEntry(
    val brand: String,
    val model: String,
    val modelNumber: String?,
    val storage: String,
    val priceSheetValue: Double?,
) {
    val hasPrice: Boolean
        get() = priceSheetValue != null || LiveDevicePricing.find(brand, model, modelNumber, storage) != null
}

data class MobilePhoneSearchResult(
    val brand: String,
    val model: String,
    val modelNumber: String?,
    val storages: List<String>,
    val hasPricedVariant: Boolean,
)

object MobilePhoneDeviceCatalog {
    private fun key(brand: String, model: String, storage: String) =
        listOf(
            brand.trim().lowercase(),
            model.trim().lowercase(),
            storage.replace(" ", "").lowercase(),
        ).joinToString("|")

    val entries: List<MobilePhoneDeviceEntry>
        get() = LiveDevicePricing.catalogue()
            .asSequence()
            .filter { it.category == "mobile_phone" }
            .flatMap { device ->
                device.storageOptions.asSequence().map { storage ->
                    MobilePhoneDeviceEntry(
                        brand = device.brand,
                        model = device.model,
                        modelNumber = device.modelNumber,
                        storage = storage,
                        priceSheetValue = LiveDevicePricing.find(
                            device.brand,
                            device.model,
                            device.modelNumber,
                            storage,
                        )?.priceAud,
                    )
                }
            }
            .distinctBy { key(it.brand, it.model, it.storage) }
            .toList()

    val brandOrder = listOf(
        "Apple", "Samsung", "Google", "OnePlus", "Xiaomi", "POCO", "OPPO", "Nothing", "CMF", "Motorola",
        "Vivo", "Realme", "Huawei", "HONOR", "ASUS", "HMD", "Nokia", "Fairphone", "Meizu", "Lenovo",
        "Sony", "ZTE", "ZTE / nubia", "TCL", "Alcatel", "Aspera",
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
            Regex("(?i)fairphone\\s+(\\d+)"),
        )
        return patterns.firstNotNullOfOrNull {
            it.find(model)?.groupValues?.getOrNull(1)?.toIntOrNull()
        } ?: Regex("\\d+").find(model)?.value?.toIntOrNull() ?: 0
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

    private val modelComparator = compareByDescending<String> { generation(it) }
        .thenBy { variantRank(it) }
        .thenBy { it.lowercase() }

    fun models(brand: String) = entries.asSequence()
        .filter { it.brand == brand }
        .map { it.model }
        .distinct()
        .toList()
        .sortedWith(modelComparator)

    fun variants(brand: String, model: String) =
        entries.filter { it.brand == brand && it.model == model }

    fun modelMatches(brand: String, model: String, query: String): Boolean {
        if (query.isBlank()) return true
        if (brand.contains(query, true) || model.contains(query, true)) return true
        return variants(brand, model).any {
            it.modelNumber?.contains(query, true) == true ||
                it.storage.contains(query, true) ||
                it.storage.replace(" ", "").contains(query.replace(" ", ""), true)
        }
    }

    fun search(query: String, limit: Int = 80): List<MobilePhoneSearchResult> {
        val needle = query.trim()
        if (needle.isBlank()) return emptyList()
        return brands().asSequence()
            .flatMap { brand -> models(brand).asSequence().map { model -> brand to model } }
            .filter { (brand, model) -> modelMatches(brand, model, needle) }
            .map { (brand, model) ->
                val variants = variants(brand, model)
                MobilePhoneSearchResult(
                    brand = brand,
                    model = model,
                    modelNumber = variants.mapNotNull { it.modelNumber }.firstOrNull(),
                    storages = variants.map { it.storage }.distinct(),
                    hasPricedVariant = variants.any { it.hasPrice },
                )
            }
            .take(limit)
            .toList()
    }

    fun pricedEntry(entry: MobilePhoneDeviceEntry): MobilePhonePriceEntry? {
        val live = LiveDevicePricing.find(entry.brand, entry.model, entry.modelNumber, entry.storage)
        if (live != null) {
            return MobilePhonePriceEntry(entry.brand, entry.model, entry.storage, live.priceAud)
        }
        return MobilePhonePricingCatalog.entries.firstOrNull {
            it.brand == entry.brand &&
                it.model == entry.model &&
                key(it.brand, it.model, it.storage) == key(entry.brand, entry.model, entry.storage)
        }
    }
}
