package com.buysloans.hub

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
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
    val createdAt: String
)

object ValuationHistoryManager {
    private fun userId(token:String):String = runCatching {
        val payload=token.split('.')[1].replace('-','+').replace('_','/')
        val padded=payload+"=".repeat((4-payload.length%4)%4)
        JSONObject(String(Base64.decode(padded,Base64.DEFAULT))).optString("sub")
    }.getOrDefault("")

    private suspend fun request(context: Context, method:String, path:String, body:JSONObject?=null, prefer:String?=null): Pair<Int,String> = withContext(Dispatchers.IO) {
        val token=AuthManager.accessToken(context)
        require(token.isNotBlank()){ "Sign in again to use valuation history." }
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

    suspend fun save(
        context:Context,
        itemType:String,
        itemSummary:String,
        specs:String,
        askingPrice:Double?,
        marketValue:Double?,
        maxBuy:Double?,
        expectedProfit:Double?,
        confidence:String
    ) {
        val token=AuthManager.accessToken(context); val uid=userId(token)
        require(uid.isNotBlank()){ "Your session is invalid. Sign in again." }
        val body=JSONObject().apply {
            put("user_id",uid);put("item_type",itemType);put("item_summary",itemSummary.take(180));put("specs",specs)
            askingPrice?.let{put("asking_price",it)};marketValue?.let{put("market_value",it)};maxBuy?.let{put("max_buy",it)};expectedProfit?.let{put("expected_profit",it)}
            put("confidence",confidence);put("status","quoted")
        }
        val (code,text)=request(context,"POST","valuation_history",body,"return=minimal")
        if(code !in 200..299) throw IllegalStateException("Could not save valuation: $text")
    }

    suspend fun list(context:Context):List<SavedValuation>{
        val (code,text)=request(context,"GET","valuation_history?select=*&order=created_at.desc&limit=100")
        if(code !in 200..299) throw IllegalStateException("Could not load valuation history: $text")
        val a=JSONArray(text)
        return List(a.length()){i-> val j=a.getJSONObject(i); SavedValuation(
            id=j.optString("id"),itemType=j.optString("item_type"),itemSummary=j.optString("item_summary"),specs=j.optString("specs"),
            askingPrice=j.optDoubleOrNull("asking_price"),marketValue=j.optDoubleOrNull("market_value"),maxBuy=j.optDoubleOrNull("max_buy"),expectedProfit=j.optDoubleOrNull("expected_profit"),
            confidence=j.optString("confidence"),status=j.optString("status"),boughtPrice=j.optDoubleOrNull("bought_price"),soldPrice=j.optDoubleOrNull("sold_price"),actualProfit=j.optDoubleOrNull("actual_profit"),notes=j.optString("notes"),createdAt=j.optString("created_at")
        )}
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

private fun JSONObject.optDoubleOrNull(key:String):Double? = if(isNull(key)||!has(key))null else optDouble(key).takeUnless{it.isNaN()}
