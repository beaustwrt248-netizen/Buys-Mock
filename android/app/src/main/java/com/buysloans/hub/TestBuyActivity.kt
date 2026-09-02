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

private val TBAccent = Color(0xFF167A5A)
private val TBBg = Color(0xFFF5F7F4)
private val TBCard = Color(0xFFEEF4F0)
private val TBMuted = Color(0xFF52645D)
private val TBGood = Color(0xFF238A63)
private val TBBad = Color(0xFFC74755)
private val TBWarn = Color(0xFFA86A12)

private fun testResultColor(result: TestResult): Color = when (result) {
    TestResult.PASS -> TBGood
    TestResult.FAIL -> TBBad
    TestResult.NOT_APPLICABLE -> TBWarn
    TestResult.NOT_TESTED -> TBAccent
}

class TestBuyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = TBAccent, background = TBBg, surface = TBCard)) {
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
    var pricingGrade by remember { mutableStateOf(TestBuyPricingGrade.A) }
    var faults by remember { mutableStateOf("") }
    var checks by remember(category) { mutableStateOf(checklistFor(category)) }
    var showInventoryConfirm by remember { mutableStateOf(false) }
    var savedInventoryId by remember { mutableStateOf<String?>(null) }
    var completedOutcome by remember { mutableStateOf<BuyOutcome?>(null) }
    var saveError by remember { mutableStateOf("") }

    val valuationValue = parseMorleyCurrencyInput(valuationText) ?: 0.0
    val automaticMaxBuy = calculatedTestBuyMaxBuy(valuationValue, pricingGrade)
    val draft = TestBuyDraft(
        itemName = itemName.trim(),
        scanValue = scanValue.trim(),
        category = category,
        askingPrice = parseMorleyCurrencyInput(askingText) ?: 0.0,
        currentValuation = valuationValue,
        maxBuyPrice = automaticMaxBuy,
        faults = faults.trim(),
        checks = checks
    )
    val availability = TestBuyOutcomePolicy.evaluate(draft)
    val guidanceState = testBuyGuidanceState(draft)
    val headroom = draft.maxBuyPrice - draft.askingPrice
    val guidanceColor = when (guidanceState) {
        TestBuyGuidanceState.READY_CLEAN -> TBGood
        TestBuyGuidanceState.READY_WITH_FAULTS -> TBWarn
        TestBuyGuidanceState.COMPLETE_TEST_AND_PRICING -> TBAccent
        TestBuyGuidanceState.REJECT_ASK_ABOVE_MAX, TestBuyGuidanceState.REJECT_FAILED_CHECKS -> TBBad
    }
    val guidanceLabel = when (guidanceState) {
        TestBuyGuidanceState.COMPLETE_TEST_AND_PRICING -> "COMPLETE TEST & PRICING"
        TestBuyGuidanceState.REJECT_ASK_ABOVE_MAX -> "REJECT — ASK ABOVE MAX BUY"
        TestBuyGuidanceState.REJECT_FAILED_CHECKS -> "REVIEW — FAILED CHECKS"
        TestBuyGuidanceState.READY_WITH_FAULTS -> "READY — FAULTS REQUIRE REVIEW"
        TestBuyGuidanceState.READY_CLEAN -> "READY — WITHIN MAX BUY"
    }

    fun evidenceSource(): TestEvidenceSource =
        if (draft.scanValue.isBlank()) TestEvidenceSource.MANUAL_ENTRY else TestEvidenceSource.BARCODE

    fun recordExplicitOutcome(desiredOutcome: BuyOutcome) {
        saveError = ""
        runCatching {
            val session = TestBuySessionFinalizer.finalize(
                draft = draft,
                evidenceSource = evidenceSource(),
                explicitOutcome = desiredOutcome
            )
            TestBuyCompletionHistoryRecorder.record(context, session)
            session.outcome
        }.onSuccess { completedOutcome = it }
            .onFailure { error -> saveError = error.message ?: "Could not record Test & Buy outcome." }
    }

    if (showInventoryConfirm) {
        AlertDialog(
            onDismissRequest = { showInventoryConfirm = false },
            title = { Text("Send to Inventory?") },
            text = {
                Text(
                    "Add ${draft.itemName} to inventory at ${draft.askingPrice} cost and ${draft.currentValuation} resale value. " +
                        "The item will start as Purchased and must move through Testing before Ready for Sale."
                )
            },
            dismissButton = {
                TextButton(onClick = { showInventoryConfirm = false }) { Text("Cancel") }
            },
            confirmButton = {
                Button(onClick = {
                    saveError = ""
                    runCatching {
                        val session = TestBuySessionFinalizer.finalize(
                            draft = draft,
                            evidenceSource = evidenceSource(),
                            explicitOutcome = BuyOutcome.SEND_TO_INVENTORY
                        )
                        WorkspaceStore.addInventoryFromTestBuy(
                            context = context,
                            draft = draft,
                            completedSession = session
                        )
                    }.onSuccess { id ->
                        savedInventoryId = id
                        completedOutcome = BuyOutcome.SEND_TO_INVENTORY
                        showInventoryConfirm = false
                    }.onFailure { error -> saveError = error.message ?: "Could not add item to inventory." }
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
            Text("Test the item, record faults and compare the seller ask against an automatically calculated max-buy figure.", color = TBMuted)

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
                OutlinedTextField(
                    askingText,
                    { askingText = it },
                    label = { Text("Seller ask") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = if (automaticMaxBuy > 0.0) "%.2f".format(automaticMaxBuy) else "",
                    onValueChange = {},
                    label = { Text("Auto max buy") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    readOnly = true
                )
            }
            OutlinedTextField(valuationText, { valuationText = it }, label = { Text("Current valuation / sale value") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            Text("Grade / target GP", color = TBMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                TestBuyPricingGrade.entries.forEach { option ->
                    FilterChip(
                        selected = pricingGrade == option,
                        onClick = { pricingGrade = option },
                        label = { Text("${option.label} ${option.targetGpPct.toInt()}%", fontSize = 9.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Text(
                "Auto max buy = current valuation × ${(100.0 - pricingGrade.targetGpPct).toInt()}% (${pricingGrade.targetGpPct.toInt()}% target GP). Seller ask remains the price quoted by the seller.",
                color = TBMuted,
                fontSize = 10.sp
            )

            if (draft.maxBuyPrice > 0.0 && askingText.isNotBlank()) {
                val headroomColor = if (headroom >= 0.0) TBGood else TBBad
                Text(
                    if (headroom >= 0.0) "Within max buy by ${"%.2f".format(headroom)}" else "Seller ask is ${"%.2f".format(-headroom)} over max buy",
                    color = headroomColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            OutlinedTextField(faults, { faults = it }, label = { Text("Faults / repair notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

            Text("Hardware checklist", fontSize = 18.sp, fontWeight = FontWeight.Black)
            checks.forEachIndexed { index, check ->
                val statusColor = testResultColor(check.result)
                val isCompleted = check.result != TestResult.NOT_TESTED
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCompleted) statusColor.copy(alpha = .09f) else TBCard
                    ),
                    border = BorderStroke(
                        1.dp,
                        statusColor.copy(alpha = if (isCompleted) .72f else .16f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(check.label, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(TestResult.PASS, TestResult.FAIL, TestResult.NOT_APPLICABLE).forEach { result ->
                                val resultColor = testResultColor(result)
                                val selected = check.result == result
                                FilterChip(
                                    selected = selected,
                                    onClick = { checks = checks.toMutableList().also { it[index] = check.copy(result = result) } },
                                    label = {
                                        Text(
                                            when (result) {
                                                TestResult.PASS -> "Pass"
                                                TestResult.FAIL -> "Fail"
                                                else -> "N/A"
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = if (selected) FontWeight.Black else FontWeight.Medium
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = resultColor.copy(alpha = .72f),
                                        selectedBorderColor = resultColor
                                    ),
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = resultColor.copy(alpha = .04f),
                                        labelColor = Color.White.copy(alpha = .86f),
                                        selectedContainerColor = resultColor.copy(alpha = .34f),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = guidanceColor.copy(alpha = .10f)),
                border = BorderStroke(1.dp, guidanceColor.copy(alpha = .45f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("GUIDANCE", color = TBMuted, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text(guidanceLabel, color = guidanceColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("${draft.completedChecks}/${draft.checks.size} checks completed · ${draft.failedChecks} failed", color = TBMuted, fontSize = 11.sp)
                    Text("Guidance explains the current state; it no longer labels an incomplete form as a rejected deal. Explicit actions still enforce checklist, valuation, max-buy and fault-recording safety.", color = TBMuted, fontSize = 11.sp)
                    if (!availability.canBuy && availability.buyBlockers.isNotEmpty()) {
                        Text("Buy blocked: ${availability.buyBlockers.joinToString(" ")}", color = TBWarn, fontSize = 10.sp)
                    }
                    if (!availability.canSendToInventory && availability.inventoryBlockers.isNotEmpty()) {
                        Text("Inventory blocked: ${availability.inventoryBlockers.joinToString(" ")}", color = TBMuted, fontSize = 10.sp)
                    }
                }
            }

            if (completedOutcome != null) {
                val completedColor = when (completedOutcome) {
                    BuyOutcome.SEND_TO_INVENTORY -> TBGood
                    BuyOutcome.BUY -> TBWarn
                    BuyOutcome.REJECT -> TBBad
                    null -> TBMuted
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = completedColor.copy(alpha = .10f)),
                    border = BorderStroke(1.dp, completedColor.copy(alpha = .45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when (completedOutcome) {
                            BuyOutcome.SEND_TO_INVENTORY -> "Completed · Sent to Inventory · Purchased"
                            BuyOutcome.BUY -> "Completed · Buy recorded · No inventory created"
                            BuyOutcome.REJECT -> "Completed · Reject recorded · No inventory created"
                            null -> ""
                        },
                        Modifier.padding(14.dp),
                        color = completedColor,
                        fontWeight = FontWeight.Black
                    )
                }
            } else {
                Text("Explicit outcome", color = TBMuted, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Button(
                        onClick = { recordExplicitOutcome(BuyOutcome.REJECT) },
                        enabled = availability.canReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = TBBad, contentColor = Color(0xFF150507))
                    ) { Text("Reject", fontSize = 11.sp, fontWeight = FontWeight.Black) }
                    Button(
                        onClick = { recordExplicitOutcome(BuyOutcome.BUY) },
                        enabled = availability.canBuy,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = TBWarn, contentColor = Color(0xFF150E02))
                    ) { Text("Buy", fontSize = 11.sp, fontWeight = FontWeight.Black) }
                    Button(
                        onClick = { showInventoryConfirm = true },
                        enabled = availability.canSendToInventory,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = TBGood, contentColor = Color(0xFF04120A))
                    ) { Text("Inventory", fontSize = 11.sp, fontWeight = FontWeight.Black) }
                }
            }

            if (savedInventoryId != null) {
                Text("Inventory handoff ID: ${savedInventoryId!!.take(8)}…", color = TBMuted, fontSize = 10.sp)
            }
            if (saveError.isNotBlank()) Text(saveError, color = TBBad, fontSize = 12.sp)

            Text("NFC checks remain Android scan/read-only. This workflow does not look up inventory from NFC, assign or link tags, or modify stock from an NFC scan.", color = TBMuted, fontSize = 10.sp)
            Spacer(Modifier.height(18.dp))
        }
    }
}
