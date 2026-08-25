package com.buysloans.hub

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val DiagAccent = Color(0xFF16C7FF)
private val DiagStrong = Color(0xFF2684FF)
private val DiagBg = Color(0xFF030712)
private val DiagCard = Color(0xFF0B1528)
private val DiagMuted = Color(0xFF8EA6C4)

class DiagnosticsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(3, 7, 18)
        window.navigationBarColor = android.graphics.Color.rgb(3, 7, 18)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = DiagAccent,
                    secondary = DiagStrong,
                    background = DiagBg,
                    surface = DiagCard
                )
            ) { DiagnosticsScreen() }
        }
    }

    private fun networkSummary(): String {
        val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return "Offline"
        val caps = manager.getNetworkCapabilities(network) ?: return "Offline"
        val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val transport = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi‑Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Network"
        }
        return if (validated) "$transport · Internet verified" else "$transport · Internet not verified"
    }

    private fun notificationSummary(): String {
        if (Build.VERSION.SDK_INT < 33) return "Allowed by Android version"
        return if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) "Allowed" else "Not allowed"
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun DiagnosticsScreen() {
        val activity = this@DiagnosticsActivity
        val scope = rememberCoroutineScope()
        var refreshKey by remember { mutableIntStateOf(0) }
        var sessionState by remember { mutableStateOf("Not checked") }
        var updateState by remember { mutableStateOf("Not checked") }
        var busy by remember { mutableStateOf(false) }

        val network = remember(refreshKey) { networkSummary() }
        val notifications = remember(refreshKey) { notificationSummary() }
        val installer = remember(refreshKey) {
            if (Build.VERSION.SDK_INT < 26 || packageManager.canRequestPackageInstalls()) "Allowed" else "Permission required"
        }
        val account = remember(refreshKey) { AuthManager.accountLabel(activity) }

        Scaffold(
            containerColor = DiagBg,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF050B16), titleContentColor = Color.White),
                    navigationIcon = { IconButton(onClick = { finish() }) { Text("‹", fontSize = 34.sp, color = DiagAccent) } },
                    title = { Text("Diagnostics", fontWeight = FontWeight.Black) }
                )
            }
        ) { padding ->
            Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DiagnosticCard("App", "B&L Morley ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                DiagnosticCard("Device", "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}")
                DiagnosticCard("Account", account)
                DiagnosticCard("Connectivity", network)
                DiagnosticCard("Notifications", notifications)
                DiagnosticCard("APK installer", installer)
                DiagnosticCard("Secure session", sessionState)
                DiagnosticCard("OTA service", updateState)

                Button(
                    onClick = {
                        refreshKey++
                        busy = true
                        sessionState = "Checking…"
                        updateState = "Checking…"
                        scope.launch {
                            runCatching { AuthManager.validAccessToken(activity) }
                                .onSuccess { sessionState = "Valid" }
                                .onFailure { sessionState = "Needs attention: ${it.message ?: "session check failed"}" }
                            runCatching { UpdateManager.check() }
                                .onSuccess { update -> updateState = if (update == null) "Online · No newer release" else "Online · ${update.versionName} available" }
                                .onFailure { updateState = "Unavailable: ${it.message ?: "network error"}" }
                            busy = false
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DiagStrong)
                ) { Text(if (busy) "Running checks…" else "Run Full Check", fontWeight = FontWeight.Black) }

                Text(
                    "Diagnostics are read-only. They do not change inventory, sales, pricing data or account settings.",
                    color = DiagMuted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    @Composable
    private fun DiagnosticCard(title: String, value: String) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DiagCard),
            border = BorderStroke(1.dp, DiagAccent.copy(alpha = .18f)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, color = DiagMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}
