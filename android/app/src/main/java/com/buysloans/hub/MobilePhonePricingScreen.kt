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

@Suppress("unused")
private enum class MobilePhonePricingContract(val catalogName: String) {
    SAMSUNG(MobilePhonePricingCatalog.SAMSUNG)
}

private val featuredPhoneBrands = listOf("Apple", "Samsung", "Google", "OnePlus", "Xiaomi")
private fun otherPhoneBrands(): List<String> =
    MobilePhoneDeviceCatalog.brands().filterNot { brand -> brand in featuredPhoneBrands }

private fun phoneBrandTitle(brand: String): String = when (brand) {
    "Apple" -> "Apple iPhone"
    "Samsung" -> "Samsung Galaxy"
    "Google" -> "Google Pixel"
    "Sony" -> "Sony Xperia"
    else -> brand
}

private fun phoneBrandVisualName(brand: String): String = when (brand) {
    "Apple", "Samsung", "Google", "OnePlus", "Xiaomi" -> brand
    else -> "Other Brands"
}

@Composable
fun MobilePhonePricingScreen() {
    var globalQuery by remember { mutableStateOf("") }
    var selectedBrand by remember { mutableStateOf<String?>(null) }
    var selectedModelFromSearch by remember { mutableStateOf<String?>(null) }
    var showingOtherBrands by remember { mutableStateOf(false) }

    when {
        selectedBrand != null -> {
            Column {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
                    OutlinedButton(
                        onClick = {
                            selectedBrand = null
                            selectedModelFromSearch = null
                        },
                        border = BorderStroke(1.dp, MorleyBorder),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("‹  Mobile Phones", color = MorleyTextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
                PhoneBrandPricingScreen(selectedBrand!!, initialModel = selectedModelFromSearch)
            }
        }
        showingOtherBrands -> {
            Column {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
                    OutlinedButton(
                        onClick = { showingOtherBrands = false },
                        border = BorderStroke(1.dp, MorleyBorder),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("‹  Mobile Phones", color = MorleyTextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
                Screen("Other Phone Brands") {
                    Text("Choose a brand to continue.", color = MorleyTextSecondary, fontSize = 15.sp)
                    otherPhoneBrands().forEach { brand ->
                        BrandCard(brand) {
                            selectedBrand = brand
                            selectedModelFromSearch = null
                        }
                    }
                }
            }
        }
        else -> {
            val globalResults = remember(globalQuery) { MobilePhoneDeviceCatalog.search(globalQuery) }
            Screen("Mobile Phones") {
                Text(
                    "Search every mobile phone category, or choose a brand to browse.",
                    color = MorleyTextSecondary,
                    fontSize = 15.sp
                )
                OutlinedTextField(
                    value = globalQuery,
                    onValueChange = { globalQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Search all mobile phones") },
                    supportingText = { Text("Brand, model, model number or storage") },
                    shape = RoundedCornerShape(16.dp)
                )

                if (globalQuery.isNotBlank()) {
                    Text(
                        if (globalResults.isEmpty()) "No matching mobile phones" else "${globalResults.size} matching phone${if (globalResults.size == 1) "" else "s"}",
                        color = MorleyTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    globalResults.forEach { result ->
                        GlobalPhoneSearchResultCard(result) {
                            selectedBrand = result.brand
                            selectedModelFromSearch = result.model
                        }
                    }
                } else {
                    featuredPhoneBrands.filter { it in MobilePhoneDeviceCatalog.brands() }.forEach { brand ->
                        BrandCard(brand, highlighted = brand == "Apple") {
                            selectedBrand = brand
                            selectedModelFromSearch = null
                        }
                    }
                    BrandCard("Other Brands") { showingOtherBrands = true }
                }
            }
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
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (title == "Other Brands") {
                PhoneBrandVisual("Other Brands", Modifier.size(42.dp))
            } else {
                MobilePhonePhoto(
                    brand = title,
                    categoryRepresentative = true,
                    modifier = Modifier.size(48.dp)
                )
            }
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
private fun GlobalPhoneSearchResultCard(result: MobilePhoneSearchResult, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MorleySurface),
        border = BorderStroke(1.dp, MorleyBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = MorleyAccentSoft, shape = RoundedCornerShape(12.dp)) {
                MobilePhonePhoto(
                    brand = result.brand,
                    model = result.model,
                    modifier = Modifier.padding(5.dp).size(50.dp)
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(result.model, color = MorleyTextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(result.brand, color = MorleyAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                result.modelNumber?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = MorleyTextSecondary, fontSize = 11.sp)
                }
                Text(result.storages.joinToString(" • "), color = MorleyTextSecondary, fontSize = 11.sp)
                if (result.hasPricedVariant) {
                    Text("Morley pricing available", color = MorleyAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text("›", color = MorleyTextSecondary, fontSize = 25.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PhoneBrandPricingScreen(brand: String, initialModel: String? = null) {
    var query by remember(brand) { mutableStateOf("") }
    var selectedModel by remember(brand, initialModel) { mutableStateOf(initialModel) }
    var selectedEntry by remember(brand, initialModel) {
        mutableStateOf(initialModel?.let { MobilePhoneDeviceCatalog.variants(brand, it).firstOrNull() })
    }
    var grade by remember(brand) { mutableStateOf("A") }
    val money = remember { NumberFormat.getCurrencyInstance(Locale("en", "AU")).apply { maximumFractionDigits = 0 } }
    val allModels = remember(brand) { MobilePhoneDeviceCatalog.models(brand) }
    val visibleModels = remember(query, allModels, brand) {
        allModels.filter { MobilePhoneDeviceCatalog.modelMatches(brand, it, query) }
    }
    val brandTitle = phoneBrandTitle(brand)

    if (selectedModel == null) {
        Screen(brandTitle) {
            Text("Select a model and storage. Pricing is shown where a Morley price-sheet value already exists.", color = MorleyTextSecondary, fontSize = 14.sp)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search model, model number or storage") },
                shape = RoundedCornerShape(16.dp)
            )
            visibleModels.forEach { model ->
                val variants = MobilePhoneDeviceCatalog.variants(brand, model)
                Card(
                    onClick = { selectedModel = model; selectedEntry = variants.firstOrNull() },
                    colors = CardDefaults.cardColors(containerColor = MorleySurface),
                    border = BorderStroke(1.dp, MorleyBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(13.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(color = MorleyAccentSoft, shape = RoundedCornerShape(10.dp)) {
                            MobilePhonePhoto(
                                brand = brand,
                                model = model,
                                modifier = Modifier.padding(5.dp).size(50.dp)
                            )
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(model, color = MorleyTextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            variants.mapNotNull { it.modelNumber }.firstOrNull()?.takeIf { it.isNotBlank() }?.let {
                                Text(it, color = MorleyTextSecondary, fontSize = 10.sp)
                            }
                            Text(variants.joinToString(" • ") { it.storage }, color = MorleyTextSecondary, fontSize = 11.sp)
                        }
                        Text("›", color = MorleyTextSecondary, fontSize = 25.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    } else {
        val variants = MobilePhoneDeviceCatalog.variants(brand, selectedModel!!)
        val current = selectedEntry ?: variants.firstOrNull()
        val pricedCurrent = current?.let { MobilePhoneDeviceCatalog.pricedEntry(it) }
        Screen(selectedModel!!) {
            OutlinedButton(
                onClick = { selectedModel = null; selectedEntry = null },
                border = BorderStroke(1.dp, MorleyBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("‹  $brandTitle", color = MorleyTextPrimary, fontWeight = FontWeight.Bold)
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MorleySurfaceRaised),
                border = BorderStroke(1.dp, MorleyBorder),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = MorleyAccentSoft, shape = RoundedCornerShape(14.dp)) {
                            MobilePhonePhoto(
                                brand = brand,
                                model = selectedModel!!,
                                modifier = Modifier.padding(7.dp).size(72.dp)
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(selectedModel!!, color = MorleyTextPrimary, fontWeight = FontWeight.Black, fontSize = 21.sp)
                            current?.modelNumber?.takeIf { it.isNotBlank() }?.let {
                                Text(it, color = MorleyTextSecondary, fontSize = 11.sp)
                            }
                            Text("Select storage${if (pricedCurrent != null) " and condition grade" else ""}", color = MorleyTextSecondary, fontSize = 12.sp)
                        }
                    }
                    Text("Select Storage Capacity", color = MorleyTextPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        variants.forEach { entry ->
                            FilterChip(
                                selected = current?.storage == entry.storage,
                                onClick = { selectedEntry = entry },
                                label = { Text(entry.storage.replace(" ", "")) }
                            )
                        }
                    }
                    if (pricedCurrent != null) {
                        Text("Select Condition Grade", color = MorleyTextPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MobilePhonePricingCatalog.grades.forEach { option ->
                                val label = when (option) { "A" -> "Excellent"; "B" -> "Good"; else -> "Fair" }
                                FilterChip(selected = grade == option, onClick = { grade = option }, label = { Text("$option  $label") })
                            }
                        }
                    }
                }
            }

            if (pricedCurrent != null) {
                val buyPrice = MobilePhonePricingCatalog.buyPrice(pricedCurrent, grade)
                val percentage = (MobilePhonePricingCatalog.gradeBuyPercent.getValue(grade) * 100).toInt()
                Card(
                    colors = CardDefaults.cardColors(containerColor = MorleySurfaceRaised),
                    border = BorderStroke(1.dp, MorleyBorder),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text("Quick Summary", color = MorleyTextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        SummaryRow("Brand", pricedCurrent.brand)
                        SummaryRow("Model", pricedCurrent.model)
                        SummaryRow("Storage", pricedCurrent.storage)
                        SummaryRow("Grade", "$grade — ${when (grade) { "A" -> "Excellent"; "B" -> "Good"; else -> "Fair" }}")
                        SummaryRow("Price sheet", money.format(pricedCurrent.priceSheetValue))
                        Text("BUY PRICE • $grade GRADE ($percentage%)", color = MorleyAccent, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        Text(money.format(buyPrice), color = MorleyTextPrimary, fontSize = 34.sp, fontWeight = FontWeight.Black)
                        Text("${money.format(pricedCurrent.priceSheetValue)} × $percentage% = ${money.format(buyPrice)}", color = MorleyTextSecondary, fontSize = 12.sp)
                    }
                }
            } else if (current != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MorleySurfaceRaised),
                    border = BorderStroke(1.dp, MorleyBorder),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Model Added", color = MorleyTextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        SummaryRow("Brand", current.brand)
                        SummaryRow("Model", current.model)
                        current.modelNumber?.takeIf { it.isNotBlank() }?.let { SummaryRow("Model number", it) }
                        SummaryRow("Storage", current.storage)
                        Text("Pricing has not been added for this storage variant yet.", color = MorleyAccent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (pricedCurrent != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MorleySurface),
                    border = BorderStroke(1.dp, MorleyBorder),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
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
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MorleyTextSecondary, fontSize = 12.sp)
        Text(value, color = MorleyTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}