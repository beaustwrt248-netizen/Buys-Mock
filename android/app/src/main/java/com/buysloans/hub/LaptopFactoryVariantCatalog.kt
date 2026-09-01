package com.buysloans.hub

/**
 * Manufacturer-verified configuration constraints for guided laptop selection.
 *
 * The legacy laptop catalogue contains broad family/year presets. Those remain available
 * while they are progressively verified, but a profile in this catalogue is authoritative
 * for the selectable model code, processor, memory and storage options shown by the guided
 * picker. Do not infer additional factory combinations from marketplace listings.
 */
data class LaptopFactoryVariant(
    val processor: String,
    val ram: String,
    val storage: String
)

data class LaptopFactoryVersion(
    val code: String,
    val processors: List<String>,
    val ramOptions: List<String>,
    val storageOptions: List<String>,
    val exactVariants: List<LaptopFactoryVariant> = emptyList(),
    val sourceLabel: String,
    val sourceUrl: String
) {
    val hasExactPairings: Boolean get() = exactVariants.isNotEmpty()
}

data class LaptopFactoryProfile(
    val brand: String,
    val modelPrefix: String,
    val versions: List<LaptopFactoryVersion>
)

object LaptopFactoryVariantCatalog {
    private val profiles = listOf(
        LaptopFactoryProfile(
            brand = "ASUS",
            modelPrefix = "Chromebook CX1",
            versions = listOf(
                LaptopFactoryVersion(
                    code = "CX1400",
                    processors = listOf("Intel Celeron N4500", "Intel Celeron N3350"),
                    ramOptions = listOf("4GB", "8GB"),
                    storageOptions = listOf("32GB", "64GB", "128GB"),
                    sourceLabel = "ASUS Chromebook CX1 (CX1400) Tech Specs",
                    sourceUrl = "https://www.asus.com/us/laptops/for-home/chromebook/asus-chromebook-cx1-cx1400/techspec/"
                )
            )
        ),
        LaptopFactoryProfile(
            brand = "ASUS",
            modelPrefix = "Chromebook CX14",
            versions = listOf(
                LaptopFactoryVersion(
                    code = "CX1405CKA",
                    processors = listOf("Intel Celeron N4500"),
                    ramOptions = listOf("4GB", "8GB"),
                    storageOptions = listOf("128GB"),
                    sourceLabel = "ASUS Australia Chromebook CX14 (CX1405) Tech Specs",
                    sourceUrl = "https://www.asus.com/au/laptops/for-home/chromebook/asus-chromebook-cx14-cx1405/techspec/"
                ),
                LaptopFactoryVersion(
                    code = "CX1405CTA",
                    processors = listOf("Intel Core 3-N355", "Intel Processor N50"),
                    ramOptions = listOf("4GB", "8GB"),
                    storageOptions = listOf("128GB"),
                    exactVariants = listOf(
                        LaptopFactoryVariant("Intel Core 3-N355", "8GB", "128GB"),
                        LaptopFactoryVariant("Intel Processor N50", "8GB", "128GB"),
                        LaptopFactoryVariant("Intel Processor N50", "4GB", "128GB")
                    ),
                    sourceLabel = "ASUS Australia Chromebook CX14 (CX1405CTA) configurations",
                    sourceUrl = "https://www.asus.com/au/laptops/for-home/chromebook/asus-chromebook-cx14-cx1405/shop-asus-chromebook-cx14-cx1405cta/"
                )
            )
        )
    )

    fun profile(preset: LaptopPreset?): LaptopFactoryProfile? {
        if (preset == null) return null
        return profiles
            .asSequence()
            .filter {
                it.brand.equals(preset.brand, ignoreCase = true) &&
                    preset.model.startsWith(it.modelPrefix, ignoreCase = true)
            }
            .maxByOrNull { it.modelPrefix.length }
    }

    fun versionCodes(preset: LaptopPreset?): List<String> = profile(preset)?.versions?.map { it.code }.orEmpty()

    fun version(preset: LaptopPreset?, code: String): LaptopFactoryVersion? =
        profile(preset)?.versions?.firstOrNull { it.code == code }

    fun processors(preset: LaptopPreset?, code: String): List<String> =
        version(preset, code)?.let { v ->
            if (v.hasExactPairings) v.exactVariants.map { it.processor }.distinct() else v.processors
        }.orEmpty()

    fun ramOptions(preset: LaptopPreset?, code: String, processor: String): List<String> =
        version(preset, code)?.let { v ->
            if (v.hasExactPairings) {
                v.exactVariants.filter { it.processor == processor }.map { it.ram }.distinct()
            } else v.ramOptions
        }.orEmpty()

    fun storageOptions(preset: LaptopPreset?, code: String, processor: String, ram: String): List<String> =
        version(preset, code)?.let { v ->
            if (v.hasExactPairings) {
                v.exactVariants.filter { it.processor == processor && it.ram == ram }.map { it.storage }.distinct()
            } else v.storageOptions
        }.orEmpty()

    fun configurationVerified(preset: LaptopPreset?, code: String, processor: String, ram: String, storage: String): Boolean {
        val v = version(preset, code) ?: return false
        return if (v.hasExactPairings) {
            v.exactVariants.any { it.processor == processor && it.ram == ram && it.storage == storage }
        } else {
            processor in v.processors && ram in v.ramOptions && storage in v.storageOptions
        }
    }

    fun sourceLabel(preset: LaptopPreset?, code: String): String? = version(preset, code)?.sourceLabel

    fun canonicalQuery(
        preset: LaptopPreset,
        code: String,
        processor: String,
        ram: String,
        storage: String
    ): String = listOf(preset.brand, preset.model, code, processor, ram, storage)
        .filter { it.isNotBlank() }
        .joinToString(" ")
}
