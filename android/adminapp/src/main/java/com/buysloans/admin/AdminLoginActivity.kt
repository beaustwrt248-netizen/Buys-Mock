package com.buysloans.admin

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch

private const val ADMIN_TURNSTILE_PAGE = "https://buyshub.me/admin/turnstile.html"

class AdminLoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdminTelemetry.installCrashHandler(applicationContext)
        window.decorView.setBackgroundColor(AndroidColor.rgb(4, 9, 18))
        setContent {
            AdminLoginScreen { session ->
                AdminSessionStore.set(session)
                startActivity(
                    Intent(this, AdminActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
                finish()
            }
        }
    }
}

private class AdminTurnstileBridge(
    private val onToken: (String) -> Unit,
    private val onExpired: () -> Unit,
    private val onError: (String) -> Unit
) {
    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onToken(token: String) = main.post { onToken.invoke(token) }

    @JavascriptInterface
    fun onExpired(ignored: String) = main.post { onExpired.invoke() }

    @JavascriptInterface
    fun onError(code: String) = main.post { onError.invoke(code) }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun AdminTurnstileChallenge(
    refreshKey: Int,
    onToken: (String) -> Unit,
    onExpired: () -> Unit,
    onError: (String) -> Unit
) {
    key(refreshKey) {
        AndroidView(
            modifier = Modifier.fillMaxWidth().height(118.dp),
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = false
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.databaseEnabled = false
                    settings.setSupportMultipleWindows(false)
                    if (Build.VERSION.SDK_INT >= 26) settings.safeBrowsingEnabled = true
                    addJavascriptInterface(
                        AdminTurnstileBridge(onToken, onExpired, onError),
                        "AndroidBridge"
                    )
                    webViewClient = object : WebViewClient() {}
                    loadUrl(ADMIN_TURNSTILE_PAGE)
                }
            },
            onRelease = { webView ->
                webView.stopLoading()
                webView.removeJavascriptInterface("AndroidBridge")
                webView.destroy()
            }
        )
    }
}

@Composable
private fun AdminLoginScreen(onSignedIn: (AdminSession) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var captchaToken by remember { mutableStateOf("") }
    var captchaRefresh by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun resetCaptcha() {
        captchaToken = ""
        captchaRefresh++
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF4CC9F0),
            onPrimary = Color(0xFF001018),
            background = Color(0xFF040912),
            onBackground = Color(0xFFF4F8FF),
            surface = Color(0xFF081426),
            onSurface = Color(0xFFF4F8FF),
            outline = Color(0xFF24476C),
            error = Color(0xFFFF8A98)
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("B&L MORLEY", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text("Admin Control", fontSize = 36.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(6.dp))
                Text("Secure Control Centre", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(28.dp))
                Text("Admin sign in", fontSize = 28.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(6.dp))
                Text("Use your authorised B&L Morley account.")
                Spacer(Modifier.height(18.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                Text("Cloudflare security check", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                AdminTurnstileChallenge(
                    refreshKey = captchaRefresh,
                    onToken = {
                        captchaToken = it
                        message = "Security check complete."
                        error = false
                    },
                    onExpired = {
                        captchaToken = ""
                        message = "Security check expired. Complete it again."
                        error = true
                    },
                    onError = {
                        captchaToken = ""
                        message = "Security check failed. Please retry."
                        error = true
                    }
                )
                if (message.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        message,
                        color = if (error) MaterialTheme.colorScheme.error else Color(0xFF25D991)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        busy = true
                        message = "Signing in…"
                        error = false
                        val token = captchaToken
                        scope.launch {
                            runCatching { AdminApi.signIn(email, password, token) }
                                .onSuccess(onSignedIn)
                                .onFailure {
                                    busy = false
                                    message = it.message ?: "Sign-in failed."
                                    error = true
                                    resetCaptcha()
                                }
                        }
                    },
                    enabled = isAdminLoginReady(email, password, captchaToken, busy),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Sign in", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
