package com.buysloans.hub

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DiagAccent = Color(0xFF167A5A)
private val DiagBg = Color(0xFFF5F7F4)
private val DiagCard = Color(0xFFEEF4F0)
private val DiagMuted = Color(0xFF52645D)
private val DiagOk = Color(0xFF238A63)
private val DiagError = Color(0xFFB42318)

class DiagnosticsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(3, 7, 18)
        window.navigationBarColor = android.graphics.Color.rgb(3, 7, 18)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
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

    private fun openNotificationSettings() {
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        )
    }

    private fun checkedTime(): String =
        SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date())

    private fun copyDiagnosticsSummary(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("B&L Morley diagnostics", text))
    }

    private suspend fun backendStatus(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL("${BuildConfig.SUPABASE_URL}/rest/v1/").openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                useCaches = false
            }
            val code = connection.responseCode
            connection.disconnect()
            val reachable = code in 200..499
            reachable to "Pricing/backend service responded with HTTP $code"
        }.getOrElse { false to "Backend check failed: ${it.message ?: "network error"}" }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun DiagnosticsScreen() {
        val scope = rememberCoroutineScope()
        var refresh by remember { mutableIntStateOf(0) }
        var lastChecked by remember { mutableStateOf(checkedTime()) }
        var liveBusy by remember { mutableStateOf(false) }
        var sessionLive by remember { mutableStateOf<Boolean?>(null) }
        var sessionDetail by remember { mutableStateOf("Not checked yet") }
        var backendLive by remember { mutableStateOf<Boolean?>(null) }
        var backendDetail by remember { mutableStateOf("Not checked yet") }
        var otaLive by remember { mutableStateOf<Boolean?>(null) }
        var otaDetail by remember { mutableStateOf("Not checked yet") }
        var copied by remember { mutableStateOf(false) }

        val online = remember(refresh) { isOnline() }
        val signedIn = remember(refresh) { AuthManager.isSignedIn(this) }
        val notifications = remember(refresh) { notificationsAllowed() }
        val installer = remember(refresh) { installerAllowed() }
        val accountLabel = remember(refresh) { AuthManager.accountLabel(this) }

        fun diagnosticsSummary(): String = buildString {
            appendLine("B&L Morley Android Diagnostics")
            appendLine("Checked: $lastChecked")
            appendLine("App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Account session: ${if (signedIn) "Authenticated locally as $accountLabel" else "No active authorised session"}")
            appendLine("Internet connection: ${if (online) "Validated" else "Unavailable"}")
            appendLine("Notifications: ${if (notifications) "Allowed" else "Disabled"}")
            appendLine("OTA installer: ${if (installer) "Ready" else "Permission required"}")
            appendLine("Live account service: $sessionDetail")
            appendLine("Live pricing/backend service: $backendDetail")
            appendLine("Live OTA service: $otaDetail")
            append("Device: ${Build.MANUFACTURER} ${Build.MODEL} • Android ${Build.VERSION.RELEASE}")
        }

        fun runLiveChecks() {
            refresh += 1
            lastChecked = checkedTime()
            copied = false
            if (!isOnline()) {
                sessionLive = false
                sessionDetail = "Live session validation skipped because the device is offline"
                backendLive = false
                backendDetail = "Pricing/backend check skipped because the device is offline"
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

                val backend = backendStatus()
                backendLive = backend.first
                backendDetail = backend.second

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
                if (!notifications) {
                    OutlinedButton(
                        onClick = { openNotificationSettings() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Open Notification Settings") }
                }
                DiagnosticCard("OTA installer", if (installer) "APK installer permission is ready" else "Install unknown apps permission is not enabled", installer)
                if (!installer) {
                    OutlinedButton(
                        onClick = { UpdateManager.openInstallerPermission(this@DiagnosticsActivity) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Enable OTA Installer Permission") }
                }
                DiagnosticCard("Live account service", sessionDetail, sessionLive != false)
                DiagnosticCard("Live pricing/backend service", backendDetail, backendLive != false)
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F684C))
                ) { Text(if (liveBusy) "Running Live Checks…" else "Refresh Diagnostics", fontWeight = FontWeight.Black) }

                if (liveBusy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

                OutlinedButton(
                    onClick = {
                        copyDiagnosticsSummary(diagnosticsSummary())
                        copied = true
                    },
                    enabled = !liveBusy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (copied) "Diagnostics Copied" else "Copy Diagnostics Summary") }

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
            border = BorderStroke(1.dp, if (ok) DiagOk.copy(alpha = .30f) else DiagError.copy(alpha = .65f)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.fillMaxWidth().padding(15.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (ok) "●" else "!", color = if (ok) DiagOk else DiagError, fontWeight = FontWeight.Black)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text(detail, color = DiagMuted, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }
    }
}