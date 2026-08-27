package com.buysloans.hub

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val MODEL_CATALOG_API = "https://ghdhairijqjqivqriigi.supabase.co/functions/v1/model-catalog-search"

data class LaptopCatalogResolution(
    val originalQuery: String,
    val canonicalQuery: String,
    val brand: String? = null,
    val family: String? = null,
    val modelName: String? = null,
    val modelNumber: String? = null,
    val score: Double = 0.0
)

object LaptopModelCatalog {
    internal fun preferredQuery(original: String, resolution: LaptopCatalogResolution): String {
        val clean = original.trim()
        val canonical = resolution.canonicalQuery.trim()
        return if (resolution.score >= 0.42 && canonical.isNotBlank()) canonical else clean
    }

    /**
     * Critical regulatory/model identifiers that must remain resolvable even when the
     * remote catalog is unavailable. Keep this list deliberately small and exact: it is
     * a safety net, not a replacement for the server-side catalog.
     */
    internal fun knownIdentifierResolution(query: String): LaptopCatalogResolution? {
        val clean = query.trim()
        val compact = clean.lowercase().replace(Regex("[^a-z0-9]"), "")
        return when (compact) {
            "a1932" -> LaptopCatalogResolution(
                originalQuery = clean,
                canonicalQuery = "Apple MacBook Air 13-inch 2018 A1932",
                brand = "Apple",
                family = "MacBook Air",
                modelName = "MacBook Air 13-inch 2018",
                modelNumber = "A1932",
                score = 1.0
            )
            else -> null
        }
    }

    suspend fun resolve(context: Context, query: String): LaptopCatalogResolution = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.isBlank()) return@withContext LaptopCatalogResolution(query, query)

        val localFallback = knownIdentifierResolution(clean)
        val token = AuthManager.validAccessToken(context)
        if (token.isBlank()) return@withContext localFallback ?: LaptopCatalogResolution(query, query)

        val connection = (URL(MODEL_CATALOG_API).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 8_000
            readTimeout = 10_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            setRequestProperty("Authorization", "Bearer $token")
        }
        try {
            connection.outputStream.use {
                it.write(JSONObject().put("query", clean).toString().toByteArray())
            }
            if (connection.responseCode !in 200..299) {
                return@withContext localFallback ?: LaptopCatalogResolution(query, query)
            }
            val root = runCatching {
                JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            }.getOrNull() ?: return@withContext localFallback ?: LaptopCatalogResolution(query, query)
            if (!root.optBoolean("success")) {
                return@withContext localFallback ?: LaptopCatalogResolution(query, query)
            }
            val best = root.optJSONObject("best")
            val remote = LaptopCatalogResolution(
                originalQuery = clean,
                canonicalQuery = root.optString("canonicalQuery", clean).ifBlank { clean },
                brand = best?.optString("brand")?.takeIf { it.isNotBlank() },
                family = best?.optString("family")?.takeIf { it.isNotBlank() },
                modelName = best?.optString("model_name")?.takeIf { it.isNotBlank() },
                modelNumber = best?.optString("model_number")?.takeIf { it.isNotBlank() },
                score = best?.optDouble("similarity_score", 0.0) ?: 0.0
            )
            if (remote.score >= 0.42 && remote.canonicalQuery.isNotBlank()) remote
            else localFallback ?: remote
        } catch (_: Exception) {
            localFallback ?: LaptopCatalogResolution(query, query)
        } finally {
            connection.disconnect()
        }
    }
}
