package com.buysloans.hub

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.launch

private val SupportBg = Color(0xFF030712)
private val SupportCard = Color(0xFF0B1528)
private val SupportAccent = Color(0xFF16C7FF)
private val SupportMuted = Color(0xFF8EA6C4)
private val SupportGood = Color(0xFF57E389)

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
                colorScheme = darkColorScheme(
                    primary = SupportAccent,
                    background = SupportBg,
                    surface = SupportCard
                )
            ) {
                SupportTicketScreen()
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SupportTicketScreen() {
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
        var category by remember { mutableStateOf("valuation") }
        var subject by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var includeDiagnostics by remember { mutableStateOf(false) }
        var busy by remember { mutableStateOf(false) }
        var status by remember { mutableStateOf("") }
        var successTicketId by remember { mutableStateOf("") }

        Scaffold(
            containerColor = SupportBg,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF050B16)),
                    title = { Text("Support Ticket Test", fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        TextButton(onClick = { finish() }) { Text("Close") }
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
                Surface(
                    color = SupportCard,
                    border = BorderStroke(1.dp, SupportAccent.copy(alpha = .3f)),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("LIVE SUPPORT BACKEND", color = SupportAccent, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text("Submit a real authenticated support ticket for testing. Production OTA/versioning is not changed by this test screen.", color = SupportMuted, fontSize = 12.sp)
                    }
                }

                Text("Category", fontWeight = FontWeight.Black)
                categories.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (value, label) ->
                            FilterChip(
                                selected = category == value,
                                onClick = { category = value },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it.take(160) },
                    label = { Text("Subject") },
                    supportingText = { Text("${subject.length}/160") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(5000) },
                    label = { Text("What happened?") },
                    supportingText = { Text("${description.length}/5000") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                    enabled = !busy,
                    minLines = 5
                )

                Surface(color = SupportCard, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text("Include diagnostics", fontWeight = FontWeight.Black)
                                Text("Opt in to app version and basic Android/device details. Authentication tokens are never included.", color = SupportMuted, fontSize = 11.sp)
                            }
                            Switch(checked = includeDiagnostics, onCheckedChange = { includeDiagnostics = it }, enabled = !busy)
                        }
                    }
                }

                OutlinedButton(
                    onClick = { pickAttachment.launch(arrayOf("image/jpeg", "image/png", "image/webp", "application/pdf")) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (attachmentUri == null) "Add Optional Attachment" else "Change Attachment") }

                if (attachmentUri != null) {
                    Surface(color = SupportCard, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(attachmentName, modifier = Modifier.weight(1f), maxLines = 1)
                            TextButton(onClick = { attachmentUri = null; attachmentName = "" }, enabled = !busy) { Text("Remove") }
                        }
                    }
                }

                Button(
                    onClick = {
                        busy = true
                        status = "Submitting…"
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
                                status = if (result.attachmentWarning.isNullOrBlank()) {
                                    "Ticket submitted successfully."
                                } else {
                                    "Ticket submitted, but attachment warning: ${result.attachmentWarning}"
                                }
                                if (result.attachmentWarning.isNullOrBlank()) {
                                    subject = ""
                                    description = ""
                                    attachmentUri = null
                                    attachmentName = ""
                                }
                            }.onFailure { error ->
                                status = error.message ?: "Support ticket submission failed."
                            }
                            busy = false
                        }
                    },
                    enabled = !busy && subject.trim().length >= 3 && description.trim().length >= 5,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (busy) "Submitting…" else "Submit Support Ticket", fontWeight = FontWeight.Black) }

                if (status.isNotBlank()) {
                    val success = successTicketId.isNotBlank()
                    Surface(
                        color = if (success) SupportGood.copy(alpha = .09f) else SupportCard,
                        border = BorderStroke(1.dp, (if (success) SupportGood else SupportAccent).copy(alpha = .3f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(status, fontWeight = FontWeight.Bold)
                            if (success) Text("Ticket ID: $successTicketId", color = SupportMuted, fontSize = 11.sp)
                        }
                    }
                }

                Text("Attachments: JPG, PNG, WebP or PDF up to 10 MB. This test uses your current authorised B&L Morley session.", color = SupportMuted, fontSize = 11.sp)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
