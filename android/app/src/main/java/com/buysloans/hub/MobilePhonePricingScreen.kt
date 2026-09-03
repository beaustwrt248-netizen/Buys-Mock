package com.buysloans.hub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

private enum class PhoneBrand(val catalogName: String, val title: String) {
    APPLE(MobilePhonePricingCatalog.APPLE, "Apple iPhone"),
    SAMSUNG(MobilePhonePricingCatalog.SAMSUNG, "Samsung Galaxy")
}

@Composable
fun MobilePhonePricingScreen() {
    var brand by remember { mutableStateOf<PhoneBrand?>(null) }
    if (brand == null) {
        Screen("Mobile Phones") {
            Text("Choose a brand to continue.", color = MorleyTextSecondary, fontSize = 15.sp)
            BrandCard("Apple", highlighted = true) { brand = PhoneBrand.APPLE }
            BrandCard("Samsung") { brand = PhoneBrand.SAMSUNG }
            BrandCard("Google") {}
            BrandCard("OnePlus") {}
            BrandCard("Xiaomi") {}
            BrandCard("Other Brands") {}
        }
    } else {
        Column {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
                OutlinedButton(onClick = { brand = null }, border = BorderStroke(1.dp, MorleyBorder), shape = RoundedCornerShape(14.dp)) {
                    Text("‹  Mobile Phones", color = MorleyTextPrimary, fontWeight = FontWeight.Bold)
                }
            }
            PhoneBrandPricingScreen(brand!!)
        }
    }
}

@Composable
private fun BrandCard(title: String, highlighted: Boolean = false, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = if (highlighted) MorleyAccentSoft.copy(alpha = .34f) else MorleySurface),
        border = BorderStroke(1.dp, if (highlighted) MorleyAccent else MorleyBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PhoneBrandVisual(title, Modifier.size(34.dp))
            Text(
                title,
                modifier = Modifier.weight(1f),
                color = if (highlighted) MorleyAccent else MorleyTextPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp
            )
            Text("›", color = if (highlighted) MorleyAccent else MorleyTextSecondary, fontSize = 25.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PhoneBrandPricingScreen(brand: PhoneBrand) {
    var query by remember(brand) { mutableStateOf("") }
    var selectedModel by remember(brand) { mutableStateOf<String?>(null) }
    var selectedEntry by remember(brand) { mutableStateOf<MobilePhonePriceEntry?>(null) }
    var grade by remember(brand) { mutableStateOf("A") }
    val money = remember { NumberFormat.getCurrencyInstance(Locale("en", "AU")).apply { maximumFractionDigits = 0 } }
    val allModels = remember(brand) { MobilePhonePricingCatalog.models(brand.catalogName) }
    val visibleModels = remember(query, allModels) { if (query.isBlank()) allModels else allModels.filter { it.contains(query, ignoreCase = true) } }

    if (selectedModel == null) {
        Screen(brand.title) {
            Text("Select a model to view the A-B-C price-sheet pricing.", color = MorleyTextSecondary, fontSize = 14.sp)
            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Search ${if (brand == PhoneBrand.APPLE) "iPhone" else "Galaxy"} model") }, shape = RoundedCornerShape(16.dp))
            visibleModels.forEach { model ->
                val variants = MobilePhonePricingCatalog.variants(brand.catalogName, model)
                Card(onClick = { selectedModel = model; selectedEntry = variants.firstOrNull() }, colors = CardDefaults.cardColors(containerColor = MorleySurface), border = BorderStroke(1.dp, MorleyBorder), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(15.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(color = MorleyAccentSoft, shape = RoundedCornerShape(10.dp)) {
                            PhoneBrandVisual(brand.catalogName, Modifier.padding(7.dp).size(38.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(model, color = MorleyTextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text(variants.joinToString(" • ") { it.storage }, color = MorleyTextSecondary, fontSize = 11.sp)
                        }
                        Text("›", color = MorleyTextSecondary, fontSize = 25.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    } else {
        val variants = MobilePhonePricingCatalog.variants(brand.catalogName, selectedModel!!)
        val current = selectedEntry ?: variants.firstOrNull()
        Screen(selectedModel!!) {
            OutlinedButton(onClick = { selectedModel = null; selectedEntry = null }, border = BorderStroke(1.dp, MorleyBorder), shape = RoundedCornerShape(14.dp)) {
                Text("‹  ${brand.title}", color = MorleyTextPrimary, fontWeight = FontWeight.Bold)
            }
            Card(colors = CardDefaults.cardColors(containerColor = MorleySurfaceRaised), border = BorderStroke(1.dp, MorleyBorder), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Surface(color = MorleyAccentSoft, shape = RoundedCornerShape(14.dp)) { PhoneBrandVisual(brand.catalogName, Modifier.padding(10.dp).size(42.dp)) }
                        Column(Modifier.weight(1f)) {
                            Text(selectedModel!!, color = MorleyTextPrimary, fontWeight = FontWeight.Black, fontSize = 21.sp)
                            Text("Select storage and condition grade", color = MorleyTextSecondary, fontSize = 12.sp)
                        }
                    }
                    Text("Select Storage Capacity", color = MorleyTextPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        variants.forEach { entry -> FilterChip(selected = current?.storage == entry.storage, onClick = { selectedEntry = entry }, label = { Text(entry.storage.replace(" ", "")) }) }
                    }
                    Text("Select Condition Grade", color = MorleyTextPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MobilePhonePricingCatalog.grades.forEach { option ->
                            val label = when (option) { "A" -> "Excellent"; "B" -> "Good"; else -> "Fair" }
                            FilterChip(selected = grade == option, onClick = { grade = option }, label = { Text("$option  $label") })
                        }
                    }
                }
            }
            current?.let { entry ->
                val buyPrice = MobilePhonePricingCatalog.buyPrice(entry, grade)
                val percentage = (MobilePhonePricingCatalog.gradeBuyPercent.getValue(grade) * 100).toInt()
                Card(colors = CardDefaults.cardColors(containerColor = MorleySurfaceRaised), border = BorderStroke(1.dp, MorleyBorder), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text("Quick Summary", color = MorleyTextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        SummaryRow("Brand", entry.brand)
                        SummaryRow("Model", entry.model)
                        SummaryRow("Storage", entry.storage)
                        SummaryRow("Grade", "$grade — ${when (grade) { "A" -> "Excellent"; "B" -> "Good"; else -> "Fair" }}")
                        SummaryRow("Price sheet", money.format(entry.priceSheetValue))
                        Text("BUY PRICE • $grade GRADE ($percentage%)", color = MorleyAccent, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Text(money.format(buyPrice), color = MorleyTextPrimary, fontSize = 34.sp, fontWeight = FontWeight.Black)
                        Text("${money.format(entry.priceSheetValue)} × $percentage% = ${money.format(buyPrice)}", color = MorleyTextSecondary, fontSize = 12.sp)
                    }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MorleySurface), border = BorderStroke(1.dp, MorleyBorder), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Grade Guide", color = MorleyTextPrimary, fontWeight = FontWeight.Black)
                    Text("A — Excellent: like new, fully functional, no material damage.", color = MorleyTextSecondary, fontSize = 12.sp)
                    Text("B — Good: light wear, fully functional.", color = MorleyTextSecondary, fontSize = 12.sp)
                    Text("C — Fair: heavier visible wear, fully functional.", color = MorleyTextSecondary, fontSize = 12.sp)
                    Text("Prices are maintained in the app price sheet and can be updated as needed. Final offer may vary after inspection.", color = MorleyAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MorleyTextSecondary, fontSize = 12.sp)
        Text(value, color = MorleyTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
