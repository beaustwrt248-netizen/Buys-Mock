package com.buysloans.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray

@Composable
internal fun UserManagementPanel(
    session: AdminSession,
    profiles: JSONArray?,
    busy: Boolean,
    onExecute: (UserControlCommand) -> Unit
) {
    val users = buildUserAccessPresentation(session, profiles)
    var pending by remember { mutableStateOf<UserControlCommand?>(null) }

    TeamInvitePanel(session = session, hostBusy = busy)

    Text("User access", fontSize = 21.sp, fontWeight = FontWeight.Black)
    Text(
        when (session.role) {
            "admin" -> "Admin-only audited controls. Android can change role, enable, or disable an existing account; delete and force-signout are not exposed."
            "manager" -> "Managers may invite new Staff above, but existing account role and enable/disable controls remain Admin-only."
            else -> "Read-only user visibility. Staff cannot invite or change account access."
        },
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp
    )

    if (users.isEmpty()) {
        Text("No user profiles returned.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    users.forEach { user ->
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .18f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(user.displayName, fontWeight = FontWeight.Bold)
                Text(
                    "${user.role.uppercase()} • ${if (user.enabled) "enabled" else "disabled"}${if (user.isSelf) " • signed in" else ""}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )

                if (user.canChangeRole) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("staff", "manager", "admin").forEach { role ->
                            OutlinedButton(
                                onClick = {
                                    pending = UserControlCoordinator.prepare(
                                        session = session,
                                        user = user,
                                        action = AdminUserAction.SET_ROLE,
                                        requestedRole = role
                                    )
                                },
                                enabled = !busy && role != user.role,
                                modifier = Modifier.weight(1f).widthIn(min = 72.dp)
                            ) { Text(role.replaceFirstChar { it.uppercase() }, fontSize = 10.sp) }
                        }
                    }
                }

                if (user.canEnable) {
                    Button(
                        onClick = {
                            pending = UserControlCoordinator.prepare(session, user, AdminUserAction.ENABLE)
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Enable account", fontWeight = FontWeight.Black) }
                }
                if (user.canDisable) {
                    OutlinedButton(
                        onClick = {
                            pending = UserControlCoordinator.prepare(session, user, AdminUserAction.DISABLE)
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Disable account", fontWeight = FontWeight.Black) }
                }

                user.readOnlyReason?.let {
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }
        }
    }

    pending?.let { command ->
        AlertDialog(
            onDismissRequest = { if (!busy) pending = null },
            title = { Text("Confirm audited access change") },
            text = { Text(command.confirmationText) },
            confirmButton = {
                Button(
                    onClick = {
                        pending = null
                        onExecute(command)
                    },
                    enabled = !busy
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }, enabled = !busy) { Text("Cancel") }
            }
        )
    }
}
