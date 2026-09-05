package com.buysloans.hub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

private fun categoryTitle(category: String?): String = when (category) {
    "mobile_phone" -> "Mobile Phones"
    "tablet" -> "Tablets"
    "wearable" -> "Smart Watches & Wearables"
    "laptop" -> "Laptops"
    "desktop" -> "Desktops"
    "console" -> "Gaming Consoles & Handhelds"
    else -> "Live Device Catalogue"
}

private fun categoryLabel(category: String): String = when (category) {
    "mobile_phone" -> "Phone"
    "tablet" -> "Tablet"
    "wearable" -> "Watch / wearable"
    "laptop" -> "Laptop"
    "desktop" -> "Desktop"
    "console" -> "Console / handheld"
    else -> category.replace('_', ' ')
}

@Composable
fun LiveDeviceCatalogueBrowser(category: String? = null) {
    var query by remember(category) { mutableStateOf("") }
    val all = LiveDevicePricing.catalogue()
    val filtered = remember(all, category, query) {
        val needle = query.trim().lowercase()
        all.asSequence()
            .filter { category == null || it.category == category }
            .filter {
                needle.isBlank() || listOfNotNull(
                    it.brand,
                    it.model,
                    it.modelNumber,
                    it.storageOptions.joinToString(" "),
                ).joinToString(" ").lowercase().contains(needle)
            }
            .sortedWith(compareBy<LiveDeviceCatalogueRow> { it.category }.thenBy { it.brand.lowercase() }.thenBy { it.model.lowercase() })
            .toList()
    }

    Screen(categoryTitle(category)) {
        Text(
            if (category == null) "Browse every live catalogue device and its verified image reference."
            else "Browse every live ${categoryTitle(category).lowercase()} catalogue record.",
            color = MorleyTextSecondary,
            fontSize = 14.sp,
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search catalogue") },
            supportingText = { Text("Brand, model, model number or storage") },
            shape = RoundedCornerShape(16.dp),
        )
        Text(
            "${filtered.size} device${if (filtered.size == 1) "" else "s"}",
            color = MorleyAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
        )

        filtered.forEach { device ->
            Card(
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
                            Text(
                                device.brand.take(2).uppercase(),
                                modifier = Modifier.padding(16.dp),
                                color = MorleyAccent,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(device.model, color = MorleyTextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        Text(device.brand, color = MorleyAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        if (category == null) {
                            Text(categoryLabel(device.category), color = MorleyTextSecondary, fontSize = 11.sp)
                        }
                        device.modelNumber?.let { Text(it, color = MorleyTextSecondary, fontSize = 11.sp) }
                        if (device.storageOptions.isNotEmpty()) {
                            Text(device.storageOptions.joinToString(" • "), color = MorleyTextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
