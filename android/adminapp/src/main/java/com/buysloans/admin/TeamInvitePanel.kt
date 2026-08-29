package com.buysloans.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

@Composable
internal fun TeamInvitePanel(session: AdminSession, hostBusy: Boolean) {
    if (!TeamInvitePolicy.canManage(session)) return

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var invites by remember(session.userId) { mutableStateOf<List<TeamInvite>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    val allowedRoles = TeamInvitePolicy.allowedRoles(session)
    var role by remember(session.role) { mutableStateOf(allowedRoles.firstOrNull() ?: "staff") }
    var revealed by remember { mutableStateOf<TeamInviteSecret?>(null) }
    var pendingReissue by remember { mutableStateOf<TeamInvite?>(null) }
    var pendingRevoke by remember { mutableStateOf<TeamInvite?>(null) }

    fun load() {
        loading = true
        error = ""
        scope.launch {
            runCatching { TeamInviteApi.list(session) }
                .onSuccess { invites = it }
                .onFailure { error = it.message ?: "Team invites could not be loaded." }
            loading = false
        }
    }

    LaunchedEffect(session.userId) { load() }

    Text("Team & staff invitations", fontSize = 21.sp, fontWeight = FontWeight.Black)
    Text(
        if (session.role == "admin")
            "Invite Staff or Managers. Invite codes are shown once; only their SHA-256 hash is stored."
        else
            "Managers can invite Staff only. Existing account access remains Admin-controlled.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .18f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(100) },
                label = { Text("First and last name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.take(254) },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text("Role", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                allowedRoles.forEach { candidate ->
                    FilterChip(
                        selected = role == candidate,
                        onClick = { role = candidate },
                        label = { Text(candidate.replaceFirstChar { it.uppercase() }) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Button(
                onClick = {
                    error = ""
                    status = ""
                    loading = true
                    scope.launch {
                        runCatching { TeamInviteApi.create(session, name, email, role) }
                            .onSuccess { secret ->
                                revealed = secret
                                name = ""
                                email = ""
                                status = "Invite created. Copy the one-time code now."
                                invites = TeamInviteApi.list(session)
                            }
                            .onFailure { error = it.message ?: "Team invite could not be created." }
                        loading = false
                    }
                },
                enabled = !hostBusy && !loading && name.isNotBlank() && email.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Create secure invite", fontWeight = FontWeight.Black) }
        }
    }

    revealed?.let { secret ->
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("ONE-TIME INVITE CODE", fontWeight = FontWeight.Black, fontSize = 11.sp)
                Text(secret.code, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text("${secret.invite.displayName} • ${secret.invite.email} • ${secret.invite.role.uppercase()} • valid for 7 days", fontSize = 11.sp)
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Morley team invite", secret.code))
                        status = "Invite code copied."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Copy code") }
                TextButton(onClick = { revealed = null }, modifier = Modifier.fillMaxWidth()) { Text("Hide code") }
            }
        }
    }

    if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
    if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
    if (loading) Text("Updating team invitations…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

    Text("Pending & previous invitations", fontWeight = FontWeight.Bold)
    if (!loading && invites.isEmpty()) {
        Text("No team invitations yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
    invites.forEach { invite ->
        val state = when {
            invite.isUsed -> "USED"
            invite.isExpired -> "EXPIRED"
            else -> "ACTIVE"
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .14f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(invite.displayName.ifBlank { invite.email }, fontWeight = FontWeight.Bold)
                Text("${invite.email} • ${invite.role.uppercase()} • $state", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Text("Expires ${invite.expiresAt}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                if (!invite.isUsed && invite.role in allowedRoles) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { pendingReissue = invite },
                            enabled = !hostBusy && !loading,
                            modifier = Modifier.weight(1f)
                        ) { Text("New code", fontSize = 11.sp) }
                        OutlinedButton(
                            onClick = { pendingRevoke = invite },
                            enabled = !hostBusy && !loading,
                            modifier = Modifier.weight(1f)
                        ) { Text("Revoke", fontSize = 11.sp) }
                    }
                }
            }
        }
    }

    pendingReissue?.let { invite ->
        AlertDialog(
            onDismissRequest = { if (!loading) pendingReissue = null },
            title = { Text("Issue a new invite code?") },
            text = { Text("The previous code for ${invite.displayName} will stop working and a fresh 7-day code will be shown once.") },
            confirmButton = {
                Button(onClick = {
                    pendingReissue = null
                    loading = true
                    error = ""
                    scope.launch {
                        runCatching { TeamInviteApi.reissue(session, invite) }
                            .onSuccess { secret ->
                                revealed = secret
                                status = "New invite code issued. Copy it now."
                                invites = TeamInviteApi.list(session)
                            }
                            .onFailure { error = it.message ?: "Invite could not be reissued." }
                        loading = false
                    }
                }, enabled = !loading) { Text("Issue new code") }
            },
            dismissButton = { TextButton(onClick = { pendingReissue = null }, enabled = !loading) { Text("Cancel") } }
        )
    }

    pendingRevoke?.let { invite ->
        AlertDialog(
            onDismissRequest = { if (!loading) pendingRevoke = null },
            title = { Text("Revoke invitation?") },
            text = { Text("${invite.displayName} will no longer be able to use this invite code.") },
            confirmButton = {
                Button(onClick = {
                    pendingRevoke = null
                    loading = true
                    error = ""
                    scope.launch {
                        runCatching { TeamInviteApi.revoke(session, invite) }
                            .onSuccess {
                                status = "Invite revoked."
                                invites = TeamInviteApi.list(session)
                            }
                            .onFailure { error = it.message ?: "Invite could not be revoked." }
                        loading = false
                    }
                }, enabled = !loading) { Text("Revoke") }
            },
            dismissButton = { TextButton(onClick = { pendingRevoke = null }, enabled = !loading) { Text("Cancel") } }
        )
    }
}
