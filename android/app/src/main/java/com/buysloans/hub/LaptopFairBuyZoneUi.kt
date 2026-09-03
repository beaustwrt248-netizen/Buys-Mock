package com.buysloans.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun LaptopFairBuyZonePanel(
    preset: LaptopPreset,
    processor: String,
    ram: String,
    storage: String,
    modelCode: String,
    market: MarketResult
) {
    val shadow = LaptopMarketResultIntelligence.evaluateShadow(
        preset = preset,
        processor = processor,
        ram = ram,
        storage = storage,
        modelCode = modelCode,
        market = market
    )
    val zone = shadow.fairBuyZone
    val accepted = zone.comparables.filter { it.accepted }
    val rejected = zone.comparables.filterNot { it.accepted }
    val exact = accepted.count { it.score >= 95 }
    val strong = accepted.count { it.score in 85..94 }
    val adjusted = accepted.count { it.score in 1..84 }

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        HorizontalDivider()
        Text("FAIR BUY ZONE • SHADOW MODE", fontWeight = FontWeight.Black, fontSize = 14.sp)
        Text(
            "Read-only intelligence check. It does not change Morley's live offer or maximum buy price.",
            color = MorleyTextSecondary,
            fontSize = 12.sp
        )

        if (zone.marketValue > 0.0) {
            Block(
                "INTELLIGENCE MARKET VALUE",
                "$${zone.marketValue.roundToInt()} AUD • quick-sale $${zone.quickSaleValue.roundToInt()} • confidence ${zone.confidence}%"
            )
            Block(
                "FAIR BUY ZONE",
                "Open $${zone.opening} • Target $${zone.recommended} • Competitive $${zone.competitive} • Hard max $${zone.hardMaximum}"
            )
        } else {
            Block("INTELLIGENCE STATUS", "${zone.decision} • confidence ${zone.confidence}%")
        }

        Text(
            "Comparables: $exact exact • $strong strong • $adjusted adjusted • ${rejected.size} rejected",
            color = MorleyTextSecondary,
            fontSize = 12.sp
        )

        zone.reasons.forEach { reason ->
            Text("• $reason", color = MorleyTextSecondary, fontSize = 12.sp)
        }

        val examples = (accepted.sortedByDescending { it.score }.take(3) + rejected.take(3)).distinctBy { it.comparable.id }
        if (examples.isNotEmpty()) {
            Text("Why listings were included or rejected", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            examples.forEach { decision ->
                val state = if (decision.accepted) "INCLUDED ${decision.score}%" else "REJECTED"
                Text(
                    "$state • ${decision.reason} • ${decision.comparable.source} • $${decision.comparable.priceAud.roundToInt()} • ${decision.comparable.id}",
                    color = MorleyTextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        if (zone.comparables.isNotEmpty() && zone.comparables.none { it.comparable.sold }) {
            Text(
                "Sold-history evidence is not verified by the current live search contract, so active asking prices cannot authorize an automatic buy.",
                color = MorleyTextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
