package com.buysloans.hub

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class LiveDevicePrice(
    val deviceCatalogId: Long,
    val brand: String,
    val model: String,
    val modelNumber: String?,
    val storage: String,
    val priceAud: Double,
    val authoritative: Boolean,
)

object LiveDevicePricing {
    private const val PREFS = "morley_live_device_pricing"
    private const val CACHE = "prices"

    @Volatile
    private var snapshot: List<LiveDevicePrice> = emptyList()

    fun normalizeStorage(value: String): String =
        value.trim().replace(" ", "").uppercase()

    fun find(
        prices: List<LiveDevicePrice>,
        brand: String,
        model: String,
        modelNumber: String?,
        storage: String,
    ): LiveDevicePrice? {
        val normalizedModelNumber = modelNumber?.trim()?.lowercase().orEmpty()
        val normalizedStorage = normalizeStorage(storage)

        return prices.firstOrNull { price ->
            val sameDevice =
                (
                    normalizedModelNumber.isNotBlank() &&
                        price.modelNumber?.trim()?.lowercase() == normalizedModelNumber
                ) || (
                    price.brand.equals(brand, ignoreCase = true) &&
                        price.model.equals(model, ignoreCase = true)
                )

            price.authoritative &&
                normalizeStorage(price.storage) == normalizedStorage &&
                sameDevice
        }
    }

    fun find(
        brand: String,
        model: String,
        modelNumber: String?,
        storage: String,
    ): LiveDevicePrice? = find(snapshot, brand, model, modelNumber, storage)

    fun cached(context: Context): List<LiveDevicePrice> {
        val rows = parse(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(CACHE, "[]")
                .orEmpty(),
        )
        if (rows.isNotEmpty()) snapshot = rows
        return rows
    }

    suspend fun refresh(context: Context): List<LiveDevicePrice> =
        withContext(Dispatchers.IO) {
            val token = AuthManager.validAccessToken(context)
            val connection = (
                URL("${BuildConfig.SUPABASE_URL}/functions/v1/app-pricing-catalogue")
                    .openConnection() as HttpURLConnection
            ).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Cache-Control", "no-cache")
            }

            try {
                if (connection.responseCode !in 200..299) {
                    throw IllegalStateException(
                        "Pricing refresh failed (${connection.responseCode}).",
                    )
                }

                val response = JSONObject(
                    connection.inputStream.bufferedReader().use { it.readText() },
                )
                val raw = response.optJSONArray("prices")?.toString() ?: "[]"
                val prices = parse(raw)

                snapshot = prices
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(CACHE, raw)
                    .apply()

                prices
            } finally {
                connection.disconnect()
            }
        }

    private fun parse(raw: String): List<LiveDevicePrice> =
        runCatching {
            val array = JSONArray(raw.ifBlank { "[]" })
            buildList {
                for (index in 0 until array.length()) {
                    val row = array.getJSONObject(index)
                    if (!row.optBoolean("authoritative", false)) continue

                    val device = row.optJSONObject("device") ?: continue
                    val id = row.optLong("device_catalog_id", -1)
                    val storage = row.optString("storage").trim()
                    val price = row.optDouble("price_aud", Double.NaN)

                    if (id > 0 && storage.isNotBlank() && price.isFinite() && price >= 0) {
                        add(
                            LiveDevicePrice(
                                deviceCatalogId = id,
                                brand = device.optString("brand"),
                                model = device.optString("model_name"),
                                modelNumber = device.optString("model_number")
                                    .takeIf { it.isNotBlank() },
                                storage = storage,
                                priceAud = price,
                                authoritative = true,
                            ),
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
}
