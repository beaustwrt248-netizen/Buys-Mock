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

    suspend fun resolve(context: Context, query: String): LaptopCatalogResolution = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.isBlank()) return@withContext LaptopCatalogResolution(query, query)
        val token = AuthManager.validAccessToken(context)
        if (token.isBlank()) return@withContext LaptopCatalogResolution(query, query)
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
            if (connection.responseCode !in 200..299) return@withContext LaptopCatalogResolution(query, query)
            val root = runCatching {
                JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            }.getOrNull() ?: return@withContext LaptopCatalogResolution(query, query)
            if (!root.optBoolean("success")) return@withContext LaptopCatalogResolution(query, query)
            val best = root.optJSONObject("best")
            LaptopCatalogResolution(
                originalQuery = clean,
                canonicalQuery = root.optString("canonicalQuery", clean).ifBlank { clean },
                brand = best?.optString("brand")?.takeIf { it.isNotBlank() },
                family = best?.optString("family")?.takeIf { it.isNotBlank() },
                modelName = best?.optString("model_name")?.takeIf { it.isNotBlank() },
                modelNumber = best?.optString("model_number")?.takeIf { it.isNotBlank() },
                score = best?.optDouble("similarity_score", 0.0) ?: 0.0
            )
        } catch (_: Exception) {
            LaptopCatalogResolution(query, query)
        } finally {
            connection.disconnect()
        }
    }
}
