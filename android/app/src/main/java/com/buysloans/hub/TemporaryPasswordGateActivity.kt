package com.buysloans.hub

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val TempPasswordBg = Color(0xFFF5F7F4)
private val TempPasswordAccent = Color(0xFF167A5A)
private val TempPasswordMuted = MorleyTextSecondary

internal fun temporaryPasswordRequired(userJson: String): Boolean = runCatching {
    JSONObject(userJson).optJSONObject("user_metadata")?.optBoolean("must_change_password", false) == true
}.getOrDefault(false)

class TemporaryPasswordGateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = TempPasswordAccent, background = TempPasswordBg)) {
                Surface(Modifier.fillMaxSize(), color = TempPasswordBg) {
                    TemporaryPasswordGate(
                        onContinue = {
                            startActivity(Intent(this, DashboardActivity::class.java))
                            finish()
                        },
                        onSignOut = {
                            AuthManager.signOut(this)
                            startActivity(Intent(this, AuthActivity::class.java))
                            finish()
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun TemporaryPasswordGate(onContinue: () -> Unit, onSignOut: () -> Unit) {
        val scope = rememberCoroutineScope()
        var checking by remember { mutableStateOf(true) }
        var required by remember { mutableStateOf(false) }
        var busy by remember { mutableStateOf(false) }
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var error by remember { mutableStateOf("") }

        LaunchedEffect(Unit) {
            runCatching {
                val token = AuthManager.validAccessToken(this@TemporaryPasswordGateActivity)
                val response = authUserRequest("GET", token, null)
                if (response.first !in 200..299) error("Could not verify your password status.")
                temporaryPasswordRequired(response.second)
            }.onSuccess { needsChange ->
                required = needsChange
                checking = false
                if (!needsChange) onContinue()
            }.onFailure {
                error = it.message ?: "Could not verify your account."
                checking = false
            }
        }

        Column(
            Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.Center
        ) {
            if (checking) {
                CircularProgressIndicator()
                Spacer(Modifier.height(18.dp))
                Text("Checking account security…", color = TempPasswordMuted)
                return@Column
            }

            if (required) {
                Text("B&L Morley", fontSize = 32.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(6.dp))
                Text("Change temporary password", color = TempPasswordAccent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Your account was created with a temporary password. Choose a new password before continuing.", color = TempPasswordMuted)
                Spacer(Modifier.height(18.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it.take(256) },
                    label = { Text("New password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it.take(256) },
                    label = { Text("Confirm new password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        error = ""
                        if (newPassword.length < 10) {
                            error = "New password must be at least 10 characters."
                            return@Button
                        }
                        if (newPassword != confirmPassword) {
                            error = "Passwords do not match."
                            return@Button
                        }
                        busy = true
                        scope.launch {
                            runCatching {
                                val token = AuthManager.validAccessToken(this@TemporaryPasswordGateActivity)
                                val payload = JSONObject()
                                    .put("password", newPassword)
                                    .put("data", JSONObject().put("must_change_password", false))
                                val response = authUserRequest("PUT", token, payload)
                                if (response.first !in 200..299) {
                                    val message = runCatching {
                                        JSONObject(response.second).optString("msg")
                                            .ifBlank { JSONObject(response.second).optString("message") }
                                    }.getOrDefault("")
                                    error(message.ifBlank { "Password could not be changed." })
                                }
                            }.onSuccess {
                                busy = false
                                onContinue()
                            }.onFailure {
                                error = it.message ?: "Password could not be changed."
                                busy = false
                            }
                        }
                    },
                    enabled = !busy && newPassword.length >= 10 && confirmPassword.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) { Text(if (busy) "Changing password…" else "Change password & continue", fontWeight = FontWeight.Black) }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onSignOut, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
            } else if (error.isNotBlank()) {
                Text("Account security check failed", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(error, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(14.dp))
                OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("Return to sign in") }
            }
        }
    }

    private suspend fun authUserRequest(method: String, token: String, payload: JSONObject?): Pair<Int, String> =
        withContext(Dispatchers.IO) {
            val connection = (URL("${BuildConfig.SUPABASE_URL}/auth/v1/user").openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "application/json")
                if (payload != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
            }
            try {
                if (payload != null) connection.outputStream.use { it.write(payload.toString().toByteArray()) }
                val code = connection.responseCode
                val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                code to text
            } finally {
                connection.disconnect()
            }
        }
}
