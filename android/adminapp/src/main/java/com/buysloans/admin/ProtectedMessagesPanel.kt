package com.buysloans.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ProtectedMessageMuted = Color(0xFF8EA6C4)

@Composable
internal fun ProtectedMessagesPanel(
    ticketSubject: String,
    messages: List<SupportMessageViewItem>
) {
    Text("Protected conversation", fontSize = 18.sp, fontWeight = FontWeight.Black)
    Text(
        ticketSubject.ifBlank { "Selected support ticket" },
        color = ProtectedMessageMuted,
        fontSize = 12.sp
    )

    if (messages.isEmpty()) {
        Text("No protected messages returned for this ticket.", color = ProtectedMessageMuted)
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        messages.take(100).forEach { message ->
            ProtectedMessageCard(message)
        }
    }

    Text(
        "Read-only Admin/Manager view. Ticket and author identifiers are intentionally omitted from presentation; existing Supabase RLS remains authoritative.",
        color = ProtectedMessageMuted,
        fontSize = 11.sp
    )
}

@Composable
private fun ProtectedMessageCard(message: SupportMessageViewItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .18f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(message.authorRole.uppercase(), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text(message.body)
            if (message.createdAt.isNotBlank()) {
                Text(message.createdAt, color = ProtectedMessageMuted, fontSize = 10.sp)
            }
        }
    }
}
