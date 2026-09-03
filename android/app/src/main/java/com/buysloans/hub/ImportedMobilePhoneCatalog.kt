package com.buysloans.hub

import java.io.ByteArrayInputStream
import java.util.Base64
import java.util.zip.GZIPInputStream

data class ImportedMobilePhoneModel(
    val brand: String,
    val model: String,
    val modelNumber: String,
    val storages: List<String>
)

object ImportedMobilePhoneCatalog {
    // Generated from the user-supplied Mobile Device Model & Storage List.
    // Prices intentionally do not live here; priced Morley entries remain authoritative.
    private const val COMPRESSED_DATA = "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"

    val models: List<ImportedMobilePhoneModel> by lazy {
        decode().lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split('|', limit = 4)
                if (parts.size != 4) return@mapNotNull null
                ImportedMobilePhoneModel(
                    brand = parts[0],
                    model = normalizeModel(parts[0], parts[1]),
                    modelNumber = parts[2],
                    storages = parts[3].split(',').filter { it.isNotBlank() }
                )
            }
            .toList()
    }

    private fun decode(): String {
        val bytes = Base64.getDecoder().decode(COMPRESSED_DATA)
        return GZIPInputStream(ByteArrayInputStream(bytes)).bufferedReader().use { it.readText() }
    }

    private fun normalizeModel(brand: String, model: String): String {
        if (brand != "Samsung") return model
        return model
            .replace(Regex("""Galaxy S(\d+)\+""")) { match -> "Galaxy S${match.groupValues[1]} Plus" }
            .replace(Regex("""Galaxy Z (Fold|Flip)(\d+)""")) { match ->
                "Galaxy Z ${match.groupValues[1]} ${match.groupValues[2]}"
            }
    }
}
