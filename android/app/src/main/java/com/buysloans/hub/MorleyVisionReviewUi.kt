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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal enum class VisionStaffDecision {
    PENDING,
    CONFIRMED,
    NOT_DAMAGE
}

internal data class VisionDamageReviewItem(
    val regionIndex: Int,
    val label: String,
    val severity: String,
    val confidence: Double,
    val decision: VisionStaffDecision = VisionStaffDecision.PENDING
)

internal data class MorleyVisionReviewState(
    val inspection: DeviceInspection,
    val storageVerifiedState: MutableState<Boolean>,
    val identityVerified: Boolean,
    val qualityWarnings: List<String>,
    val consistencyWarnings: List<String>,
    val componentFindings: List<String>,
    val damageReviews: List<VisionDamageReviewItem>,
    val suggestedPricingAllowed: Boolean,
    val pricingBlockedReason: String?
) {
    val storageVerified: Boolean
        get() = storageVerifiedState.value && inspection.verifiedStorage.isNotBlank()

    val hasBlockingEvidenceGap: Boolean
        get() = !identityVerified || qualityWarnings.isNotEmpty() || consistencyWarnings.isNotEmpty()

    val unresolvedDamageCount: Int
        get() = damageReviews.count { it.decision == VisionStaffDecision.PENDING }

    val canCompleteStaffReview: Boolean
        get() = identityVerified && storageVerified && !hasBlockingEvidenceGap && unresolvedDamageCount == 0
}

internal object MorleyVisionReviewPolicy {
    private val staffStoragePattern = Regex("^(\\d{1,4})\\s*(GB|TB)$", RegexOption.IGNORE_CASE)

    fun from(inspection: DeviceInspection, pricing: LivePricingResult?): MorleyVisionReviewState {
        val storageVerified = inspection.verifiedStorage.isNotBlank()
        val damageReviews = inspection.damageRegions.mapIndexed { index, region ->
            VisionDamageReviewItem(
                regionIndex = index,
                label = region.label,
                severity = region.severity,
                confidence = region.confidence
            )
        }
        val pricingReason = when {
            !inspection.identityVerified -> "Verify the exact device identity before using a suggested price."
            !storageVerified -> "Confirm the device storage before using a suggested price."
            inspection.qualityWarnings.isNotEmpty() -> "Retake unclear photos before using a suggested price."
            inspection.consistencyWarnings.isNotEmpty() -> "Resolve cross-photo inconsistencies before using a suggested price."
            pricing?.recommendationBlockedReason != null -> pricing.recommendationBlockedReason
            pricing?.canUseSuggestedPrice == true -> null
            else -> "Not enough verified market evidence to suggest a price."
        }
        return MorleyVisionReviewState(
            inspection = inspection,
            storageVerifiedState = mutableStateOf(storageVerified),
            identityVerified = inspection.identityVerified,
            qualityWarnings = inspection.qualityWarnings.distinct(),
            consistencyWarnings = inspection.consistencyWarnings.distinct(),
            componentFindings = inspection.componentFindings.distinct(),
            damageReviews = damageReviews,
            suggestedPricingAllowed = pricingReason == null,
            pricingBlockedReason = pricingReason
        )
    }

    fun decideDamage(
        state: MorleyVisionReviewState,
        regionIndex: Int,
        decision: VisionStaffDecision
    ): MorleyVisionReviewState = state.copy(
        damageReviews = state.damageReviews.map {
            if (it.regionIndex == regionIndex) it.copy(decision = decision) else it
        }
    )

    fun confirmStorage(state: MorleyVisionReviewState, rawStorage: String): Boolean {
        val cleaned = MorleyVisionPolicy.clean(rawStorage)
        if (cleaned.isBlank()) return false

        val catalogueOptions = state.inspection.catalogueMatch?.storageOptions.orEmpty()
            .map(MorleyVisionPolicy::clean)
            .filter { it.isNotBlank() }
        val catalogueMatch = catalogueOptions.firstOrNull { it.equals(cleaned, ignoreCase = true) }
        val verified = catalogueMatch ?: staffStoragePattern.matchEntire(cleaned)?.let { match ->
            "${match.groupValues[1]} ${match.groupValues[2].uppercase()}"
        } ?: return false

        state.inspection.storage = verified
        state.storageVerifiedState.value = true
        return true
    }
}

@Composable
internal fun MorleyVisionReviewPanel(
    state: MorleyVisionReviewState,
    onDamageDecision: (Int, VisionStaffDecision) -> Unit,
    modifier: Modifier = Modifier
) {
    var storageInput by remember(state.inspection) { mutableStateOf("") }
    var storageError by remember(state.inspection) { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ReviewStatusCard(
            title = if (state.identityVerified) "Device identity verified" else "Device identity not verified",
            body = if (state.identityVerified) {
                if (state.storageVerified) {
                    "Model identity and ${state.inspection.verifiedStorage} storage are verified for review."
                } else {
                    "Model identity is verified. Staff must confirm the storage shown on the device or its label."
                }
            } else {
                "Do not rely on model, storage or pricing until staff verify the exact device."
            },
            warning = !state.identityVerified || !state.storageVerified
        )

        if (state.identityVerified && !state.storageVerified) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFD8E2EE))
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Confirm storage", fontWeight = FontWeight.Black)
                    Text("Only confirm a value you can verify from the device, settings screen or physical label. Do not guess.")

                    state.inspection.catalogueMatch?.storageOptions.orEmpty()
                        .map(MorleyVisionPolicy::clean)
                        .filter { it.isNotBlank() }
                        .distinct()
                        .forEach { option ->
                            OutlinedButton(
                                onClick = {
                                    if (MorleyVisionReviewPolicy.confirmStorage(state, option)) {
                                        storageInput = option
                                        storageError = ""
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Confirm $option")
                            }
                        }

                    OutlinedTextField(
                        value = storageInput,
                        onValueChange = {
                            storageInput = it.take(12)
                            storageError = ""
                        },
                        label = { Text("Verified storage") },
                        placeholder = { Text("e.g. 128 GB") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            storageError = if (MorleyVisionReviewPolicy.confirmStorage(state, storageInput)) {
                                ""
                            } else {
                                "Enter a verified storage value such as 128 GB or 1 TB."
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0878F9))
                    ) {
                        Text("Confirm Verified Storage")
                    }
                    if (storageError.isNotBlank()) {
                        Text(storageError, color = Color(0xFF8E211F), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        ReviewWarnings("Photo quality", state.qualityWarnings)
        ReviewWarnings("Cross-photo consistency", state.consistencyWarnings)
        ReviewWarnings("Visible component findings", state.componentFindings, warning = false)

        if (state.damageReviews.isNotEmpty()) {
            Text("Damage review", fontWeight = FontWeight.Black)
            state.damageReviews.forEach { review ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFD8E2EE))
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(review.label, fontWeight = FontWeight.Black)
                        Text("${review.severity.replaceFirstChar { it.uppercase() }} • ${(review.confidence * 100).toInt()}% visual confidence")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onDamageDecision(review.regionIndex, VisionStaffDecision.CONFIRMED) },
                                modifier = Modifier.weight(1f),
                                enabled = review.decision != VisionStaffDecision.CONFIRMED,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0878F9))
                            ) { Text("Confirm") }
                            OutlinedButton(
                                onClick = { onDamageDecision(review.regionIndex, VisionStaffDecision.NOT_DAMAGE) },
                                modifier = Modifier.weight(1f),
                                enabled = review.decision != VisionStaffDecision.NOT_DAMAGE
                            ) { Text("Not Damage") }
                        }
                    }
                }
            }
        }

        if (!state.suggestedPricingAllowed) {
            ReviewStatusCard(
                title = "Suggested pricing unavailable",
                body = state.pricingBlockedReason ?: "Verified evidence is incomplete.",
                warning = true
            )
        }

        Text(
            if (state.canCompleteStaffReview) "Staff review complete." else "Staff verification is still required before completing the purchase.",
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ReviewWarnings(title: String, items: List<String>, warning: Boolean = true) {
    if (items.isEmpty()) return
    ReviewStatusCard(title, items.joinToString(" • "), warning)
}

@Composable
private fun ReviewStatusCard(title: String, body: String, warning: Boolean) {
    val background = if (warning) Color(0xFFFFE8E7) else Color(0xFFE7F8EF)
    val foreground = if (warning) Color(0xFF8E211F) else Color(0xFF12633E)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = background)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = foreground, fontWeight = FontWeight.Black)
            Text(body, color = foreground)
        }
    }
}
