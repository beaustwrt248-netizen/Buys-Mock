package com.buysloans.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal val MorleyBlue = Color(0xFF0878F9)

internal data class MorleyAssessmentDiagnosticUi(
    val label: String,
    val status: String = "not_tested",
    val detail: String? = null,
)

internal data class MorleyAssessmentReviewUiState(
    val model: String? = null,
    val storageGb: Int? = null,
    val identityConfidence: Double? = null,
    val diagnostics: List<MorleyAssessmentDiagnosticUi> = emptyList(),
    val conditionSummary: String = "Condition evidence is still being reviewed.",
    val valuationSummary: String = "Valuation is not ready until verified market and device evidence is complete.",
    val repairSummary: String = "Repair or Buy recommendation is advisory until staff confirmation.",
    val riskSummary: String = "Risk review is required when evidence or commercial inputs need staff attention.",
    val requiresStaffConfirmation: Boolean = true,
)

private fun diagnosticStatusLabel(status: String): String = when (status.trim().lowercase()) {
    "pass" -> "Pass"
    "fail" -> "Fail"
    "unknown" -> "Unknown"
    "not_tested" -> "Not tested"
    else -> "Not tested"
}

@Composable
internal fun MorleyAssessmentReview(
    state: MorleyAssessmentReviewUiState,
    explicitStaffConfirmation: Boolean,
    onExplicitStaffConfirmationChange: (Boolean) -> Unit,
    onConfirmRecommendation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val identity = state.model?.trim()?.takeIf { it.isNotEmpty() } ?: "Identity unresolved"
    val storage = state.storageGb?.takeIf { it > 0 }?.let { "$it GB" } ?: "Storage unresolved"
    val confidence = state.identityConfidence
        ?.takeIf { it.isFinite() }
        ?.coerceIn(0.0, 1.0)
        ?.let { "${(it * 100).toInt()}% identity confidence" }

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(primary = MorleyBlue),
    ) {
        LazyColumn(
            modifier = modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Morley device assessment review" },
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("Device assessment", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(identity, style = MaterialTheme.typography.titleMedium)
                        Text(storage, style = MaterialTheme.typography.bodyLarge)
                        confidence?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        if (identity == "Identity unresolved" || storage == "Storage unresolved") {
                            Text(
                                "Resolve identity and storage from verified evidence before any commercial recommendation is confirmed.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            item {
                AssessmentSection("Condition", state.conditionSummary) {
                    if (state.diagnostics.isEmpty()) {
                        Text("Hardware checks: Not tested", style = MaterialTheme.typography.bodySmall)
                    } else {
                        state.diagnostics.forEach { diagnostic ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(diagnostic.label, modifier = Modifier.weight(1f))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    diagnosticStatusLabel(diagnostic.status),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            diagnostic.detail?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }

            item { AssessmentSection("Valuation", state.valuationSummary) }
            item { AssessmentSection("Repair or Buy", state.repairSummary) }
            item { AssessmentSection("Risk review", state.riskSummary) }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Staff confirmation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            if (state.requiresStaffConfirmation) {
                                "Morley AI recommendations are advisory. Confirm the evidence and commercial decision before continuing."
                            } else {
                                "Review the evidence before continuing."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = explicitStaffConfirmation,
                                onCheckedChange = onExplicitStaffConfirmationChange,
                                modifier = Modifier.semantics { contentDescription = "Explicit staff confirmation" },
                            )
                            Text("I have reviewed the evidence and recommendation")
                        }
                        Button(
                            onClick = onConfirmRecommendation,
                            enabled = explicitStaffConfirmation && state.requiresStaffConfirmation,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Confirm recommendation" },
                        ) {
                            Text("Confirm recommendation")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssessmentSection(
    title: String,
    summary: String,
    content: @Composable (() -> Unit)? = null,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(summary, style = MaterialTheme.typography.bodyMedium)
            content?.invoke()
        }
    }
}