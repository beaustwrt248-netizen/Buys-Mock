package com.buysloans.hub

enum class UniversalBuyCategory {
    PHONE,
    LAPTOP,
    CONSOLE,
    GENERAL_BUYS
}

data class UniversalBuySearchResult(
    val category: UniversalBuyCategory,
    val title: String,
    val subtitle: String,
    val searchKey: String,
    val priceSheetValue: Double? = null,
    val referenceValue: Double? = null,
    val modelNumber: String? = null,
    val canAuthoriseBuy: Boolean = false
)

/**
 * Cross-category resolver for the Morley buy flow.
 *
 * Pricing boundaries are deliberate:
 * - authoritative phone/console price-sheet rows may carry priceSheetValue and canAuthoriseBuy=true;
 * - imported phone catalogue rows remain searchable but unpriced;
 * - Australian retail laptop observations are identity/reference data only and never authorise a buy;
 * - General Buys is a workflow fallback for arbitrary items and never invents a price.
 */
object UniversalBuySearch {
    private fun tokens(query: String): List<String> = query
        .trim()
        .lowercase()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }

    private fun matches(haystack: String, tokens: List<String>): Boolean {
        val normalized = haystack.lowercase()
        return tokens.all(normalized::contains)
    }

    private fun score(haystack: String, query: String, tokens: List<String>): Int {
        val normalized = haystack.lowercase()
        val q = query.trim().lowercase()
        var score = 0
        if (normalized == q) score += 1000
        if (normalized.startsWith(q)) score += 500
        if (normalized.contains(q)) score += 250
        score += tokens.count(normalized::contains) * 25
        return score
    }

    fun search(query: String, limit: Int = 24): List<UniversalBuySearchResult> {
        val queryTokens = tokens(query)
        if (queryTokens.isEmpty()) return emptyList()

        val pricedPhoneKeys = MobilePhonePricingCatalog.entries
            .associateBy { "${it.brand}|${it.model}|${it.storage}".lowercase() }

        val phoneResults = buildList {
            MobilePhonePricingCatalog.entries.forEach { entry ->
                val haystack = "${entry.brand} ${entry.model} ${entry.storage}"
                if (matches(haystack, queryTokens)) {
                    add(
                        UniversalBuySearchResult(
                            category = UniversalBuyCategory.PHONE,
                            title = entry.model,
                            subtitle = "${entry.brand} • ${entry.storage} • Morley price sheet",
                            searchKey = haystack,
                            priceSheetValue = entry.priceSheetValue,
                            canAuthoriseBuy = true
                        )
                    )
                }
            }

            ImportedMobilePhoneCatalog.models.forEach { model ->
                model.storages.forEach { storage ->
                    val key = "${model.brand}|${model.model}|$storage".lowercase()
                    if (key in pricedPhoneKeys) return@forEach
                    val haystack = "${model.brand} ${model.model} ${model.modelNumber} $storage"
                    if (matches(haystack, queryTokens)) {
                        add(
                            UniversalBuySearchResult(
                                category = UniversalBuyCategory.PHONE,
                                title = model.model,
                                subtitle = "${model.brand} • $storage • Price to be added",
                                searchKey = haystack,
                                modelNumber = model.modelNumber.ifBlank { null },
                                canAuthoriseBuy = false
                            )
                        )
                    }
                }
            }
        }

        val laptopResults = AuRetailLaptopCatalog.listings.mapNotNull { listing ->
            val haystack = listOfNotNull(
                listing.brand,
                listing.familyModel,
                listing.modelSku,
                listing.processor,
                listing.ram,
                listing.storage
            ).joinToString(" ")
            if (!matches(haystack, queryTokens)) return@mapNotNull null
            UniversalBuySearchResult(
                category = UniversalBuyCategory.LAPTOP,
                title = listing.familyModel,
                subtitle = listOfNotNull(listing.brand, listing.modelSku, listing.processor, listing.ram, listing.storage)
                    .joinToString(" • "),
                searchKey = haystack,
                referenceValue = listing.priceAud,
                modelNumber = listing.modelSku,
                canAuthoriseBuy = false
            )
        }

        val consoleResults = ConsolePricingCatalog.catalogue.mapNotNull { entry ->
            val haystack = "${entry.family} ${entry.series} ${entry.name}"
            if (!matches(haystack, queryTokens)) return@mapNotNull null
            UniversalBuySearchResult(
                category = UniversalBuyCategory.CONSOLE,
                title = entry.name,
                subtitle = if (entry.hasPrice) "${entry.family} • ${entry.series} • Morley price sheet"
                else "${entry.family} • ${entry.series} • Price to be added",
                searchKey = haystack,
                priceSheetValue = entry.priceSheetValue,
                canAuthoriseBuy = entry.hasPrice
            )
        }

        val concrete = (phoneResults + laptopResults + consoleResults)
            .distinctBy { "${it.category}|${it.title}|${it.subtitle}" }
            .sortedWith(
                compareByDescending<UniversalBuySearchResult> {
                    score(it.searchKey, query, queryTokens)
                }.thenByDescending { it.canAuthoriseBuy }
                    .thenBy { it.category.ordinal }
                    .thenBy { it.title }
            )
            .take(limit.coerceAtLeast(1))

        if (concrete.size >= limit.coerceAtLeast(1)) return concrete

        val general = UniversalBuySearchResult(
            category = UniversalBuyCategory.GENERAL_BUYS,
            title = query.trim(),
            subtitle = "Use General Buys valuation workflow",
            searchKey = query.trim(),
            canAuthoriseBuy = false
        )

        return concrete + general
    }
}
