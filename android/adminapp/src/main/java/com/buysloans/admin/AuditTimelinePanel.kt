package com.buysloans.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

internal fun filterAuditTimeline(entries: List<AuditTimelineEntry>, query: String): List<AuditTimelineEntry> {
    val needle = query.trim().lowercase()
    if (needle.isBlank()) return entries
    return entries.filter { entry ->
        entry.action.lowercase().contains(needle) ||
            entry.targetType.lowercase().contains(needle) ||
            entry.targetId.lowercase().contains(needle) ||
            entry.createdAt.lowercase().contains(needle) ||
            auditTimelineTitle(entry).lowercase().contains(needle)
    }
}

@Composable
internal fun AuditTimelinePanel(rows: JSONArray?) {
    val entries = buildAuditTimeline(rows)
    var query by remember { mutableStateOf("") }
    val visibleEntries = remember(entries, query) { filterAuditTimeline(entries, query) }

    Text("Admin audit timeline", fontSize = 21.sp, fontWeight = FontWeight.Black)
    Text(
        "Read-only operational history for Admin/Manager accounts. Android intentionally does not display audit details payloads, ticket/message content, credentials, or release/config values.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp
    )
    OutlinedTextField(
        value = query,
        onValueChange = { query = it.take(120) },
        label = { Text("Search audit activity") },
        supportingText = { Text("Action, target, ID or timestamp") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    if (entries.isEmpty()) {
        Text("No audit entries returned.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    if (visibleEntries.isEmpty()) {
        Text("No audit entries match this search.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Text(
        "Showing ${visibleEntries.size} of ${entries.size} returned entries",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp
    )
    visibleEntries.take(50).forEach { entry ->
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
