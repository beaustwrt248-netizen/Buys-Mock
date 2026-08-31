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

private val ComputerAccent = Color(0xFF2F7CFF)
private val ComputerCyan = Color(0xFF12C9FF)
private val ComputerCard = Color(0xFF07172C)
private val ComputerMuted = Color(0xFF8FA6C6)

private enum class ComputerType { LAPTOP, DESKTOP }

/**
 * Single entry point for computer pricing. The user chooses the hardware class
 * first, then Morley renders the existing purpose-built laptop or desktop flow.
 * This deliberately reuses the existing valuation engines rather than merging
 * their pricing logic.
 */
@Composable
fun ComputerPricingScreen() {
    var type by remember { mutableStateOf<ComputerType?>(null) }

    if (type == null) {
        Screen("💻 Computer Pricing") {
            Card(
                colors = CardDefaults.cardColors(containerColor = ComputerCard),
                border = BorderStroke(1.dp, ComputerAccent.copy(alpha = .55f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("CHOOSE COMPUTER TYPE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = ComputerCyan)
                    Text("What are you pricing?", fontSize = 23.sp, fontWeight = FontWeight.Black)
                    Text(
                        "Choose Laptop / MacBook for guided exact-model pricing, or Desktop / Gaming PC for component-based valuation.",
                        color = ComputerMuted,
                        fontSize = 13.sp
                    )
                    Button(
                        onClick = { type = ComputerType.LAPTOP },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ComputerAccent, contentColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("💻  Laptop / MacBook", fontWeight = FontWeight.Black) }
                    OutlinedButton(
                        onClick = { type = ComputerType.DESKTOP },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("🖥  Desktop / Gaming PC", fontWeight = FontWeight.Black) }
                }
            }
        }
    } else {
        Column {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
                OutlinedButton(onClick = { type = null }) { Text("← Change computer type") }
            }
            when (type) {
                ComputerType.LAPTOP -> LaptopGuidedScreen()
                ComputerType.DESKTOP -> Desktop()
                null -> Unit
            }
        }
    }
}

/**
 * Reserved console workspace. Pricing rules and supported console generations
 * will be populated from the dedicated console pricing dataset once supplied.
 */
@Composable
fun ConsolePricingScreen() = Screen("🎮 Console Pricing") {
    Card(
        colors = CardDefaults.cardColors(containerColor = ComputerCard),
        border = BorderStroke(1.dp, ComputerCyan.copy(alpha = .45f)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("CONSOLE PRICING", fontSize = 11.sp, fontWeight = FontWeight.Black, color = ComputerCyan)
            Text("Pricing dataset ready to load", fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(
                "This section is reserved for console model, generation, storage, edition, condition and buying-price rules. No placeholder prices are used.",
                color = ComputerMuted,
                fontSize = 13.sp
            )
            Text("Awaiting console pricing data", color = Color.LightGray, fontWeight = FontWeight.SemiBold)
        }
    }
}
