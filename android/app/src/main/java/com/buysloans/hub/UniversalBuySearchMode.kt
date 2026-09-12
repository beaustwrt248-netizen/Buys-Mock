package com.buysloans.hub

enum class UniversalBuySearchMode(
    val wireValue: String,
    val title: String,
    val flow: String,
    val usesUniversalEngine: Boolean
) {
    QUICK_SEARCH("quick_search", "Quick Search", "instant_lookup", true),
    MANUAL_SEARCH("manual_search", "Manual Search", "guided_lookup", true),
    PRICE_CHECK("price_check", "Price Check", "valuation_shortcut", true),
    AI_SCAN("ai_scan", "AI Device Scan", "device_lens", false);

    companion object {
        const val EXTRA_MODE = "morley_search_mode"

        fun from(value: String?): UniversalBuySearchMode =
            entries.firstOrNull { it.wireValue == value?.trim()?.lowercase() } ?: QUICK_SEARCH
    }
}
