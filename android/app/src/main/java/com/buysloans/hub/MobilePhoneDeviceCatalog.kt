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
        val importedByKey = imported.associateBy { key(it.brand, it.model, it.storage) }
        val existingKeys = mutableSetOf<String>()
        val existing = MobilePhonePricingCatalog.entries.map { priced ->
            val entryKey = key(priced.brand, priced.model, priced.storage)
            existingKeys += entryKey
            val source = importedByKey[entryKey]
            MobilePhoneDeviceEntry(
                brand = priced.brand,
                model = priced.model,
                modelNumber = source?.modelNumber,
                storage = priced.storage,
                priceSheetValue = priced.priceSheetValue
            )
        }

        // Existing Morley price-sheet records are always authoritative. Imported rows only
        // extend the catalogue when that exact brand/model/storage key does not already exist.
        existing + imported.filter { key(it.brand, it.model, it.storage) !in existingKeys }
    }

    val brandOrder = listOf(
        "Apple", "Samsung", "Google", "OnePlus", "Xiaomi", "OPPO", "Nothing",
        "Motorola", "Vivo", "Realme", "Huawei", "Lenovo", "Sony", "ZTE / nubia", "TCL"
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
        if (model.contains(query, ignoreCase = true)) return true
        return variants(brand, model).any { it.modelNumber?.contains(query, ignoreCase = true) == true }
    }

    fun pricedEntry(entry: MobilePhoneDeviceEntry): MobilePhonePriceEntry? =
        MobilePhonePricingCatalog.entries.firstOrNull {
            it.brand == entry.brand && it.model == entry.model && it.storage == entry.storage
        }
}
