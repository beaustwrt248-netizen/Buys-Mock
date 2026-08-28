package com.buysloans.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray

internal data class AuditTimelineEntry(
    val action: String,
    val targetType: String,
    val targetId: String,
    val createdAt: String
)

internal fun buildAuditTimeline(rows: JSONArray?): List<AuditTimelineEntry> {
    if (rows == null) return emptyList()
    return (0 until rows.length()).mapNotNull { index ->
        val row = rows.optJSONObject(index) ?: return@mapNotNull null
        val action = row.optString("action").trim()
        val createdAt = row.optString("created_at").trim()
        if (action.isBlank() || createdAt.isBlank()) return@mapNotNull null
        AuditTimelineEntry(
            action = action,
            targetType = row.optString("target_type").trim(),
            targetId = row.optString("target_id").trim(),
            createdAt = createdAt
        )
    }
}

internal fun auditTimelineTitle(entry: AuditTimelineEntry): String {
    val action = entry.action.replace('_', ' ').replaceFirstChar { it.uppercase() }
    val target = listOf(entry.targetType, entry.targetId)
        .filter { it.isNotBlank() }
        .joinToString(" • ")
    return if (target.isBlank()) action else "$action • $target"
}

@Composable
internal fun AuditTimelinePanel(rows: JSONArray?) {
    val entries = buildAuditTimeline(rows)
    Text("Admin audit timeline", fontSize = 21.sp, fontWeight = FontWeight.Black)
    Text(
        "Read-only operational history for Admin/Manager accounts. Android intentionally does not display audit details payloads, ticket/message content, credentials, or release/config values.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp
    )
    if (entries.isEmpty()) {
        Text("No audit entries returned.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    entries.take(50).forEach { entry ->
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .18f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(auditTimelineTitle(entry), fontWeight = FontWeight.Bold)
                Text(entry.createdAt, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
    }
}
