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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class PricingCategory { LAPTOPS, DESKTOPS, MOBILE_PHONES, GAMING_CONSOLES }

@Composable
fun CategoriesPricingScreen() {
    var selected by remember { mutableStateOf<PricingCategory?>(null) }

    if (selected == null) {
        Screen("Categories") {
            Text("Select a category to get started.", color = MorleyTextSecondary, fontSize = 14.sp)
            CategoryCard(MorleyIcons.Laptop, "Laptops", "Price laptops & MacBooks") { selected = PricingCategory.LAPTOPS }
            CategoryCard(MorleyIcons.Computer, "Desktops", "Price desktop computers") { selected = PricingCategory.DESKTOPS }
            CategoryCard(MorleyIcons.Phone, "Mobile Phones", "Price mobile phones", highlighted = true) { selected = PricingCategory.MOBILE_PHONES }
            CategoryCard(MorleyIcons.Console, "Gaming Consoles", "Price consoles & handhelds") { selected = PricingCategory.GAMING_CONSOLES }
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
    icon: ImageVector,
    title: String,
    subtitle: String,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = if (highlighted) MorleyAccentSoft.copy(alpha = .45f) else MorleySurface),
        border = BorderStroke(1.dp, if (highlighted) MorleyAccent else MorleyBorder),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 17.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = MorleySurfaceRaised,
                border = BorderStroke(1.dp, MorleyBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                MorleyIcon(icon, title, MorleyTextPrimary, Modifier.padding(10.dp).size(25.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = if (highlighted) MorleyAccent else MorleyTextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(subtitle, color = MorleyTextSecondary, fontSize = 12.sp)
            }
            Text("›", color = MorleyTextSecondary, fontSize = 25.sp, fontWeight = FontWeight.Black)
        }
    }
}
