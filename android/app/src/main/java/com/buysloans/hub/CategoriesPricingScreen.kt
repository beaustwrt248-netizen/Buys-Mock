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
import androidx.compose.material3.OutlinedButton
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

private enum class PricingCategory {
    LAPTOPS,
    DESKTOPS,
    MOBILE_PHONES,
    GAMING_CONSOLES
}

@Composable
fun CategoriesPricingScreen() {
    var selected by remember { mutableStateOf<PricingCategory?>(null) }

    if (selected == null) {
        Screen("B&L Morley Pricing") {
            Card(
                colors = CardDefaults.cardColors(containerColor = MorleySurfaceRaised),
                border = BorderStroke(1.dp, MorleyBorder),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "CHOOSE CATEGORY TYPE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MorleyAccent
                    )
                    Text(
                        "What type of device are you pricing?",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MorleyTextPrimary
                    )
                    Text(
                        "Select a sub-category below to continue.",
                        color = MorleyTextSecondary,
                        fontSize = 13.sp
                    )

                    CategoryButton("Laptops", primary = true) { selected = PricingCategory.LAPTOPS }
                    CategoryButton("Desktops") { selected = PricingCategory.DESKTOPS }
                    CategoryButton("Mobile Phones") { selected = PricingCategory.MOBILE_PHONES }
                    CategoryButton("Gaming Consoles") { selected = PricingCategory.GAMING_CONSOLES }
                }
            }
        }
    } else {
        Column {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
                OutlinedButton(
                    onClick = { selected = null },
                    border = BorderStroke(1.dp, MorleyBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("← Categories", color = MorleyTextPrimary)
                }
            }
            when (selected) {
                PricingCategory.LAPTOPS -> LaptopGuidedScreen()
                PricingCategory.DESKTOPS -> Desktop()
                PricingCategory.GAMING_CONSOLES -> ConsolePricingScreen()
                PricingCategory.MOBILE_PHONES -> MobilePhoneCategoryPlaceholder()
                null -> Unit
            }
        }
    }
}

@Composable
private fun CategoryButton(
    label: String,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    if (primary) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MorleyAccentStrong,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(999.dp)
        ) {
            Text(label, fontWeight = FontWeight.Black)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MorleyBorder),
            shape = RoundedCornerShape(999.dp)
        ) {
            Text(label, color = MorleyTextPrimary, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun MobilePhoneCategoryPlaceholder() = Screen("Mobile Phone Pricing") {
    Card(
        colors = CardDefaults.cardColors(containerColor = MorleySurfaceRaised),
        border = BorderStroke(1.dp, MorleyBorder),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("MOBILE PHONES", color = MorleyAccent, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text("Phone pricing is being connected to the category workflow.", color = MorleyTextPrimary, fontWeight = FontWeight.Black, fontSize = 19.sp)
            Text(
                "This category is now part of the navigation structure without changing any existing valuation authority or permissions.",
                color = MorleyTextSecondary,
                fontSize = 13.sp
            )
        }
    }
}
