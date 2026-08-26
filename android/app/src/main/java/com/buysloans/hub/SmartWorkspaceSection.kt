package com.buysloans.hub

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

private val SWAccent = Color(0xFF16C7FF)
private val SWStrong = Color(0xFF2684FF)
private val SWCard = Color(0xFF0B1528)
private val SWMuted = Color(0xFF8EA6C4)
private val SWGood = Color(0xFF57E389)
private val SWWarn = Color(0xFFFFC857)

private fun swMoney(value: Double?): String = if (value == null) "—" else
    NumberFormat.getCurrencyInstance(Locale("en", "AU")).apply { maximumFractionDigits = 0 }.format(value)

private fun swVerdict(item: SavedValuation): Pair<String, Color> {
    val ask = item.askingPrice ?: return "REVIEW" to SWMuted
    val max = item.maxBuy
    val market = item.marketValue
    return when {
        max != null && ask <= max -> "GREAT BUY" to SWGood
        market != null && ask <= market * .78 -> "GOOD BUY" to SWGood
        market != null && ask < market -> "MARGINAL" to SWWarn
        else -> "AVOID" to Color(0xFFFF6B7A)
    }
}

private fun swPotentialMargin(item: SavedValuation): Double {
    val ask = item.askingPrice ?: return 0.0
    val market = item.marketValue ?: return 0.0
    return (market - ask).coerceAtLeast(0.0)
}

@Composable
fun SmartWorkspaceSection() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<SavedValuation>>(emptyList()) }
    var favouriteIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    val favPrefs = context.getSharedPreferences("valuation_favourites", android.content.Context.MODE_PRIVATE)

    fun reload() {
        scope.launch {
            loading = true
            error = ""
            favouriteIds = favPrefs.getStringSet("ids", emptySet())?.toSet() ?: emptySet()
            runCatching { ValuationHistoryManager.list(context) }
                .onSuccess { items = it }
                .onFailure { error = it.message ?: "Could not load smart workspace" }
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) reload()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val opportunities = items.filter { swVerdict(it).first == "GREAT BUY" || swVerdict(it).first == "GOOD BUY" }
    val potentialMargin = opportunities.sumOf(::swPotentialMargin)
    val latest = items.take(3)

    Card(
        colors = CardDefaults.cardColors(containerColor = SWCard),
        border = BorderStroke(1.dp, SWAccent.copy(alpha = .3f)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("SMART WORKSPACE", color = SWAccent, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text("Welcome, ${AuthManager.displayName(context).ifBlank { "back" }.substringBefore(' ')}", fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("Recent valuations, watchlist activity and buy opportunities at a glance.", color = SWMuted, fontSize = 13.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SWMetric("VALUATIONS", items.size.toString(), Modifier.weight(1f))
                SWMetric("WATCHLIST", favouriteIds.size.toString(), Modifier.weight(1f))
                SWMetric("OPPORTUNITIES", opportunities.size.toString(), Modifier.weight(1f))
            }
            Surface(color = Color(0xFF07101F), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("POTENTIAL GROSS MARGIN", color = SWMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(swMoney(potentialMargin), color = SWGood, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                    Text("${opportunities.size} live buy ${if (opportunities.size == 1) "opportunity" else "opportunities"}", color = SWMuted, fontSize = 10.sp)
                }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (error.isNotBlank()) Text("Smart workspace will refresh when connected.", color = SWMuted, fontSize = 11.sp)
            if (!loading && latest.isEmpty()) {
                Text("Run or save a valuation and it will appear here.", color = SWMuted, fontSize = 12.sp)
            }
            latest.forEach { item ->
                val verdict = swVerdict(item)
                Surface(color = Color(0xFF07101F), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.itemSummary, fontWeight = FontWeight.Black, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            if (favouriteIds.contains(item.id)) Text("★", color = SWWarn, fontSize = 14.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(verdict.first, color = verdict.second, fontWeight = FontWeight.Black, fontSize = 10.sp)
                        }
                        Text("Market ${swMoney(item.marketValue)} • Max buy ${swMoney(item.maxBuy)}${item.askingPrice?.let { " • Ask ${swMoney(it)}" } ?: ""}", color = SWMuted, fontSize = 11.sp)
                        if (swPotentialMargin(item) > 0.0) Text("${swMoney(swPotentialMargin(item))} potential gross margin", color = SWGood, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Button(
                onClick = { context.startActivity(Intent(context, ValuationHistoryActivity::class.java)) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SWStrong),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Open Valuations & Deals", fontWeight = FontWeight.Black) }
            OutlinedButton(onClick = { reload() }, modifier = Modifier.fillMaxWidth()) { Text("Refresh Smart Workspace") }
        }
    }
}

@Composable
private fun SWMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(color = Color(0xFF07101F), shape = RoundedCornerShape(14.dp), modifier = modifier) {
        Column(Modifier.padding(10.dp)) {
            Text(label, color = SWMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}
