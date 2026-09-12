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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

enum class MorleyRepairDisposition(val label: String) {
    BUY_AS_IS("Buy as-is"),
    BUY_AND_REPAIR("Buy and repair"),
    PARTS_ONLY("Parts only"),
    REVIEW_REQUIRED("Review required")
}

data class MorleyRepairDecisionState(
    val recommendation: MorleyRepairDisposition,
    val reason: String,
    val asIsMargin: Double?,
    val repairedMargin: Double?,
    val partsMargin: Double?,
    val repairCost: Double?,
    val repairDays: Double?,
    val requiresStaffConfirmation: Boolean = true,
    val staffDecision: MorleyRepairDisposition? = null,
    val confirmed: Boolean = false
) {
    companion object {
        fun awaitingCanonicalRecommendation(
            staffDecision: MorleyRepairDisposition? = null,
            confirmed: Boolean = false
        ) = MorleyRepairDecisionState(
            recommendation = MorleyRepairDisposition.REVIEW_REQUIRED,
            reason = "Verified Repair-or-Buy economics have not been returned by MorleyAssessmentCore. Review the commercial evidence and make an explicit staff decision.",
            asIsMargin = null,
            repairedMargin = null,
            partsMargin = null,
            repairCost = null,
            repairDays = null,
            requiresStaffConfirmation = true,
            staffDecision = staffDecision,
            confirmed = confirmed
        )
    }
}

@Composable
fun MorleyRepairDecisionPanel(
    state: MorleyRepairDecisionState,
    buyCost: String,
    onBuyCostChange: (String) -> Unit,
    repairedResale: String,
    onRepairedResaleChange: (String) -> Unit,
    repairCost: String,
    onRepairCostChange: (String) -> Unit,
    partsRecovery: String,
    onPartsRecoveryChange: (String) -> Unit,
    repairDays: String,
    onRepairDaysChange: (String) -> Unit,
    onSelect: (MorleyRepairDisposition) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val border = Color(0xFFD8E2EE)
    val blue = Color(0xFF0878F9)
    val navy = Color(0xFF102033)
    val muted = Color(0xFF667487)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, border),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("AI recommendation", color = muted, fontWeight = FontWeight.Bold)
                Text(state.recommendation.label, color = navy, fontWeight = FontWeight.Black)
                Text(state.reason, color = muted)
                RepairEvidenceRow("As-is margin", state.asIsMargin)
                RepairEvidenceRow("Repaired margin", state.repairedMargin)
                RepairEvidenceRow("Parts margin", state.partsMargin)
                RepairEvidenceRow("Repair cost", state.repairCost)
                Text("Estimated repair time: ${state.repairDays?.let { "${it.toInt()} day${if (it.toInt() == 1) "" else "s"}" } ?: "Not verified"}", color = muted)
                Text(
                    if (state.requiresStaffConfirmation) "AI is advisory. Staff confirmation is required before stock preparation." else "",
                    color = muted,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            "Commercial inputs are captured for staff review. Android does not calculate a second recommendation; MorleyAssessmentCore remains the canonical Repair-or-Buy rules engine.",
            color = muted
        )
        OutlinedTextField(buyCost, onBuyCostChange, label = { Text("Buy cost (A$)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(repairedResale, onRepairedResaleChange, label = { Text("Resale after repair") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(repairCost, onRepairCostChange, label = { Text("Repair cost") }, modifier = Modifier.weight(1f), singleLine = true)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(partsRecovery, onPartsRecoveryChange, label = { Text("Parts recovery") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(repairDays, onRepairDaysChange, label = { Text("Repair days") }, modifier = Modifier.weight(1f), singleLine = true)
        }

        Text("Staff decision", color = navy, fontWeight = FontWeight.Black)
        MorleyRepairDisposition.entries.filter { it != MorleyRepairDisposition.REVIEW_REQUIRED }.forEach { disposition ->
            OutlinedButton(
                onClick = { onSelect(disposition) },
                border = BorderStroke(1.dp, if (state.staffDecision == disposition) blue else border),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (state.staffDecision == disposition) "✓ ${disposition.label}" else disposition.label) }
        }
        Button(
            onClick = onConfirm,
            enabled = state.staffDecision != null,
            colors = ButtonDefaults.buttonColors(containerColor = blue),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Confirm staff decision", fontWeight = FontWeight.Black) }
    }
}

@Composable
private fun RepairEvidenceRow(label: String, value: Double?) {
    val currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-AU"))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF667487))
        Text(value?.let(currency::format) ?: "Not verified", color = Color(0xFF102033), fontWeight = FontWeight.Bold)
    }
}
