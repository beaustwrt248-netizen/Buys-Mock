package com.buysloans.hub

import android.content.Intent
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val NCAccent = Color(0xFF167A5A)
private val NCBg = Color(0xFFF5F7F4)
private val NCCard = Color(0xFFEEF4F0)
private val NCMuted = Color(0xFF52645D)

class NotificationCentreActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = NCAccent,
                    background = NCBg,
                    surface = NCCard
                )
            ) { NotificationCentreScreen() }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun NotificationCentreScreen() {
        var refresh by remember { mutableIntStateOf(0) }
        val items = remember(refresh) { NotificationInboxStore.items(this) }
        val unread = items.count { !it.read }
        val readCount = items.size - unread

        Scaffold(
            containerColor = NCBg,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF050B16),
                        titleContentColor = Color.White
                    ),
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Text("‹", fontSize = 34.sp, color = NCAccent)
                        }
                    },
                    title = { Text("Notification Centre", fontWeight = FontWeight.Black) }
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
                Card(
                    colors = CardDefaults.cardColors(containerColor = NCCard),
                    border = BorderStroke(1.dp, NCAccent.copy(alpha = .18f)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Inbox", fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text(
                            if (unread == 0) "You're all caught up." else "$unread unread notification${if (unread == 1) "" else "s"}.",
                            color = NCMuted
                        )
                        if (unread > 0) {
                            OutlinedButton(
                                onClick = {
                                    NotificationInboxStore.markAllRead(this@NotificationCentreActivity)
                                    refresh++
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Mark all as read") }
                        }
                        if (readCount > 0) {
                            TextButton(
                                onClick = {
                                    NotificationInboxStore.clearRead(this@NotificationCentreActivity)
                                    refresh++
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Clear $readCount read notification${if (readCount == 1) "" else "s"}") }
                        }
                    }
                }

                if (items.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NCCard),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("No notifications yet", fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Text("Update alerts and important B&L Morley messages will appear here.", color = NCMuted)
                        }
                    }
                } else {
                    items.forEach { item ->
                        val canOpenUpdate = item.type.equals("update", ignoreCase = true) &&
                            item.versionCode > 0 &&
                            item.versionName.isNotBlank() &&
                            UpdateManager.isTrustedApkUrl(item.apkUrl) &&
                            UpdateManager.isValidSha256(item.sha256)
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (item.read) NCCard else Color(0xFF0E2038)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (item.read) NCAccent.copy(alpha = .12f) else NCAccent.copy(alpha = .45f)
                            ),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(item.title, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                                    if (!item.read) Text("NEW", color = NCAccent, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                }
                                if (item.body.isNotBlank()) Text(item.body, color = Color.White)
                                Text(
                                    "${labelFor(item.type)} • ${formatTime(item.createdAt)}",
                                    color = NCMuted,
                                    fontSize = 12.sp
                                )
                                if (canOpenUpdate) {
                                    Button(
                                        onClick = {
                                            NotificationInboxStore.markRead(this@NotificationCentreActivity, item.id)
                                            startActivity(Intent(this@NotificationCentreActivity, UpdateActivity::class.java).apply {
                                                putExtra("versionCode", item.versionCode)
                                                putExtra("versionName", item.versionName)
                                                putExtra("apkUrl", item.apkUrl)
                                                putExtra("notes", item.notes)
                                                putExtra("sha256", item.sha256)
                                            })
                                            refresh++
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Open update") }
                                } else if (!item.read) {
                                    TextButton(
                                        onClick = {
                                            NotificationInboxStore.markRead(this@NotificationCentreActivity, item.id)
                                            refresh++
                                        }
                                    ) { Text("Mark as read") }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    private fun labelFor(type: String): String = when (type.lowercase(Locale.US)) {
        "update" -> "App update"
        "admin" -> "B&L Morley alert"
        "valuation" -> "Valuation"
        else -> "Message"
    }

    private fun formatTime(value: Long): String =
        if (value <= 0L) "Recently"
        else SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault()).format(Date(value))
}
