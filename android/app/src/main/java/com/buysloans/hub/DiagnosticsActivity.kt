package com.buysloans.hub

import android.Manifest
import android.content.Intent
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DiagAccent = Color(0xFF16C7FF)
private val DiagBg = Color(0xFF030712)
private val DiagCard = Color(0xFF0B1528)
private val DiagMuted = Color(0xFF8EA6C4)
private val DiagOk = Color(0xFF57E389)

class DiagnosticsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(3, 7, 18)
        window.navigationBarColor = android.graphics.Color.rgb(3, 7, 18)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = DiagAccent,
                    background = DiagBg,
                    surface = DiagCard
                )
            ) { DiagnosticsScreen() }
        }
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun notificationsAllowed(): Boolean =
        Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun installerAllowed(): Boolean =
        Build.VERSION.SDK_INT < 26 || packageManager.canRequestPackageInstalls()

    private fun checkedTime(): String =
        SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date())

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun DiagnosticsScreen() {
        val scope = rememberCoroutineScope()
        var refresh by remember { mutableIntStateOf(0) }
        var lastChecked by remember { mutableStateOf(checkedTime()) }
        var liveBusy by remember { mutableStateOf(false) }
        var sessionLive by remember { mutableStateOf<Boolean?>(null) }
        var sessionDetail by remember { mutableStateOf("Not checked yet") }
        var otaLive by remember { mutableStateOf<Boolean?>(null) }
        var otaDetail by remember { mutableStateOf("Not checked yet") }

        val online = remember(refresh) { isOnline() }
        val signedIn = remember(refresh) { AuthManager.isSignedIn(this) }
        val notifications = remember(refresh) { notificationsAllowed() }
        val installer = remember(refresh) { installerAllowed() }
        val accountLabel = remember(refresh) { AuthManager.accountLabel(this) }

        fun runLiveChecks() {
            refresh += 1
            lastChecked = checkedTime()
            if (!isOnline()) {
                sessionLive = false
                sessionDetail = "Live session validation skipped because the device is offline"
                otaLive = false
                otaDetail = "OTA service check skipped because the device is offline"
                return
            }
            liveBusy = true
            scope.launch {
                runCatching { AuthManager.validAccessToken(this@DiagnosticsActivity) }
                    .onSuccess {
                        sessionLive = true
                        sessionDetail = "Authorised session validated with the account service"
                    }
                    .onFailure {
                        sessionLive = false
                        sessionDetail = "Live session validation failed: ${it.message ?: "unknown error"}"
                    }

                runCatching { UpdateManager.check() }
                    .onSuccess { update ->
                        otaLive = true
                        otaDetail = if (update == null) {
                            "OTA metadata is reachable and this build is current"
                        } else {
                            "OTA metadata is reachable; ${update.versionName} is available"
                        }
                    }
                    .onFailure {
                        otaLive = false
                        otaDetail = "OTA service check failed: ${it.message ?: "unknown error"}"
                    }
                liveBusy = false
                lastChecked = checkedTime()
            }
        }

        Scaffold(
            containerColor = DiagBg,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF050B16), titleContentColor = Color.White),
                    navigationIcon = {
                        IconButton(onClick = { finish() }) { Text("‹", fontSize = 34.sp, color = DiagAccent) }
                    },
                    title = { Text("System Diagnostics", fontWeight = FontWeight.Black) }
                )
            }
        ) { padding ->
            Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DiagnosticCard("App", "B&L Morley ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", true)
                DiagnosticCard("Account session", if (signedIn) "Authenticated locally as $accountLabel" else "No active authorised session", signedIn)
                DiagnosticCard("Internet connection", if (online) "Validated network connection is available" else "No validated internet connection", online)
                DiagnosticCard("Notifications", if (notifications) "Android notification permission is available" else "Notification permission is disabled", notifications)
                DiagnosticCard("OTA installer", if (installer) "APK installer permission is ready" else "Install unknown apps permission is not enabled", installer)
                DiagnosticCard("Live account service", sessionDetail, sessionLive != false)
                DiagnosticCard("Live OTA service", otaDetail, otaLive != false)
                DiagnosticCard("Device", "${Build.MANUFACTURER} ${Build.MODEL} • Android ${Build.VERSION.RELEASE}", true)

                Text(
                    "Last checked: $lastChecked",
                    color = DiagMuted,
                    fontSize = 12.sp
                )

                Button(
                    onClick = { runLiveChecks() },
                    enabled = !liveBusy,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2684FF))
                ) { Text(if (liveBusy) "Running Live Checks…" else "Refresh Diagnostics", fontWeight = FontWeight.Black) }

                if (liveBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

                OutlinedButton(
                    onClick = { startActivity(Intent(this@DiagnosticsActivity, UpdateActivity::class.java)) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open Update Centre") }

                Text(
                    "Diagnostics are read-only. They do not change inventory, sales, valuation or account data.",
                    color = DiagMuted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    @Composable
    private fun DiagnosticCard(title: String, detail: String, ok: Boolean) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DiagCard),
            border = BorderStroke(1.dp, if (ok) DiagOk.copy(alpha = .30f) else Color(0xFFFF8A9B).copy(alpha = .35f)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.fillMaxWidth().padding(15.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (ok) "●" else "!", color = if (ok) DiagOk else Color(0xFFFF8A9B), fontWeight = FontWeight.Black)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text(detail, color = DiagMuted, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }
    }
}
