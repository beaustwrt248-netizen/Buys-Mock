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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal enum class VisionStaffDecision {
    PENDING,
    CONFIRMED,
    NOT_DAMAGE
}

internal data class VisionDamageReview(
    val regionIndex: Int,
    val label: String,
    val severity: String,
    val confidence: Double,
    val decision: VisionStaffDecision = VisionStaffDecision.PENDING
)

internal data class MorleyVisionReviewState(
    val identityVerified: Boolean,
    val storageVerified: Boolean,
    val qualityWarnings: List<String>,
    val consistencyWarnings: List<String>,
    val componentFindings: List<String>,
    val damageReviews: List<VisionDamageReview>,
    val suggestedPricingAllowed: Boolean,
    val pricingBlockedReason: String?
) {
    val hasBlockingEvidenceGap: Boolean
        get() = !identityVerified || qualityWarnings.isNotEmpty() || consistencyWarnings.isNotEmpty()

    val unresolvedDamageCount: Int
        get() = damageReviews.count { it.decision == VisionStaffDecision.PENDING }

    val canCompleteStaffReview: Boolean
        get() = identityVerified && unresolvedDamageCount == 0
}

internal object MorleyVisionReviewPolicy {
    fun from(inspection: DeviceInspection, pricing: LivePricingResult?): MorleyVisionReviewState {
        val storageVerified = inspection.verifiedStorage.isNotBlank()
        val damageReviews = inspection.damageRegions.mapIndexed { index, region ->
            VisionDamageReview(
                regionIndex = index,
                label = region.label,
                severity = region.severity,
                confidence = region.confidence
            )
        }
        val pricingReason = when {
            !inspection.identityVerified -> "Verify the exact device identity before using a suggested price."
            inspection.qualityWarnings.isNotEmpty() -> "Retake unclear photos before using a suggested price."
            inspection.consistencyWarnings.isNotEmpty() -> "Resolve cross-photo inconsistencies before using a suggested price."
            pricing?.recommendationBlockedReason != null -> pricing.recommendationBlockedReason
            pricing?.canUseSuggestedPrice == true -> null
            else -> "Not enough verified market evidence to suggest a price."
        }
        return MorleyVisionReviewState(
            identityVerified = inspection.identityVerified,
            storageVerified = storageVerified,
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
}

@Composable
internal fun MorleyVisionReviewPanel(
    state: MorleyVisionReviewState,
    onDamageDecision: (Int, VisionStaffDecision) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ReviewStatusCard(
            title = if (state.identityVerified) "Device identity verified" else "Device identity not verified",
            body = if (state.identityVerified) {
                if (state.storageVerified) "Model identity and storage are verified for review." else "Model identity is verified. Storage still needs staff confirmation."
            } else {
                "Do not rely on model, storage or pricing until staff verify the exact device."
            },
            warning = !state.identityVerified
        )

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
