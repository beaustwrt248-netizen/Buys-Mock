package com.buysloans.hub

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MaintenanceActivity : ComponentActivity() {
    companion object {
        const val EXTRA_MESSAGE = "maintenance_message"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(3, 7, 18)
        window.navigationBarColor = android.graphics.Color.rgb(3, 7, 18)
        val initialMessage = intent.getStringExtra(EXTRA_MESSAGE)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: MaintenanceModePolicy.DEFAULT_MESSAGE
        setContent {
            var message by remember { mutableStateOf(initialMessage) }
            var checking by remember { mutableStateOf(false) }
            var status by remember { mutableStateOf("") }
            MaintenanceScreen(
                message = message,
                checking = checking,
                status = status,
                onRetry = {
                    checking = true
                    status = "Checking maintenance status…"
                    lifecycleScope.launch {
                        runCatching { MaintenanceModeClient.fetch(this@MaintenanceActivity) }
                            .onSuccess { state ->
                                when {
                                    state == null -> status = "Could not verify maintenance status. Please try again."
                                    state.enabled -> {
                                        message = state.message
                                        status = "Maintenance is still in progress."
                                    }
                                    else -> {
                                        startActivity(Intent(this@MaintenanceActivity, DashboardActivity::class.java).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                        })
                                        finish()
                                    }
                                }
                            }
                            .onFailure { status = "Could not verify maintenance status. Please try again." }
                        checking = false
                    }
                },
                onSignOut = {
                    AuthManager.signOut(this@MaintenanceActivity)
                    startActivity(Intent(this@MaintenanceActivity, AuthActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    })
                    finish()
                }
            )
        }
    }
}

@Composable
private fun MaintenanceScreen(
    message: String,
    checking: Boolean,
    status: String,
    onRetry: () -> Unit,
    onSignOut: () -> Unit
) {
    val bg = Color(0xFFF5F7F4)
    val card = Color(0xFFEEF4F0)
    val accent = Color(0xFF167A5A)
    MaterialTheme(colorScheme = lightColorScheme(primary = accent, background = bg, surface = card)) {
        Surface(color = bg, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("B&L Morley", fontSize = 30.sp, fontWeight = FontWeight.Black, color = MorleyTextPrimary)
                Spacer(Modifier.height(10.dp))
                Text("Maintenance mode", color = accent, fontSize = 25.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(18.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = card),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(message, color = MorleyTextPrimary, lineHeight = 22.sp)
                        Text("Your account and saved data are not changed while maintenance mode is active.", color = MorleyTextSecondary, fontSize = 13.sp)
                    }
                }
                if (status.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Text(status, color = MorleyTextSecondary, fontSize = 13.sp)
                }
                Spacer(Modifier.height(18.dp))
                Button(onClick = onRetry, enabled = !checking, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                    Text(if (checking) "Checking…" else "Try again", fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = onSignOut, enabled = !checking, modifier = Modifier.fillMaxWidth()) {
                    Text("Sign out")
                }
            }
        }
    }
}
