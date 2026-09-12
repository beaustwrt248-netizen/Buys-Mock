package com.buysloans.admin

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

private val AdminBackground = Color(0xFF040912)
private val AdminSurface = Color(0xFF081426)
private val AdminAccent = Color(0xFF4CC9F0)
private val AdminText = Color(0xFFF4F8FF)
private val AdminMuted = Color(0xFF8EA6C4)

class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdminTelemetry.installCrashHandler(applicationContext)
        window.decorView.setBackgroundColor(AndroidColor.rgb(4, 9, 18))
        if (BuildConfig.IS_RECOVERY_BUILD) title = "Morley Admin Recovery"

        val session = AdminSessionStore.current()
        if (session == null || !AdminSessionStore.hasAuthorizedSession()) {
            AdminSessionStore.clear()
            returnToNativeLogin()
            return
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = AdminAccent,
                    onPrimary = Color(0xFF001018),
                    background = AdminBackground,
                    onBackground = AdminText,
                    surface = AdminSurface,
                    onSurface = AdminText,
                    onSurfaceVariant = AdminMuted,
                    outline = Color(0xFF24476C),
                    error = Color(0xFFFF8A98)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ) {
                    AdminNativeDashboard(
                        session = session,
                        onSignOut = {
                            AdminSessionStore.clear()
                            returnToNativeLogin()
                        },
                        onSessionExpired = {
                            AdminSessionStore.clear()
                            returnToNativeLogin()
                        }
                    )
                }
            }
        }
    }

    private fun returnToNativeLogin() {
        startActivity(
            Intent(this, AdminLoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }
}
