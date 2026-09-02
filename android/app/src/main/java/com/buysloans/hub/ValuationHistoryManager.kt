package com.buysloans.hub

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class SavedValuation(
    val id: String,
    val itemType: String,
    val itemSummary: String,
    val specs: String,
    val askingPrice: Double?,
    val marketValue: Double?,
    val maxBuy: Double?,
    val expectedProfit: Double?,
    val confidence: String,
    val status: String,
    val boughtPrice: Double?,
    val soldPrice: Double?,
    val actualProfit: Double?,
    val notes: String,
    val createdAt: String,
    val itemGrade: String? = null
)

private class ValuationHistoryLoadException(message: String) : IllegalStateException(message)

object ValuationHistoryManager {
    private const val CACHE_PREFS = "valuation_history_cache"
    private const val CACHE_JSON = "last_successful_history"
    private const val CACHE_AT = "last_successful_history_at"

    private fun userId(token:String):String = runCatching {
        val payload=token.split('.')[1].replace('-','+').replace('_','/')
        val padded=payload+"=".repeat((4-payload.length%4)%4)
        JSONObject(String(Base64.decode(padded,Base64.DEFAULT))).optString("sub")
    }.getOrDefault("")

    private suspend fun request(context: Context, method:String, path:String, body:JSONObject?=null, prefer:String?=null): Pair<Int,String> {
        val token=AuthManager.validAccessToken(context)
        return withContext(Dispatchers.IO) {
            val c=(URL("${BuildConfig.SUPABASE_URL}/rest/v1/$path").openConnection() as HttpURLConnection).apply {
                requestMethod=method; connectTimeout=10000; readTimeout=10000
                setRequestProperty("apikey",BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                setRequestProperty("Authorization","Bearer $token")
                setRequestProperty("Content-Type","application/json")
                if(prefer!=null)setRequestProperty("Prefer",prefer)
                if(body!=null)doOutput=true
            }
            try {
                if(body!=null)c.outputStream.use{it.write(body.toString().toByteArray())}
                val code=c.responseCode
                val text=(if(code in 200..299)c.inputStream else c.errorStream)?.bufferedReader()?.use{it.readText()}.orEmpty()
                code to text
            } finally { c.disconnect() }
        }
    }

    private fun parseList(text:String):List<SavedValuation> = try {
        val array=JSONArray(text)
        buildList {
            for(i in 0 until array.length()) {
                val value=array.opt(i)
                val item=value as? JSONObject ?: throw ValuationHistoryLoadException("Valuation history data has an invalid format. Try again later.")
                add(SavedValuation(
                    id=item.requiredString("id"),
                    itemType=item.optionalString("item_type"),
                    itemSummary=item.optionalString("item_summary"),
                    specs=item.optionalString("specs"),
                    askingPrice=item.optionalDouble("asking_price"),
                    marketValue=item.optionalDouble("market_value"),
                    maxBuy=item.optionalDouble("max_buy"),
                    expectedProfit=item.optionalDouble("expected_profit"),
                    confidence=item.optionalString("confidence"),
                    status=item.optionalString("status"),
                    boughtPrice=item.optionalDouble("bought_price"),
                    soldPrice=item.optionalDouble("sold_price"),
                    actualProfit=item.optionalDouble("actual_profit"),
                    notes=item.optionalString("notes"),
                    createdAt=item.optionalString("created_at"),
                    itemGrade=item.optionalString("item_grade").takeIf{it in setOf("A","B","C")}
                ))
            }
        }
    } catch(error: ValuationHistoryLoadException) {
        throw error
    } catch(error: Exception) {
        throw ValuationHistoryLoadException("Valuation history data has an invalid format. Try again later.")
    }

    private fun cache(context:Context,text:String){
        context.getSharedPreferences(CACHE_PREFS,Context.MODE_PRIVATE).edit()
            .putString(CACHE_JSON,text)
            .putLong(CACHE_AT,System.currentTimeMillis())
            .apply()
    }

    private fun cachedList(context:Context):List<SavedValuation>?{
        val text=context.getSharedPreferences(CACHE_PREFS,Context.MODE_PRIVATE).getString(CACHE_JSON,null)?.takeIf{it.isNotBlank()} ?: return null
        return runCatching{parseList(text)}.getOrNull()
    }

    fun cachedAt(context:Context):Long = context.getSharedPreferences(CACHE_PREFS,Context.MODE_PRIVATE).getLong(CACHE_AT,0L)

    suspend fun save(
        context:Context,
        itemType:String,
        itemSummary:String,
        specs:String,
        askingPrice:Double?,
        marketValue:Double?,
        maxBuy:Double?,
        expectedProfit:Double?,
        confidence:String,
        itemGrade:String?=null
    ) {
        val token=AuthManager.validAccessToken(context); val uid=userId(token)
        require(uid.isNotBlank()){ "Your session is invalid. Sign in again." }
        val grade=itemGrade?.uppercase()?.takeIf{it in setOf("A","B","C")}
        val body=JSONObject().apply {
            put("user_id",uid);put("item_type",itemType);put("item_summary",itemSummary.take(180));put("specs",specs)
            askingPrice?.let{put("asking_price",it)};marketValue?.let{put("market_value",it)};maxBuy?.let{put("max_buy",it)};expectedProfit?.let{put("expected_profit",it)}
            grade?.let{put("item_grade",it)}
            put("confidence",confidence);put("status","quoted")
        }
        val (code,text)=request(context,"POST","valuation_history",body,"return=minimal")
        if(code !in 200..299) throw IllegalStateException("Could not save valuation: $text")
    }

    suspend fun list(context:Context):List<SavedValuation>{
        return try {
            val (code,text)=request(context,"GET","valuation_history?select=*&order=created_at.desc&limit=100")
            when {
                code in 200..299 -> {
                    val items=parseList(text)
                    cache(context,text)
                    items
                }
                code == 401 -> throw ValuationHistoryLoadException("Your session has expired. Sign in again.")
                code == 403 -> throw ValuationHistoryLoadException("You do not have access to valuation history.")
                code == 429 || code >= 500 -> {
                    cachedList(context) ?: throw ValuationHistoryLoadException("Valuation history is temporarily unavailable. Try again shortly.")
                }
                else -> throw ValuationHistoryLoadException("Valuation history could not be loaded. Try again later.")
            }
        } catch(error: CancellationException) {
            throw error
        } catch(error: IOException) {
            cachedList(context) ?: throw ValuationHistoryLoadException("Unable to reach valuation history. Check your connection and try again.")
        } catch(error: ValuationHistoryLoadException) {
            throw error
        } catch(error: Exception) {
            throw ValuationHistoryLoadException("Valuation history could not be loaded. Try again later.")
        }
    }

    suspend fun updateStatus(context:Context,id:String,status:String,boughtPrice:Double?=null,soldPrice:Double?=null,notes:String?=null){
        val body=JSONObject().apply{put("status",status);put("updated_at",java.time.Instant.now().toString());boughtPrice?.let{put("bought_price",it)};soldPrice?.let{put("sold_price",it)};notes?.let{put("notes",it)}}
        val safe=URLEncoder.encode(id,"UTF-8")
        val (code,text)=request(context,"PATCH","valuation_history?id=eq.$safe",body,"return=minimal")
        if(code !in 200..299)throw IllegalStateException("Could not update deal: $text")
    }

    suspend fun delete(context:Context,id:String){
        val safe=URLEncoder.encode(id,"UTF-8");val (code,text)=request(context,"DELETE","valuation_history?id=eq.$safe")
        if(code !in 200..299)throw IllegalStateException("Could not delete valuation: $text")
    }
}

private fun JSONObject.requiredString(key:String):String {
    val value=optionalString(key)
    if(value.isBlank()) throw ValuationHistoryLoadException("Valuation history data has an invalid format. Try again later.")
    return value
}

private fun JSONObject.optionalString(key:String):String {
    if(!has(key)||isNull(key)) return ""
    return opt(key) as? String ?: throw ValuationHistoryLoadException("Valuation history data has an invalid format. Try again later.")
}

private fun JSONObject.optionalDouble(key:String):Double? {
    if(!has(key)||isNull(key)) return null
    val value=when(val raw=opt(key)) {
        is Number -> raw.toDouble()
        is String -> raw.toDoubleOrNull()
        else -> null
    } ?: throw ValuationHistoryLoadException("Valuation history data has an invalid format. Try again later.")
    if(value.isNaN()||value.isInfinite()) throw ValuationHistoryLoadException("Valuation history data has an invalid format. Try again later.")
    return value
}
