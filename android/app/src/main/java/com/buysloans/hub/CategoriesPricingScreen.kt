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

private enum class PricingCategory {
    ALL_DEVICES, LAPTOPS, DESKTOPS, MOBILE_PHONES, TABLETS, WEARABLES, GAMING_CONSOLES
}

@Composable
fun CategoriesPricingScreen() {
    var selected by remember { mutableStateOf<PricingCategory?>(null) }

    if (selected == null) {
        Screen("Categories") {
            Text("Select a category to get started.", color = MorleyTextSecondary, fontSize = 15.sp)
            CategoryCard(null, "All Device Catalogue", "Browse every live device with images", "ALL") { selected = PricingCategory.ALL_DEVICES }
            CategoryCard(PricingVisual.PHONE, "Mobile Phones", "Price mobile phones", highlighted = true) { selected = PricingCategory.MOBILE_PHONES }
            CategoryCard(null, "Tablets", "Browse iPad, Galaxy Tab, Surface & more", "TAB") { selected = PricingCategory.TABLETS }
            CategoryCard(null, "Smart Watches", "Browse Apple Watch, Galaxy Watch & wearables", "WATCH") { selected = PricingCategory.WEARABLES }
            CategoryCard(PricingVisual.LAPTOP, "Laptops", "Price laptops & MacBooks") { selected = PricingCategory.LAPTOPS }
            CategoryCard(PricingVisual.DESKTOP, "Desktops", "Price desktop computers") { selected = PricingCategory.DESKTOPS }
            CategoryCard(PricingVisual.CONSOLE, "Gaming Consoles", "Price consoles & handhelds") { selected = PricingCategory.GAMING_CONSOLES }
        }
    } else {
        Column {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
                OutlinedButton(
                    onClick = { selected = null },
                    border = BorderStroke(1.dp, MorleyBorder),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("‹  Categories", color = MorleyTextPrimary, fontWeight = FontWeight.Bold) }
            }
            when (selected) {
                PricingCategory.ALL_DEVICES -> LiveDeviceCatalogueBrowser()
                PricingCategory.TABLETS -> LiveDeviceCatalogueBrowser("tablet")
                PricingCategory.WEARABLES -> LiveDeviceCatalogueBrowser("wearable")
                PricingCategory.LAPTOPS -> LaptopGuidedScreen()
                PricingCategory.DESKTOPS -> Desktop()
                PricingCategory.GAMING_CONSOLES -> ConsolePricingScreen()
                PricingCategory.MOBILE_PHONES -> MobilePhonePricingScreen()
                null -> Unit
            }
        }
    }
}

@Composable
private fun CategoryCard(
    visual: PricingVisual?,
    title: String,
    subtitle: String,
    badge: String? = null,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = if (highlighted) MorleyAccentSoft.copy(alpha = .45f) else MorleySurface),
        border = BorderStroke(1.dp, if (highlighted) MorleyAccent else MorleyBorder),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (visual != null) {
                PricingCategoryVisual(visual, Modifier.size(54.dp))
            } else {
                Text(
                    badge.orEmpty(),
                    modifier = Modifier.size(54.dp),
                    color = MorleyAccent,
                    fontSize = if (badge == "WATCH") 10.sp else 12.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = if (highlighted) MorleyAccent else MorleyTextPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(subtitle, color = MorleyTextSecondary, fontSize = 13.sp)
            }
            Text("›", color = if (highlighted) MorleyAccent else MorleyTextSecondary, fontSize = 28.sp, fontWeight = FontWeight.Black)
        }
    }
}
