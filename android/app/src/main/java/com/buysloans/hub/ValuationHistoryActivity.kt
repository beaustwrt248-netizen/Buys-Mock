package com.buysloans.hub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ValuationHistoryActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{HistoryScreen{finish()}}}
}

private val HistYellow=Color(0xFFFFD400);private val HistBg=Color(0xFF111111);private val HistCard=Color(0xFF222222)
private fun moneyHist(v:Double?)=if(v==null)"—" else NumberFormat.getCurrencyInstance(Locale("en","AU")).apply{maximumFractionDigits=0}.format(v)

@Composable private fun HistoryScreen(onBack:()->Unit){
    val context=androidx.compose.ui.platform.LocalContext.current
    val scope=rememberCoroutineScope()
    var items by remember{mutableStateOf<List<SavedValuation>>(emptyList())}
    var loading by remember{mutableStateOf(true)}
    var error by remember{mutableStateOf("")}
    var filter by remember{mutableStateOf("all")}
    var showAdd by remember{mutableStateOf(false)}
    fun reload(){scope.launch{loading=true;error="";runCatching{ValuationHistoryManager.list(context)}.onSuccess{items=it}.onFailure{error=it.message?:"Could not load history"};loading=false}}
    LaunchedEffect(Unit){reload()}
    if(showAdd) AddValuationDialog(onDismiss={showAdd=false},onSaved={showAdd=false;reload()})
    MaterialTheme(colorScheme=darkColorScheme(primary=HistYellow,background=HistBg,surface=HistCard)){
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Valuations & Deals",fontSize=26.sp,fontWeight=FontWeight.Black);TextButton(onClick=onBack){Text("Back")}}
            Text("Saved quotes and less-used deal tracking live here so the main calculator stays uncluttered.",color=Color.LightGray)
            Button(onClick={showAdd=true},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=HistYellow,contentColor=Color.Black)){Text("+ Save valuation / deal",fontWeight=FontWeight.Black)}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("all","quoted","bought","sold","passed").forEach{s->FilterChip(selected=filter==s,onClick={filter=s},label={Text(s.replaceFirstChar{it.uppercase()},fontSize=11.sp)})}}
            if(loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if(error.isNotBlank()) Text(error,color=MaterialTheme.colorScheme.error)
            val shown=items.filter{filter=="all"||it.status==filter}
            if(!loading&&shown.isEmpty()) Card(colors=CardDefaults.cardColors(containerColor=HistCard),shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth()){Text("No saved valuations yet.",Modifier.padding(18.dp),color=Color.LightGray)}
            shown.forEach{item->HistoryCard(item,reload)}
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable private fun AddValuationDialog(onDismiss:()->Unit,onSaved:()->Unit){
    val context=androidx.compose.ui.platform.LocalContext.current;val scope=rememberCoroutineScope()
    var type by remember{mutableStateOf("laptop")};var summary by remember{mutableStateOf("")};var specs by remember{mutableStateOf("")};var ask by remember{mutableStateOf("")};var market by remember{mutableStateOf("")};var maxBuy by remember{mutableStateOf("")};var confidence by remember{mutableStateOf("manual")};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Save valuation")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("laptop","desktop","gp","other").forEach{x->FilterChip(selected=type==x,onClick={type=x},label={Text(x.uppercase(),fontSize=10.sp)})}}
        OutlinedTextField(summary,{summary=it},label={Text("Item / model")},singleLine=true)
        OutlinedTextField(specs,{specs=it},label={Text("Specs / notes")},minLines=2)
        OutlinedTextField(ask,{ask=it},label={Text("Seller asking price")},singleLine=true)
        OutlinedTextField(market,{market=it},label={Text("Market value")},singleLine=true)
        OutlinedTextField(maxBuy,{maxBuy=it},label={Text("Max buy")},singleLine=true)
        OutlinedTextField(confidence,{confidence=it},label={Text("Confidence")},singleLine=true)
        if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error,fontSize=12.sp)
        if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
    }},dismissButton={TextButton(onClick=onDismiss,enabled=!busy){Text("Cancel")}},confirmButton={Button(onClick={busy=true;error="";scope.launch{runCatching{val a=ask.toDoubleOrNull();val m=market.toDoubleOrNull();val mx=maxBuy.toDoubleOrNull();ValuationHistoryManager.save(context,type,summary.trim(),specs.trim(),a,m,mx,if(m!=null&&mx!=null)m-mx else null,confidence.trim())}.onSuccess{onSaved()}.onFailure{error=it.message?:"Could not save"};busy=false}},enabled=!busy&&summary.isNotBlank()){Text("Save")}})
}

@Composable private fun HistoryCard(item:SavedValuation,reload:()->Unit){
    val context=androidx.compose.ui.platform.LocalContext.current;val scope=rememberCoroutineScope();var menu by remember{mutableStateOf(false)};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")};var priceAction by remember{mutableStateOf<String?>(null)}
    priceAction?.let{action->PriceDialog(action,item,onDismiss={priceAction=null},onSaved={priceAction=null;reload()})}
    Card(colors=CardDefaults.cardColors(containerColor=HistCard),shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth()){
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text(item.itemSummary,fontSize=18.sp,fontWeight=FontWeight.Black);Text("${item.itemType.uppercase()} • ${item.status.uppercase()}",color=HistYellow,fontSize=11.sp,fontWeight=FontWeight.Bold)};Box{TextButton(onClick={menu=true}){Text("Manage")};DropdownMenu(expanded=menu,onDismissRequest={menu=false}){
                DropdownMenuItem(text={Text("Mark Quoted")},onClick={menu=false;busy=true;scope.launch{runCatching{ValuationHistoryManager.updateStatus(context,item.id,"quoted")}.onFailure{error=it.message?:"Update failed"};busy=false;reload()}})
                DropdownMenuItem(text={Text("Mark Bought…")},onClick={menu=false;priceAction="bought"})
                DropdownMenuItem(text={Text("Mark Sold…")},onClick={menu=false;priceAction="sold"})
                DropdownMenuItem(text={Text("Mark Passed")},onClick={menu=false;busy=true;scope.launch{runCatching{ValuationHistoryManager.updateStatus(context,item.id,"passed")}.onFailure{error=it.message?:"Update failed"};busy=false;reload()}})
                HorizontalDivider();DropdownMenuItem(text={Text("Delete")},onClick={menu=false;busy=true;scope.launch{runCatching{ValuationHistoryManager.delete(context,item.id)}.onFailure{error=it.message?:"Delete failed"};busy=false;reload()}})
            }}}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Metric("Ask",moneyHist(item.askingPrice));Metric("Market",moneyHist(item.marketValue));Metric("Max buy",moneyHist(item.maxBuy))}
            if(item.boughtPrice!=null)Text("Bought: ${moneyHist(item.boughtPrice)}",fontWeight=FontWeight.Bold)
            if(item.soldPrice!=null)Text("Sold: ${moneyHist(item.soldPrice)}",fontWeight=FontWeight.Bold)
            if(item.actualProfit!=null) Text("Actual profit ${moneyHist(item.actualProfit)}",color=if(item.actualProfit>=0)Color(0xFF57E389) else Color(0xFFFF6B6B),fontWeight=FontWeight.Black)
            if(item.expectedProfit!=null)Text("Expected profit ${moneyHist(item.expectedProfit)}",color=Color.LightGray,fontSize=12.sp)
            if(item.confidence.isNotBlank())Text("Confidence: ${item.confidence}",color=Color.LightGray,fontSize=12.sp)
            if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error,fontSize=12.sp)
            if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

@Composable private fun PriceDialog(action:String,item:SavedValuation,onDismiss:()->Unit,onSaved:()->Unit){
    val context=androidx.compose.ui.platform.LocalContext.current;val scope=rememberCoroutineScope();var price by remember{mutableStateOf(if(action=="bought")item.askingPrice?.toString().orEmpty() else "")};var notes by remember{mutableStateOf(item.notes)};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest=onDismiss,title={Text(if(action=="bought")"Mark as bought" else "Mark as sold")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(price,{price=it},label={Text(if(action=="bought")"Actual buy price" else "Actual sold price")},singleLine=true);OutlinedTextField(notes,{notes=it},label={Text("Notes")},minLines=2);if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error);if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())}},dismissButton={TextButton(onClick=onDismiss,enabled=!busy){Text("Cancel")}},confirmButton={Button(onClick={val p=price.toDoubleOrNull();if(p==null){error="Enter a valid price"}else{busy=true;scope.launch{runCatching{ValuationHistoryManager.updateStatus(context,item.id,action,boughtPrice=if(action=="bought")p else null,soldPrice=if(action=="sold")p else null,notes=notes)}.onSuccess{onSaved()}.onFailure{error=it.message?:"Update failed"};busy=false}}}){Text("Save")}})
}

@Composable private fun Metric(label:String,value:String){Column{Text(label,color=Color.Gray,fontSize=10.sp);Text(value,fontWeight=FontWeight.Bold)}}
