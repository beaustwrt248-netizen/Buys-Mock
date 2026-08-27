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
private val SWBad = Color(0xFFFF6B7A)

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
        else -> "AVOID" to SWBad
    }
}

private fun swDealVerdict(ask: Double?, market: Double?, maxBuy: Double?): Pair<String, Color> {
    if (ask == null) return "ENTER ASK" to SWMuted
    return when {
        maxBuy != null && ask <= maxBuy -> "GREAT BUY" to SWGood
        market != null && ask <= market * .78 -> "GOOD BUY" to SWGood
        market != null && ask < market -> "MARGINAL" to SWWarn
        market != null -> "AVOID" to SWBad
        else -> "REVIEW" to SWMuted
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
    var showDealMode by remember { mutableStateOf(false) }
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
    if (showDealMode) DealModeDialog(onDismiss = { showDealMode = false }, onSaved = { showDealMode = false; reload() })

    val opportunities = items.filter { swVerdict(it).first == "GREAT BUY" || swVerdict(it).first == "GOOD BUY" }
    val potentialMargin = opportunities.sumOf(::swPotentialMargin)
    val latest = items.take(3)
    val watched = items.filter { favouriteIds.contains(it.id) }.take(3)
    val watchlistMargin = watched.sumOf(::swPotentialMargin)

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
            Button(
                onClick = { showDealMode = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF168E61)),
                shape = RoundedCornerShape(14.dp)
            ) { Text("⚡ Quick Deal Mode", fontWeight = FontWeight.Black) }
            OutlinedButton(
                onClick = { context.startActivity(Intent(context, NfcScannerActivity::class.java)) },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, SWAccent.copy(alpha = .55f)),
                shape = RoundedCornerShape(14.dp)
            ) { Text("⌁ NFC Scanner", color = SWAccent, fontWeight = FontWeight.Black) }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (error.isNotBlank()) Text("Smart workspace will refresh when connected.", color = SWMuted, fontSize = 11.sp)
            if (!loading && latest.isEmpty()) {
                Text("Run or save a valuation and it will appear here.", color = SWMuted, fontSize = 12.sp)
            }
            latest.forEach { item ->
                SmartValuationRow(item = item, isFavourite = favouriteIds.contains(item.id))
            }

            if (!loading && favouriteIds.isNotEmpty()) {
                HorizontalDivider(color = SWAccent.copy(alpha = .16f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("WATCHLIST", color = SWAccent, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (watchlistMargin > 0.0) "${swMoney(watchlistMargin)} potential margin" else "${favouriteIds.size} saved",
                        color = if (watchlistMargin > 0.0) SWGood else SWMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (watched.isEmpty()) {
                    Text("Saved watchlist items will appear here when their valuation history is available on this device.", color = SWMuted, fontSize = 11.sp)
                } else {
                    watched.forEach { item -> SmartValuationRow(item = item, isFavourite = true, compact = true) }
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
private fun SmartValuationRow(item: SavedValuation, isFavourite: Boolean, compact: Boolean = false) {
    val verdict = swVerdict(item)
    Surface(color = Color(0xFF07101F), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(if (compact) 10.dp else 12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.itemSummary, fontWeight = FontWeight.Black, fontSize = if (compact) 13.sp else 14.sp, modifier = Modifier.weight(1f))
                if (isFavourite) Text("★", color = SWWarn, fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                Text(verdict.first, color = verdict.second, fontWeight = FontWeight.Black, fontSize = 10.sp)
            }
            Text("Market ${swMoney(item.marketValue)} • Max buy ${swMoney(item.maxBuy)}${item.askingPrice?.let { " • Ask ${swMoney(it)}" } ?: ""}", color = SWMuted, fontSize = 11.sp)
            item.itemGrade?.let { Text("$it Grade", color = SWAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            if (swPotentialMargin(item) > 0.0) Text("${swMoney(swPotentialMargin(item))} potential gross margin", color = SWGood, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DealModeDialog(onDismiss: () -> Unit, onSaved: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var item by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("B") }
    var askText by remember { mutableStateOf("") }
    var marketText by remember { mutableStateOf("") }
    var maxText by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val ask = askText.toDoubleOrNull()
    val market = marketText.toDoubleOrNull()
    val maxBuy = maxText.toDoubleOrNull()
    val verdict = swDealVerdict(ask, market, maxBuy)
    val margin = if (ask != null && market != null) (market - ask).coerceAtLeast(0.0) else null
    val headroom = if (ask != null && maxBuy != null) maxBuy - ask else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Deal Mode") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Enter the seller ask and your current market/max-buy figures for an instant decision.", color = SWMuted, fontSize = 12.sp)
                OutlinedTextField(item, { item = it }, label = { Text("Item / model") }, singleLine = true)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Item grade", color = SWMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("A", "B", "C").forEach { value ->
                            FilterChip(
                                selected = grade == value,
                                onClick = { grade = value },
                                label = { Text("$value Grade", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                OutlinedTextField(askText, { askText = it }, label = { Text("Seller asking price") }, singleLine = true)
                OutlinedTextField(marketText, { marketText = it }, label = { Text("Market value") }, singleLine = true)
                OutlinedTextField(maxText, { maxText = it }, label = { Text("Max buy") }, singleLine = true)
                Surface(color = verdict.second.copy(alpha = .10f), border = BorderStroke(1.dp, verdict.second.copy(alpha = .35f)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(verdict.first, color = verdict.second, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text("$grade Grade", color = SWAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        if (margin != null) Text("Potential gross margin ${swMoney(margin)}", color = SWGood, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        if (headroom != null) Text(if (headroom >= 0) "${swMoney(headroom)} below max buy" else "${swMoney(-headroom)} above max buy", color = if (headroom >= 0) SWGood else SWWarn, fontSize = 11.sp)
                    }
                }
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } },
        confirmButton = {
            Button(
                onClick = {
                    if (item.isBlank() || ask == null) { error = "Enter an item and valid asking price."; return@Button }
                    busy = true; error = ""
                    scope.launch {
                        runCatching {
                            ValuationHistoryManager.save(
                                context = context,
                                itemType = "other",
                                itemSummary = item.trim(),
                                specs = "Quick Deal Mode",
                                askingPrice = ask,
                                marketValue = market,
                                maxBuy = maxBuy,
                                expectedProfit = margin,
                                confidence = if (market != null && maxBuy != null) "deal mode" else "manual review",
                                itemGrade = grade
                            )
                        }.onSuccess { onSaved() }
                            .onFailure { error = it.message ?: "Could not save deal" }
                        busy = false
                    }
                },
                enabled = !busy
            ) { Text("Save Deal") }
        }
    )
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
