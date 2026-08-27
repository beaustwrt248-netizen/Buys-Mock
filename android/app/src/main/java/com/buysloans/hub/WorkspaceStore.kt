package com.buysloans.hub

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class StockItem(
    val id:String,
    val name:String,
    val barcode:String,
    val cost:Double,
    val resale:Double,
    val quantity:Int,
    val createdAt:Long,
    val nfcTagId:String=""
)

data class SaleRecord(
    val id:String,
    val name:String,
    val barcode:String,
    val cost:Double,
    val salePrice:Double,
    val quantity:Int,
    val soldAt:Long
)

object WorkspaceStore {
    private const val PREFS="morley_workspace"
    private const val INVENTORY="inventory_json"
    private const val SALES="sales_json"

    private fun prefs(context:Context)=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)

    fun inventory(context:Context):List<StockItem>{
        val arr=runCatching{JSONArray(prefs(context).getString(INVENTORY,"[]")?:"[]")}.getOrElse{JSONArray()}
        return (0 until arr.length()).mapNotNull{i->arr.optJSONObject(i)?.let{j->
            StockItem(j.optString("id"),j.optString("name"),j.optString("barcode"),j.optDouble("cost"),j.optDouble("resale"),j.optInt("quantity",1),j.optLong("createdAt"),j.optString("nfcTagId"))
        }}
    }

    fun sales(context:Context):List<SaleRecord>{
        val arr=runCatching{JSONArray(prefs(context).getString(SALES,"[]")?:"[]")}.getOrElse{JSONArray()}
        return (0 until arr.length()).mapNotNull{i->arr.optJSONObject(i)?.let{j->
            SaleRecord(j.optString("id"),j.optString("name"),j.optString("barcode"),j.optDouble("cost"),j.optDouble("salePrice"),j.optInt("quantity",1),j.optLong("soldAt"))
        }}
    }

    private fun saveInventory(context:Context,items:List<StockItem>){
        val arr=JSONArray();items.forEach{x->arr.put(JSONObject().apply{put("id",x.id);put("name",x.name);put("barcode",x.barcode);put("cost",x.cost);put("resale",x.resale);put("quantity",x.quantity);put("createdAt",x.createdAt);put("nfcTagId",x.nfcTagId)})}
        prefs(context).edit().putString(INVENTORY,arr.toString()).apply()
    }

    private fun saveSales(context:Context,items:List<SaleRecord>){
        val arr=JSONArray();items.forEach{x->arr.put(JSONObject().apply{put("id",x.id);put("name",x.name);put("barcode",x.barcode);put("cost",x.cost);put("salePrice",x.salePrice);put("quantity",x.quantity);put("soldAt",x.soldAt)})}
        prefs(context).edit().putString(SALES,arr.toString()).apply()
    }

    fun addInventory(context:Context,name:String,barcode:String,cost:Double,resale:Double,quantity:Int){
        require(name.isNotBlank()){ "Enter an item name." }
        require(quantity>0){ "Quantity must be at least 1." }
        val items=inventory(context).toMutableList()
        items.add(0,StockItem(UUID.randomUUID().toString(),name.trim(),barcode.trim(),cost.coerceAtLeast(0.0),resale.coerceAtLeast(0.0),quantity,System.currentTimeMillis()))
        saveInventory(context,items)
    }

    fun deleteInventory(context:Context,id:String){saveInventory(context,inventory(context).filterNot{it.id==id})}

    fun sellOne(context:Context,id:String,salePrice:Double){
        val items=inventory(context).toMutableList();val idx=items.indexOfFirst{it.id==id};if(idx<0)return
        val item=items[idx]
        val sales=sales(context).toMutableList();sales.add(0,SaleRecord(UUID.randomUUID().toString(),item.name,item.barcode,item.cost,salePrice.coerceAtLeast(0.0),1,System.currentTimeMillis()));saveSales(context,sales)
        if(item.quantity<=1)items.removeAt(idx) else items[idx]=item.copy(quantity=item.quantity-1)
        saveInventory(context,items)
    }

    fun findByBarcode(context:Context,barcode:String):StockItem?=inventory(context).firstOrNull{it.barcode.isNotBlank()&&it.barcode.equals(barcode.trim(),true)}

    fun findByNfcTag(context:Context,tagId:String):StockItem?=NfcInventoryLogic.find(inventory(context),tagId)

    fun linkNfcTag(context:Context,itemId:String,tagId:String){saveInventory(context,NfcInventoryLogic.link(inventory(context),itemId,tagId))}

    fun unlinkNfcTag(context:Context,itemId:String){saveInventory(context,NfcInventoryLogic.unlink(inventory(context),itemId))}

    fun deleteSale(context:Context,id:String){saveSales(context,sales(context).filterNot{it.id==id})}

    fun exportJson(context:Context):String=JSONObject().apply{
        put("format","bl-morley-workspace-v1")
        put("exportedAt",System.currentTimeMillis())
        put("inventory",JSONArray(prefs(context).getString(INVENTORY,"[]")?:"[]"))
        put("sales",JSONArray(prefs(context).getString(SALES,"[]")?:"[]"))
    }.toString(2)

    fun importJson(context:Context,text:String){
        val root=JSONObject(text);require(root.optString("format")=="bl-morley-workspace-v1"){"This is not a B&L Morley workspace backup."}
        val inv=root.optJSONArray("inventory")?:JSONArray();val sales=root.optJSONArray("sales")?:JSONArray()
        prefs(context).edit().putString(INVENTORY,inv.toString()).putString(SALES,sales.toString()).apply()
    }

    fun clearWorkspace(context:Context){prefs(context).edit().clear().apply()}
}
