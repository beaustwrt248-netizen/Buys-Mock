package com.buysloans.hub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

private enum class PhoneBrand { APPLE }

@Composable
fun MobilePhonePricingScreen() {
    var brand by remember { mutableStateOf<PhoneBrand?>(null) }

    if (brand == null) {
        Screen("Mobile Phone Pricing") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MorleySurfaceRaised),
                border = BorderStroke(1.dp, MorleyBorder),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("CHOOSE PHONE BRAND", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MorleyAccent)
                    Text("Which phone brand are you pricing?", fontSize = 23.sp, fontWeight = FontWeight.Black, color = MorleyTextPrimary)
                    Text("Price-sheet categories use Morley's A / B / C grading rules and do not require live marketplace pricing.", color = MorleyTextSecondary, fontSize = 13.sp)
                    OutlinedButton(
                        onClick = { brand = PhoneBrand.APPLE },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MorleyBorder),
                        shape = RoundedCornerShape(999.dp)
                    ) { Text("Apple", color = MorleyTextPrimary, fontWeight = FontWeight.Black) }
                }
            }
        }
    } else {
        Column {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
                OutlinedButton(onClick = { brand = null }, border = BorderStroke(1.dp, MorleyBorder), shape = RoundedCornerShape(14.dp)) {
                    Text("← Mobile Phones", color = MorleyTextPrimary)
                }
            }
            when (brand) {
                PhoneBrand.APPLE -> ApplePhonePricingScreen()
                null -> Unit
            }
        }
    }
}

@Composable
private fun ApplePhonePricingScreen() = Screen("Apple iPhone Pricing") {
    var selectedModel by remember { mutableStateOf<String?>(null) }
    var selectedEntry by remember { mutableStateOf<MobilePhonePriceEntry?>(null) }
    var modelMenuOpen by remember { mutableStateOf(false) }
    var storageMenuOpen by remember { mutableStateOf(false) }
    var grade by remember { mutableStateOf("A") }
    val money = remember { NumberFormat.getCurrencyInstance(Locale("en", "AU")).apply { maximumFractionDigits = 0 } }
    val models = remember { MobilePhonePricingCatalog.models(MobilePhonePricingCatalog.APPLE) }
    val variants = selectedModel?.let { MobilePhonePricingCatalog.variants(MobilePhonePricingCatalog.APPLE, it) }.orEmpty()

    Card(
        colors = CardDefaults.cardColors(containerColor = MorleySurfaceRaised),
        border = BorderStroke(1.dp, MorleyBorder),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("APPLE PRICE SHEET", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MorleyAccent)
            Text("Select iPhone + storage + grade", fontSize = 22.sp, fontWeight = FontWeight.Black, color = MorleyTextPrimary)
            Text("A Grade = 70% • B Grade = 50% • C Grade = 30% of the maintained phone price-sheet value.", color = MorleyTextSecondary, fontSize = 13.sp)
        }
    }

    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { modelMenuOpen = true },
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MorleyBorder),
            shape = RoundedCornerShape(16.dp)
        ) { Text(selectedModel ?: "Choose iPhone model", color = MorleyTextPrimary) }
        DropdownMenu(expanded = modelMenuOpen, onDismissRequest = { modelMenuOpen = false }) {
            models.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model) },
                    onClick = {
                        selectedModel = model
                        selectedEntry = null
                        modelMenuOpen = false
                    }
                )
            }
        }
    }

    if (selectedModel != null) {
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { storageMenuOpen = true },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MorleyBorder),
                shape = RoundedCornerShape(16.dp)
            ) { Text(selectedEntry?.storage ?: "Choose storage", color = MorleyTextPrimary) }
            DropdownMenu(expanded = storageMenuOpen, onDismissRequest = { storageMenuOpen = false }) {
                variants.forEach { entry ->
                    DropdownMenuItem(
                        text = { Text("${entry.storage} — ${money.format(entry.priceSheetValue)}") },
                        onClick = {
                            selectedEntry = entry
                            storageMenuOpen = false
                        }
                    )
                }
            }
        }
    }

    selectedEntry?.let { entry ->
        val buyPrice = MobilePhonePricingCatalog.buyPrice(entry, grade)
        val percentage = (MobilePhonePricingCatalog.gradeBuyPercent.getValue(grade) * 100).toInt()
        Card(
            colors = CardDefaults.cardColors(containerColor = MorleySurfaceRaised),
            border = BorderStroke(1.dp, MorleyBorder),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(entry.displayName, fontSize = 19.sp, fontWeight = FontWeight.Black, color = MorleyTextPrimary)
                Text("PRICE SHEET VALUE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MorleyAccent)
                Text(money.format(entry.priceSheetValue), fontSize = 28.sp, fontWeight = FontWeight.Black, color = MorleyTextPrimary)
                Text("Grade", color = MorleyTextSecondary, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MobilePhonePricingCatalog.grades.forEach { option ->
                        FilterChip(selected = grade == option, onClick = { grade = option }, label = { Text("$option Grade") })
                    }
                }
                Text("AUTO BUY PRICE • $grade GRADE ($percentage%)", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MorleyAccent)
                Text(money.format(buyPrice), fontSize = 30.sp, fontWeight = FontWeight.Black, color = MorleyTextPrimary)
                Text("${money.format(entry.priceSheetValue)} × $percentage% = ${money.format(buyPrice)}", color = MorleyTextSecondary, fontSize = 12.sp)
            }
        }
    }

    Text("Apple price-sheet models", color = MorleyTextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
    models.forEach { model ->
        val modelVariants = MobilePhonePricingCatalog.variants(MobilePhonePricingCatalog.APPLE, model)
        Card(
            onClick = {
                selectedModel = model
                selectedEntry = modelVariants.firstOrNull()
            },
            colors = CardDefaults.cardColors(containerColor = MorleySurfaceRaised),
            border = BorderStroke(1.dp, MorleyBorder.copy(alpha = .75f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(model, color = MorleyTextPrimary, fontWeight = FontWeight.Black)
                Text(modelVariants.joinToString(" • ") { it.storage }, color = MorleyTextSecondary, fontSize = 12.sp)
            }
        }
    }
}
