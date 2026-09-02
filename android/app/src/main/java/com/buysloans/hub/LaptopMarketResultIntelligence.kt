package com.buysloans.hub

/**
 * Converts the real MarketResult used by LaptopGuidedScreen into shadow intelligence evidence.
 * The market-search contract does not currently prove completed/sold state, so live rows are
 * deliberately marked sold=false. This prevents active asking prices from authorising a buy.
 */
object LaptopMarketResultIntelligence {
    fun evaluateShadow(
        preset: LaptopPreset,
        processor: String,
        ram: String,
        storage: String,
        modelCode: String,
        market: MarketResult,
        inputs: LaptopBuyInputs = LaptopBuyInputs()
    ): LaptopShadowDecision {
        val evidence = buildList {
            market.exactGoogle.forEach { listing -> add(listing.toEvidence("Google Shopping AU")) }
            market.similarGoogle.forEach { listing -> add(listing.toEvidence("Google Shopping AU")) }
            market.exactEbay.forEach { listing -> add(listing.toEvidence("eBay AU")) }
            market.similarEbay.forEach { listing -> add(listing.toEvidence("eBay AU")) }
            market.rejected.forEach { listing -> add(listing.toEvidence("Rejected live result")) }
        }
        return LaptopLiveIntelligenceAdapter.shadowEvaluate(
            preset = preset,
            processor = processor,
            ram = ram,
            storage = storage,
            modelCode = modelCode,
            evidence = evidence,
            inputs = inputs
        )
    }

    private fun Listing.toEvidence(platform: String): LiveLaptopEvidence = LiveLaptopEvidence(
        id = url.takeIf { it.isNotBlank() } ?: "$platform|$title|${price.toInt()}",
        title = title,
        priceAud = price,
        source = platform,
        condition = condition,
        sellerKey = source.takeIf { it.isNotBlank() },
        sold = false,
        ageDays = 0
    )
}
