package com.buysloans.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val GuidedAccent = Color(0xFF167A5A)
private const val GuidedApi = "https://ghdhairijqjqivqriigi.supabase.co/functions/v1/ebay-search"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaptopGuidedScreen() = Screen("💻 Laptop / MacBook") {
    var brand by remember { mutableStateOf("") }
    var preset by remember { mutableStateOf<LaptopPreset?>(null) }
    var versionCode by remember { mutableStateOf("") }
    var processor by remember { mutableStateOf("") }
    var ram by remember { mutableStateOf("") }
    var storage by remember { mutableStateOf("") }
    var ask by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<MarketResult?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Choose the laptop configuration.") }
    val scope = rememberCoroutineScope()

    Text(
        "Select the exact configuration before Morley searches the market. Different generations are excluded from the valuation.",
        color = MorleyTextSecondary
    )

    GuidedDropdown("Brand", brand, LaptopSelectionCatalog.brands) { selected ->
        brand = selected
        preset = null
        versionCode = ""
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
        versionCode = ""
        processor = ""
        ram = ""
        storage = ""
        result = null
    }

    val selected = preset
    val factoryProfile = LaptopFactoryVariantCatalog.profile(selected)
    val versionOptions = LaptopFactoryVariantCatalog.versionCodes(selected)

    if (factoryProfile != null) {
        GuidedDropdown(
            label = "Verified version / model code",
            value = versionCode,
            options = versionOptions,
            enabled = selected != null
        ) {
            versionCode = it
            processor = ""
            ram = ""
            storage = ""
            result = null
        }
        if (versionCode.isNotBlank()) {
            LaptopFactoryVariantCatalog.sourceLabel(selected, versionCode)?.let { source ->
                Text("Manufacturer-verified configuration • $source", color = MorleyTextSecondary, fontSize = 12.sp)
            }
        }
    } else if (selected != null) {
        Text(
            "Factory configuration verification is still being expanded for this catalogue model. Legacy options remain available but are not labelled manufacturer-verified.",
            color = MorleyTextSecondary,
            fontSize = 12.sp
        )
    }

    val processorOptions = when {
        selected == null -> emptyList()
        factoryProfile != null && versionCode.isNotBlank() -> LaptopFactoryVariantCatalog.processors(selected, versionCode)
        factoryProfile != null -> emptyList()
        else -> selected.processors
    }
    GuidedDropdown("Processor", processor, processorOptions, selected != null && (factoryProfile == null || versionCode.isNotBlank())) {
        processor = it
        ram = ""
        storage = ""
        result = null
    }

    val ramOptions = when {
        selected == null -> emptyList()
        factoryProfile != null && versionCode.isNotBlank() && processor.isNotBlank() ->
            LaptopFactoryVariantCatalog.ramOptions(selected, versionCode, processor)
        factoryProfile != null -> emptyList()
        else -> selected.ramOptions
    }
    GuidedDropdown("RAM", ram, ramOptions, processor.isNotBlank()) {
        ram = it
        storage = ""
        result = null
    }

    val storageOptions = when {
        selected == null -> emptyList()
        factoryProfile != null && versionCode.isNotBlank() && processor.isNotBlank() && ram.isNotBlank() ->
            LaptopFactoryVariantCatalog.storageOptions(selected, versionCode, processor, ram)
        factoryProfile != null -> emptyList()
        else -> selected.storageOptions
    }
    GuidedDropdown("Storage", storage, storageOptions, ram.isNotBlank()) {
        storage = it
        result = null
    }

    OutlinedTextField(
        value = ask,
        onValueChange = { ask = it.filter { ch -> ch.isDigit() || ch == '.' } },
        label = { Text("Seller Ask") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    val basicReady = selected != null && processor.isNotBlank() && ram.isNotBlank() && storage.isNotBlank()
    val ready = basicReady && if (factoryProfile != null) {
        versionCode.isNotBlank() && LaptopFactoryVariantCatalog.configurationVerified(selected, versionCode, processor, ram, storage)
    } else true
    val displayProcessor = if (ready) processor.removePrefix("${selected!!.brand} ") else ""
    val displayConfiguration = if (ready) {
        listOf(selected!!.brand, selected.model, versionCode.takeIf { it.isNotBlank() }, displayProcessor, ram, storage)
            .filterNotNull()
            .joinToString(" ")
    } else ""
    if (displayConfiguration.isNotBlank()) Block("SELECTED CONFIGURATION", displayConfiguration)

    Button(
        onClick = {
            result = null
            busy = true
            status = "Searching exact configuration evidence…"
            scope.launch {
                runCatching { guidedMarket(selected!!, processor, ram, storage, versionCode) }
                    .onSuccess {
                        result = it
                        val exact = it.exactGoogle.size + it.exactEbay.size
                        val similar = it.similarGoogle.size + it.similarEbay.size
                        status = "Google/eBay: $exact exact • $similar similar • ${it.rejected.size} rejected"
                    }
                    .onFailure { status = it.message ?: "Search failed" }
                busy = false
            }
        },
        enabled = ready && !busy,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GuidedAccent, contentColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(if (busy) "Searching…" else "Analyse Laptop", fontWeight = FontWeight.Black, fontSize = 17.sp)
    }

    Text(status, color = MorleyTextSecondary)
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

private suspend fun guidedMarket(
    preset: LaptopPreset,
    processor: String,
    ram: String,
    storage: String,
    versionCode: String = ""
): MarketResult {
    val query = if (versionCode.isNotBlank()) {
        LaptopFactoryVariantCatalog.canonicalQuery(preset, versionCode, processor, ram, storage)
    } else {
        LaptopSelectionCatalog.canonicalQuery(preset, processor, ram, storage)
    }
    val root = guidedRequest(query)
    val google = guidedParse(root.optJSONObject("google"), preset, processor, ram, storage)
    val ebay = guidedParse(root.optJSONObject("ebay"), preset, processor, ram, storage)
    return MarketResult(
        exactGoogle = google.filter { it.tier == MatchTier.EXACT }.distinctBy { it.title.lowercase() to it.price.toInt() },
        exactEbay = ebay.filter { it.tier == MatchTier.EXACT }.distinctBy { it.title.lowercase() to it.price.toInt() },
        similarGoogle = google.filter { it.tier == MatchTier.SIMILAR }.distinctBy { it.title.lowercase() to it.price.toInt() },
        similarEbay = ebay.filter { it.tier == MatchTier.SIMILAR }.distinctBy { it.title.lowercase() to it.price.toInt() },
        rejected = (google + ebay).filter { it.tier == MatchTier.REJECTED }.distinctBy { it.title.lowercase() to it.price.toInt() },
        searches = listOf(query)
    )
}

private suspend fun guidedRequest(query: String): JSONObject = withContext(Dispatchers.IO) {
    val token = AuthManager.validAccessToken(MorleyApplication.instance)
    if (token.isBlank()) throw IllegalStateException("Your secure session has expired. Sign in again.")
    val connection = (URL(GuidedApi).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 15_000
        readTimeout = 20_000
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
        setRequestProperty("Authorization", "Bearer $token")
    }
    try {
        val body = JSONObject()
            .put("query", query)
            .put("limit", 50)
            .put("australiaOnly", true)
            .put("mode", "device")
            .toString()
        connection.outputStream.use { it.write(body.toByteArray()) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        val root = runCatching { JSONObject(text) }.getOrElse { JSONObject() }
        if (code !in 200..299 || !root.optBoolean("success")) {
            throw IllegalStateException(root.optString("error", "HTTP $code"))
        }
        root
    } finally {
        connection.disconnect()
    }
}

private fun guidedParse(
    obj: JSONObject?,
    preset: LaptopPreset,
    processor: String,
    ram: String,
    storage: String
): List<Listing> {
    if (obj == null) return emptyList()
    val items = obj.optJSONArray("items") ?: JSONArray()
    return buildList {
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val title = item.optString("title", item.optString("name", "Untitled result"))
            val price = listOf("deliveredPrice", "price", "itemPrice")
                .asSequence().map { item.optDouble(it, 0.0) }.firstOrNull { it > 0 } ?: 0.0
            if (price <= 0) continue
            val decision = guidedClassify(title, preset, processor, ram, storage)
            add(
                Listing(
                    title = title,
                    price = price,
                    source = item.optString("seller", item.optString("store", item.optString("source", item.optString("merchant", "")))),
                    url = item.optString("url", item.optString("link", item.optString("itemWebUrl", ""))),
                    condition = item.optString("condition", ""),
                    tier = decision.first,
                    score = decision.second,
                    reasons = decision.third
                )
            )
        }
    }
}

private fun guidedClassify(
    title: String,
    preset: LaptopPreset,
    processor: String,
    ram: String,
    storage: String
): Triple<MatchTier, Int, String> {
    val t = normalizeGuided(title)
    val reasons = mutableListOf<String>()
    val hard = LaptopListingFilter.decision(title)
    if (hard.rejected) return Triple(MatchTier.REJECTED, 0, hard.reason ?: "Rejected listing")

    val brand = normalizeGuided(preset.brand)
    val family = normalizeGuided(preset.model.substringBefore(" (").replace("retina ", ""))
    val year = preset.year.toString()
    val cpu = normalizeGuided(processor)
    val selectedRam = normalizeGuided(ram)
    val selectedStorage = normalizeGuided(storage)

    val brandHit = t.contains(Regex("\\b${Regex.escape(brand)}\\b"))
    val familyTokens = family.split(" ").filter { it.length > 2 && it !in setOf("inch", "retina") }
    val familyHit = familyTokens.all { t.contains(Regex("\\b${Regex.escape(it)}\\b")) }
    val yearHit = t.contains(Regex("\\b${Regex.escape(year)}\\b"))
    val cpuHit = cpu.split(" ").filter { it.length > 1 }.all { t.contains(Regex("\\b${Regex.escape(it)}\\b")) }
    val ramHit = t.contains(Regex("\\b${Regex.escape(selectedRam.replace(" ", ""))}\\b")) || t.contains(normalizeGuided(ram.replace("GB", " gb")))
    val storageHit = t.contains(normalizeGuided(storage.replace("GB", " gb").replace("TB", " tb"))) || t.replace(" ", "").contains(selectedStorage.replace(" ", ""))

    if (brandHit) reasons += "Brand"
    if (familyHit) reasons += "Model"
    if (yearHit) reasons += "Year"
    if (cpuHit) reasons += "Processor"
    if (ramHit) reasons += "RAM"
    if (storageHit) reasons += "Storage"

    val conflictingYear = Regex("\\b(20\\d{2})\\b").findAll(t).map { it.value }.any { it != year }
    if (conflictingYear) reasons += "Generation mismatch"

    val exact = brandHit && familyHit && yearHit && cpuHit && ramHit && storageHit && !conflictingYear
    val base = listOf(brandHit, familyHit, yearHit, cpuHit, ramHit, storageHit).count { it }
    val score = (base * 100 / 6).coerceIn(0, 100)
    val similar = !conflictingYear && brandHit && familyHit && yearHit && base >= 4
    val tier = when {
        exact -> MatchTier.EXACT
        similar -> MatchTier.SIMILAR
        else -> MatchTier.REJECTED
    }
    if (!exact && !similar) reasons += "Insufficient exact configuration identity"
    return Triple(tier, score, reasons.distinct().joinToString(" + "))
}

private fun normalizeGuided(value: String): String = value.lowercase()
    .replace(Regex("[^a-z0-9]+"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()
