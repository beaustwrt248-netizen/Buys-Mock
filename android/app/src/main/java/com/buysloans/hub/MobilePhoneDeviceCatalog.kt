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
                MobilePhoneDeviceEntry(
                    brand = model.brand,
                    model = model.model,
                    modelNumber = model.modelNumber,
                    storage = storage,
                    priceSheetValue = null
                )
            }
        }
        val expanded = ExpandedMobilePhoneCatalog.models.flatMap { model ->
            model.storages.map { storage ->
                MobilePhoneDeviceEntry(
                    brand = model.brand,
                    model = model.model,
                    modelNumber = model.modelNumber,
                    storage = storage,
                    priceSheetValue = null
                )
            }
        }
        val additive = (imported + expanded)
            .distinctBy { key(it.brand, it.model, it.storage) }
        val additiveByKey = additive.associateBy { key(it.brand, it.model, it.storage) }
        val existingKeys = mutableSetOf<String>()
        val existing = MobilePhonePricingCatalog.entries.map { priced ->
            val entryKey = key(priced.brand, priced.model, priced.storage)
            existingKeys += entryKey
            val source = additiveByKey[entryKey]
            MobilePhoneDeviceEntry(
                brand = priced.brand,
                model = priced.model,
                modelNumber = source?.modelNumber,
                storage = priced.storage,
                priceSheetValue = priced.priceSheetValue
            )
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

    fun models(brand: String): List<String> =
        entries.asSequence().filter { it.brand == brand }.map { it.model }.distinct().toList()

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
                    brand = brand,
                    model = model,
                    modelNumber = variants.mapNotNull { it.modelNumber }.firstOrNull(),
                    storages = variants.map { it.storage }.distinct(),
                    hasPricedVariant = variants.any { it.hasPrice }
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
