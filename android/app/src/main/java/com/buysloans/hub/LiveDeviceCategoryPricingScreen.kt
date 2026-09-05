package com.buysloans.hub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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

private val genericGradePercent = mapOf("A" to .70, "B" to .50, "C" to .30)

@Composable
fun LiveDeviceCategoryPricingScreen(category: String) {
    var query by remember(category) { mutableStateOf("") }
    var selectedDevice by remember(category) { mutableStateOf<LiveDeviceCatalogueRow?>(null) }
    var selectedStorage by remember(selectedDevice?.id) { mutableStateOf<String?>(null) }
    var grade by remember(selectedDevice?.id) { mutableStateOf("A") }
    val all = LiveDevicePricing.catalogue()
    val devices = remember(all, category, query) { filterCatalogueDevices(all, category, query) }
    val title = if (normalizeCatalogueCategory(category) == "wearable") "Smart Watches & Wearables" else "Tablets"
    val money = remember { NumberFormat.getCurrencyInstance(Locale("en", "AU")).apply { maximumFractionDigits = 0 } }

    if (selectedDevice == null) {
        Screen(title) {
            Text("Select a device to choose storage and condition grade.", color = MorleyTextSecondary, fontSize = 14.sp)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search catalogue") },
                supportingText = { Text("Brand, model, model number or storage") },
                shape = RoundedCornerShape(16.dp),
            )
            Text("${devices.size} device${if (devices.size == 1) "" else "s"}", color = MorleyAccent, fontSize = 12.sp, fontWeight = FontWeight.Black)
            devices.forEach { device ->
                val hasPrice = device.storageOptions.any { LiveDevicePricing.find(device.brand, device.model, device.modelNumber, it) != null }
                Card(
                    onClick = {
                        selectedDevice = device
                        selectedStorage = device.storageOptions.firstOrNull()
                    },
                    colors = CardDefaults.cardColors(containerColor = MorleySurfaceRaised),
                    border = BorderStroke(1.dp, MorleyBorder),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(color = MorleyAccentSoft, shape = RoundedCornerShape(14.dp)) {
                            DeviceCataloguePhoto(
                                brand = device.brand,
                                model = device.model,
                                imageReferenceUrl = device.imageReferenceUrl,
                                modifier = Modifier.padding(6.dp).size(64.dp),
                            ) {
                                Text(device.brand.take(2).uppercase(), modifier = Modifier.padding(16.dp), color = MorleyAccent, fontWeight = FontWeight.Black)
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(device.model, color = MorleyTextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text(device.brand, color = MorleyAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            device.modelNumber?.let { Text(it, color = MorleyTextSecondary, fontSize = 11.sp) }
                            if (device.storageOptions.isNotEmpty()) Text(device.storageOptions.joinToString(" • "), color = MorleyTextSecondary, fontSize = 11.sp)
                            Text(if (hasPrice) "Morley pricing available" else "Pricing not set", color = if (hasPrice) MorleyAccent else MorleyTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("›", color = MorleyTextSecondary, fontSize = 25.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        return
    }

    val device = selectedDevice!!
    val storages = device.storageOptions.ifEmpty { listOf("") }
    val currentStorage = selectedStorage ?: storages.first()
    val price = LiveDevicePricing.find(device.brand, device.model, device.modelNumber, currentStorage)
    val pct = genericGradePercent.getValue(grade)
    val buyPrice = price?.priceAud?.times(pct)

    Screen(device.model) {
        OutlinedButton(
            onClick = { selectedDevice = null; selectedStorage = null },
            border = BorderStroke(1.dp, MorleyBorder),
            shape = RoundedCornerShape(14.dp),
        ) { Text("‹  $title", color = MorleyTextPrimary, fontWeight = FontWeight.Bold) }

        Card(
            colors = CardDefaults.cardColors(containerColor = MorleySurfaceRaised),
            border = BorderStroke(1.dp, MorleyBorder),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = MorleyAccentSoft, shape = RoundedCornerShape(14.dp)) {
                        DeviceCataloguePhoto(device.brand, device.model, device.imageReferenceUrl, Modifier.padding(7.dp).size(72.dp)) {
                            Text(device.brand.take(2).uppercase(), modifier = Modifier.padding(16.dp), color = MorleyAccent, fontWeight = FontWeight.Black)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(device.model, color = MorleyTextPrimary, fontWeight = FontWeight.Black, fontSize = 21.sp)
                        device.modelNumber?.let { Text(it, color = MorleyTextSecondary, fontSize = 11.sp) }
                        Text("Select storage and condition grade", color = MorleyTextSecondary, fontSize = 12.sp)
                    }
                }

                if (storages.any { it.isNotBlank() }) {
                    Text("Select Storage Capacity", color = MorleyTextPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        storages.filter { it.isNotBlank() }.forEach { storage ->
                            FilterChip(selected = currentStorage == storage, onClick = { selectedStorage = storage }, label = { Text(storage.replace(" ", "")) })
                        }
                    }
                }

                Text("Select Condition Grade", color = MorleyTextPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("A", "B", "C").forEach { option ->
                        val label = when (option) { "A" -> "Excellent"; "B" -> "Good"; else -> "Fair" }
                        FilterChip(selected = grade == option, onClick = { grade = option }, label = { Text("$option  $label") })
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MorleySurfaceRaised),
            border = BorderStroke(1.dp, MorleyBorder),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Quick Summary", color = MorleyTextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                GenericSummaryRow("Brand", device.brand)
                GenericSummaryRow("Model", device.model)
                if (currentStorage.isNotBlank()) GenericSummaryRow("Storage", currentStorage.replace(" ", ""))
                GenericSummaryRow("Grade", "$grade — ${when (grade) { "A" -> "Excellent"; "B" -> "Good"; else -> "Fair" }}")
                if (price != null && buyPrice != null) {
                    GenericSummaryRow("Price sheet", money.format(price.priceAud))
                    Text("BUY PRICE • $grade GRADE (${(pct * 100).toInt()}%)", color = MorleyAccent, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text(money.format(buyPrice), color = MorleyTextPrimary, fontSize = 34.sp, fontWeight = FontWeight.Black)
                    Text("${money.format(price.priceAud)} × ${(pct * 100).toInt()}% = ${money.format(buyPrice)}", color = MorleyTextSecondary, fontSize = 12.sp)
                } else {
                    GenericSummaryRow("Price sheet", "Pricing not set")
                    Text("No authorised Morley price exists for this storage variant yet.", color = MorleyTextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun GenericSummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MorleyTextSecondary, fontSize = 12.sp)
        Text(value, color = MorleyTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
