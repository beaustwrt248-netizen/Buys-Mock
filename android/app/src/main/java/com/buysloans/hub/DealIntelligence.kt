package com.buysloans.hub

import kotlin.math.abs

data class SavedDealSignalInput(
    val id: String,
    val normalizedIdentity: String,
    val askingPrice: Double?,
    val originalMarketValue: Double?,
    val currentMarketValue: Double?,
    val maxBuyPrice: Double?,
    val createdAgeDays: Int,
    val expectedMarginRate: Double?
)

data class DealIntelligence(
    val underpricedOpportunityIds: Set<String>,
    val staleValuationIds: Set<String>,
    val unusuallyHighMarginIds: Set<String>,
    val duplicateGroups: List<Set<String>>,
    val meaningfulMarketMovementIds: Set<String>
)

object DealIntelligenceEngine {
    fun analyse(
        deals: List<SavedDealSignalInput>,
        staleAfterDays: Int = 30,
        highMarginThreshold: Double = 0.55,
        marketMovementThreshold: Double = 0.10
    ): DealIntelligence {
        require(staleAfterDays >= 0) { "staleAfterDays cannot be negative." }
        require(highMarginThreshold in 0.0..1.0) { "highMarginThreshold must be between 0 and 1." }
        require(marketMovementThreshold in 0.0..1.0) { "marketMovementThreshold must be between 0 and 1." }

        val underpriced = deals.filter { deal ->
            val ask = deal.askingPrice
            val maxBuy = deal.maxBuyPrice
            ask != null && maxBuy != null && maxBuy > 0.0 && ask >= 0.0 && ask <= maxBuy * 0.80
        }.mapTo(linkedSetOf()) { it.id }

        val stale = deals.filter { it.createdAgeDays >= staleAfterDays }
            .mapTo(linkedSetOf()) { it.id }

        val highMargin = deals.filter { (it.expectedMarginRate ?: 0.0) >= highMarginThreshold }
            .mapTo(linkedSetOf()) { it.id }

        val duplicateGroups = deals
            .filter { it.normalizedIdentity.isNotBlank() }
            .groupBy { it.normalizedIdentity.trim().lowercase() }
            .values
            .filter { it.size > 1 }
            .map { group -> group.mapTo(linkedSetOf()) { it.id } }

        val movement = deals.filter { deal ->
            val original = deal.originalMarketValue ?: return@filter false
            val current = deal.currentMarketValue ?: return@filter false
            original > 0.0 && current >= 0.0 && abs(current - original) / original >= marketMovementThreshold
        }.mapTo(linkedSetOf()) { it.id }

        return DealIntelligence(
            underpricedOpportunityIds = underpriced,
            staleValuationIds = stale,
            unusuallyHighMarginIds = highMargin,
            duplicateGroups = duplicateGroups,
            meaningfulMarketMovementIds = movement
        )
    }
}
