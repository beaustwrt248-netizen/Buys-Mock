package com.buysloans.hub

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

data class LiveDeviceCatalogueRow(
    val id: Long,
    val category: String,
    val brand: String,
    val model: String,
    val modelNumber: String?,
    val storageOptions: List<String>,
    val imageReferenceUrl: String? = null,
)

object LiveDevicePricing {
    private const val PREFS = "morley_live_device_pricing"
    private const val PRICE_CACHE = "prices"
    private const val DEVICE_CACHE = "devices"

    @Volatile
    private var snapshot: List<LiveDevicePrice> = emptyList()

    private var catalogueSnapshot by mutableStateOf<List<LiveDeviceCatalogueRow>>(emptyList())

    fun catalogue(): List<LiveDeviceCatalogueRow> = catalogueSnapshot

    fun device(
        category: String,
        brand: String,
        model: String,
        modelNumber: String? = null,
    ): LiveDeviceCatalogueRow? {
        val normalizedCode = modelNumber?.trim()?.lowercase().orEmpty()
        return catalogueSnapshot.firstOrNull { row ->
            row.category == category &&
                (
                    normalizedCode.isNotBlank() && row.modelNumber?.trim()?.lowercase() == normalizedCode ||
                        row.brand.equals(brand, ignoreCase = true) && row.model.equals(model, ignoreCase = true)
                )
        }
    }

    internal fun replaceSnapshotsForTesting(
        prices: List<LiveDevicePrice>,
        devices: List<LiveDeviceCatalogueRow>,
    ) {
        snapshot = prices
        catalogueSnapshot = devices
    }

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
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val prices = parsePrices(prefs.getString(PRICE_CACHE, "[]").orEmpty())
        val devices = parseDevices(prefs.getString(DEVICE_CACHE, "[]").orEmpty())
        snapshot = prices
        catalogueSnapshot = devices
        return prices
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
                val rawPrices = response.optJSONArray("prices")?.toString() ?: "[]"
                val rawDevices = response.optJSONArray("devices")?.toString() ?: "[]"
                val prices = parsePrices(rawPrices)
                val devices = parseDevices(rawDevices)

                snapshot = prices
                catalogueSnapshot = devices
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(PRICE_CACHE, rawPrices)
                    .putString(DEVICE_CACHE, rawDevices)
                    .apply()

                prices
            } finally {
                connection.disconnect()
            }
        }

    private fun parsePrices(raw: String): List<LiveDevicePrice> =
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

    private fun parseDevices(raw: String): List<LiveDeviceCatalogueRow> =
        runCatching {
            val array = JSONArray(raw.ifBlank { "[]" })
            buildList {
                for (index in 0 until array.length()) {
                    val row = array.getJSONObject(index)
                    val id = row.optLong("id", -1)
                    val category = row.optString("category").trim()
                    val brand = row.optString("brand").trim()
                    val model = row.optString("model_name").trim()
                    val storages = row.optJSONArray("storage_options")?.let { storageArray ->
                        buildList {
                            for (storageIndex in 0 until storageArray.length()) {
                                storageArray.optString(storageIndex).trim()
                                    .takeIf { it.isNotBlank() }
                                    ?.let(::add)
                            }
                        }.distinct()
                    }.orEmpty()

                    if (id > 0 && category.isNotBlank() && brand.isNotBlank() && model.isNotBlank()) {
                        add(
                            LiveDeviceCatalogueRow(
                                id = id,
                                category = category,
                                brand = brand,
                                model = model,
                                modelNumber = row.optString("model_number")
                                    .trim()
                                    .takeIf { it.isNotBlank() },
                                storageOptions = storages,
                                imageReferenceUrl = row.optString("image_reference_url")
                                    .trim()
                                    .takeIf { it.isNotBlank() },
                            ),
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
}
