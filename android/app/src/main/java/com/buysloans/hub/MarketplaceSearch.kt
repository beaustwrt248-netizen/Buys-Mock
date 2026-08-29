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
    .replace(Regex("[^a-z0-9.]+"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()

private val marketplaceBrands = listOf(
    "apple", "lenovo", "dell", "hp", "hewlett packard", "acer", "asus", "msi", "alienware",
    "gigabyte", "razer", "microsoft", "samsung", "lg", "huawei", "toshiba", "dynabook",
    "fujitsu", "panasonic", "framework", "medion", "clevo", "chuwi", "gateway", "vaio"
)

private val marketplaceFamilies = listOf(
    "macbook pro", "macbook air", "macbook", "thinkpad", "ideapad", "thinkbook", "yoga",
    "legion", "loq", "latitude", "inspiron", "xps", "precision", "vostro", "alienware",
    "elitebook", "probook", "pavilion", "envy", "spectre", "omen", "victus", "zbook",
    "aspire", "swift", "travelmate", "chromebook", "predator", "nitro", "zenbook", "vivobook",
    "expertbook", "proart", "rog", "zephyrus", "tuf", "modern", "prestige", "stealth",
    "katana", "raider", "creator", "blade", "aero", "aorus", "surface laptop", "surface book",
    "galaxy book", "gram", "matebook", "tecra", "satellite", "dynabook", "lifebook",
    "toughbook", "framework laptop"
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
    val size: String = "",
    val modelTokens: List<String> = emptyList()
)

private fun marketplaceFeatures(s: String): MarketplaceFeatures {
    val x = marketplaceNorm(s)
    val explicitBrand = marketplaceBrands.firstOrNull { x.contains(Regex("\\b${Regex.escape(it)}\\b")) }.orEmpty()
    val family = marketplaceFamilies.firstOrNull { x.contains(it) }.orEmpty()
    val inferredBrand = when {
        family.startsWith("macbook") -> "apple"
        family.startsWith("think") || family in listOf("ideapad", "yoga", "legion", "loq") -> "lenovo"
        family in listOf("latitude", "inspiron", "xps", "precision", "vostro", "alienware") -> "dell"
        family in listOf("elitebook", "probook", "pavilion", "envy", "spectre", "omen", "victus", "zbook") -> "hp"
        family in listOf("aspire", "swift", "travelmate", "predator", "nitro") -> "acer"
        family in listOf("zenbook", "vivobook", "expertbook", "proart", "rog", "zephyrus", "tuf") -> "asus"
        family in listOf("modern", "prestige", "stealth", "katana", "raider", "creator") -> "msi"
        family == "blade" -> "razer"
        family.startsWith("surface") -> "microsoft"
        family == "galaxy book" -> "samsung"
        family == "gram" -> "lg"
        family == "matebook" -> "huawei"
        family == "lifebook" -> "fujitsu"
        family == "toughbook" -> "panasonic"
        family == "framework laptop" -> "framework"
        else -> ""
    }
    val brand = explicitBrand.ifBlank { inferredBrand }
    val appleCpu = Regex("\\bm[1-4](?:\\s*(?:pro|max|ultra))?\\b").find(x)?.value.orEmpty()
    val intelCpuMatch = Regex("\\bi([3579])\\s*[- ]?\\s*(\\d{4,5}[a-z]{0,3})\\b").find(x)
    val intelCpu = intelCpuMatch?.let { "i${it.groupValues[1]} ${it.groupValues[2]}" }.orEmpty()
    val ryzenCpu = Regex("\\bryzen\\s*[3579]\\s*\\d{4}[a-z]{0,2}\\b").find(x)?.value.orEmpty()
    val cpuTier = Regex("\\bi[3579]\\b").find(x)?.value.orEmpty()
    val cpu = when {
        appleCpu.isNotBlank() -> appleCpu
        intelCpu.isNotBlank() -> intelCpu
        ryzenCpu.isNotBlank() -> ryzenCpu
        else -> cpuTier
    }
    val gpu = Regex("\\b((?:rtx|gtx|rx|arc)\\s*\\d{3,4}(?:\\s*(?:ti|super|xt|xtx))?)\\b").find(x)?.groupValues?.get(1).orEmpty()
    val ram = Regex("\\b(4|8|12|16|18|24|32|36|48|64|96|128)\\s*gb\\b").find(x)?.groupValues?.get(1).orEmpty()
    val storage = Regex("\\b(?:64|128|256|512)\\s*gb\\b|\\b(?:1|2|4|8)\\s*tb\\b").find(x)?.value.orEmpty()
    val generation = Regex("\\bgen(?:eration)?\\s*(\\d+)\\b").find(x)?.groupValues?.get(1).orEmpty()
    val year = Regex("\\b(20(?:0[8-9]|[12]\\d|3[0-5]))\\b").find(x)?.value.orEmpty()
    val size = Regex("\\b(10(?:\\.\\d)?|11(?:\\.\\d)?|12(?:\\.\\d)?|13(?:\\.\\d)?|14(?:\\.\\d)?|15(?:\\.\\d)?|16(?:\\.\\d)?|17(?:\\.\\d)?|18(?:\\.\\d)?)\\s*(?:inch|inches|\\\")\\b")
        .find(x)?.groupValues?.get(1).orEmpty()
    val alphaNum = x.split(" ").filter {
        it.length >= 3 && it.any(Char::isLetter) && it.any(Char::isDigit) &&
            !it.startsWith("gen") && !Regex("^20\\d{2}$").matches(it) && it != storage.replace(" ", "")
    }
    return MarketplaceFeatures(brand, family, cpu, gpu, ram, storage, generation, year, size, alphaNum.distinct())
}

private fun familyCompatible(queryFamily: String, titleFamily: String): Boolean = when {
    queryFamily.isBlank() -> false
    queryFamily == "macbook" -> titleFamily.startsWith("macbook")
    else -> queryFamily == titleFamily
}

internal fun classifyMarketplace(query: String, title: String): Triple<Boolean, Int, String> {
    val q = marketplaceFeatures(query)
    val t = marketplaceFeatures(title)
    val tx = marketplaceNorm(title)
    val compact = tx.replace(" ", "")
    var score = 0
    val reasons = mutableListOf<String>()

    val hardFilter = LaptopListingFilter.decision(title)
    val identifierConflict = LaptopListingFilter.conflictingIdentifier(query, title)
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
    val sizeMatched = q.size.isBlank() || t.size.isBlank() || q.size == t.size

    if (brandMatched) { score += 25; reasons += "Brand" }
    if (familyMatched) { score += 25; reasons += "Family" }
    if (yearMatched && q.year.isNotBlank()) { score += 15; reasons += "Year" }
    if (cpuMatched && q.cpu.isNotBlank()) { score += 15; reasons += "CPU" }
    if (gpuMatched && q.gpu.isNotBlank()) { score += 15; reasons += "GPU" }
    if (storageMatched && q.storage.isNotBlank()) { score += 10; reasons += "Storage" }
    if (ramMatched && q.ram.isNotBlank()) { score += 5; reasons += "RAM" }
    if (sizeMatched && q.size.isNotBlank() && t.size.isNotBlank()) { score += 10; reasons += "Size" }

    val hits = q.modelTokens.count {
        compact.contains(it.replace(" ", "")) || tx.contains(Regex("\\b${Regex.escape(it)}\\b"))
    }
    if (hits > 0) { score += minOf(25, hits * 12); reasons += "Model" }
    score = score.coerceAtMost(100)

    val generationMismatch = q.generation.isNotBlank() && t.generation.isNotBlank() && q.generation != t.generation
    val yearMismatch = q.year.isNotBlank() && t.year.isNotBlank() && q.year != t.year
    val cpuMismatch = q.cpu.isNotBlank() && t.cpu.isNotBlank() && !cpuMatched
    val storageMismatch = q.storage.isNotBlank() && t.storage.isNotBlank() && !storageMatched
    val sizeMismatch = q.size.isNotBlank() && t.size.isNotBlank() && q.size != t.size
    val familyMismatch = q.family.isNotBlank() && t.family.isNotBlank() && !familyMatched

    val requiredModelHits = when {
        q.modelTokens.isEmpty() -> 0
        q.modelTokens.size <= 2 -> 1
        else -> (q.modelTokens.size + 1) / 2
    }
    val modelSatisfied = q.modelTokens.isEmpty() || hits >= requiredModelHits
    val macbookIdentity = q.family.startsWith("macbook") && !genericMacbook && yearMatched && cpuMatched && storageMatched && sizeMatched && !identifierConflict
    val generalIdentity = when {
        identifierConflict -> false
        q.modelTokens.isNotEmpty() -> modelSatisfied
        q.gpu.isNotBlank() -> gpuMatched
        q.cpu.isNotBlank() -> cpuMatched
        else -> false
    }
    val hasSpecificIdentity = if (q.family.startsWith("macbook")) macbookIdentity else generalIdentity

    val exact = !hardFilter.rejected && !identifierConflict && !brandMismatch && !familyMismatch &&
        !generationMismatch && !yearMismatch && !cpuMismatch && !storageMismatch && !sizeMismatch &&
        brandMatched && familyMatched && hasSpecificIdentity

    if (hardFilter.rejected) reasons += hardFilter.reason ?: "Rejected listing"
    if (genericMacbook) reasons += "Specify MacBook Air or Pro"
    if (brandMismatch) reasons += "Different brand"
    if (familyMismatch) reasons += "Different product family"
    if (identifierConflict) reasons += "Model identifier mismatch"
    if (generationMismatch) reasons += "Generation mismatch"
    if (yearMismatch) reasons += "Year mismatch"
    if (cpuMismatch) reasons += "CPU mismatch"
    if (storageMismatch) reasons += "Storage mismatch"
    if (sizeMismatch) reasons += "Size mismatch"
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
    return out.distinctBy {
        val canonicalTitle = marketplaceNorm(it.title)
            .replace(Regex("\\b(aud|au|pickup|postage|shipping|obo|ono)\\b"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        "$canonicalTitle|${it.price.toInt()}"
    }
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
