package com.buysloans.hub

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val MARKETPLACE_SEARCH_API = "https://ghdhairijqjqivqriigi.supabase.co/functions/v1/marketplace-search"

data class MarketplaceListing(
    val title: String,
    val price: Double,
    val source: String,
    val url: String,
    val condition: String,
    val exact: Boolean,
    val score: Int,
    val reasons: String
)

data class MarketplaceEvidence(
    val gumtree: List<MarketplaceListing>,
    val facebook: List<MarketplaceListing>,
    val gumtreeError: String? = null,
    val facebookError: String? = null
)

private fun marketplaceNorm(s: String): String = s.lowercase()
    .replace(Regex("[^a-z0-9]+"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()

private val marketplaceBrands = listOf(
    "apple", "lenovo", "dell", "hp", "acer", "asus", "msi", "alienware",
    "gigabyte", "razer", "microsoft", "thermaltake", "corsair"
)

private val marketplaceFamilies = listOf(
    "mpg trident as", "trident as", "trident", "macbook pro", "macbook air", "macbook",
    "thinkcentre", "thinkstation", "thinkpad", "optiplex", "precision", "elitedesk",
    "prodesk", "legion", "loq", "omen", "victus", "predator", "nitro", "tuf",
    "rog", "zephyrus", "vivobook", "zenbook", "inspiron", "latitude", "xps",
    "aegis", "infinite", "codex", "surface laptop"
)

private data class MarketplaceFeatures(
    val brand: String = "",
    val family: String = "",
    val cpu: String = "",
    val gpu: String = "",
    val ram: String = "",
    val storage: String = "",
    val generation: String = "",
    val year: String = "",
    val modelTokens: List<String> = emptyList()
)

private fun marketplaceFeatures(s: String): MarketplaceFeatures {
    val x = marketplaceNorm(s)
    val explicitBrand = marketplaceBrands.firstOrNull { x.contains(Regex("\\b${Regex.escape(it)}\\b")) }.orEmpty()
    val family = marketplaceFamilies.firstOrNull { x.contains(it) }.orEmpty()
    val brand = if (explicitBrand.isBlank() && family.startsWith("macbook")) "apple" else explicitBrand
    val appleCpu = Regex("\\bm[1-4](?:\\s*(?:pro|max|ultra))?\\b").find(x)?.value.orEmpty()
    val intelCpuMatch = Regex("\\bi([3579])\\s*[- ]?\\s*(\\d{4,5}[a-z]{0,3})\\b").find(x)
    val intelCpu = intelCpuMatch?.let { "i${it.groupValues[1]} ${it.groupValues[2]}" }.orEmpty()
    val cpuTier = Regex("\\bi[3579]\\b").find(x)?.value.orEmpty()
    val cpu = when {
        appleCpu.isNotBlank() -> appleCpu
        intelCpu.isNotBlank() -> intelCpu
        else -> cpuTier
    }
    val gpu = Regex("\\b((?:rtx|gtx|rx|arc)\\s*\\d{3,4}(?:\\s*(?:ti|super|xt|xtx))?)\\b").find(x)?.groupValues?.get(1).orEmpty()
    val ram = Regex("\\b(8|12|16|18|24|32|36|48|64|96|128)\\s*gb\\b").find(x)?.groupValues?.get(1).orEmpty()
    val storage = Regex("\\b(?:128|256|512)\\s*gb\\b|\\b(?:1|2|4|8)\\s*tb\\b").find(x)?.value.orEmpty()
    val generation = Regex("\\bgen(?:eration)?\\s*(\\d+)\\b").find(x)?.groupValues?.get(1).orEmpty()
    val year = Regex("\\b(20(?:0[8-9]|[12]\\d|3[0-5]))\\b").find(x)?.value.orEmpty()
    val alphaNum = x.split(" ").filter {
        it.length >= 3 && it.any(Char::isLetter) && it.any(Char::isDigit) &&
            !it.startsWith("gen") && !Regex("^20\\d{2}$").matches(it) && it != storage.replace(" ", "")
    }
    return MarketplaceFeatures(brand, family, cpu, gpu, ram, storage, generation, year, alphaNum.distinct())
}

private fun familyCompatible(queryFamily: String, titleFamily: String): Boolean = when {
    queryFamily.isBlank() -> false
    queryFamily == "macbook" -> titleFamily.startsWith("macbook")
    else -> queryFamily == titleFamily
}

private fun isAccessoryListing(title: String, family: String): Boolean {
    val explicitAccessory = Regex("\\b(replacement|box only|empty box|for parts|faulty|repair|spares only)\\b").containsMatchIn(title)
    if (explicitAccessory) return true
    val accessoryTerms = Regex("\\b(cable|adapter|charger|battery|keyboard|mouse|case|cover|stand|webcam|module|ssd|ram kit|memory|motherboard|heatsink|fan|screen|display|bezel|hinge|dock|docking|power supply|psu)\\b").containsMatchIn(title)
    return family.isBlank() && accessoryTerms
}

internal fun classifyMarketplace(query: String, title: String): Triple<Boolean, Int, String> {
    val q = marketplaceFeatures(query)
    val t = marketplaceFeatures(title)
    val tx = marketplaceNorm(title)
    val compact = tx.replace(" ", "")
    var score = 0
    val reasons = mutableListOf<String>()

    val brandMatched = q.brand.isNotBlank() && q.brand == t.brand
    val brandMismatch = q.brand.isNotBlank() && t.brand.isNotBlank() && q.brand != t.brand
    val familyMatched = familyCompatible(q.family, t.family)
    val genericMacbook = q.family == "macbook"
    val qCpuTier = Regex("\\bi[3579]\\b").find(q.cpu)?.value.orEmpty()
    val cpuMatched = q.cpu.isBlank() ||
        (qCpuTier.isNotBlank() && tx.contains(Regex("\\b${Regex.escape(qCpuTier)}\\b"))) ||
        tx.contains(marketplaceNorm(q.cpu))
    val gpuMatched = q.gpu.isBlank() || tx.contains(marketplaceNorm(q.gpu))
    val yearMatched = q.year.isBlank() || q.year == t.year || tx.contains(Regex("\\b${Regex.escape(q.year)}\\b"))
    val storageMatched = q.storage.isBlank() || tx.contains(marketplaceNorm(q.storage))
    val ramMatched = q.ram.isBlank() || tx.contains(Regex("\\b${q.ram}\\s*gb\\b"))

    if (brandMatched) { score += 25; reasons += "Brand" }
    if (familyMatched) { score += 25; reasons += "Family" }
    if (yearMatched && q.year.isNotBlank()) { score += 15; reasons += "Year" }
    if (cpuMatched && q.cpu.isNotBlank()) { score += 15; reasons += "CPU" }
    if (gpuMatched && q.gpu.isNotBlank()) { score += 15; reasons += "GPU" }
    if (storageMatched && q.storage.isNotBlank()) { score += 10; reasons += "Storage" }
    if (ramMatched && q.ram.isNotBlank()) { score += 5; reasons += "RAM" }

    val hits = q.modelTokens.count {
        compact.contains(it.replace(" ", "")) || tx.contains(Regex("\\b${Regex.escape(it)}\\b"))
    }
    if (hits > 0) { score += minOf(25, hits * 12); reasons += "Model" }
    score = score.coerceAtMost(100)

    val accessory = isAccessoryListing(tx, t.family)
    val generationMismatch = q.generation.isNotBlank() && t.generation.isNotBlank() && q.generation != t.generation
    val yearMismatch = q.year.isNotBlank() && t.year.isNotBlank() && q.year != t.year
    val cpuMismatch = q.cpu.isNotBlank() && t.cpu.isNotBlank() && !cpuMatched
    val storageMismatch = q.storage.isNotBlank() && t.storage.isNotBlank() && !storageMatched
    val familyMismatch = q.family.isNotBlank() && t.family.isNotBlank() && !familyMatched

    val requiredModelHits = when {
        q.modelTokens.isEmpty() -> 0
        q.modelTokens.size <= 2 -> 1
        else -> (q.modelTokens.size + 1) / 2
    }
    val modelSatisfied = q.modelTokens.isEmpty() || hits >= requiredModelHits
    val macbookIdentity = q.family.startsWith("macbook") && !genericMacbook && yearMatched && cpuMatched && storageMatched
    val generalIdentity = when {
        q.modelTokens.isNotEmpty() -> modelSatisfied
        q.gpu.isNotBlank() -> gpuMatched
        q.cpu.isNotBlank() -> cpuMatched
        else -> false
    }
    val hasSpecificIdentity = if (q.family.startsWith("macbook")) macbookIdentity else generalIdentity

    val exact = !brandMismatch && !familyMismatch && !accessory && !generationMismatch &&
        !yearMismatch && !cpuMismatch && !storageMismatch && brandMatched && familyMatched && hasSpecificIdentity

    if (genericMacbook) reasons += "Specify MacBook Air or Pro"
    if (brandMismatch) reasons += "Different brand"
    if (familyMismatch) reasons += "Different product family"
    if (accessory) reasons += "Part/accessory"
    if (generationMismatch) reasons += "Generation mismatch"
    if (yearMismatch) reasons += "Year mismatch"
    if (cpuMismatch) reasons += "CPU mismatch"
    if (storageMismatch) reasons += "Storage mismatch"
    if (q.modelTokens.isNotEmpty() && !modelSatisfied) reasons += "Model mismatch"
    if (!hasSpecificIdentity && !genericMacbook) reasons += "Insufficient model identity"

    return Triple(exact, score, reasons.distinct().joinToString(" + "))
}

private fun parseMarketplaceItems(query: String, obj: JSONObject?): List<MarketplaceListing> {
    if (obj == null) return emptyList()
    val arr: JSONArray = obj.optJSONArray("items") ?: JSONArray()
    val out = mutableListOf<MarketplaceListing>()
    for (i in 0 until arr.length()) {
        val x = arr.optJSONObject(i) ?: continue
        val price = x.optDouble("price", 0.0)
        if (price <= 0) continue
        val title = x.optString("title")
        val (exact, score, reasons) = classifyMarketplace(query, title)
        out += MarketplaceListing(
            title = title,
            price = price,
            source = x.optString("source"),
            url = x.optString("url"),
            condition = x.optString("condition"),
            exact = exact,
            score = score,
            reasons = reasons
        )
    }
    return out.distinctBy { "${marketplaceNorm(it.title)}|${it.price.toInt()}|${it.url}" }
}

suspend fun searchMarketplaceEvidence(context: Context, query: String): MarketplaceEvidence = withContext(Dispatchers.IO) {
    val token = AuthManager.validAccessToken(context)
    require(token.isNotBlank()) { "Sign in again to search marketplace evidence." }
    val connection = (URL(MARKETPLACE_SEARCH_API).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 15_000
        readTimeout = 20_000
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
        setRequestProperty("Authorization", "Bearer $token")
    }
    try {
        connection.outputStream.use { it.write(JSONObject().put("query", query.trim()).toString().toByteArray()) }
        val code = connection.responseCode
        val body = (if (code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        val root = runCatching { JSONObject(body.ifBlank { "{}" }) }.getOrElse { JSONObject() }
        if (code == 401) throw IllegalStateException("Your session has expired. Please sign in again.")
        if (code !in 200..299 || !root.optBoolean("success")) {
            throw IllegalStateException(root.optString("error", "Marketplace search failed ($code)"))
        }
        val errors = root.optJSONObject("sourceErrors")
        MarketplaceEvidence(
            gumtree = parseMarketplaceItems(query, root.optJSONObject("gumtree")),
            facebook = parseMarketplaceItems(query, root.optJSONObject("facebook")),
            gumtreeError = errors?.optString("gumtree")?.takeIf { it.isNotBlank() && it != "null" },
            facebookError = errors?.optString("facebook")?.takeIf { it.isNotBlank() && it != "null" }
        )
    } finally {
        connection.disconnect()
    }
}
