package com.buysloans.hub

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class LiveDevicePrice(val deviceCatalogId: Long, val brand: String, val model: String, val modelNumber: String?, val storage: String, val priceAud: Double, val authoritative: Boolean)

object LiveDevicePricing {
    private const val PREFS="morley_live_device_pricing"; private const val CACHE="prices"
    fun normalizeStorage(value:String)=value.trim().replace(" ","").uppercase()
    fun find(prices:List<LiveDevicePrice>,brand:String,model:String,modelNumber:String?,storage:String):LiveDevicePrice? {
        val normalizedModelNumber=modelNumber?.trim()?.lowercase().orEmpty()
        return prices.firstOrNull { p -> p.authoritative && normalizeStorage(p.storage)==normalizeStorage(storage) && ((normalizedModelNumber.isNotBlank() && p.modelNumber?.trim()?.lowercase()==normalizedModelNumber) || (p.brand.equals(brand,true)&&p.model.equals(model,true))) }
    }
    fun cached(context:Context)=parse(context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(CACHE,"[]").orEmpty())
    suspend fun refresh(context:Context):List<LiveDevicePrice> = withContext(Dispatchers.IO) {
        val token=AuthManager.validAccessToken(context); val c=(URL("${BuildConfig.SUPABASE_URL}/functions/v1/app-pricing-catalogue").openConnection() as HttpURLConnection).apply { requestMethod="GET";connectTimeout=10_000;readTimeout=10_000;setRequestProperty("apikey",BuildConfig.SUPABASE_PUBLISHABLE_KEY);setRequestProperty("Authorization","Bearer $token");setRequestProperty("Cache-Control","no-cache") }
        try { if(c.responseCode !in 200..299)throw IllegalStateException("Pricing refresh failed (${c.responseCode})."); val raw=JSONObject(c.inputStream.bufferedReader().use{it.readText()}).optJSONArray("prices")?.toString()?:"[]"; val prices=parse(raw); context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(CACHE,raw).apply(); prices } finally { c.disconnect() }
    }
    private fun parse(raw:String):List<LiveDevicePrice> = runCatching { val a=JSONArray(raw.ifBlank{"[]"}); buildList { for(i in 0 until a.length()){ val r=a.getJSONObject(i); if(!r.optBoolean("authoritative",false))continue; val d=r.optJSONObject("device")?:continue; val id=r.optLong("device_catalog_id",-1);val storage=r.optString("storage").trim();val price=r.optDouble("price_aud",Double.NaN); if(id>0&&storage.isNotBlank()&&price.isFinite()&&price>=0)add(LiveDevicePrice(id,d.optString("brand"),d.optString("model_name"),d.optString("model_number").takeIf{it.isNotBlank()},storage,price,true)) } } }.getOrDefault(emptyList())
}
