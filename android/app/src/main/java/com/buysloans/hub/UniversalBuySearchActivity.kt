package com.buysloans.hub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

class UniversalBuySearchActivity : ComponentActivity() {
    private var scanning by mutableStateOf(false)
    private var scanNotice by mutableStateOf("")
    private val googleCodeScanner by lazy { GmsBarcodeScanning.getClient(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = MorleyColorScheme) {
                UniversalBuySearchScreen(
                    scanning = scanning,
                    scanNotice = scanNotice,
                    onScan = ::scanForSearch,
                    onBack = { finish() }
                )
            }
        }
    }

    private fun scanForSearch(onResult: (String) -> Unit) {
        if (scanning) return
        scanning = true
        scanNotice = "Opening Google scanner…"
        googleCodeScanner.startScan()
            .addOnSuccessListener { result ->
                val value = result.rawValue.orEmpty().trim()
                if (value.isBlank()) {
                    scanNotice = "No searchable code was returned."
                } else {
                    onResult(value)
                    scanNotice = "Code scanned — searching Morley."
                }
                scanning = false
            }
            .addOnCanceledListener {
                scanNotice = "Google scanner closed."
                scanning = false
            }
            .addOnFailureListener {
                scanNotice = "Google scanner is unavailable right now. You can still search manually."
                scanning = false
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UniversalBuySearchScreen(
    scanning: Boolean,
    scanNotice: String,
    onScan: ((String) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<UniversalBuySearchResult?>(null) }
    val results = remember(query) { if (query.isBlank()) emptyList() else UniversalBuySearch.search(query, 30) }

    Scaffold(
        containerColor = MorleyBackground,
        topBar = {
            TopAppBar(
                title = { Text("Universal Buy Search", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    Button(onClick = onBack, modifier = Modifier.padding(start = 8.dp)) { Text("Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MorleyBackground)
            )
        }
    ) { pad ->
        LazyColumn(
            Modifier.fillMaxSize().padding(pad).padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        selected = null
                    },
                    label = { Text("Search phones, laptops, consoles or any item") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Button(
                    onClick = {
                        onScan { scannedValue ->
                            query = scannedValue
                            selected = null
                        }
                    },
                    enabled = !scanning,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MorleyAccent,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        if (scanning) "Opening Google Scanner…" else "Scan Code with Google Camera",
                        fontWeight = FontWeight.Black
                    )
                }
            }
            item {
                Text(
                    "Use the Google Play services camera shown in the barcode scanner to scan a barcode, QR code or encoded model/stock label. Google returns the code result and Morley uses it as the search text.",
                    color = MorleyTextSecondary,
                    fontSize = 12.sp
                )
            }
            if (scanNotice.isNotBlank()) {
                item {
                    Surface(
                        color = MorleyAccentSoft,
                        border = BorderStroke(1.dp, MorleyBorder),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(scanNotice, Modifier.padding(12.dp), color = MorleyTextPrimary, fontSize = 12.sp)
                    }
                }
            }
            if (query.isBlank()) {
                item {
                    Surface(
                        color = MorleySurface,
                        border = BorderStroke(1.dp, MorleyBorder),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("One search across Morley", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MorleyTextPrimary)
                            Text("Search by friendly name, storage, model number or a code returned by the Google scanner. Unpriced catalogue items stay searchable without authorising a buy.", color = MorleyTextSecondary)
                        }
                    }
                }
            }
            items(results) { result ->
                UniversalResultCard(result = result, selected = selected == result, onClick = { selected = result })
            }
            selected?.let { result ->
                item { UniversalBuyDecisionCard(result) }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun UniversalResultCard(result: UniversalBuySearchResult, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (selected) MorleyAccentSoft else MorleySurface),
        border = BorderStroke(1.dp, if (selected) MorleyAccent else MorleyBorder),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(result.title, fontWeight = FontWeight.Black, color = MorleyTextPrimary, modifier = Modifier.weight(1f))
                Text(result.category.name.replace('_', ' '), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MorleyAccent)
            }
            Text(result.subtitle, color = MorleyTextSecondary, fontSize = 12.sp)
            val status = if (result.canAuthoriseBuy && result.priceSheetValue != null) {
                "Authorised Morley price: $${result.priceSheetValue.toInt()}"
            } else {
                "Reference/search result only — no authorised buy price"
            }
            Text(status, fontWeight = FontWeight.Bold, color = if (result.canAuthoriseBuy) MorleySuccess else MorleyTextMuted)
        }
    }
}

@Composable
private fun UniversalBuyDecisionCard(result: UniversalBuySearchResult) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MorleySurfaceRaised),
        border = BorderStroke(1.dp, MorleyBorder),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Buy decision", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MorleyTextPrimary)
            if (result.canAuthoriseBuy && result.priceSheetValue != null) {
                Text("Select condition/grade in the unified buy flow next. A/B/C price controls remain 70% / 50% / 30% of the authoritative Morley price-sheet value.", color = MorleyTextSecondary)
            } else {
                Text("This result can be identified and reviewed, but it cannot calculate or authorise a buy until an approved Morley price exists.", color = MorleyTextSecondary)
            }
        }
    }
}
