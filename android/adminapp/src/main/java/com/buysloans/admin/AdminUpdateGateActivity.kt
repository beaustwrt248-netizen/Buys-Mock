package com.buysloans.admin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
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
import kotlinx.coroutines.launch

/**
 * Launcher gate for the dedicated Admin OTA channel.
 *
 * Update discovery fails open so an unavailable metadata host never blocks Admin access.
 * APK installation remains an explicit Android package-installer action and the downloaded
 * package is SHA-256 verified before that handoff.
 */
class AdminUpdateGateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF0B6D54),
                    onPrimary = Color.White,
                    background = Color(0xFFF7FBF9),
                    onBackground = Color(0xFF17231F),
                    surface = Color.White,
                    onSurface = Color(0xFF17231F),
                    outline = Color(0xFFD7E1DC)
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var checking by remember { mutableStateOf(true) }
                    var release by remember { mutableStateOf<AdminUpdateRelease?>(null) }
                    var status by remember { mutableStateOf("Checking the signed Admin release channel…") }
                    var installing by remember { mutableStateOf(false) }
                    val scope = rememberCoroutineScope()

                    fun continueToAdmin() {
                        startActivity(Intent(this@AdminUpdateGateActivity, AdminActivity::class.java))
                        finish()
                    }

                    LaunchedEffect(Unit) {
                        runCatching { AdminUpdateManager.fetchRelease() }
                            .onSuccess { candidate ->
                                if (shouldOfferAdminUpdate(BuildConfig.VERSION_CODE, candidate)) {
                                    release = candidate
                                    status = "Admin ${candidate.versionName} is ready."
                                    checking = false
                                } else {
                                    continueToAdmin()
                                }
                            }
                            .onFailure {
                                // OTA availability must never lock administrators out of the app.
                                continueToAdmin()
                            }
                    }

                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("MORLEY ADMIN", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text("Secure Admin update", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(12.dp))
                        Text("Installed ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                        release?.let { Text("Available ${it.versionName} (${it.versionCode})") }
                        Spacer(Modifier.height(12.dp))
                        Text(status)
                        Text("Only the trusted Buys-Mock Admin release channel is accepted. The APK checksum is verified before Android is asked to install it.", style = MaterialTheme.typography.bodySmall)
                        if (checking || installing) {
                            Spacer(Modifier.height(16.dp))
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                        release?.let { candidate ->
                            Spacer(Modifier.height(18.dp))
                            Button(
                                onClick = {
                                    installing = true
                                    status = "Downloading and verifying Admin ${candidate.versionName}…"
                                    scope.launch {
                                        runCatching { AdminUpdateManager.downloadVerifiedApk(this@AdminUpdateGateActivity, candidate) }
                                            .onSuccess { apk ->
                                                installing = false
                                                val launched = AdminUpdateManager.launchInstaller(this@AdminUpdateGateActivity, apk)
                                                status = if (launched) {
                                                    "Android installer opened. Confirm the signed Admin update."
                                                } else {
                                                    "Allow Morley Admin to install updates, then return and tap Update now again."
                                                }
                                            }
                                            .onFailure { error ->
                                                installing = false
                                                status = error.message ?: "The Admin update could not be verified."
                                            }
                                    }
                                },
                                enabled = !installing,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(if (installing) "Verifying…" else "Update now", fontWeight = FontWeight.Black) }
                            if (!candidate.required) {
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(onClick = { continueToAdmin() }, enabled = !installing, modifier = Modifier.fillMaxWidth()) {
                                    Text("Not now")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
