package com.buysloans.hub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

private enum class ComputerType { LAPTOP, DESKTOP }

@Composable
fun ComputerPricingScreen() {
    var type by remember { mutableStateOf<ComputerType?>(null) }
    if (type == null) {
        Screen("Computer Pricing") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MorleySurfaceRaised),
                border = BorderStroke(1.dp, MorleyBorder),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("CHOOSE COMPUTER TYPE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MorleyAccent)
                    Text("What are you pricing?", fontSize = 23.sp, fontWeight = FontWeight.Black, color = MorleyTextPrimary)
                    Text(
                        "Choose Laptop / MacBook for guided exact-model pricing, or Desktop / Gaming PC for component-based valuation.",
                        color = MorleyTextSecondary,
                        fontSize = 13.sp
                    )
                    Button(
                        onClick = { type = ComputerType.LAPTOP },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MorleyAccentStrong, contentColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Laptop / MacBook", fontWeight = FontWeight.Black) }
                    OutlinedButton(
                        onClick = { type = ComputerType.DESKTOP },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MorleyBorder),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Desktop / Gaming PC", color = MorleyTextPrimary, fontWeight = FontWeight.Black) }
                }
            }
        }
    } else {
        Column {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
                OutlinedButton(onClick = { type = null }, border = BorderStroke(1.dp, MorleyBorder)) {
                    Text("← Change computer type", color = MorleyTextPrimary)
                }
            }
            when (type) {
                ComputerType.LAPTOP -> LaptopGuidedScreen()
                ComputerType.DESKTOP -> Desktop()
                null -> Unit
            }
        }
    }
}

@Composable
fun ConsolePricingScreen() = Screen("Console Pricing") {
    var selected by remember { mutableStateOf<ConsoleDeviceEntry?>(null) }
    var grade by remember { mutableStateOf("A") }
    var query by remember { mutableStateOf("") }
    val money = remember { NumberFormat.getCurrencyInstance(Locale("en", "AU")) }
    val visible = remember(query) { ConsolePricingCatalog.search(query) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MorleySurfaceRaised),
            border = BorderStroke(1.dp, MorleyBorder),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("CONSOLE CATALOGUE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MorleyAccent)
                Text("Find a console by family or series", fontSize = 22.sp, fontWeight = FontWeight.Black, color = MorleyTextPrimary)
                Text(
                    "Consoles are grouped newest-first by family and series. Existing Morley prices remain active; catalogue-only models are marked Price to be added and cannot calculate a buy price.",
                    color = MorleyTextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search all consoles") },
            supportingText = { Text("Try PS5 Slim, Xbox Series X, Nintendo DS or Game Boy") },
            shape = RoundedCornerShape(16.dp)
        )

        selected?.let { console ->
            val percentage = (ConsolePricingCatalog.gradeBuyPercent.getValue(grade) * 100).toInt()
            val buyPrice = ConsolePricingCatalog.buyPrice(console, grade)
            Card(
                colors = CardDefaults.cardColors(containerColor = MorleySurfaceRaised),
                border = BorderStroke(1.dp, MorleyBorder),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(console.name, fontSize = 19.sp, fontWeight = FontWeight.Black, color = MorleyTextPrimary)
                    Text("${console.family} • ${console.series}", color = MorleyTextSecondary, fontSize = 12.sp)
                    if (console.priceSheetValue != null && buyPrice != null) {
                        Text("PRICE SHEET VALUE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MorleyAccent)
                        Text(money.format(console.priceSheetValue), fontSize = 28.sp, fontWeight = FontWeight.Black, color = MorleyTextPrimary)
                        Text("Condition grade", color = MorleyTextSecondary, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ConsolePricingCatalog.grades.forEach { option ->
                                FilterChip(selected = grade == option, onClick = { grade = option }, label = { Text("$option Grade") })
                            }
                        }
                        Text("AUTO BUY PRICE • $grade GRADE ($percentage%)", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MorleyAccent)
                        Text(money.format(buyPrice), fontSize = 30.sp, fontWeight = FontWeight.Black, color = MorleyTextPrimary)
                        Text("${money.format(console.priceSheetValue)} × $percentage% = ${money.format(buyPrice)}", color = MorleyTextSecondary, fontSize = 12.sp)
                    } else {
                        Text("PRICE TO BE ADDED", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MorleyAccent)
                        Text(
                            "This model is in the Morley catalogue but has no approved price-sheet value yet. No buy price will be calculated.",
                            color = MorleyTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        if (visible.isEmpty()) {
            Text("No consoles match ‘${query.trim()}’.", color = MorleyTextSecondary)
        } else {
            ConsolePricingCatalog.families().forEach { family ->
                val familyEntries = visible.filter { it.family == family }
                if (familyEntries.isNotEmpty()) {
                    Text(family, color = MorleyTextPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    ConsolePricingCatalog.series(family).forEach { series ->
                        val seriesEntries = familyEntries.filter { it.series == series }
                        if (seriesEntries.isNotEmpty()) {
                            Text(series, color = MorleyAccent, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            seriesEntries.forEach { entry ->
                                Card(
                                    onClick = { selected = entry },
                                    colors = CardDefaults.cardColors(containerColor = MorleySurfaceRaised),
                                    border = BorderStroke(1.dp, MorleyBorder.copy(alpha = .75f)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(entry.name, modifier = Modifier.weight(1f), color = MorleyTextPrimary, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            entry.priceSheetValue?.let(money::format) ?: "Price to be added",
                                            color = if (entry.hasPrice) MorleyAccent else MorleyTextSecondary,
                                            fontWeight = FontWeight.Black,
                                            fontSize = if (entry.hasPrice) 14.sp else 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
