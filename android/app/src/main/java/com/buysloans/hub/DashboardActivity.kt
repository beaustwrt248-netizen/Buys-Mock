package com.buysloans.hub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DashYellow = Color(0xFFFFD400)
private val DashBg = Color(0xFF111111)
private val DashCard = Color(0xFF222222)

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(17,17,17)
        window.navigationBarColor = android.graphics.Color.rgb(17,17,17)
        setContent { DashboardApp() }
    }
}

@Composable
private fun DashboardApp() {
    var page by remember { mutableStateOf(Page.Home) }
    MaterialTheme(colorScheme = darkColorScheme(primary = DashYellow, background = DashBg, surface = DashCard)) {
        Scaffold(
            containerColor = DashBg,
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF101010)) {
                    Page.entries.forEach { p ->
                        NavigationBarItem(
                            selected = page == p,
                            onClick = { page = p },
                            icon = { Text(p.icon, fontSize = 20.sp) },
                            label = { Text(p.label) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = DashYellow.copy(alpha = .25f),
                                selectedTextColor = DashYellow
                            )
                        )
                    }
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when (page) {
                    Page.Home -> ParityHome(
                        onLaptop = { page = Page.Laptop },
                        onDesktop = { page = Page.Desktop },
                        onGp = { page = Page.GP }
                    )
                    Page.Laptop -> Laptop()
                    Page.Desktop -> Desktop()
                    Page.GP -> GP()
                    Page.More -> More()
                }
            }
        }
    }
}

@Composable
private fun ParityHome(onLaptop: () -> Unit, onDesktop: () -> Unit, onGp: () -> Unit) {
    var quickText by remember { mutableStateOf("") }
    var ask by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Auto detect") }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("B&L Morley", fontSize = 25.sp, fontWeight = FontWeight.Black)
            Surface(
                color = DashYellow.copy(alpha = .14f),
                border = BorderStroke(1.dp, DashYellow.copy(alpha = .45f)),
                shape = RoundedCornerShape(999.dp)
            ) { Text("PRODUCTION", Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = DashYellow, fontSize = 11.sp, fontWeight = FontWeight.Black) }
        }

        DashboardCard("WELCOME", "Buys and Loans Calculator", "Live valuation, buying targets and native pricing tools in one workspace.")

        Card(
            colors = CardDefaults.cardColors(containerColor = DashCard),
            border = BorderStroke(2.dp, DashYellow),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("⚡ Quick Deal Capture", fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("Paste a seller listing, specs or product details. We'll route it to the right valuation tool.", color = Color.LightGray)
                OutlinedTextField(quickText, { quickText = it }, label = { Text("Listing / specs / URL") }, modifier = Modifier.fillMaxWidth(), minLines = 4)
                OutlinedTextField(ask, { ask = it }, label = { Text("Seller asking price") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Auto detect", "Laptop", "Desktop").forEach { option ->
                        FilterChip(selected = type == option, onClick = { type = option }, label = { Text(option, fontSize = 11.sp) })
                    }
                }
                Button(
                    onClick = {
                        val text = quickText.lowercase()
                        if (type == "Laptop" || (type == "Auto detect" && listOf("laptop","macbook","notebook").any { text.contains(it) })) onLaptop()
                        else onDesktop()
                    },
                    enabled = quickText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = DashYellow, contentColor = Color.Black),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) { Text("Analyse Now", fontWeight = FontWeight.Black, fontSize = 17.sp) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onLaptop, modifier = Modifier.weight(1f)) { Text("💻 Laptop") }
                    OutlinedButton(onDesktop, modifier = Modifier.weight(1f)) { Text("🖥 Desktop") }
                }
            }
        }

        DashboardCard("◷ RECENT VALUATIONS", "Recent Valuations", "Your latest native valuations will appear here as we add persistent history in the next pass.")

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusTile("LIVE PRICING", "READY", Modifier.weight(1f))
            StatusTile("ONLINE STATUS", "ONLINE", Modifier.weight(1f))
        }

        NavCard("💻", "Laptops / MacBooks", "Whole-device Google + eBay AU valuation", onLaptop)
        NavCard("🖥", "Desktops / Gaming PCs", "Component-based live pricing", onDesktop)
        NavCard("💰", "General Buys / GP", "A / B / C / Luxury buying targets", onGp)
    }
}

@Composable
private fun DashboardCard(kicker: String, title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = DashCard), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(kicker, color = DashYellow, fontSize = 12.sp, fontWeight = FontWeight.Black)
            Text(title, fontSize = 27.sp, fontWeight = FontWeight.Black)
            Text(body, color = Color.LightGray, lineHeight = 22.sp)
        }
    }
}

@Composable
private fun StatusTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = DashCard), shape = RoundedCornerShape(18.dp), modifier = modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color(0xFF57E389), fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun NavCard(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = DashCard), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(icon, fontSize = 24.sp)
            Text(title, fontSize = 19.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = Color.LightGray, fontSize = 13.sp)
            HorizontalDivider(color = DashYellow, thickness = 3.dp)
        }
    }
}
