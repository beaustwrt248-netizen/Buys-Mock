package com.buysloans.hub

import android.content.Intent
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.provider.Settings
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
import java.text.DateFormat
import java.util.Date

private val NfcBg = Color(0xFF030712)
private val NfcCard = Color(0xFF0B1528)
private val NfcAccent = Color(0xFF16C7FF)
private val NfcMuted = Color(0xFF8EA6C4)
private val NfcGood = Color(0xFF57E389)
private val NfcWarn = Color(0xFFFFC857)

data class RecentNfcScan(
    val tagId: String,
    val payloads: List<NfcPayload>,
    val technologies: List<String>,
    val readAt: Long
)

class NfcScannerActivity : ComponentActivity(), NfcAdapter.ReaderCallback {
    private var adapter: NfcAdapter? = null
    private var capability by mutableStateOf(NfcCapability.UNAVAILABLE)
    private var latest by mutableStateOf<RecentNfcScan?>(null)
    private var recent by mutableStateOf<List<RecentNfcScan>>(emptyList())
    private var status by mutableStateOf("Ready to check NFC")
    private var lastTagId: String? = null
    private var lastReadMs: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = NfcAdapter.getDefaultAdapter(this)
        refreshCapability()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = NfcAccent, background = NfcBg, surface = NfcCard)) {
                NfcScannerScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshCapability()
        if (capability == NfcCapability.READY) {
            adapter?.enableReaderMode(
                this,
                this,
                NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                null
            )
            status = "Hold a supported NFC tag near the back of the phone."
        }
    }

    override fun onPause() {
        adapter?.disableReaderMode(this)
        super.onPause()
    }

    override fun onTagDiscovered(tag: Tag) {
        val now = System.currentTimeMillis()
        val tagId = NfcScanLogic.tagIdHex(tag.id)
        if (!NfcScanLogic.shouldAccept(tagId, now, lastTagId, lastReadMs)) return
        lastTagId = tagId
        lastReadMs = now

        val payloads = runCatching {
            val message = Ndef.get(tag)?.cachedNdefMessage
            message?.records.orEmpty().mapNotNull { record ->
                if (record.tnf == NdefRecord.TNF_WELL_KNOWN) {
                    NfcScanLogic.parseWellKnown(record.type, record.payload)
                } else null
            }
        }.getOrDefault(emptyList())

        val scan = RecentNfcScan(
            tagId = tagId,
            payloads = payloads,
            technologies = tag.techList.map { it.substringAfterLast('.') },
            readAt = now
        )
        runOnUiThread {
            latest = scan
            recent = (listOf(scan) + recent.filterNot { it.tagId == scan.tagId }).take(8)
            status = if (payloads.isEmpty()) "Tag detected successfully. No supported NDEF text or URI payload was exposed." else "NFC tag read successfully."
        }
    }

    private fun refreshCapability() {
        capability = NfcScanLogic.capability(adapter != null, adapter?.isEnabled == true)
        status = when (capability) {
            NfcCapability.UNAVAILABLE -> "This device does not expose NFC hardware to the app."
            NfcCapability.DISABLED -> "NFC is available but currently switched off."
            NfcCapability.READY -> "NFC is ready for scan testing."
        }
    }

    @Composable
    private fun NfcScannerScreen() {
        val scan = latest

        Scaffold(containerColor = NfcBg, topBar = {
            Surface(color = Color(0xFF050B16)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("NFC Scanner", fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text("SCAN TEST", color = NfcAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { finish() }) { Text("Close") }
                }
            }
        }) { pad ->
            Column(
                Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusCard()
                if (capability == NfcCapability.DISABLED) {
                    Button(
                        onClick = { startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Open NFC Settings", fontWeight = FontWeight.Black) }
                }
                scan?.let { ScanCard(it) }
                if (recent.isNotEmpty()) {
                    HorizontalDivider(color = NfcAccent.copy(alpha = .15f))
                    Text("RECENT NFC SCANS", color = NfcAccent, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    recent.forEach { RecentCard(it) }
                }
                Surface(
                    color = NfcWarn.copy(alpha = .07f),
                    border = BorderStroke(1.dp, NfcWarn.copy(alpha = .25f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Scan-test only: this tool checks that NFC hardware and supported tags respond correctly. It reads tag identifiers, detected technologies, and common NDEF text/URI records exposed by Android. It does not link tags to inventory, change stock, read payment credentials, access secure contactless-card data, or bypass protected tags.",
                        Modifier.padding(14.dp),
                        color = NfcMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }

    @Composable
    private fun StatusCard() {
        val colour = when (capability) {
            NfcCapability.READY -> NfcGood
            NfcCapability.DISABLED -> NfcWarn
            NfcCapability.UNAVAILABLE -> Color(0xFFFF6B7A)
        }
        Surface(
            color = NfcCard,
            border = BorderStroke(1.dp, colour.copy(alpha = .35f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    when (capability) {
                        NfcCapability.READY -> "NFC READY"
                        NfcCapability.DISABLED -> "NFC OFF"
                        NfcCapability.UNAVAILABLE -> "NFC UNAVAILABLE"
                    },
                    color = colour,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
                Text(status, color = NfcMuted, fontSize = 12.sp)
            }
        }
    }

    @Composable
    private fun ScanCard(scan: RecentNfcScan) {
        Surface(color = NfcCard, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("LAST SCAN — PASS", color = NfcGood, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text(scan.tagId, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text(scan.technologies.joinToString(" • ").ifBlank { "Unknown tag technology" }, color = NfcMuted, fontSize = 11.sp)
                if (scan.payloads.isEmpty()) {
                    Text("Tag responded correctly; no supported NDEF text/URI payload found.", color = NfcMuted, fontSize = 12.sp)
                } else {
                    scan.payloads.forEach { payload ->
                        Text("${payload.kind}: ${payload.value}", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    @Composable
    private fun RecentCard(scan: RecentNfcScan) {
        Surface(color = Color(0xFF07101F), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(scan.tagId, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(scan.payloads.firstOrNull()?.value ?: "Tag detected — no readable NDEF payload", color = NfcMuted, fontSize = 10.sp, maxLines = 1)
                }
                Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(scan.readAt)), color = NfcMuted, fontSize = 10.sp)
            }
        }
    }
}
