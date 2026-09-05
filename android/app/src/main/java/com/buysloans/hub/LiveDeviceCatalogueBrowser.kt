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

private const val CATALOGUE_PAGE_SIZE = 60

internal fun normalizeCatalogueCategory(value: String): String {
    val normalized = value.trim().lowercase().replace('-', '_').replace(' ', '_')
    return when (normalized) {
        "smart_watch", "smartwatch", "smart_watches", "watch", "watches", "wearables" -> "wearable"
        else -> normalized
    }
}

internal fun deviceMatchesCatalogueCategory(deviceCategory: String, requestedCategory: String?): Boolean =
    requestedCategory == null || normalizeCatalogueCategory(deviceCategory) == normalizeCatalogueCategory(requestedCategory)

internal fun filterCatalogueDevices(
    devices: List<LiveDeviceCatalogueRow>,
    category: String?,
    query: String = "",
): List<LiveDeviceCatalogueRow> {
    val needle = query.trim().lowercase()
    return devices.asSequence()
        .filter { deviceMatchesCatalogueCategory(it.category, category) }
        .filter {
            needle.isBlank() || listOfNotNull(
                it.brand,
                it.model,
                it.modelNumber,
                it.storageOptions.joinToString(" "),
            ).joinToString(" ").lowercase().contains(needle)
        }
        .sortedWith(
            compareBy<LiveDeviceCatalogueRow> { normalizeCatalogueCategory(it.category) }
                .thenBy { it.brand.lowercase() }
                .thenBy { it.model.lowercase() },
        )
        .toList()
}

private fun categoryTitle(category: String?): String = when (category?.let(::normalizeCatalogueCategory)) {
    "mobile_phone" -> "Mobile Phones"
    "tablet" -> "Tablets"
    "wearable" -> "Smart Watches & Wearables"
    "laptop" -> "Laptops"
    "desktop" -> "Desktops"
    "console" -> "Gaming Consoles & Handhelds"
    else -> "Live Device Catalogue"
}

private fun categoryLabel(category: String): String = when (normalizeCatalogueCategory(category)) {
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
    var visibleCount by remember(category, query) { mutableStateOf(CATALOGUE_PAGE_SIZE) }
    val all = LiveDevicePricing.catalogue()
    val filtered = remember(all, category, query) { filterCatalogueDevices(all, category, query) }
    val visibleDevices = remember(filtered, visibleCount) { filtered.take(visibleCount) }

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
            if (filtered.size > visibleDevices.size) {
                "Showing ${visibleDevices.size} of ${filtered.size} devices"
            } else {
                "${filtered.size} device${if (filtered.size == 1) "" else "s"}"
            },
            color = MorleyAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
        )

        visibleDevices.forEach { device ->
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

        if (visibleDevices.size < filtered.size) {
            OutlinedButton(
                onClick = { visibleCount = (visibleCount + CATALOGUE_PAGE_SIZE).coerceAtMost(filtered.size) },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MorleyAccent),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    "Show ${minOf(CATALOGUE_PAGE_SIZE, filtered.size - visibleDevices.size)} more",
                    color = MorleyAccent,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
