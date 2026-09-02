package com.buysloans.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONArray

private data class ResetUser(val id: String, val label: String)

@Composable
internal fun AdminPasswordResetPanel(session: AdminSession, profiles: JSONArray?, hostBusy: Boolean) {
    if (session.role != "admin") return
    val users = remember(profiles, session.userId) {
        buildList {
            if (profiles != null) for (i in 0 until profiles.length()) {
                val p = profiles.optJSONObject(i) ?: continue
                val id = p.optString("id")
                if (id.isBlank() || id == session.userId) continue
                val name = p.optString("display_name").ifBlank { p.optString("email").ifBlank { id } }
                add(ResetUser(id, "$name • ${p.optString("role").ifBlank { "user" }}"))
            }
        }.sortedBy { it.label.lowercase() }
    }
    var selected by remember(users) { mutableStateOf<ResetUser?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .18f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Password reset", fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(
                "Generate a one-time temporary password. The user is signed out everywhere and must choose a new password at next sign-in.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            OutlinedButton(onClick = { menuOpen = true }, enabled = users.isNotEmpty() && !busy && !hostBusy, modifier = Modifier.fillMaxWidth()) {
                Text(selected?.label ?: "Choose user")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                users.forEach { user ->
                    DropdownMenuItem(text = { Text(user.label) }, onClick = { selected = user; password = ""; status = ""; menuOpen = false })
                }
            }
            Button(
                onClick = {
                    val target = selected ?: return@Button
                    busy = true
                    password = ""
                    status = "Generating secure temporary password…"
                    scope.launch {
                        runCatching { AdminPasswordResetApi.reset(session, target.id) }
                            .onSuccess { result ->
                                password = result.temporaryPassword
                                status = "Temporary password created. It is shown only on this screen."
                            }
                            .onFailure { status = it.message ?: "Password reset failed." }
                        busy = false
                    }
                },
                enabled = selected != null && !busy && !hostBusy,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (busy) "Resetting…" else "Generate temporary password", fontWeight = FontWeight.Black) }

            if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            if (password.isNotBlank()) {
                Text("TEMPORARY PASSWORD", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text(password, fontSize = 24.sp, fontWeight = FontWeight.Black)
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.take(24) },
                    label = { Text("Mobile number") },
                    placeholder = { Text("04xx xxx xxx") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val message = "B&L Morley temporary password for ${selected?.label?.substringBefore(" • ") ?: "your account"}: $password\n\nSign in with this password, then choose a new password."
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, message)
                            }
                            context.startActivity(Intent.createChooser(send, "Share temporary password"))
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Share") }
                    Button(
                        onClick = {
                            val number = phone.filter { it.isDigit() || it == '+' }
                            if (number.isBlank()) { status = "Enter the mobile number first."; return@Button }
                            val message = "B&L Morley temporary password: $password\n\nSign in, then choose a new password."
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).apply { putExtra("sms_body", message) }
                            runCatching { context.startActivity(intent) }.onFailure { status = "No SMS app is available on this device." }
                        },
                        enabled = phone.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Open SMS") }
                }
                Text(
                    "No SMS gateway fee: Open SMS uses this phone's normal Messages app and SIM plan. Fully automatic SMS delivery still requires a paid carrier/provider.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}
