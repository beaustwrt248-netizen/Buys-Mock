package com.buysloans.hub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

private fun gpMoney(value: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "AU")).apply {
    maximumFractionDigits = 0
}.format(value)

@Composable
fun GPFix() {
    var sale by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("A") }
    val pct = when (grade) {
        "B" -> 50.0
        "C" -> 70.0
        else -> 30.0
    }
    val saleValue = sale.toDoubleOrNull() ?: 0.0
    val cost = saleValue * (1 - pct / 100.0)
    val profit = saleValue - cost

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("GP Calculator", fontSize = 26.sp, fontWeight = FontWeight.Black, color = MorleyTextPrimary)
        Text("Gross profit targets at a glance", fontSize = 13.sp, color = MorleyTextSecondary)

        MetricGrid(
            listOf(
                Triple("SALE PRICE", gpMoney(saleValue), false),
                Triple("COST PRICE", gpMoney(cost), false),
                Triple("TARGET PROFIT", gpMoney(profit), false),
                Triple("TARGET GP", "${pct.toInt()}%", true)
            )
        )

        OutlinedTextField(
            value = sale,
            onValueChange = { sale = it },
            label = { Text("Sale price") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MorleyAccent,
                focusedLabelColor = MorleyAccent,
                unfocusedBorderColor = MorleyBorder,
                focusedTextColor = MorleyTextPrimary,
                unfocusedTextColor = MorleyTextPrimary
            )
        )

        Text("Grade / GP target", color = MorleyTextPrimary, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("A", "B", "C", "Luxury").forEach { option ->
                FilterChip(
                    selected = grade == option,
                    onClick = { grade = option },
                    modifier = Modifier.weight(if (option == "Luxury") 1.35f else 1f),
                    label = {
                        Text(
                            option,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MorleyAccentStrong,
                        selectedLabelColor = androidx.compose.ui.graphics.Color.White,
                        containerColor = MorleySurfaceRaised,
                        labelColor = MorleyTextPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = grade == option,
                        borderColor = MorleyBorder,
                        selectedBorderColor = MorleyAccent
                    )
                )
            }
        }

        GpInfoCard(
            "${grade.uppercase()} — ${pct.toInt()}% GP",
            "Cost price: ${gpMoney(cost)}\nProfit: ${gpMoney(profit)}"
        )

        Text("Item grades", fontSize = 22.sp, fontWeight = FontWeight.Black, color = MorleyTextPrimary)

        GradeCard("A — 30% GP", listOf(
            "Newest 2 generations of iPhone + iPad / Samsung phones + tablets",
            "Newest game consoles (PS5, Nintendo, Xbox)",
            "Newest MacBook / laptop (high end)",
            "Latest smartwatches"
        ))
        GradeCard("B — 50% GP", listOf(
            "Older iPhone + iPad / Samsung phones + tablets",
            "Tools (Milwaukee, AEG, Makita, DeWalt — depending on condition + accessories)",
            "Laptops (middle range)",
            "Music equipment (depending on condition)",
            "High + mid-range speakers / Bluetooth speakers",
            "Bluetooth headphones",
            "Sunglasses over $200 new",
            "High-end camera equipment",
            "Drones (high end)",
            "Electric + acoustic guitars"
        ))
        GradeCard("C — 70% GP", listOf(
            "Pop vinyls",
            "Old consoles",
            "Cheap mobiles",
            "DVD players",
            "Tools — Ozito, old Makita / DeWalt (depending on condition)",
            "Older smartwatches",
            "Older than 5+ years MacBook / laptop",
            "General household items (usually cheap)",
            "Golf sets",
            "Skateboards",
            "Shoes",
            "Fans",
            "Cheap speakers / Bluetooth speakers",
            "Gaming headsets",
            "Sunglasses under $200 new",
            "General camera gear",
            "Fishing rods / reels",
            "Drones (mid + low end)"
        ))
        GradeCard("LUXURY — 30% GP", listOf("Luxury uses the 30% GP target."))
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MetricGrid(items: List<Triple<String, String, Boolean>>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (label, value, isPercent) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MorleySurfaceRaised),
                        border = BorderStroke(1.dp, MorleyBorder.copy(alpha = .75f)),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(label, color = MorleyTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(
                                value,
                                color = if (isPercent) MorleyAccent else MorleyTextPrimary,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GpInfoCard(title: String, body: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MorleySurfaceRaised),
        border = BorderStroke(1.dp, MorleyBorder.copy(alpha = .75f)),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = MorleyAccent, fontWeight = FontWeight.Black)
            Text(body, color = MorleyTextSecondary, lineHeight = 22.sp)
        }
    }
}

@Composable
private fun GradeCard(title: String, items: List<String>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MorleySurfaceRaised),
        border = BorderStroke(1.dp, MorleyBorder.copy(alpha = .55f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = MorleyAccent, fontWeight = FontWeight.Black)
            items.forEach { Text("• $it", color = MorleyTextSecondary, fontSize = 13.sp, lineHeight = 18.sp) }
        }
    }
}
