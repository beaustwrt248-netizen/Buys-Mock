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
    "mpg trident as", "trident as", "trident", "macbook pro", "macbook air",
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
    val modelTokens: List<String> = emptyList()
)

private fun marketplaceFeatures(s: String): MarketplaceFeatures {
    val x = marketplaceNorm(s)
    val brand = marketplaceBrands.firstOrNull { x.contains(Regex("\\b${Regex.escape(it)}\\b")) }.orEmpty()
    val family = marketplaceFamilies.firstOrNull { x.contains(it) }.orEmpty()
    val cpu = Regex("\\b(?:i[3579][ -]?)?(\\d{4,5}[a-z]{0,3})\\b").find(x)?.groupValues?.get(1).orEmpty()
    val gpu = Regex("\\b((?:rtx|gtx|rx|arc)\\s*\\d{3,4}(?:\\s*(?:ti|super|xt|xtx))?)\\b").find(x)?.groupValues?.get(1).orEmpty()
    val ram = Regex("\\b(8|12|16|18|24|32|36|48|64|96|128)\\s*gb\\b").find(x)?.groupValues?.get(1).orEmpty()
    val storage = Regex("\\b(?:128|256|512)\\s*gb\\b|\\b(?:1|2|4|8)\\s*tb\\b").find(x)?.value.orEmpty()
    val alphaNum = x.split(" ").filter { it.length >= 3 && it.any(Char::isLetter) && it.any(Char::isDigit) && it != cpu }
    val gen = Regex("\\bgen\\s*(\\d+)\\b").find(x)?.groupValues?.get(1)?.let { "gen$it" }
    return MarketplaceFeatures(brand, family, cpu, gpu, ram, storage, (alphaNum + listOfNotNull(gen)).distinct())
}

private fun classifyMarketplace(query: String, title: String): Triple<Boolean, Int, String> {
    val q = marketplaceFeatures(query)
    val t = marketplaceFeatures(title)
    val tx = marketplaceNorm(title)
    val compact = tx.replace(" ", "")
    var score = 0
    val reasons = mutableListOf<String>()
    val brandMismatch = q.brand.isNotBlank() && t.brand.isNotBlank() && q.brand != t.brand
    if (q.brand.isNotBlank() && q.brand == t.brand) { score += 30; reasons += "Brand" }
    if (q.family.isNotBlank() && tx.contains(q.family)) { score += 28; reasons += "Family" }
    val hits = q.modelTokens.count { compact.contains(it.replace(" ", "")) || tx.contains(Regex("\\b${Regex.escape(it)}\\b")) }
    if (hits > 0) { score += minOf(32, hits * 16); reasons += "Model" }
    if (q.cpu.isNotBlank() && tx.contains(Regex("\\b${Regex.escape(q.cpu)}\\b"))) { score += 14; reasons += "CPU" }
    if (q.gpu.isNotBlank() && tx.contains(marketplaceNorm(q.gpu))) { score += 14; reasons += "GPU" }
    if (q.ram.isNotBlank() && tx.contains(Regex("\\b${q.ram}\\s*gb\\b"))) { score += 5; reasons += "RAM" }
    if (q.storage.isNotBlank() && tx.contains(marketplaceNorm(q.storage))) { score += 5; reasons += "Storage" }
    score = score.coerceAtMost(100)
    val accessory = Regex("\\b(cable|adapter|charger|battery|keyboard|mouse|case|cover|stand|webcam|module|ssd|ram kit|memory|motherboard|heatsink|fan|screen|display|replacement|part|parts|bezel|hinge|dock|docking|power supply|psu|box only|empty box|for parts|faulty|repair)\\b").containsMatchIn(tx)
    val modelSatisfied = q.modelTokens.isEmpty() || hits == q.modelTokens.size
    val exact = !brandMismatch && !accessory && q.brand.isNotBlank() && q.brand == t.brand && q.family.isNotBlank() && tx.contains(q.family) && modelSatisfied
    if (brandMismatch) reasons += "Different brand"
    if (accessory) reasons += "Part/accessory"
    if (q.modelTokens.isNotEmpty() && !modelSatisfied) reasons += "Model mismatch"
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
    val connection = (URL(MARKETPLACE_SEARCH_API).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 15_000
        readTimeout = 20_000
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Authorization", "Bearer $token")
    }
    try {
        connection.outputStream.use { it.write(JSONObject().put("query", query.trim()).toString().toByteArray()) }
        val code = connection.responseCode
        val body = (if (code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        val root = JSONObject(body.ifBlank { "{}" })
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
