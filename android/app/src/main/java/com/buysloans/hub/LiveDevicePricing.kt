package com.buysloans.hub

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class LiveDevicePrice(val deviceCatalogId: Long, val storage: String, val priceAud: Double, val authoritative: Boolean)

object LiveDevicePricing {
    private const val PREFS = "morley_live_device_pricing"
    private const val CACHE = "prices"
    fun normalizeStorage(value: String): String = value.trim().replace(" ", "").uppercase()
    fun find(prices: List<LiveDevicePrice>, deviceCatalogId: Long, storage: String): LiveDevicePrice? = prices.firstOrNull { it.deviceCatalogId == deviceCatalogId && it.authoritative && normalizeStorage(it.storage) == normalizeStorage(storage) }
    fun cached(context: Context): List<LiveDevicePrice> = parse(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CACHE, "[]").orEmpty())
    suspend fun refresh(context: Context): List<LiveDevicePrice> = withContext(Dispatchers.IO) {
        val token = AuthManager.validAccessToken(context)
        val connection = (URL("${BuildConfig.SUPABASE_URL}/functions/v1/app-pricing-catalogue").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"; connectTimeout = 10_000; readTimeout = 10_000
            setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY); setRequestProperty("Authorization", "Bearer $token"); setRequestProperty("Cache-Control", "no-cache")
        }
        try {
            if (connection.responseCode !in 200..299) throw IllegalStateException("Pricing refresh failed (${connection.responseCode}).")
            val body = JSONObject(connection.inputStream.bufferedReader().use { it.readText() }); val raw = body.optJSONArray("prices")?.toString() ?: "[]"; val prices = parse(raw)
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(CACHE, raw).apply(); prices
        } finally { connection.disconnect() }
    }
    private fun parse(raw: String): List<LiveDevicePrice> = runCatching {
        val array = JSONArray(raw.ifBlank { "[]" }); buildList { for (index in 0 until array.length()) { val row=array.getJSONObject(index); if(!row.optBoolean("authoritative",false)) continue; val id=row.optLong("device_catalog_id",-1L); val storage=row.optString("storage").trim(); val price=row.optDouble("price_aud",Double.NaN); if(id>0&&storage.isNotBlank()&&price.isFinite()&&price>=0)add(LiveDevicePrice(id,storage,price,true)) } }
    }.getOrDefault(emptyList())
}
