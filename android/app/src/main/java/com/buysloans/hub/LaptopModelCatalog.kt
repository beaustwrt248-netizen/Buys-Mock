package com.buysloans.hub

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val MODEL_CATALOG_API = "https://ghdhairijqjqivqriigi.supabase.co/functions/v1/model-catalog-search"

data class LaptopCatalogCandidate(
    val brand: String? = null,
    val family: String? = null,
    val modelName: String? = null,
    val modelNumber: String? = null,
    val yearFrom: Int? = null,
    val yearTo: Int? = null,
    val score: Double = 0.0
) {
    val years: List<Int>
        get() {
            val start = yearFrom ?: return emptyList()
            val end = (yearTo ?: start).coerceAtLeast(start)
            return (start..end).toList()
        }
}

data class LaptopCatalogResolution(
    val originalQuery: String,
    val canonicalQuery: String,
    val brand: String? = null,
    val family: String? = null,
    val modelName: String? = null,
    val modelNumber: String? = null,
    val score: Double = 0.0,
    val candidates: List<LaptopCatalogCandidate> = emptyList()
) {
    val candidateYears: List<Int>
        get() = candidates
            .filter { it.score >= 0.42 }
            .flatMap { it.years }
            .distinct()
            .sorted()

    val requiresYearSelection: Boolean
        get() = candidateYears.size > 1
}

object LaptopModelCatalog {
    internal fun preferredQuery(original: String, resolution: LaptopCatalogResolution): String {
        val clean = original.trim()
        val canonical = resolution.canonicalQuery.trim()
        return if (resolution.score >= 0.42 && canonical.isNotBlank()) canonical else clean
    }

    internal fun queryForYear(resolution: LaptopCatalogResolution, year: Int): String {
        val matching = resolution.candidates
            .filter { year in it.years && it.score >= 0.42 }
            .maxByOrNull { it.score }
        val brand = matching?.brand ?: resolution.brand
        val model = matching?.modelName ?: resolution.modelName ?: resolution.family
        val number = matching?.modelNumber ?: resolution.modelNumber
        val yearToken = year.toString()
        val modelAlreadyIncludesYear = model
            ?.lowercase()
            ?.contains(Regex("\\b${Regex.escape(yearToken)}\\b")) == true
        return listOf(
            brand,
            model,
            yearToken.takeUnless { modelAlreadyIncludesYear },
            number
        )
            .filterNotNull()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" ")
            .ifBlank { resolution.canonicalQuery }
    }

    /**
     * Critical regulatory/model identifiers that must remain resolvable even when the
     * remote catalog is unavailable. Keep this list deliberately small and exact: it is
     * a safety net, not a replacement for the server-side catalog.
     *
     * Ambiguous identifiers must never silently pick one year. The candidate list is
     * deliberately preserved so the UI can require a year/generation choice.
     */
    internal fun knownIdentifierResolution(query: String): LaptopCatalogResolution? {
        val clean = query.trim()
        val compact = clean.lowercase().replace(Regex("[^a-z0-9]"), "")
        return when (compact) {
            "a1932" -> {
                val candidates = listOf(
                    LaptopCatalogCandidate("Apple", "MacBook Air", "MacBook Air 13-inch 2018", "A1932", 2018, 2018, 1.0),
                    LaptopCatalogCandidate("Apple", "MacBook Air", "MacBook Air 13-inch 2019", "A1932", 2019, 2019, 1.0)
                )
                LaptopCatalogResolution(
                    originalQuery = clean,
                    canonicalQuery = "Apple MacBook Air 13-inch A1932",
                    brand = "Apple",
                    family = "MacBook Air",
                    modelName = "MacBook Air 13-inch 2018-2019",
                    modelNumber = "A1932",
                    score = 1.0,
                    candidates = candidates
                )
            }
            else -> null
        }
    }

    private fun parseCandidate(value: JSONObject): LaptopCatalogCandidate = LaptopCatalogCandidate(
        brand = value.optString("brand").takeIf { it.isNotBlank() },
        family = value.optString("family").takeIf { it.isNotBlank() },
        modelName = value.optString("model_name").takeIf { it.isNotBlank() },
        modelNumber = value.optString("model_number").takeIf { it.isNotBlank() },
        yearFrom = value.optInt("year_from", 0).takeIf { it > 0 },
        yearTo = value.optInt("year_to", 0).takeIf { it > 0 },
        score = value.optDouble("similarity_score", 0.0)
    )

    private fun parseCandidates(values: JSONArray?): List<LaptopCatalogCandidate> {
        if (values == null) return emptyList()
        return buildList {
            for (i in 0 until values.length()) {
                values.optJSONObject(i)?.let { add(parseCandidate(it)) }
            }
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
            val remoteCandidates = parseCandidates(root.optJSONArray("matches"))
            val remote = LaptopCatalogResolution(
                originalQuery = clean,
                canonicalQuery = root.optString("canonicalQuery", clean).ifBlank { clean },
                brand = best?.optString("brand")?.takeIf { it.isNotBlank() },
                family = best?.optString("family")?.takeIf { it.isNotBlank() },
                modelName = best?.optString("model_name")?.takeIf { it.isNotBlank() },
                modelNumber = best?.optString("model_number")?.takeIf { it.isNotBlank() },
                score = best?.optDouble("similarity_score", 0.0) ?: 0.0,
                candidates = remoteCandidates
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
