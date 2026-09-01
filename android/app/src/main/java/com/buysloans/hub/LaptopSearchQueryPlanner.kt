package com.buysloans.hub

/**
 * Builds a small, deterministic set of equivalent laptop search queries.
 *
 * Retrieval is broadened here, while exact/similar valuation classification remains a
 * separate safety gate. Query variants must never be treated as proof of configuration.
 */
object LaptopSearchQueryPlanner {
    private fun spacedCapacity(value: String): String = value
        .replace(Regex("(?i)(\\d)(GB|TB)\\b"), "$1 $2")
        .replace(Regex("\\s+"), " ")
        .trim()

    fun queries(
        preset: LaptopPreset,
        processor: String,
        ram: String,
        storage: String,
        versionCode: String = ""
    ): List<String> {
        val canonical = if (versionCode.isNotBlank()) {
            LaptopFactoryVariantCatalog.canonicalQuery(preset, versionCode, processor, ram, storage)
        } else {
            LaptopSelectionCatalog.canonicalQuery(preset, processor, ram, storage)
        }
        val modelWithoutYear = preset.model.substringBefore(" (").trim()
        val processorWithoutBrand = processor.removePrefix("${preset.brand} ").trim()
        val compactConfiguration = listOf(
            preset.brand,
            modelWithoutYear,
            processorWithoutBrand,
            ram,
            storage
        ).filter { it.isNotBlank() }.joinToString(" ")
        val retailerStyle = listOf(
            preset.brand,
            modelWithoutYear.replace("-inch", " inch", ignoreCase = true),
            processorWithoutBrand,
            spacedCapacity(ram),
            spacedCapacity(storage),
            "Australia"
        ).filter { it.isNotBlank() }.joinToString(" ")
        val modelCodeQuery = if (versionCode.isNotBlank()) {
            listOf(preset.brand, versionCode, processorWithoutBrand, ram, storage)
                .filter { it.isNotBlank() }
                .joinToString(" ")
        } else null

        return listOfNotNull(canonical, compactConfiguration, retailerStyle, modelCodeQuery)
            .map { it.replace(Regex("\\s+"), " ").trim() }
            .distinctBy { it.lowercase() }
            .take(4)
    }
}
