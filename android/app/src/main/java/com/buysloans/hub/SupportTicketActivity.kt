package com.buysloans.hub

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import kotlinx.coroutines.launch

private val SupportBg = Color(0xFFF5F7F4)
private val SupportCard = Color(0xFFEEF4F0)
private val SupportAccent = Color(0xFF167A5A)
private val SupportMuted = Color(0xFF52645D)
private val SupportGood = Color(0xFF238A63)
private val SupportWarn = Color(0xFFA86A12)

class SupportTicketActivity : ComponentActivity() {
    private var attachmentUri by mutableStateOf<Uri?>(null)
    private var attachmentName by mutableStateOf("")

    private val pickAttachment = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        attachmentUri = uri
        attachmentName = if (uri == null) "" else resolveDisplayName(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = SupportAccent,
                    background = SupportBg,
                    surface = SupportCard
                )
            ) {
                SupportCentreScreen()
            }
        }
    }

    private fun resolveDisplayName(uri: Uri): String {
        return runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index).orEmpty() else ""
                } else ""
            }.orEmpty()
        }.getOrDefault("").ifBlank { "Selected attachment" }
    }

    private fun latestAdminMessageId(ticketId: String, messages: List<SupportTicketClient.TicketMessage>): String? =
        messages.lastOrNull { it.ticketId == ticketId && it.authorRole == "admin" }?.id

    private fun lastSeenAdminMessageId(ticketId: String): String? =
        getSharedPreferences("support_read_state", MODE_PRIVATE).getString("admin_$ticketId", null)

    private fun markAdminRepliesSeen(ticketId: String, messages: List<SupportTicketClient.TicketMessage>) {
        val latest = latestAdminMessageId(ticketId, messages) ?: return
        getSharedPreferences("support_read_state", MODE_PRIVATE).edit().putString("admin_$ticketId", latest).apply()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SupportCentreScreen() {
        val scope = rememberCoroutineScope()
        val categories = listOf(
            "valuation" to "Valuation",
            "pricing" to "Pricing",
            "inventory" to "Inventory",
            "scanner" to "Scanner",
            "account" to "Account",
            "update" to "Update",
            "other" to "Other"
        )

        var section by remember { mutableStateOf("tickets") }
        var selectedTicketId by remember { mutableStateOf<String?>(null) }
        var tickets by remember { mutableStateOf<List<SupportTicketClient.TicketSummary>>(emptyList()) }
        var messages by remember { mutableStateOf<List<SupportTicketClient.TicketMessage>>(emptyList()) }
        var loading by remember { mutableStateOf(true) }
        var loadError by remember { mutableStateOf("") }
        var refreshKey by remember { mutableIntStateOf(0) }

        var category by remember { mutableStateOf("valuation") }
        var subject by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var includeDiagnostics by remember { mutableStateOf(false) }
        var submitting by remember { mutableStateOf(false) }
        var submitStatus by remember { mutableStateOf("") }
        var successTicketId by remember { mutableStateOf("") }

        var reply by remember { mutableStateOf("") }
        var replying by remember { mutableStateOf(false) }
        var replyStatus by remember { mutableStateOf("") }

        suspend fun refreshSupport() {
            loading = true
            loadError = ""
            runCatching {
                val loadedTickets = SupportTicketClient.listMyTickets(this@SupportTicketActivity)
                val loadedMessages = SupportTicketClient.listMyMessages(this@SupportTicketActivity)
                loadedTickets to loadedMessages
            }.onSuccess { (loadedTickets, loadedMessages) ->
                tickets = loadedTickets
                messages = loadedMessages
                selectedTicketId?.let { id ->
                    if (loadedTickets.none { it.id == id }) selectedTicketId = null
                    else markAdminRepliesSeen(id, loadedMessages)
                }
            }.onFailure { error ->
                loadError = error.message ?: "Support could not be loaded."
            }
            loading = false
        }

        LaunchedEffect(refreshKey) { refreshSupport() }

        val selectedTicket = tickets.firstOrNull { it.id == selectedTicketId }
        val selectedMessages = selectedTicket?.let { ticket -> messages.filter { it.ticketId == ticket.id } }.orEmpty()

        Scaffold(
            containerColor = SupportBg,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF050B16)),
                    title = { Text("B&L Morley Support", color = Color.White, fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        TextButton(onClick = {
                            if (selectedTicketId != null) selectedTicketId = null else finish()
                        }) { Text(if (selectedTicketId != null) "Back" else "Close") }
                    },
                    actions = {
                        if (selectedTicketId == null && section == "tickets") {
                            TextButton(onClick = { refreshKey++ }, enabled = !loading) { Text("Refresh") }
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedTicket != null) {
                    TicketThread(
                        ticket = selectedTicket,
                        messages = selectedMessages,
                        reply = reply,
                        onReplyChanged = { reply = it.take(5000) },
                        replying = replying,
                        replyStatus = replyStatus,
                        onSendReply = {
                            replying = true
                            replyStatus = "Sending…"
                            scope.launch {
                                runCatching {
                                    SupportTicketClient.reply(this@SupportTicketActivity, selectedTicket.id, reply)
                                }.onSuccess {
                                    reply = ""
                                    replyStatus = "Reply sent."
                                    refreshSupport()
                                }.onFailure { error ->
                                    replyStatus = error.message ?: "Reply could not be sent."
                                }
                                replying = false
                            }
                        }
                    )
                } else {
                    SupportHeader()

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = section == "tickets",
                            onClick = { section = "tickets" },
                            label = { Text("My Tickets") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = section == "new",
                            onClick = { section = "new" },
                            label = { Text("New Ticket") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (section == "tickets") {
                        when {
                            loading -> {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Text("Loading your support tickets…", color = SupportMuted)
                            }
                            loadError.isNotBlank() -> {
                                StatusCard(loadError, false)
                                OutlinedButton(onClick = { refreshKey++ }, modifier = Modifier.fillMaxWidth()) { Text("Try Again") }
                            }
                            tickets.isEmpty() -> {
                                Surface(color = SupportCard, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("No support tickets yet", fontSize = 19.sp, fontWeight = FontWeight.Black)
                                        Text("When you contact support, your ticket and any replies will appear here.", color = SupportMuted)
                                        Button(onClick = { section = "new" }, modifier = Modifier.fillMaxWidth()) { Text("Create Support Ticket") }
                                    }
                                }
                            }
                            else -> tickets.forEach { ticket ->
                                val latestAdminId = latestAdminMessageId(ticket.id, messages)
                                val unread = SupportTicketLogic.hasUnreadSupportReply(latestAdminId, lastSeenAdminMessageId(ticket.id))
                                TicketCard(ticket, unread) {
                                    markAdminRepliesSeen(ticket.id, messages)
                                    selectedTicketId = ticket.id
                                    reply = ""
                                    replyStatus = ""
                                }
                            }
                        }
                    } else {
                        NewTicketForm(
                            categories = categories,
                            category = category,
                            onCategoryChanged = { category = it },
                            subject = subject,
                            onSubjectChanged = { subject = it.take(160) },
                            description = description,
                            onDescriptionChanged = { description = it.take(5000) },
                            includeDiagnostics = includeDiagnostics,
                            onDiagnosticsChanged = { includeDiagnostics = it },
                            attachmentUri = attachmentUri,
                            attachmentName = attachmentName,
                            onPickAttachment = { pickAttachment.launch(arrayOf("image/jpeg", "image/png", "image/webp", "application/pdf")) },
                            onRemoveAttachment = { attachmentUri = null; attachmentName = "" },
                            busy = submitting,
                            status = submitStatus,
                            successTicketId = successTicketId,
                            onSubmit = {
                                submitting = true
                                submitStatus = "Submitting…"
                                successTicketId = ""
                                scope.launch {
                                    runCatching {
                                        SupportTicketClient.submit(
                                            context = this@SupportTicketActivity,
                                            category = category,
                                            subject = subject,
                                            description = description,
                                            includeDiagnostics = includeDiagnostics,
                                            attachment = attachmentUri
                                        )
                                    }.onSuccess { result ->
                                        successTicketId = result.ticketId
                                        submitStatus = if (result.attachmentWarning.isNullOrBlank()) {
                                            "Ticket submitted successfully."
                                        } else {
                                            "Ticket submitted, but attachment warning: ${result.attachmentWarning}"
                                        }
                                        if (result.attachmentWarning.isNullOrBlank()) {
                                            subject = ""
                                            description = ""
                                            attachmentUri = null
                                            attachmentName = ""
                                            refreshSupport()
                                        }
                                    }.onFailure { error ->
                                        submitStatus = error.message ?: "Support ticket submission failed."
                                    }
                                    submitting = false
                                }
                            },
                            onViewTicket = {
                                val id = successTicketId
                                if (id.isNotBlank()) {
                                    section = "tickets"
                                    selectedTicketId = id
                                    markAdminRepliesSeen(id, messages)
                                }
                            }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    @Composable
    private fun SupportHeader() {
        Surface(
            color = SupportCard,
            border = BorderStroke(1.dp, SupportAccent.copy(alpha = .3f)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("SUPPORT", color = SupportAccent, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Text("Get help and keep the whole conversation in one place.", fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text("View your tickets, see replies from B&L Morley Support and reply securely from your signed-in account.", color = SupportMuted, fontSize = 12.sp)
            }
        }
    }

    @Composable
    private fun TicketCard(ticket: SupportTicketClient.TicketSummary, unread: Boolean, onClick: () -> Unit) {
        Surface(
            color = if (unread) SupportAccent.copy(alpha = .10f) else SupportCard,
            border = BorderStroke(1.dp, (if (unread) SupportAccent else SupportMuted).copy(alpha = .25f)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
        ) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(ticket.subject, modifier = Modifier.weight(1f), fontWeight = FontWeight.Black, fontSize = 17.sp)
                    if (unread) {
                        Surface(color = SupportAccent.copy(alpha = .18f), shape = RoundedCornerShape(999.dp)) {
                            Text("NEW REPLY", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = SupportAccent, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Text("${SupportTicketLogic.statusLabel(ticket.status)} • ${ticket.category.replaceFirstChar { it.uppercase() }}", color = if (ticket.status == "waiting_on_user") SupportWarn else SupportMuted, fontSize = 12.sp)
                Text(ticket.description, color = MorleyTextSecondary, fontSize = 12.sp, maxLines = 2)
                Text("Updated ${prettyTime(ticket.updatedAt)}", color = SupportMuted, fontSize = 10.sp)
            }
        }
    }

    @Composable
    private fun TicketThread(
        ticket: SupportTicketClient.TicketSummary,
        messages: List<SupportTicketClient.TicketMessage>,
        reply: String,
        onReplyChanged: (String) -> Unit,
        replying: Boolean,
        replyStatus: String,
        onSendReply: () -> Unit
    ) {
        Text(ticket.subject, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("${SupportTicketLogic.statusLabel(ticket.status)} • Ticket ${ticket.id.take(8).uppercase()}", color = SupportMuted, fontSize = 12.sp)

        MessageCard("You", "Original report", ticket.description, ticket.createdAt, false)
        messages.forEach { message ->
            val admin = message.authorRole == "admin"
            MessageCard(
                author = if (admin) "B&L Morley Support" else "You",
                label = if (admin) "Support reply" else "Your reply",
                body = message.body,
                createdAt = message.createdAt,
                admin = admin
            )
        }

        if (messages.none { it.authorRole == "admin" }) {
            Text("No support reply yet. This ticket is still visible here while you wait.", color = SupportMuted, fontSize = 12.sp)
        }

        if (ticket.status != "closed") {
            HorizontalDivider(color = SupportAccent.copy(alpha = .2f))
            Text("Reply to support", fontSize = 18.sp, fontWeight = FontWeight.Black)
            OutlinedTextField(
                value = reply,
                onValueChange = onReplyChanged,
                label = { Text("Message") },
                supportingText = { Text("${reply.length}/5000") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
                enabled = !replying
            )
            Button(
                onClick = onSendReply,
                enabled = !replying && reply.trim().isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (replying) "Sending…" else "Send Reply", fontWeight = FontWeight.Black) }
            if (replyStatus.isNotBlank()) Text(replyStatus, color = SupportMuted, fontSize = 12.sp)
        } else {
            StatusCard("This ticket is closed. You can still read the full conversation.", true)
        }
    }

    @Composable
    private fun MessageCard(author: String, label: String, body: String, createdAt: String, admin: Boolean) {
        Surface(
            color = if (admin) SupportAccent.copy(alpha = .08f) else SupportCard,
            border = BorderStroke(1.dp, (if (admin) SupportAccent else SupportMuted).copy(alpha = .22f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(author, fontWeight = FontWeight.Black, color = if (admin) SupportAccent else Color.White)
                Text(label, color = SupportMuted, fontSize = 10.sp)
                Text(body, color = Color.White, lineHeight = 20.sp)
                Text(prettyTime(createdAt), color = SupportMuted, fontSize = 10.sp)
            }
        }
    }

    @Composable
    private fun NewTicketForm(
        categories: List<Pair<String, String>>,
        category: String,
        onCategoryChanged: (String) -> Unit,
        subject: String,
        onSubjectChanged: (String) -> Unit,
        description: String,
        onDescriptionChanged: (String) -> Unit,
        includeDiagnostics: Boolean,
        onDiagnosticsChanged: (Boolean) -> Unit,
        attachmentUri: Uri?,
        attachmentName: String,
        onPickAttachment: () -> Unit,
        onRemoveAttachment: () -> Unit,
        busy: Boolean,
        status: String,
        successTicketId: String,
        onSubmit: () -> Unit,
        onViewTicket: () -> Unit
    ) {
        Text("Create Support Ticket", fontSize = 22.sp, fontWeight = FontWeight.Black)
        Text("Category", fontWeight = FontWeight.Black)
        categories.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (value, label) ->
                    FilterChip(
                        selected = category == value,
                        onClick = { onCategoryChanged(value) },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        OutlinedTextField(
            value = subject,
            onValueChange = onSubjectChanged,
            label = { Text("Subject") },
            supportingText = { Text("${subject.length}/160") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy,
            singleLine = true
        )

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChanged,
            label = { Text("What happened?") },
            supportingText = { Text("${description.length}/5000") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
            enabled = !busy,
            minLines = 5
        )

        Surface(color = SupportCard, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Include diagnostics", fontWeight = FontWeight.Black)
                    Text("Opt in to app version and basic Android/device details. Authentication tokens are never included.", color = SupportMuted, fontSize = 11.sp)
                }
                Switch(checked = includeDiagnostics, onCheckedChange = onDiagnosticsChanged, enabled = !busy)
            }
        }
        OutlinedButton(onClick = onPickAttachment, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text(if (attachmentUri == null) "Add Optional Attachment" else "Change Attachment")
        }

        if (attachmentUri != null) {
            Surface(color = SupportCard, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(attachmentName, modifier = Modifier.weight(1f), maxLines = 1)
                    TextButton(onClick = onRemoveAttachment, enabled = !busy) { Text("Remove") }
                }
            }
        }

        Button(
            onClick = onSubmit,
            enabled = !busy && subject.trim().length >= 3 && description.trim().length >= 5,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (busy) "Submitting…" else "Submit Support Ticket", fontWeight = FontWeight.Black) }

        if (status.isNotBlank()) {
            StatusCard(status, successTicketId.isNotBlank())
            if (successTicketId.isNotBlank()) {
                Text("Ticket ID: $successTicketId", color = SupportMuted, fontSize = 11.sp)
                OutlinedButton(onClick = onViewTicket, modifier = Modifier.fillMaxWidth()) { Text("View Ticket") }
            }
        }

        Text("Attachments: JPG, PNG, WebP or PDF up to 10 MB. Support uses your current authorised B&L Morley session. Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}).", color = SupportMuted, fontSize = 11.sp)
    }

    @Composable
    private fun StatusCard(message: String, success: Boolean) {
        Surface(
            color = if (success) SupportGood.copy(alpha = .09f) else SupportCard,
            border = BorderStroke(1.dp, (if (success) SupportGood else SupportAccent).copy(alpha = .3f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(message, Modifier.padding(14.dp), fontWeight = FontWeight.Bold)
        }
    }

    private fun prettyTime(value: String): String {
        if (value.isBlank()) return ""
        return value.take(16).replace('T', ' ')
    }
}
