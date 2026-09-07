package com.buysloans.hub

import android.os.Bundle
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.launch

class DriveRecoveryActivity : ComponentActivity() {
    private var googleAccount by mutableStateOf<GoogleSignInAccount?>(null)
    private var statusText by mutableStateOf("Connect Google Drive to protect this account.")
    private var snapshot by mutableStateOf<DriveBackupClient.Snapshot?>(null)
    private var busy by mutableStateOf(false)

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
            googleAccount = account
            statusText = "Google Drive connected."
        } catch (error: Exception) {
            statusText = "Google Drive connection failed: ${error.message ?: "authorization was cancelled"}"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(245, 247, 244)
        window.navigationBarColor = android.graphics.Color.rgb(245, 247, 244)
        googleAccount = GoogleSignIn.getLastSignedInAccount(this)?.takeIf { it.grantedScopes.any { scope -> scope.scopeUri == DriveBackupClient.DRIVE_SCOPE } }
        setContent {
            MaterialTheme(colorScheme = MorleyColorScheme) { RecoveryScreen() }
        }
    }

    private fun connectGoogleDrive() {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveBackupClient.DRIVE_SCOPE))
            .build()
        googleSignInLauncher.launch(GoogleSignIn.getClient(this, options).signInIntent)
    }

    private suspend fun accessToken(): String {
        val account = googleAccount ?: error("Connect Google Drive first.")
        return DriveBackupClient.googleAccessToken(this, account)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun RecoveryScreen() {
        val scope = rememberCoroutineScope()
        var pendingRestore by remember { mutableStateOf<DriveBackupClient.BackupRow?>(null) }
        var previewText by remember { mutableStateOf("") }
        var pendingDelete by remember { mutableStateOf<DriveBackupClient.BackupRow?>(null) }

        fun refresh() {
            if (googleAccount == null) {
                statusText = "Connect Google Drive first."
                return
            }
            busy = true
            scope.launch {
                runCatching {
                    val token = accessToken()
                    DriveBackupClient.snapshot(this@DriveRecoveryActivity, token)
                }.onSuccess {
                    snapshot = it
                    statusText = if (it.backups.isEmpty()) "Connected • AES-256-GCM • no backup yet" else "Protected • AES-256-GCM • ${it.backups.size} version${if (it.backups.size == 1) "" else "s"}"
                }.onFailure { statusText = it.message ?: "Could not refresh backup status." }
                busy = false
            }
        }

        LaunchedEffect(googleAccount?.id) {
            if (googleAccount != null) refresh()
        }

        pendingRestore?.let { row ->
            AlertDialog(
                onDismissRequest = { if (!busy) pendingRestore = null },
                title = { Text("Restore encrypted backup?") },
                text = { Text(previewText.ifBlank { "A fresh safety backup will be created first. Only this signed-in Morley user and the matching Google Drive account can restore this version." }) },
                dismissButton = { TextButton(enabled = !busy, onClick = { pendingRestore = null }) { Text("Cancel") } },
                confirmButton = {
                    Button(
                        enabled = !busy,
                        onClick = {
                            busy = true
                            scope.launch {
                                runCatching {
                                    val token = accessToken()
                                    DriveBackupClient.restore(this@DriveRecoveryActivity, token, row.id)
                                    DriveBackupClient.snapshot(this@DriveRecoveryActivity, token)
                                }.onSuccess {
                                    snapshot = it
                                    statusText = "Restore complete. The pre-restore safety backup is retained in Google Drive."
                                    pendingRestore = null
                                }.onFailure { statusText = it.message ?: "Restore failed." }
                                busy = false
                            }
                        }
                    ) { Text(if (busy) "Restoring…" else "Restore") }
                }
            )
        }

        pendingDelete?.let { row ->
            AlertDialog(
                onDismissRequest = { if (!busy) pendingDelete = null },
                title = { Text("Delete this backup version?") },
                text = { Text("This deletes only the selected encrypted version from this user's Google Drive app data.") },
                dismissButton = { TextButton(enabled = !busy, onClick = { pendingDelete = null }) { Text("Cancel") } },
                confirmButton = {
                    Button(
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7D2B38)),
                        onClick = {
                            busy = true
                            scope.launch {
                                runCatching {
                                    val token = accessToken()
                                    DriveBackupClient.delete(this@DriveRecoveryActivity, token, row.id)
                                    DriveBackupClient.snapshot(this@DriveRecoveryActivity, token)
                                }.onSuccess {
                                    snapshot = it
                                    statusText = "Backup version deleted."
                                    pendingDelete = null
                                }.onFailure { statusText = it.message ?: "Could not delete backup." }
                                busy = false
                            }
                        }
                    ) { Text("Delete") }
                }
            )
        }

        Scaffold(
            containerColor = MorleyBackground,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MorleyBackground, titleContentColor = MorleyTextPrimary),
                    navigationIcon = { IconButton(onClick = { finish() }) { Text("‹", fontSize = 34.sp, color = MorleyAccent) } },
                    title = { Text("Backup & Data", fontWeight = FontWeight.Black) }
                )
            }
        ) { padding ->
            Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MorleySurface),
                    border = BorderStroke(1.dp, MorleyBorder),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Encrypted Google Drive backup", fontSize = 23.sp, fontWeight = FontWeight.Black, color = MorleyTextPrimary)
                        Text("Your workspace is encrypted before upload and stored in the connected user's private Google Drive app data. Login credentials, Supabase sessions and Google tokens are never included.", color = MorleyTextSecondary)
                    }
                }

                RecoveryStat("MORLEY ACCOUNT", AuthManager.accountLabel(this@DriveRecoveryActivity))
                RecoveryStat("GOOGLE ACCOUNT", snapshot?.googleEmail?.ifBlank { null } ?: googleAccount?.email ?: "Not connected")
                RecoveryStat("ENCRYPTION", "AES-256-GCM")
                RecoveryStat("RETENTION", "Up to 30 encrypted versions")
                RecoveryStat("LAST BACKUP", snapshot?.lastBackupAt?.takeIf { it.isNotBlank() }?.let(::friendlyDate) ?: "Never")

                Button(
                    onClick = { connectGoogleDrive() },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MorleyAccent)
                ) { Text(if (googleAccount == null) "Connect Google Drive" else "Change Google Account", fontWeight = FontWeight.Black) }

                Button(
                    onClick = {
                        busy = true
                        scope.launch {
                            runCatching {
                                val token = accessToken()
                                DriveBackupClient.createBackup(this@DriveRecoveryActivity, token)
                                DriveBackupClient.snapshot(this@DriveRecoveryActivity, token)
                            }.onSuccess {
                                snapshot = it
                                statusText = "Encrypted Google Drive backup complete."
                            }.onFailure { statusText = it.message ?: "Backup failed." }
                            busy = false
                        }
                    },
                    enabled = !busy && googleAccount != null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F684C))
                ) { Text(if (busy) "Working…" else "Back Up Now", fontWeight = FontWeight.Black) }

                OutlinedButton(onClick = { refresh() }, enabled = !busy && googleAccount != null, modifier = Modifier.fillMaxWidth()) { Text("Refresh Backup Status") }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE9F4EF)),
                    border = BorderStroke(1.dp, MorleyBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(statusText, Modifier.padding(14.dp), color = MorleyTextPrimary) }

                Text("Backup history", fontSize = 21.sp, fontWeight = FontWeight.Black, color = MorleyTextPrimary)
                val rows = snapshot?.backups.orEmpty()
                if (rows.isEmpty()) {
                    Text("No encrypted Google Drive backups yet.", color = MorleyTextSecondary)
                }
                rows.take(12).forEach { row ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MorleySurface),
                        border = BorderStroke(1.dp, MorleyBorder),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(friendlyDate(row.createdAt), fontWeight = FontWeight.Black, color = MorleyTextPrimary)
                            Text("${friendlyBytes(row.byteSize)} • ${row.status}", color = MorleyTextSecondary, fontSize = 12.sp)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                OutlinedButton(
                                    enabled = !busy,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        busy = true
                                        scope.launch {
                                            runCatching { DriveBackupClient.verify(this@DriveRecoveryActivity, accessToken(), row.id) }
                                                .onSuccess { statusText = "Backup integrity verified." }
                                                .onFailure { statusText = it.message ?: "Integrity verification failed." }
                                            busy = false
                                        }
                                    }
                                ) { Text("Verify") }
                                Button(
                                    enabled = !busy,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        busy = true
                                        scope.launch {
                                            runCatching { DriveBackupClient.previewRestore(this@DriveRecoveryActivity, accessToken(), row.id) }
                                                .onSuccess { preview ->
                                                    val changes = preview.optJSONObject("changes")
                                                    previewText = buildString {
                                                        append("Restore ${friendlyDate(row.createdAt)}?\n\n")
                                                        if (changes != null) {
                                                            append("Local inventory: ${changes.optInt("local_inventory", 0)}\n")
                                                            append("Local sales: ${changes.optInt("local_sales", 0)}\n")
                                                            append("Valuation history: ${changes.optInt("valuation_history_rows", 0)} rows\n\n")
                                                        }
                                                        append("A fresh encrypted safety backup will be created before anything is replaced.")
                                                    }
                                                    pendingRestore = row
                                                }
                                                .onFailure { statusText = it.message ?: "Restore preview failed." }
                                            busy = false
                                        }
                                    }
                                ) { Text("Restore") }
                                TextButton(enabled = !busy, onClick = { pendingDelete = row }, modifier = Modifier.weight(1f)) { Text("Delete", color = Color(0xFF7D2B38)) }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    @Composable
    private fun RecoveryStat(label: String, value: String) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MorleySurface),
            border = BorderStroke(1.dp, MorleyBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                Text(label, color = MorleyTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(value, color = MorleyTextPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }

    private fun friendlyDate(value: String): String = value
        .replace('T', ' ')
        .replace(Regex("\\.\\d+Z$"), " UTC")
        .replace("Z", " UTC")
        .ifBlank { "Unknown time" }

    private fun friendlyBytes(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    }
}
