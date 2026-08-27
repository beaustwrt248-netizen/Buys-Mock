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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TBAccent = Color(0xFF16C7FF)
private val TBBg = Color(0xFF030712)
private val TBCard = Color(0xFF0B1528)
private val TBMuted = Color(0xFF8EA6C4)
private val TBGood = Color(0xFF57E389)
private val TBBad = Color(0xFFFF6B7A)
private val TBWarn = Color(0xFFFFC857)

class TestBuyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = TBAccent, background = TBBg, surface = TBCard)) {
                TestBuyScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestBuyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var itemName by remember { mutableStateOf("") }
    var scanValue by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(DeviceCategory.OTHER) }
    var askingText by remember { mutableStateOf("") }
    var valuationText by remember { mutableStateOf("") }
    var maxBuyText by remember { mutableStateOf("") }
    var faults by remember { mutableStateOf("") }
    var checks by remember(category) { mutableStateOf(checklistFor(category)) }
    var showInventoryConfirm by remember { mutableStateOf(false) }
    var savedInventoryId by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf("") }

    val draft = TestBuyDraft(
        itemName = itemName.trim(),
        scanValue = scanValue.trim(),
        category = category,
        askingPrice = askingText.toDoubleOrNull() ?: 0.0,
        currentValuation = valuationText.toDoubleOrNull() ?: 0.0,
        maxBuyPrice = maxBuyText.toDoubleOrNull() ?: 0.0,
        faults = faults.trim(),
        checks = checks
    )
    val outcome = recommendedOutcome(draft)
    val outcomeColor = when (outcome) {
        BuyOutcome.SEND_TO_INVENTORY -> TBGood
        BuyOutcome.BUY -> TBWarn
        BuyOutcome.REJECT -> TBBad
    }
    val outcomeLabel = when (outcome) {
        BuyOutcome.SEND_TO_INVENTORY -> "SEND TO INVENTORY"
        BuyOutcome.BUY -> "BUY — FAULTS RECORDED"
        BuyOutcome.REJECT -> "REJECT / NOT READY"
    }

    if (showInventoryConfirm) {
        AlertDialog(
            onDismissRequest = { showInventoryConfirm = false },
            title = { Text("Send to Inventory?") },
            text = {
                Text(
                    "Add ${draft.itemName} to inventory at ${draft.askingPrice} cost and ${draft.currentValuation} resale value. " +
                        "The item will start as Ready for Sale because all required Test & Buy checks have passed."
                )
            },
            dismissButton = {
                TextButton(onClick = { showInventoryConfirm = false }) { Text("Cancel") }
            },
            confirmButton = {
                Button(onClick = {
                    saveError = ""
                    runCatching { WorkspaceStore.addInventoryFromTestBuy(context, draft) }
                        .onSuccess { id -> savedInventoryId = id; showInventoryConfirm = false }
                        .onFailure { error -> saveError = error.message ?: "Could not add item to inventory." }
                }) { Text("Confirm & Add") }
            }
        )
    }

    Scaffold(
        containerColor = TBBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF050B16), titleContentColor = Color.White),
                navigationIcon = { IconButton(onClick = onBack) { Text("‹", color = TBAccent, fontSize = 34.sp) } },
                title = { Text("Test & Buy", fontWeight = FontWeight.Black) }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("TEST & BUY WORKSPACE", color = TBAccent, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text("Test the item, record faults and compare the seller ask against the approved max-buy figure.", color = TBMuted)

            OutlinedTextField(itemName, { itemName = it }, label = { Text("Item / model") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(scanValue, { scanValue = it }, label = { Text("Barcode / scan reference (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            Text("Device category", color = TBMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                DeviceCategory.entries.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        row.forEach { option ->
                            FilterChip(
                                selected = category == option,
                                onClick = { category = option },
                                label = { Text(option.name.replace('_', ' '), fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(askingText, { askingText = it }, label = { Text("Seller ask") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(maxBuyText, { maxBuyText = it }, label = { Text("Max buy") }, modifier = Modifier.weight(1f), singleLine = true)
            }
            OutlinedTextField(valuationText, { valuationText = it }, label = { Text("Current valuation") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(faults, { faults = it }, label = { Text("Faults / repair notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

            Text("Hardware checklist", fontSize = 18.sp, fontWeight = FontWeight.Black)
            checks.forEachIndexed { index, check ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = TBCard),
                    border = BorderStroke(1.dp, TBAccent.copy(alpha = .16f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(check.label, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(TestResult.PASS, TestResult.FAIL, TestResult.NOT_APPLICABLE).forEach { result ->
                                FilterChip(
                                    selected = check.result == result,
                                    onClick = { checks = checks.toMutableList().also { it[index] = check.copy(result = result) } },
                                    label = { Text(when (result) { TestResult.PASS -> "Pass"; TestResult.FAIL -> "Fail"; else -> "N/A" }, fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = outcomeColor.copy(alpha = .10f)),
                border = BorderStroke(1.dp, outcomeColor.copy(alpha = .45f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("RECOMMENDED OUTCOME", color = TBMuted, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text(outcomeLabel, color = outcomeColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("${draft.completedChecks}/${draft.checks.size} checks completed · ${draft.failedChecks} failed", color = TBMuted, fontSize = 11.sp)
                    if (outcome == BuyOutcome.REJECT) {
                        Text("Complete every applicable check, resolve failures, enter a max-buy price and keep the seller ask at or below that limit.", color = TBMuted, fontSize = 11.sp)
                    } else if (outcome == BuyOutcome.SEND_TO_INVENTORY) {
                        Text("Item passed testing and is within max-buy. Confirm below before any inventory record is created.", color = TBMuted, fontSize = 11.sp)
                    } else {
                        Text("The item is within max-buy but has recorded faults. Review repair risk before completing the purchase.", color = TBMuted, fontSize = 11.sp)
                    }
                }
            }

            if (savedInventoryId != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = TBGood.copy(alpha = .10f)),
                    border = BorderStroke(1.dp, TBGood.copy(alpha = .45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Added to Inventory · Ready for Sale", Modifier.padding(14.dp), color = TBGood, fontWeight = FontWeight.Black)
                }
            } else if (outcome == BuyOutcome.SEND_TO_INVENTORY) {
                Button(
                    onClick = { showInventoryConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TBGood, contentColor = Color(0xFF04120A))
                ) { Text("Send to Inventory", fontWeight = FontWeight.Black) }
            }

            if (saveError.isNotBlank()) Text(saveError, color = TBBad, fontSize = 12.sp)

            Text("NFC checks remain scan/read-only. This workflow does not assign NFC tags or modify inventory from an NFC scan.", color = TBMuted, fontSize = 10.sp)
            Spacer(Modifier.height(18.dp))
        }
    }
}
