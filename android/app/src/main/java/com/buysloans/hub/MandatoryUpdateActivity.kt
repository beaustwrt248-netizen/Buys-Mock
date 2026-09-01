package com.buysloans.hub

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.addCallback
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val GateAccent = Color(0xFF167A5A)
private val GateBg = Color(0xFFF5F7F4)
private val GateCard = Color(0xFFEEF4F0)
private val GateError = Color(0xFFB42318)

class MandatoryUpdateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this) { }
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = GateAccent, background = GateBg, surface = GateCard)) {
                MandatoryUpdateScreen()
            }
        }
    }

    @Composable
    private fun MandatoryUpdateScreen() {
        val scope = rememberCoroutineScope()
        var policy by remember { mutableStateOf<ReleaseSupportPolicy?>(null) }
        var update by remember { mutableStateOf<AppUpdate?>(null) }
        var status by remember { mutableStateOf("Checking required app version…") }
        var busy by remember { mutableStateOf(true) }

        fun refresh() {
            if (busy) return
            busy = true
            status = "Checking required app version…"
            scope.launch {
                runGateCheck(
                    onReady = { p, u -> policy = p; update = u; status = if (u == null) "A required update is not currently available from the verified OTA feed." else "Version ${u.versionName} is ready to install." },
                    onError = { status = it },
                    onComplete = { busy = false }
                )
            }
        }

        LaunchedEffect(Unit) {
            runGateCheck(
                onReady = { p, u -> policy = p; update = u; status = if (u == null) "A required update is not currently available from the verified OTA feed." else "Version ${u.versionName} is ready to install." },
                onError = { status = it },
                onComplete = { busy = false }
            )
        }

        Surface(color = GateBg, modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GateCard),
                    border = BorderStroke(1.dp, GateAccent.copy(alpha = .35f)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Update required", fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text(
                            policy?.let { p ->
                                val name = p.minimumVersionName.ifBlank { "version ${p.minimumVersionCode}" }
                                "This install is below the minimum supported B&L Morley version ($name). Update before continuing."
                            } ?: "B&L Morley is verifying the minimum supported version.",
                            color = MorleyTextSecondary,
                            lineHeight = 22.sp
                        )
                        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            status,
                            color = if (update == null && !busy) GateError else MorleyTextPrimary
                        )
                        update?.let { available ->
                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= 26 && !packageManager.canRequestPackageInstalls()) {
                                        UpdateManager.openInstallerPermission(this@MandatoryUpdateActivity)
                                    } else {
                                        UpdateManager.openDownload(this@MandatoryUpdateActivity, available)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) { Text("Update to ${available.versionName}", fontWeight = FontWeight.Black) }
                        }
                        OutlinedButton(onClick = { refresh() }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                            Text("Check again")
                        }
                    }
                }
            }
        }
    }

    private suspend fun runGateCheck(
        onReady: (ReleaseSupportPolicy, AppUpdate?) -> Unit,
        onError: (String) -> Unit,
        onComplete: () -> Unit
    ) {
        try {
            val currentPolicy = ReleasePolicyManager.load(this)
            if (!currentPolicy.requiresMandatoryUpdate()) {
                finish()
                return
            }
            val available = UpdateManager.check()
            onReady(currentPolicy, available)
        } catch (error: Exception) {
            onError(error.message ?: "Could not verify the required update. Check your connection and try again.")
        } finally {
            onComplete()
        }
    }
}
