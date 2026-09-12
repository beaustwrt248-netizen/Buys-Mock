package com.buysloans.hub

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONObject

class MorleyRepairDecisionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val assessmentId = intent.getStringExtra(EXTRA_ASSESSMENT_ID).orEmpty()
        if (assessmentId.isBlank()) {
            finish()
            return
        }
        val initialBuyCost = intent.getStringExtra(EXTRA_BUY_COST).orEmpty()
        val asIsResale = intent.getStringExtra(EXTRA_AS_IS_RESALE).orEmpty()

        setContent {
            MaterialTheme {
                RepairDecisionScreen(
                    assessmentId = assessmentId,
                    initialBuyCost = initialBuyCost,
                    asIsResale = asIsResale,
                    confirm = { decision, metadata ->
                        MorleyRepairDecisionConfirmationStore.confirm(this, assessmentId, decision)
                        lifecycleScope.launch {
                            DeviceAssessmentStore.checkpoint(
                                context = applicationContext,
                                assessmentId = assessmentId,
                                checkpoint = "staff_confirmed",
                                metadata = metadata
                            )
                            finish()
                        }
                    }
                )
            }
        }
    }

    companion object {
        private const val EXTRA_ASSESSMENT_ID = "assessment_id"
        private const val EXTRA_BUY_COST = "buy_cost"
        private const val EXTRA_AS_IS_RESALE = "as_is_resale"

        fun createIntent(
            context: Context,
            assessmentId: String,
            buyCost: Double,
            asIsResale: Double
        ): Intent = Intent(context, MorleyRepairDecisionActivity::class.java).apply {
            putExtra(EXTRA_ASSESSMENT_ID, assessmentId)
            putExtra(EXTRA_BUY_COST, moneyText(buyCost))
            putExtra(EXTRA_AS_IS_RESALE, moneyText(asIsResale))
        }

        private fun moneyText(value: Double): String =
            if (value.isFinite() && value >= 0.0) "%.2f".format(value) else ""
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepairDecisionScreen(
    assessmentId: String,
    initialBuyCost: String,
    asIsResale: String,
    confirm: (MorleyRepairDisposition, JSONObject) -> Unit
) {
    val context = LocalContext.current
    var buyCost by remember { mutableStateOf(initialBuyCost) }
    var repairedResale by remember { mutableStateOf("") }
    var repairCost by remember { mutableStateOf("") }
    var partsRecovery by remember { mutableStateOf("") }
    var repairDays by remember { mutableStateOf("") }
    var staffDecision by remember {
        mutableStateOf(MorleyRepairDecisionConfirmationStore.confirmedDecision(context, assessmentId))
    }
    var saving by remember { mutableStateOf(false) }

    val state = MorleyRepairDecisionState.awaitingCanonicalRecommendation(
        staffDecision = staffDecision,
        confirmed = MorleyRepairDecisionConfirmationStore.isConfirmed(context, assessmentId)
    ).copy(
        repairCost = repairCost.toDoubleOrNull(),
        repairDays = repairDays.toDoubleOrNull()
    )

    Scaffold(
        containerColor = Color(0xFFF3F6FA),
        topBar = {
            TopAppBar(
                title = { Text("Repair-or-Buy", color = Color.White, fontWeight = FontWeight.Black) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF032A4F))
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Current as-is resale evidence: ${asIsResale.ifBlank { "Not verified" }}. MorleyAssessmentCore remains authoritative for recommendation logic; this screen never auto-confirms a commercial decision.",
                color = Color(0xFF667487)
            )
            MorleyRepairDecisionPanel(
                state = state,
                buyCost = buyCost,
                onBuyCostChange = { buyCost = moneyInput(it) },
                repairedResale = repairedResale,
                onRepairedResaleChange = { repairedResale = moneyInput(it) },
                repairCost = repairCost,
                onRepairCostChange = { repairCost = moneyInput(it) },
                partsRecovery = partsRecovery,
                onPartsRecoveryChange = { partsRecovery = moneyInput(it) },
                repairDays = repairDays,
                onRepairDaysChange = { repairDays = it.filter(Char::isDigit).take(3) },
                onSelect = { staffDecision = it },
                onConfirm = {
                    val selected = staffDecision ?: return@MorleyRepairDecisionPanel
                    saving = true
                    val metadata = JSONObject().apply {
                        put("repairDecision", selected.name.lowercase())
                        put("commercialAuthority", "advisory")
                        put("rulesSource", "MorleyAssessmentCore")
                        put("requiresStaffConfirmation", true)
                        put("commercialInputsCaptured", listOf(buyCost, asIsResale, repairedResale, repairCost).any { it.isNotBlank() })
                    }
                    confirm(selected, metadata)
                },
                modifier = Modifier.fillMaxWidth()
            )
            if (saving) CircularProgressIndicator()
            Text(
                "Confirm staff decision before returning to Device Lens. Repair-or-Buy must be confirmed before adding stock.",
                color = Color(0xFF667487),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun moneyInput(value: String): String {
    val cleaned = value.filter { it.isDigit() || it == '.' }
    val dot = cleaned.indexOf('.')
    if (dot < 0) return cleaned.take(8)
    val whole = cleaned.substring(0, dot).take(8)
    val decimals = cleaned.substring(dot + 1).replace(".", "").take(2)
    return "$whole.$decimals"
}
