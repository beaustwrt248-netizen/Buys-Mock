package com.buysloans.hub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

private fun integratedMoney(value: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "AU")).apply {
    maximumFractionDigits = 0
}.format(value)

@Composable
fun IntegratedMarketEvidencePanel(value: IntegratedMarketValue, loading: Boolean = false) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF07172C)),
        border = BorderStroke(1.dp, Color(0xFF235A91)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("MARKET EVIDENCE", color = Color(0xFF12C9FF), fontWeight = FontWeight.Black, fontSize = 11.sp)
            if (loading) {
                Text("Checking Australian market sources…", color = Color(0xFF8FA6C6))
            } else {
                val protectedReady = value.usedValue > 0.0 && value.confidence in setOf("MEDIUM", "HIGH")
                Text(
                    if (protectedReady) "Protected used-market value: ${integratedMoney(value.usedValue)}"
                    else "Insufficient exact market evidence for a protected valuation",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 19.sp
                )
                if (!protectedReady && value.sources.isNotEmpty()) {
                    Text(
                        "Reference evidence below is not used to set the used value or max buy.",
                        color = Color(0xFFFFC266),
                        fontSize = 12.sp
                    )
                }
                value.sources.forEach { source ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(source.source, color = Color(0xFFDCE9FF), fontWeight = FontWeight.SemiBold)
                        Text("${integratedMoney(source.value)}  •  n=${source.sampleSize}", color = Color(0xFF8FA6C6))
                    }
                }
                if (value.excludedSources.isNotEmpty()) {
                    Text("Outlier sources excluded: ${value.excludedSources.joinToString()}", color = Color(0xFFFFA726), fontSize = 12.sp)
                }
                val confidenceColor = when (value.confidence) {
                    "HIGH", "MEDIUM" -> Color(0xFF25D991)
                    "LOW" -> Color(0xFFFFC266)
                    else -> Color(0xFF8FA6C6)
                }
                Text("Confidence: ${value.confidence}", color = confidenceColor, fontWeight = FontWeight.Bold)
            }
        }
    }

    val evidence = value.marketplaceEvidence ?: return
    val gumtreeExact = evidence.gumtree.filter { it.exact }
    val facebookExact = evidence.facebook.filter { it.exact }
    if (gumtreeExact.isNotEmpty() || facebookExact.isNotEmpty()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF061327)),
            border = BorderStroke(1.dp, Color(0xFF143A63)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("MARKETPLACE EXACT CONFIGURATION MATCHES", color = Color(0xFF9B4DFF), fontWeight = FontWeight.Black, fontSize = 11.sp)
                gumtreeExact.take(6).forEach { item ->
                    Text("Gumtree • ${integratedMoney(item.price)} • ${item.title}", color = Color(0xFFDCE9FF), fontSize = 12.sp)
                }
                facebookExact.take(6).forEach { item ->
                    Text("Facebook • ${integratedMoney(item.price)} • ${item.title}", color = Color(0xFFDCE9FF), fontSize = 12.sp)
                }
            }
        }
    }
}
