package com.buysloans.hub

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class AiScanHistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = MorleyColorScheme) {
                ScanHistoryScreen(
                    onBack = { finish() },
                    onResume = { row ->
                        DeviceLensScanSession.resume(this, row.id)
                        startActivity(Intent(this, DeviceLensActivity::class.java))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanHistoryScreen(
    onBack: () -> Unit,
    onResume: (DeviceScanHistoryRow) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf<List<DeviceScanHistoryRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    fun reload() {
        loading = true
        error = ""
        scope.launch {
            runCatching { DeviceScanHistoryClient.list(context) }
                .onSuccess { rows = it }
                .onFailure { error = it.message ?: "AI scan history could not be loaded." }
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    Scaffold(
        containerColor = MorleyBackground,
        topBar = {
            TopAppBar(
                title = { Text("AI Scan History", fontWeight = FontWeight.Black) },
                navigationIcon = { OutlinedButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp)) { Text("Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MorleyBackground)
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MorleySurface),
                    border = BorderStroke(1.dp, MorleyBorder),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Every Morley Vision scan is retained", fontSize = 19.sp, fontWeight = FontWeight.Black, color = MorleyTextPrimary)
                        Text("Completed, failed, cancelled and unfinished scans stay here so staff can retry or continue them instead of losing the assessment.", color = MorleyTextSecondary, fontSize = 12.sp)
                    }
                }
            }
            if (loading) {
                item { Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() } }
            }
            if (error.isNotBlank()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFECEA)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(error, color = Color(0xFF8E211F), fontWeight = FontWeight.Bold)
                            Button(onClick = ::reload) { Text("Retry History") }
                        }
                    }
                }
            }
            if (!loading && error.isBlank() && rows.isEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MorleySurface), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("No AI scans have been saved yet.", Modifier.padding(18.dp), color = MorleyTextSecondary)
                    }
                }
            }
            items(rows, key = { it.id }) { row ->
                ScanHistoryCard(row, onResume)
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ScanHistoryCard(row: DeviceScanHistoryRow, onResume: (DeviceScanHistoryRow) -> Unit) {
    val status = when {
        row.isCompleted -> "Completed"
        row.isFailed -> "Analysis failed"
        row.isCancelled -> "Cancelled"
        else -> row.checkpoint.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
    val statusColor = when {
        row.isCompleted -> MorleySuccess
        row.isFailed -> Color(0xFFC0342B)
        row.isCancelled -> MorleyTextMuted
        else -> MorleyAccent
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MorleySurface),
        border = BorderStroke(1.dp, MorleyBorder),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(row.displayName, Modifier.weight(1f), color = MorleyTextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(status, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            Text(formatHistoryDate(row.createdAt), color = MorleyTextSecondary, fontSize = 11.sp)
            row.confidence?.let { Text("Identity confidence ${(it.coerceIn(0.0, 1.0) * 100).toInt()}%", color = MorleyTextSecondary, fontSize = 11.sp) }
            row.errorCode?.let { Text("Last issue: ${it.replace('_', ' ')}", color = Color(0xFFC0342B), fontSize = 11.sp) }
            if (row.canResume) {
                val label = if (row.isFailed || row.isCancelled) "Retry Scan" else "Resume Scan"
                Button(onClick = { onResume(row) }, modifier = Modifier.fillMaxWidth()) { Text(label, fontWeight = FontWeight.Black) }
            } else {
                Text("Assessment retained for audit/history.", color = MorleyTextSecondary, fontSize = 11.sp)
            }
        }
    }
}

private fun formatHistoryDate(raw: String): String {
    if (raw.isBlank()) return "Time unavailable"
    return runCatching {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val date = parser.parse(raw.take(19)) ?: return@runCatching raw
        SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(date)
    }.getOrDefault(raw)
}
