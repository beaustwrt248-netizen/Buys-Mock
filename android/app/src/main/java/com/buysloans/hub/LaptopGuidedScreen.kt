package com.buysloans.hub

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val GuidedAccent = Color(0xFFFFD400)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaptopGuidedScreen() = Screen("💻 Laptop / MacBook") {
    var brand by remember { mutableStateOf("") }
    var preset by remember { mutableStateOf<LaptopPreset?>(null) }
    var processor by remember { mutableStateOf("") }
    var ram by remember { mutableStateOf("") }
    var storage by remember { mutableStateOf("") }
    var ask by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<MarketResult?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Choose the laptop configuration.") }
    val scope = rememberCoroutineScope()

    Text(
        "Select the exact configuration before Morley searches the market. This prevents different generations from being mixed together.",
        color = Color.LightGray
    )

    GuidedDropdown("Brand", brand, LaptopSelectionCatalog.brands) { selected ->
        brand = selected
        preset = null
        processor = ""
        ram = ""
        storage = ""
        result = null
    }

    val modelOptions = if (brand.isBlank()) emptyList() else LaptopSelectionCatalog.models(brand)
    GuidedDropdown(
        label = "Model / generation",
        value = preset?.model.orEmpty(),
        options = modelOptions.map { it.model },
        enabled = brand.isNotBlank()
    ) { modelName ->
        preset = modelOptions.firstOrNull { it.model == modelName }
        processor = ""
        ram = ""
        storage = ""
        result = null
    }

    val selected = preset
    GuidedDropdown(
        "Processor",
        processor,
        selected?.processors.orEmpty(),
        enabled = selected != null
    ) { processor = it; result = null }

    GuidedDropdown(
        "RAM",
        ram,
        selected?.ramOptions.orEmpty(),
        enabled = selected != null
    ) { ram = it; result = null }

    GuidedDropdown(
        "Storage",
        storage,
        selected?.storageOptions.orEmpty(),
        enabled = selected != null
    ) { storage = it; result = null }

    OutlinedTextField(
        value = ask,
        onValueChange = { ask = it.filter { ch -> ch.isDigit() || ch == '.' } },
        label = { Text("Seller asking price") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    val ready = selected != null && processor.isNotBlank() && ram.isNotBlank() && storage.isNotBlank()
    val canonical = if (ready) LaptopSelectionCatalog.canonicalQuery(selected!!, processor, ram, storage) else ""

    if (canonical.isNotBlank()) {
        Block("SELECTED CONFIGURATION", canonical)
    }

    Button(
        onClick = {
            result = null
            busy = true
            status = "Searching exact configuration evidence…"
            scope.launch {
                runCatching { market(canonical) }
                    .onSuccess {
                        result = it
                        val exact = it.exactGoogle.size + it.exactEbay.size
                        val similar = it.similarGoogle.size + it.similarEbay.size
                        status = "$exact exact • $similar similar • ${it.rejected.size} rejected"
                    }
                    .onFailure { status = it.message ?: "Search failed" }
                busy = false
            }
        },
        enabled = ready && !busy,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GuidedAccent, contentColor = Color.Black),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(if (busy) "Searching…" else "Analyse Laptop", fontWeight = FontWeight.Black, fontSize = 17.sp)
    }

    Text(status, color = Color.LightGray)
    if (!busy) result?.let { Valuation(it, ask, 0.30, 0.58) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GuidedDropdown(
    label: String,
    value: String,
    options: List<String>,
    enabled: Boolean = true,
    onSelected: (String) -> Unit
) {
    var expanded by remember(label, value, enabled) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (enabled && options.isNotEmpty()) expanded = !expanded }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                label = { Text(label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.distinct().forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
