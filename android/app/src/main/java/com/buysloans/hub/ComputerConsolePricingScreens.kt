package com.buysloans.hub

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

private val ComputerAccent = Color(0xFF2F7CFF)
private val ComputerCyan = Color(0xFF12C9FF)
private val ComputerCard = Color(0xFF07172C)
private val ComputerMuted = Color(0xFF8FA6C6)

private enum class ComputerType { LAPTOP, DESKTOP }

@Composable
fun ComputerPricingScreen() {
    var type by remember { mutableStateOf<ComputerType?>(null) }
    if (type == null) {
        Screen("💻 Computer Pricing") {
            Card(colors = CardDefaults.cardColors(containerColor = ComputerCard), border = BorderStroke(1.dp, ComputerAccent.copy(alpha = .55f)), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("CHOOSE COMPUTER TYPE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = ComputerCyan)
                    Text("What are you pricing?", fontSize = 23.sp, fontWeight = FontWeight.Black)
                    Text("Choose Laptop / MacBook for guided exact-model pricing, or Desktop / Gaming PC for component-based valuation.", color = ComputerMuted, fontSize = 13.sp)
                    Button(onClick = { type = ComputerType.LAPTOP }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = ComputerAccent, contentColor = Color.White), shape = RoundedCornerShape(16.dp)) { Text("💻  Laptop / MacBook", fontWeight = FontWeight.Black) }
                    OutlinedButton(onClick = { type = ComputerType.DESKTOP }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("🖥  Desktop / Gaming PC", fontWeight = FontWeight.Black) }
                }
            }
        }
    } else {
        Column {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) { OutlinedButton(onClick = { type = null }) { Text("← Change computer type") } }
            when (type) { ComputerType.LAPTOP -> LaptopGuidedScreen(); ComputerType.DESKTOP -> Desktop(); null -> Unit }
        }
    }
}

@Composable
fun ConsolePricingScreen() = Screen("🎮 Console Pricing") {
    var selected by remember { mutableStateOf<ConsolePriceEntry?>(null) }
    var grade by remember { mutableStateOf("A") }
    var menuOpen by remember { mutableStateOf(false) }
    val money = remember { NumberFormat.getCurrencyInstance(Locale("en", "AU")) }

    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = ComputerCard), border = BorderStroke(1.dp, ComputerCyan.copy(alpha = .45f)), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("CONSOLE PRICING", fontSize = 11.sp, fontWeight = FontWeight.Black, color = ComputerCyan)
                Text("Select console + condition grade", fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("Buy price automatically follows Morley's standard grade rules: A 70% • B 50% • C 30% of the supplied console price-sheet value.", color = ComputerMuted, fontSize = 13.sp)
            }
        }
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text(selected?.name ?: "Choose console") }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                ConsolePricingCatalog.entries.forEach { entry -> DropdownMenuItem(text = { Text("${entry.name} — ${money.format(entry.rrp)}") }, onClick = { selected = entry; menuOpen = false }) }
            }
        }
        selected?.let { console ->
            val buyPrice = ConsolePricingCatalog.buyPrice(console, grade)
            val percentage = (ConsolePricingCatalog.gradeBuyPercent.getValue(grade) * 100).toInt()
            Card(colors = CardDefaults.cardColors(containerColor = ComputerCard), border = BorderStroke(1.dp, ComputerAccent.copy(alpha = .35f)), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(console.name, fontSize = 19.sp, fontWeight = FontWeight.Black)
                    Text("PRICE SHEET VALUE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = ComputerCyan)
                    Text(money.format(console.rrp), fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text("Grade", color = ComputerMuted, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { ConsolePricingCatalog.grades.forEach { option -> FilterChip(selected = grade == option, onClick = { grade = option }, label = { Text("$option Grade") }) } }
                    Text("AUTO BUY PRICE • $grade GRADE ($percentage%)", fontSize = 10.sp, fontWeight = FontWeight.Black, color = ComputerCyan)
                    Text(money.format(buyPrice), fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Text("${money.format(console.rrp)} × $percentage% = ${money.format(buyPrice)}", color = ComputerMuted, fontSize = 12.sp)
                }
            }
        }
        Text("Supported consoles", fontWeight = FontWeight.Black, fontSize = 16.sp)
        ConsolePricingCatalog.entries.forEach { entry ->
            Card(onClick = { selected = entry }, colors = CardDefaults.cardColors(containerColor = ComputerCard), border = BorderStroke(1.dp, ComputerCyan.copy(alpha = .16f)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(entry.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Text(money.format(entry.rrp), color = ComputerCyan, fontWeight = FontWeight.Black) }
            }
        }
    }
}
