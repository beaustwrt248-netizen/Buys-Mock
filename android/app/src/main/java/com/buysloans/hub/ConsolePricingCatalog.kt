package com.buysloans.hub

data class ConsolePriceEntry(
    val name: String,
    val rrp: Double
)

data class ConsoleDeviceEntry(
    val family: String,
    val series: String,
    val name: String,
    val modelNumber: String? = null,
    val storage: String? = null,
    val priceSheetValue: Double? = null
) {
    val hasPrice: Boolean get() = priceSheetValue != null
}

object ConsolePricingCatalog {
    private fun normalized(value: String): String = value.lowercase().replace(Regex("[^a-z0-9]"), "")

    private fun displayName(device: LiveDeviceCatalogueRow, storage: String): String {
        if (storage.isBlank()) return device.model
        return if (normalized(device.model).contains(normalized(storage))) device.model else "${device.model} $storage"
    }

    val catalogue: List<ConsoleDeviceEntry>
        get() = LiveDevicePricing.catalogue().asSequence()
            .filter { it.category == "console" }
            .flatMap { device ->
                val storages = device.storageOptions.ifEmpty { listOf("") }
                storages.asSequence().map { storage ->
                    val price = LiveDevicePricing.find(device.brand, device.model, device.modelNumber, storage)
                    ConsoleDeviceEntry(
                        family = device.brand,
                        series = device.family?.takeIf { it.isNotBlank() } ?: device.brand,
                        name = displayName(device, storage),
                        modelNumber = device.modelNumber,
                        storage = storage.takeIf { it.isNotBlank() },
                        priceSheetValue = price?.priceAud,
                    )
                }
            }
            .distinctBy { listOf(it.family.lowercase(), it.series.lowercase(), it.name.lowercase()) }
            .sortedWith(compareBy<ConsoleDeviceEntry> { it.family.lowercase() }.thenBy { it.series.lowercase() }.thenBy { it.name.lowercase() })
            .toList()

    val entries: List<ConsolePriceEntry>
        get() = catalogue.mapNotNull { row -> row.priceSheetValue?.let { ConsolePriceEntry(row.name, it) } }

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
            val haystack = "${entry.family} ${entry.series} ${entry.name} ${entry.modelNumber.orEmpty()} ${entry.storage.orEmpty()}".lowercase()
            tokens.all(haystack::contains)
        }
    }

    val grades = listOf("A", "B", "C")
    val gradeBuyPercent = mapOf("A" to 0.70, "B" to 0.50, "C" to 0.30)

    fun buyPrice(entry: ConsolePriceEntry, grade: String): Double =
        entry.rrp * (gradeBuyPercent[grade] ?: error("Unsupported grade: $grade"))

    fun buyPrice(entry: ConsoleDeviceEntry, grade: String): Double? =
        entry.priceSheetValue?.times(gradeBuyPercent[grade] ?: error("Unsupported grade: $grade"))
}
