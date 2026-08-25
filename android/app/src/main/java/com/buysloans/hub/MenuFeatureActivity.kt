package com.buysloans.hub

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.firebase.messaging.FirebaseMessaging
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val MFAccent = Color(0xFF16C7FF)
private val MFStrong = Color(0xFF2684FF)
private val MFBg = Color(0xFF030712)
private val MFCard = Color(0xFF0B1528)
private val MFMuted = Color(0xFF8EA6C4)

class MenuFeatureActivity : ComponentActivity() {
    companion object { const val EXTRA_FEATURE = "feature" }

    private var notice by mutableStateOf("")

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                DeviceRegistrar.register(this, token)
            }
            notice = "Notifications enabled."
        } else notice = "Notification permission was not granted."
    }

    private val createBackup = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            runCatching {
                contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write(WorkspaceStore.exportJson(this))
                } ?: error("Unable to open backup destination.")
            }.onSuccess {
                notice = "Backup exported successfully."
            }.onFailure { error ->
                notice = "Backup failed: ${error.message ?: "unknown error"}"
            }
        }
    }

    private val openBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Unable to open backup file.")
                WorkspaceStore.importJson(this, text)
            }.onSuccess {
                notice = "Backup imported successfully."
            }.onFailure { error ->
                notice = "Import failed: ${error.message ?: "unknown error"}"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(3, 7, 18)
        window.navigationBarColor = android.graphics.Color.rgb(3, 7, 18)
        val feature = intent.getStringExtra(EXTRA_FEATURE).orEmpty()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = MFAccent,
                    secondary = MFStrong,
                    background = MFBg,
                    surface = MFCard
                )
            ) { FeatureScreen(feature) }
        }
    }

    private fun openNotificationSettings() {
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        )
    }

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                DeviceRegistrar.register(this, token)
            }
            notice = "Notifications are enabled."
        }
    }

    private fun emailIssue() {
        val body = """B&L Morley issue report

Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
Device: ${Build.MANUFACTURER} ${Build.MODEL}
Android: ${Build.VERSION.RELEASE}
Signed in as: ${AuthManager.accountLabel(this)}

What happened:
"""
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
            .putExtra(Intent.EXTRA_SUBJECT, "B&L Morley issue - ${BuildConfig.VERSION_NAME}")
            .putExtra(Intent.EXTRA_TEXT, body)
        runCatching { startActivity(Intent.createChooser(intent, "Report B&L Morley issue")) }
            .onFailure { notice = "No email app is available on this device." }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun FeatureScreen(feature: String) {
        val title = when (feature) {
            "inventory" -> "Inventory"
            "sales" -> "Sales History"
            "scanner" -> "Barcode Scanner"
            "account" -> "Account & Profile"
            "privacy" -> "Privacy & Security"
            "backup" -> "Backup & Data"
            "notifications" -> "Notifications"
            "display" -> "Display"
            "updates" -> "Updates"
            "report" -> "Report an Issue"
            "legal" -> "Legal & Privacy"
            "about" -> "About B&L Morley"
            else -> "B&L Morley"
        }

        Scaffold(
            containerColor = MFBg,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF050B16),
                        titleContentColor = Color.White
                    ),
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Text("‹", fontSize = 34.sp, color = MFAccent)
                        }
                    },
                    title = { Text(title, fontWeight = FontWeight.Black) }
                )
            }
        ) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (notice.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E2B35)),
                        border = BorderStroke(1.dp, MFAccent.copy(alpha = .4f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(notice, Modifier.padding(14.dp), color = Color.White) }
                }

                when (feature) {
                    "inventory" -> InventoryFeature()
                    "sales" -> SalesFeature()
                    "scanner" -> ScannerFeature()
                    "account" -> AccountFeature()
                    "privacy" -> PrivacyFeature()
                    "backup" -> BackupFeature()
                    "notifications" -> NotificationsFeature()
                    "display" -> DisplayFeature()
                    "updates" -> UpdatesFeature()
                    "report" -> ReportFeature()
                    "legal" -> LegalFeature()
                    "about" -> AboutFeature()
                    else -> InfoCard("Unknown option", "This menu option is not available in this build.")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    @Composable
    private fun InventoryFeature() {
        val activity = this@MenuFeatureActivity
        var name by remember { mutableStateOf("") }
        var barcode by remember { mutableStateOf("") }
        var cost by remember { mutableStateOf("") }
        var resale by remember { mutableStateOf("") }
        var qty by remember { mutableStateOf("1") }
        var refresh by remember { mutableIntStateOf(0) }
        val items = remember(refresh) { WorkspaceStore.inventory(activity) }

        InfoCard(
            "Stock control",
            "Add stock, keep barcode/serial details, track cost and resale value, then mark an item sold directly into Sales History."
        )
        CardBlock {
            Text("Add inventory item", fontSize = 20.sp, fontWeight = FontWeight.Black)
            OutlinedTextField(name, { name = it }, label = { Text("Item name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(barcode, { barcode = it }, label = { Text("Barcode / serial") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(cost, { cost = it }, label = { Text("Cost") }, modifier = Modifier.weight(1f))
                OutlinedTextField(resale, { resale = it }, label = { Text("Resale") }, modifier = Modifier.weight(1f))
                OutlinedTextField(qty, { qty = it }, label = { Text("Qty") }, modifier = Modifier.weight(.7f))
            }
            Button(
                onClick = {
                    runCatching {
                        WorkspaceStore.addInventory(
                            activity,
                            name,
                            barcode,
                            cost.toDoubleOrNull() ?: 0.0,
                            resale.toDoubleOrNull() ?: 0.0,
                            qty.toIntOrNull() ?: 1
                        )
                    }.onSuccess {
                        name = ""; barcode = ""; cost = ""; resale = ""; qty = "1"; refresh++
                        notice = "Item added to inventory."
                    }.onFailure { error -> notice = error.message.orEmpty() }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MFStrong)
            ) { Text("Add Item", fontWeight = FontWeight.Black) }
        }

        Text("Current stock (${items.sumOf { it.quantity }} units)", fontSize = 20.sp, fontWeight = FontWeight.Black)
        if (items.isEmpty()) Text("No stock yet.", color = MFMuted)
        items.forEach { item ->
            CardBlock {
                Text(item.name, fontWeight = FontWeight.Black, fontSize = 17.sp)
                if (item.barcode.isNotBlank()) Text("Barcode / serial: ${item.barcode}", color = MFMuted, fontSize = 12.sp)
                Text("Qty ${item.quantity}  •  Cost ${formatMoney(item.cost)}  •  Resale ${formatMoney(item.resale)}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            WorkspaceStore.sellOne(activity, item.id, item.resale)
                            refresh++
                            notice = "${item.name} moved to Sales History."
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF168E61))
                    ) { Text("Mark Sold") }
                    OutlinedButton(
                        onClick = { WorkspaceStore.deleteInventory(activity, item.id); refresh++ },
                        modifier = Modifier.weight(1f)
                    ) { Text("Delete") }
                }
            }
        }
    }

    @Composable
    private fun SalesFeature() {
        val activity = this@MenuFeatureActivity
        var refresh by remember { mutableIntStateOf(0) }
        val sales = remember(refresh) { WorkspaceStore.sales(activity) }
        val revenue = sales.sumOf { it.salePrice * it.quantity }
        val cost = sales.sumOf { it.cost * it.quantity }

        InfoCard("Sales summary", "Revenue ${formatMoney(revenue)}  •  Cost ${formatMoney(cost)}  •  Gross profit ${formatMoney(revenue - cost)}")
        if (sales.isEmpty()) Text("No completed sales yet.", color = MFMuted)
        sales.forEach { sale ->
            CardBlock {
                Text(sale.name, fontWeight = FontWeight.Black, fontSize = 17.sp)
                Text("Sold ${formatDate(sale.soldAt)}", color = MFMuted, fontSize = 12.sp)
                Text("Sale ${formatMoney(sale.salePrice)}  •  Cost ${formatMoney(sale.cost)}  •  Profit ${formatMoney(sale.salePrice - sale.cost)}")
                OutlinedButton(
                    onClick = { WorkspaceStore.deleteSale(activity, sale.id); refresh++ },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Remove sale record") }
            }
        }
    }

    @Composable
    private fun ScannerFeature() {
        val activity = this@MenuFeatureActivity
        var code by remember { mutableStateOf("") }
        var match by remember { mutableStateOf<StockItem?>(null) }
        var scanning by remember { mutableStateOf(false) }
        val scanner = remember(activity) { GmsBarcodeScanning.getClient(activity) }

        InfoCard(
            "Barcode scanner",
            "Scan with the device camera using Google Play services, or enter a barcode/serial manually, then match it against current inventory."
        )
        Button(
            onClick = {
                scanning = true
                scanner.startScan()
                    .addOnSuccessListener { barcodeResult ->
                        val scannedValue = barcodeResult.rawValue.orEmpty().trim()
                        code = scannedValue
                        match = if (scannedValue.isBlank()) null else WorkspaceStore.findByBarcode(activity, scannedValue)
                        notice = when {
                            scannedValue.isBlank() -> "No barcode value was returned."
                            match == null -> "Scanned $scannedValue — not currently in inventory."
                            else -> "Found ${match?.name}."
                        }
                        scanning = false
                    }
                    .addOnCanceledListener {
                        notice = "Scanner cancelled."
                        scanning = false
                    }
                    .addOnFailureListener { error ->
                        notice = "Camera scanner failed: ${error.message ?: "unknown error"}"
                        scanning = false
                    }
            },
            enabled = !scanning,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MFStrong)
        ) { Text(if (scanning) "Opening Camera…" else "Scan Barcode with Camera", fontWeight = FontWeight.Black) }

        OutlinedTextField(code, { code = it }, label = { Text("Barcode / serial") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                match = WorkspaceStore.findByBarcode(activity, code)
                notice = if (match == null) "No matching stock found." else "Found ${match?.name}."
            },
            enabled = code.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Find in Inventory") }
        match?.let { InfoCard(it.name, "Qty ${it.quantity} • Cost ${formatMoney(it.cost)} • Resale ${formatMoney(it.resale)}") }
    }

    @Composable
    private fun AccountFeature() {
        val activity = this@MenuFeatureActivity
        val scope = rememberCoroutineScope()
        var name by remember { mutableStateOf(AuthManager.displayName(activity)) }
        var busy by remember { mutableStateOf(false) }
        var refreshKey by remember { mutableIntStateOf(0) }
        val accountLabel = remember(refreshKey) { AuthManager.accountLabel(activity) }
        val email = remember(refreshKey) { AuthManager.email(activity) }

        InfoCard("Signed-in account", accountLabel)
        CardBlock {
            Text("Email", color = MFMuted, fontSize = 12.sp)
            Text(email.ifBlank { "Not available" })
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("First and last name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy
            )
            Button(
                onClick = {
                    busy = true
                    scope.launch {
                        runCatching { AuthManager.updateDisplayName(activity, name) }
                            .onSuccess {
                                refreshKey++
                                name = AuthManager.displayName(activity)
                                notice = "Profile name updated."
                            }
                            .onFailure { error -> notice = error.message.orEmpty() }
                        busy = false
                    }
                },
                enabled = !busy && name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (busy) "Saving…" else "Save Profile") }
            OutlinedButton(
                onClick = {
                    busy = true
                    scope.launch {
                        runCatching { AuthManager.validAccessToken(activity) }
                            .onSuccess {
                                refreshKey++
                                name = AuthManager.displayName(activity)
                                notice = "Account profile refreshed."
                            }
                            .onFailure { error -> notice = error.message.orEmpty() }
                        busy = false
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Refresh Account") }
        }
    }

    @Composable
    private fun PrivacyFeature() {
        val activity = this@MenuFeatureActivity
        val scope = rememberCoroutineScope()
        var busy by remember { mutableStateOf(false) }
        InfoCard("Session security", "B&L Morley uses an authorised Supabase session. Authentication tokens are excluded from workspace backups, and you can revoke every active session from this device.")
        Button(
            onClick = {
                busy = true
                scope.launch {
                    runCatching { AuthManager.validAccessToken(activity) }
                        .onSuccess { notice = "Secure session is valid." }
                        .onFailure { error -> notice = error.message.orEmpty() }
                    busy = false
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (busy) "Checking…" else "Check Secure Session") }
        OutlinedButton(
            onClick = {
                AuthManager.signOut(activity)
                startActivity(Intent(activity, AuthActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                finish()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy
        ) { Text("Sign out this device") }
        OutlinedButton(
            onClick = {
                busy = true
                scope.launch {
                    runCatching { AuthManager.signOutEverywhere(activity) }
                        .onSuccess {
                            startActivity(Intent(activity, AuthActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                            finish()
                        }
                        .onFailure { error -> notice = error.message.orEmpty() }
                    busy = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy
        ) { Text("Sign out all devices") }
    }

    @Composable
    private fun BackupFeature() {
        val activity = this@MenuFeatureActivity
        var confirmClear by remember { mutableStateOf(false) }
        InfoCard("Workspace backup", "Export inventory and sales data to a JSON backup. Login credentials and authentication tokens are deliberately excluded.")
        Button(
            onClick = { createBackup.launch("BL-Morley-backup-${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.json") },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Export Backup") }
        OutlinedButton(
            onClick = { openBackup.launch(arrayOf("application/json", "text/plain")) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Import Backup") }
        OutlinedButton(
            onClick = { confirmClear = true },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Clear Local Workspace Data") }
        if (confirmClear) {
            AlertDialog(
                onDismissRequest = { confirmClear = false },
                title = { Text("Clear local data?") },
                text = { Text("This removes the local workspace from this device. Export a backup first if you may need it later.") },
                confirmButton = {
                    TextButton(onClick = {
                        WorkspaceStore.clearWorkspace(activity)
                        confirmClear = false
                        notice = "Local workspace data cleared."
                    }) { Text("Clear") }
                },
                dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
            )
        }
    }

    @Composable
    private fun NotificationsFeature() {
        val activity = this@MenuFeatureActivity
        val prefs = activity.getSharedPreferences("notification_settings", MODE_PRIVATE)
        var updateAlerts by remember { mutableStateOf(prefs.getBoolean("update_alerts", true)) }
        InfoCard("Notifications", "Control B&L Morley update alerts and Android notification permission from one place.")
        CardBlock {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Update alerts", fontWeight = FontWeight.Black)
                    Text("Allow B&L Morley to surface new app version notifications.", color = MFMuted, fontSize = 12.sp)
                }
                Switch(
                    checked = updateAlerts,
                    onCheckedChange = {
                        updateAlerts = it
                        prefs.edit().putBoolean("update_alerts", it).apply()
                        notice = if (it) "Update alerts enabled." else "Update alerts disabled."
                    }
                )
            }
        }
        Button(onClick = { requestNotifications() }, modifier = Modifier.fillMaxWidth()) { Text("Enable Android Notifications") }
        OutlinedButton(onClick = { openNotificationSettings() }, modifier = Modifier.fillMaxWidth()) { Text("Open Android Notification Settings") }
    }

    @Composable
    private fun DisplayFeature() {
        val activity = this@MenuFeatureActivity
        val prefs = activity.getSharedPreferences("display_settings", MODE_PRIVATE)
        var keepAwake by remember { mutableStateOf(prefs.getBoolean("keep_awake", false)) }
        var reducedMotion by remember { mutableStateOf(prefs.getBoolean("reduced_motion", false)) }
        var compact by remember { mutableStateOf(prefs.getBoolean("compact_interface", false)) }
        InfoCard("Display preferences", "Save interface preferences for the native app. Keep-awake applies immediately; accessibility preferences are retained for screens that support them.")
        CardBlock {
            SettingToggle("Keep screen awake", "Useful during valuation and stock entry.", keepAwake) { enabled ->
                keepAwake = enabled
                prefs.edit().putBoolean("keep_awake", enabled).apply()
                if (enabled) activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                notice = if (enabled) "Keep-awake enabled." else "Keep-awake disabled."
            }
            SettingToggle("Reduced motion", "Prefer fewer interface animations where supported.", reducedMotion) { enabled ->
                reducedMotion = enabled
                prefs.edit().putBoolean("reduced_motion", enabled).apply()
                notice = if (enabled) "Reduced motion enabled." else "Reduced motion disabled."
            }
            SettingToggle("Compact interface", "Prefer denser controls where supported.", compact) { enabled ->
                compact = enabled
                prefs.edit().putBoolean("compact_interface", enabled).apply()
                notice = if (enabled) "Compact interface enabled." else "Compact interface disabled."
            }
        }
    }

    @Composable
    private fun UpdatesFeature() {
        val activity = this@MenuFeatureActivity
        InfoCard("Installed version", "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        InfoCard("OTA updates", "Use the built-in update screen to check the signed release manifest, view release notes and install an available APK update.")
        Button(
            onClick = { startActivity(Intent(activity, UpdateActivity::class.java)) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Check for Updates") }
    }

    @Composable
    private fun ReportFeature() {
        InfoCard("Report an issue", "Create a pre-filled issue email containing app version, device, Android version and signed-in account details. Add what happened before sending it.")
        Button(onClick = { emailIssue() }, modifier = Modifier.fillMaxWidth()) { Text("Create Issue Report") }
    }

    @Composable
    private fun LegalFeature() {
        InfoCard("Privacy", "B&L Morley is a private business system for authorised accounts. Workspace backups exclude authentication tokens. Pricing requests may use configured external pricing services to return market evidence.")
        InfoCard("Local data", "Inventory and sales created in the native workspace are stored locally on this device unless you explicitly export them.")
        InfoCard("Account security", "Authentication is handled by Supabase. Session validation can revoke access when an account is disabled, and global sign-out can revoke active sessions across devices.")
    }

    @Composable
    private fun AboutFeature() {
        InfoCard("B&L Morley", "Buys & Loans Hub\nVersion ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n\nNative valuation, inventory, barcode, sales, account, backup and secure update workspace.")
        InfoCard("Release channel", "Signed production APK with OTA update support and a shared desktop web companion.")
    }

    @Composable
    private fun SettingToggle(title: String, subtitle: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Black)
                Text(subtitle, color = MFMuted, fontSize = 12.sp)
            }
            Switch(checked = checked, onCheckedChange = onChanged)
        }
    }

    @Composable
    private fun CardBlock(content: @Composable ColumnScope.() -> Unit) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MFCard),
            border = BorderStroke(1.dp, MFAccent.copy(alpha = .18f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        }
    }

    @Composable
    private fun InfoCard(title: String, body: String) {
        CardBlock {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(body, color = MFMuted, lineHeight = 20.sp)
        }
    }

    private fun formatMoney(value: Double): String =
        NumberFormat.getCurrencyInstance(Locale("en", "AU")).apply { maximumFractionDigits = 0 }.format(value)

    private fun formatDate(value: Long): String =
        SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault()).format(Date(value))
}
