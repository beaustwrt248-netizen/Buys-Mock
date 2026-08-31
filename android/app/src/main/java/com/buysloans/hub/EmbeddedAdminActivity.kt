package com.buysloans.hub

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

class EmbeddedAdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!AuthManager.isSignedIn(this) || !AuthManager.canUseAdminMode(this)) {
            finish()
            return
        }
        setContent { MaterialTheme(colorScheme = MorleyColorScheme) { EmbeddedAdminScreen() } }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun EmbeddedAdminScreen() {
        val context = this@EmbeddedAdminActivity
        val scope = rememberCoroutineScope()
        var snapshot by remember { mutableStateOf<EmbeddedAdminSnapshot?>(null) }
        var busy by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf("") }

        fun refresh() {
            if (busy) return
            busy = true
            error = ""
            scope.launch {
                runCatching { EmbeddedAdminClient.load(context) }
                    .onSuccess { snapshot = it }
                    .onFailure {
                        error = it.message ?: "Admin mode could not be loaded."
                        if (!AuthManager.canUseAdminMode(context)) finish()
                    }
                busy = false
            }
        }

        LaunchedEffect(Unit) { refresh() }
        Scaffold(
            containerColor = MorleyBackground,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MorleyBackground, titleContentColor = MorleyTextPrimary),
                    navigationIcon = { IconButton(onClick = { finish() }) { Text("‹", color = MorleyAccent, fontSize = 34.sp) } },
                    title = { Text("Admin Mode", fontWeight = FontWeight.Black) }
                )
            }
        ) { padding ->
            Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminCard("PRIVILEGED SESSION", "${AuthManager.accountLabel(context)} • ${AuthManager.role(context).replaceFirstChar { it.uppercase() }}")
                Text("Operational overview", color = MorleyTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                if (busy && snapshot == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (error.isNotBlank()) Text(error, color = MorleyDanger)
                snapshot?.let { s ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AdminMetric("Open tickets", s.openTickets.toString(), Modifier.weight(1f))
                        AdminMetric("Active users", s.enabledUsers.toString(), Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AdminMetric("Devices", s.registeredDevices.toString(), Modifier.weight(1f))
                        AdminMetric("Recent errors", s.recentErrors.toString(), Modifier.weight(1f))
                    }
                    AdminMetric("Recent audit events", s.recentAuditEvents.toString(), Modifier.fillMaxWidth())
                }
                Button(onClick = { refresh() }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Refreshing…" else "Refresh Admin Overview") }
                OutlinedButton(onClick = { startActivity(Intent(context, DiagnosticsActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("Open System Diagnostics") }
                AdminCard(
                    "SAFE MERGE FOUNDATION",
                    "Admin Mode is now part of Morley Buys for Admin/Manager accounts only. Backend RLS remains authoritative. Guardian, user-control and high-impact write operations stay behind their existing audited Admin controls until each is migrated with the same server-side policies."
                )
            }
        }
    }
}

@Composable
private fun AdminMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = MorleySurfaceRaised), border = BorderStroke(1.dp, MorleyBorder), shape = RoundedCornerShape(18.dp), modifier = modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = MorleyTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(value, color = MorleyAccent, fontSize = 26.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun AdminCard(kicker: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MorleySurface), border = BorderStroke(1.dp, MorleyBorder), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(kicker, color = MorleyAccent, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text(body, color = MorleyTextSecondary, lineHeight = 20.sp)
        }
    }
}
