package com.buysloans.hub

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class LaptopFingerprint(
    val brand: String,
    val family: String,
    val generation: String?,
    val modelCode: String?,
    val screenInches: Double?,
    val cpu: String?,
    val gpu: String?,
    val ramGb: Int?,
    val storageGb: Int?
)

data class LaptopComparable(
    val id: String,
    val fingerprint: LaptopFingerprint,
    val priceAud: Double,
    val sold: Boolean,
    val ageDays: Int = 0,
    val source: String = "unknown",
    val sellerKey: String? = null,
    val wholeDevice: Boolean = true,
    val suspicious: Boolean = false
)

data class LaptopComparableDecision(
    val comparable: LaptopComparable,
    val accepted: Boolean,
    val score: Int,
    val reason: String,
    val weight: Double
)

data class LaptopBuyInputs(
    val expectedCosts: Double = 0.0,
    val riskReserve: Double = 0.0,
    val minimumProfit: Double = 0.0,
    val inventoryPressure: Double = 0.0,
    val demandStrength: Double = 0.5,
    val targetOfferRatio: Double = 0.65,
    val hardMaxRatio: Double = 0.70
)

data class LaptopFairBuyZone(
    val marketValue: Double,
    val quickSaleValue: Double,
    val confidence: Int,
    val opening: Int,
    val recommended: Int,
    val competitive: Int,
    val hardMaximum: Int,
    val decision: String,
    val reasons: List<String>,
    val comparables: List<LaptopComparableDecision>
)

object LaptopBuyIntelligence {
    private val partTerms = Regex("\\b(for|charger|adapter|screen|display|lcd|keyboard|keycap|battery|shell|case|cover|motherboard|logic board|parts?|spares?|repair|box only)\\b", RegexOption.IGNORE_CASE)

    fun canonical(value: String?): String = value.orEmpty().lowercase()
        .replace(Regex("[^a-z0-9]+"), " ").trim()

    fun classify(target: LaptopFingerprint, c: LaptopComparable): LaptopComparableDecision {
        if (!c.wholeDevice || partTerms.containsMatchIn(c.id)) return reject(c, "not a whole laptop")
        if (c.suspicious) return reject(c, "suspicious or contradictory listing")
        if (!same(target.brand, c.fingerprint.brand)) return reject(c, "wrong brand")
        if (!same(target.family, c.fingerprint.family)) return reject(c, "wrong model family")
        if (conflict(target.modelCode, c.fingerprint.modelCode)) return reject(c, "wrong model code")
        if (conflict(target.generation, c.fingerprint.generation)) return reject(c, "wrong generation")
        if (conflict(target.cpu, c.fingerprint.cpu)) return reject(c, "wrong processor")
        if (target.screenInches != null && c.fingerprint.screenInches != null && kotlin.math.abs(target.screenInches - c.fingerprint.screenInches) > 0.6)
            return reject(c, "wrong screen size")

        var score = 55
        if (sameOptional(target.modelCode, c.fingerprint.modelCode)) score += 15
        if (sameOptional(target.generation, c.fingerprint.generation)) score += 10
        if (sameOptional(target.cpu, c.fingerprint.cpu)) score += 10
        if (target.ramGb != null && target.ramGb == c.fingerprint.ramGb) score += 5
        if (target.storageGb != null && target.storageGb == c.fingerprint.storageGb) score += 5
        if (target.gpu != null && sameOptional(target.gpu, c.fingerprint.gpu)) score += 5
        score = min(100, score)

        val specPenalty = (if (target.ramGb != null && c.fingerprint.ramGb != null && target.ramGb != c.fingerprint.ramGb) 0.12 else 0.0) +
            (if (target.storageGb != null && c.fingerprint.storageGb != null && target.storageGb != c.fingerprint.storageGb) 0.08 else 0.0)
        val soldWeight = if (c.sold) 1.0 else 0.55
        val recency = max(0.35, 1.0 - c.ageDays.coerceAtLeast(0) / 365.0)
        val weight = soldWeight * recency * (score / 100.0) * (1.0 - specPenalty)
        val label = when { score >= 95 -> "exact comparable"; score >= 85 -> "strong comparable"; else -> "adjusted comparable" }
        return LaptopComparableDecision(c, true, score, label, weight)
    }

    fun evaluate(target: LaptopFingerprint, raw: List<LaptopComparable>, inputs: LaptopBuyInputs = LaptopBuyInputs()): LaptopFairBuyZone {
        val deduped = raw.distinctBy { listOf(it.source, it.sellerKey.orEmpty(), canonical(it.id), it.priceAud.roundToInt()).joinToString("|") }
        val decisions = deduped.map { classify(target, it) }
        val accepted = decisions.filter { it.accepted && it.weight > 0.0 && it.comparable.priceAud > 0.0 }
        if (accepted.size < 3) return LaptopFairBuyZone(0.0, 0.0, 20, 0, 0, 0, 0, "MANUAL REVIEW", listOf("Insufficient verified comparable evidence"), decisions)

        val prices = accepted.map { it.comparable.priceAud }.sorted()
        val median = percentile(prices, 0.5)
        val lower = percentile(prices, 0.15)
        val upper = percentile(prices, 0.85)
        val trimmed = accepted.filter { it.comparable.priceAud in lower..upper }.ifEmpty { accepted }
        val weighted = trimmed.sumOf { it.comparable.priceAud * it.weight } / trimmed.sumOf { it.weight }
        val soldShare = trimmed.count { it.comparable.sold }.toDouble() / trimmed.size
        val sourceDiversity = trimmed.map { it.comparable.source }.distinct().size
        val exactShare = trimmed.count { it.score >= 95 }.toDouble() / trimmed.size
        val confidence = min(99, (45 + min(20, trimmed.size * 2) + soldShare * 15 + exactShare * 15 + min(5, sourceDiversity * 2)).roundToInt())

        val market = (weighted * 0.7) + (median * 0.3)
        val quickSale = market * (0.91 + 0.05 * inputs.demandStrength.coerceIn(0.0, 1.0))
        val inventoryFactor = (1.0 - 0.12 * inputs.inventoryPressure.coerceIn(0.0, 1.0))
        val economicCeiling = max(0.0, quickSale - inputs.expectedCosts - inputs.riskReserve - inputs.minimumProfit)
        val ratioCeiling = market * inputs.hardMaxRatio.coerceIn(0.0, 0.95)
        val hardMax = min(economicCeiling, ratioCeiling) * inventoryFactor
        val recommended = min(hardMax, market * inputs.targetOfferRatio.coerceIn(0.0, inputs.hardMaxRatio))
        val opening = recommended * 0.93
        val competitive = min(hardMax, recommended * 1.045)

        val challengeReasons = mutableListOf<String>()
        if (confidence < 70) challengeReasons += "Comparable confidence is below automatic-buy threshold"
        if (soldShare < 0.35) challengeReasons += "Too little sold evidence; active asking prices dominate"
        if (sourceDiversity < 2) challengeReasons += "Insufficient source diversity"
        if (inputs.inventoryPressure > 0.75) challengeReasons += "Existing inventory pressure is high"
        if (economicCeiling <= 0.0) challengeReasons += "Expected costs, risk and required profit consume resale value"
        val decision = when {
            challengeReasons.any { it.contains("consume") } -> "PASS"
            confidence < 70 || soldShare < 0.35 -> "MANUAL REVIEW"
            else -> "BUY / NEGOTIATE"
        }
        if (challengeReasons.isEmpty()) challengeReasons += "Identity, comparable quality and purchase economics passed challenge checks"

        return LaptopFairBuyZone(market, quickSale, confidence, opening.roundToInt(), recommended.roundToInt(), competitive.roundToInt(), hardMax.roundToInt(), decision, challengeReasons, decisions)
    }

    private fun reject(c: LaptopComparable, reason: String) = LaptopComparableDecision(c, false, 0, reason, 0.0)
    private fun same(a: String?, b: String?) = canonical(a).isNotBlank() && canonical(a) == canonical(b)
    private fun sameOptional(a: String?, b: String?) = canonical(a).isNotBlank() && canonical(a) == canonical(b)
    private fun conflict(a: String?, b: String?): Boolean = canonical(a).isNotBlank() && canonical(b).isNotBlank() && canonical(a) != canonical(b)
    private fun percentile(sorted: List<Double>, p: Double): Double {
        if (sorted.isEmpty()) return 0.0
        val idx = ((sorted.size - 1) * p.coerceIn(0.0, 1.0)).roundToInt()
        return sorted[idx]
    }
}
