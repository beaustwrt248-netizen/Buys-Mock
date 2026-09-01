package com.buysloans.hub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

private val SourceYellow = Color(0xFF167A5A)
private val SourceCard = Color(0xFF222222)

data class SourceStats(
    val name: String,
    val available: Boolean,
    val count: Int,
    val average: Double,
    val estimated: Double,
    val note: String
)

private fun sourceMoney(v: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "AU")).apply {
    maximumFractionDigits = 0
}.format(v)

private fun sourceMedian(values: List<Double>): Double {
    val s = values.filter { it > 0 }.sorted()
    if (s.isEmpty()) return 0.0
    val m = s.size / 2
    return if (s.size % 2 == 1) s[m] else (s[m - 1] + s[m]) / 2
}

private fun trimmedAverage(values: List<Double>): Double {
    val s = values.filter { it > 0 }.sorted()
    if (s.isEmpty()) return 0.0
    val cleaned = if (s.size >= 5) s.drop(1).dropLast(1) else s
    return cleaned.average()
}

private fun statsFor(name: String, listings: List<Listing>, estimateMultiplier: Double = 1.0, note: String): SourceStats {
    val prices = listings.filter { it.tier == MatchTier.EXACT && it.price > 0 }.map { it.price }
    val avg = if (prices.isEmpty()) 0.0 else trimmedAverage(prices)
    val estimateBase = sourceMedian(prices)
    return SourceStats(
        name = name,
        available = prices.isNotEmpty(),
        count = prices.size,
        average = avg,
        estimated = estimateBase * estimateMultiplier,
        note = note
    )
}

private fun consensus(values: List<Double>): Pair<Double, String> {
    val v = values.filter { it > 0 }.sorted()
    if (v.isEmpty()) return 0.0 to "UNAVAILABLE"
    val estimate = sourceMedian(v)
    if (v.size == 1) return estimate to "LOW — ONE VERIFIED SOURCE"
    val min = v.first()
    val max = v.last()
    val spread = if (estimate > 0) (max - min) / estimate else 1.0
    val label = when {
        v.size >= 3 && spread <= 0.15 -> "HIGH — SOURCES AGREE"
        spread <= 0.25 -> "MEDIUM — SOURCES CLOSE"
        else -> "LOW — SOURCE DISAGREEMENT"
    }
    return estimate to label
}

@Composable
fun CrossSourceEvidence(result: MarketResult, googleUsedRate: Double) {
    val ebay = statsFor(
        name = "eBay",
        listings = result.exactEbay,
        estimateMultiplier = 1.0,
        note = "Verified exact-model used comparables"
    )
    val google = statsFor(
        name = "Google",
        listings = result.exactGoogle,
        estimateMultiplier = googleUsedRate,
        note = "Verified exact-model new retail converted to used estimate"
    )
    val gumtree = SourceStats(
        name = "Gumtree",
        available = false,
        count = 0,
        average = 0.0,
        estimated = 0.0,
        note = "Not connected yet — excluded from valuation"
    )
    val facebook = SourceStats(
        name = "Facebook",
        available = false,
        count = 0,
        average = 0.0,
        estimated = 0.0,
        note = "Not connected yet — excluded from valuation"
    )
    val sources = listOf(ebay, google, gumtree, facebook)
    val (combined, confidence) = consensus(sources.filter { it.available }.map { it.estimated })

    Card(
        colors = CardDefaults.cardColors(containerColor = SourceCard),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("CROSS-SOURCE MARKET CHECK", color = SourceYellow, fontWeight = FontWeight.Black)
            Text(
                if (combined > 0) sourceMoney(combined) else "No cross-source value yet",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )
            Text(confidence, color = Color(0xFFD0D0D0), fontWeight = FontWeight.Bold)
            Text(
                "Only verified exact-model sources can contribute. Similar, rejected and unavailable marketplaces never affect this figure.",
                color = Color(0xFFBEBEBE),
                fontSize = 13.sp
            )
        }
    }

    Text("Source evidence", fontSize = 22.sp, fontWeight = FontWeight.Black)
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        sources.forEach { source -> SourcePanel(source) }
    }
}

@Composable
private fun SourcePanel(source: SourceStats) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B1B)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.width(270.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(source.name, color = SourceYellow, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Surface(
                color = Color(0xFF151515),
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, if (source.available) Color(0xFF238A63) else Color(0xFF666666))
            ) {
                Text(
                    if (source.available) "${source.count} verified" else "Unavailable",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text("ESTIMATED", color = MorleyTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(if (source.estimated > 0) sourceMoney(source.estimated) else "—", fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("AVERAGE", color = MorleyTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(if (source.average > 0) sourceMoney(source.average) else "—", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(source.note, color = Color(0xFFBEBEBE), fontSize = 13.sp)
        }
    }
}
