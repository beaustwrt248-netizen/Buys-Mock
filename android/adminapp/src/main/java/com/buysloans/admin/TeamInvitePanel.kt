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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
    var useTemporaryPassword by remember { mutableStateOf(false) }
    var temporaryPassword by remember { mutableStateOf(TeamInviteApi.generateTemporaryPassword()) }
    var revealed by remember { mutableStateOf<TeamInviteSecret?>(null) }
    var revealedTemporaryUser by remember { mutableStateOf<TemporaryUserSecret?>(null) }
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
            "Invite Staff or Managers, or provision an account with a temporary password."
        else
            "Managers can invite or provision Staff only. Existing account access remains Admin-controlled.",
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Temporary password", fontWeight = FontWeight.Bold)
                    Text(
                        "Skip email verification and force a password change on first sign-in.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = useTemporaryPassword,
                    onCheckedChange = {
                        useTemporaryPassword = it
                        if (it && temporaryPassword.isBlank()) temporaryPassword = TeamInviteApi.generateTemporaryPassword()
                    },
                    enabled = !hostBusy && !loading
                )
            }

            if (useTemporaryPassword) {
                OutlinedTextField(
                    value = temporaryPassword,
                    onValueChange = { temporaryPassword = it.take(256) },
                    label = { Text("Temporary password") },
                    supportingText = { Text("Minimum 10 characters. It is shown once and is never stored in Admin logs.") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { temporaryPassword = TeamInviteApi.generateTemporaryPassword() },
                    enabled = !hostBusy && !loading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Generate secure temporary password") }
            }

            Button(
                onClick = {
                    error = ""
                    status = ""
                    loading = true
                    scope.launch {
                        val result = if (useTemporaryPassword) {
                            runCatching { TeamInviteApi.createTemporaryUser(session, name, email, role, temporaryPassword) }
                                .onSuccess { secret ->
                                    revealedTemporaryUser = secret
                                    revealed = null
                                    name = ""
                                    email = ""
                                    temporaryPassword = TeamInviteApi.generateTemporaryPassword()
                                    status = "Account created. Copy the temporary password now."
                                }
                        } else {
                            runCatching { TeamInviteApi.create(session, name, email, role) }
                                .onSuccess { secret ->
                                    revealed = secret
                                    revealedTemporaryUser = null
                                    name = ""
                                    email = ""
                                    status = "Invite created. Copy the one-time code now."
                                    invites = TeamInviteApi.list(session)
                                }
                        }
                        result.onFailure { error = it.message ?: "Account access could not be created." }
                        loading = false
                    }
                },
                enabled = !hostBusy && !loading && name.isNotBlank() && email.isNotBlank() &&
                    (!useTemporaryPassword || temporaryPassword.length >= 10),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (useTemporaryPassword) "Create account with temporary password" else "Create secure invite",
                    fontWeight = FontWeight.Black
                )
            }
        }
    }

    revealedTemporaryUser?.let { secret ->
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("TEMPORARY PASSWORD — SHOWN ONCE", fontWeight = FontWeight.Black, fontSize = 11.sp)
                Text(secret.temporaryPassword, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text("${secret.displayName} • ${secret.email} • ${secret.role.uppercase()}", fontSize = 11.sp)
                Text("Email is already verified. The user must choose a new password before entering Morley.", fontSize = 11.sp)
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Morley temporary password", secret.temporaryPassword))
                        status = "Temporary password copied."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Copy temporary password") }
                TextButton(onClick = { revealedTemporaryUser = null }, modifier = Modifier.fillMaxWidth()) { Text("Hide password") }
            }
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
    if (loading) Text("Updating team access…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

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
