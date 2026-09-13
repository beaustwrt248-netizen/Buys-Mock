package com.buysloans.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONArray

@Composable
internal fun CatalogueNativePanel(session: AdminSession) {
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf<JSONArray?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    fun refresh() {
        if (loading) return
        loading = true
        error = ""
        scope.launch {
            runCatching { AdminApi.loadCatalogue(session) }
                .onSuccess { rows = it }
                .onFailure { error = it.message.orEmpty().ifBlank { "Catalogue could not be loaded." } }
            loading = false
        }
    }

    LaunchedEffect(session.userId) { refresh() }

    Text("Catalogue", fontSize = 22.sp, fontWeight = FontWeight.Black)
    Text(
        "Live active device catalogue from the same Supabase source used by web Admin. This workspace is read-only.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp
    )

    Button(onClick = ::refresh, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
        Text(if (loading) "Refreshing…" else "Refresh catalogue", fontWeight = FontWeight.Black)
    }
    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())

    if (error.isNotBlank()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = .45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Catalogue data error", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Black)
                Text(error)
            }
        }
    }

    val catalogue = rows
    if (catalogue == null) {
        if (!loading && error.isBlank()) Text("Catalogue has not loaded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }

    Text("${catalogue.length()} active devices", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    if (catalogue.length() == 0) {
        Text("No active catalogue devices returned.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }

    for (index in 0 until minOf(catalogue.length(), 250)) {
        val row = catalogue.optJSONObject(index) ?: continue
        val brand = row.optString("brand").ifBlank { "Unknown brand" }
        val model = row.optString("model_name").ifBlank { row.optString("model") }.ifBlank { "Unknown model" }
        val category = row.optString("category")
        val modelNumber = row.optString("model_number")
        val storage = listOf(row.optString("storage"), row.optString("storage_gb"))
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
        val details = listOf(category, modelNumber, storage)
            .filter { it.isNotBlank() }
            .joinToString(" • ")

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(brand, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("ACTIVE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
                Text(model, fontWeight = FontWeight.Black)
                if (details.isNotBlank()) Text(details, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
    }
}
