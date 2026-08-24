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
    fun reload(){scope.launch{loading=true;error="";runCatching{ValuationHistoryManager.list(context)}.onSuccess{items=it}.onFailure{error=it.message?:"Could not load history"};loading=false}}
    LaunchedEffect(Unit){reload()}
    MaterialTheme(colorScheme=darkColorScheme(primary=HistYellow,background=HistBg,surface=HistCard)){
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Valuations & Deals",fontSize=26.sp,fontWeight=FontWeight.Black);TextButton(onClick=onBack){Text("Back")}}
            Text("Saved quotes and less-used deal tracking live here so the main calculator stays uncluttered.",color=Color.LightGray)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("all","quoted","bought","sold","passed").forEach{s->FilterChip(selected=filter==s,onClick={filter=s},label={Text(s.replaceFirstChar{it.uppercase()})})}}
            if(loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if(error.isNotBlank()) Text(error,color=MaterialTheme.colorScheme.error)
            val shown=items.filter{filter=="all"||it.status==filter}
            if(!loading&&shown.isEmpty()) Card(colors=CardDefaults.cardColors(containerColor=HistCard),shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth()){Text("No saved valuations yet.",Modifier.padding(18.dp),color=Color.LightGray)}
            shown.forEach{item->HistoryCard(item,reload)}
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable private fun HistoryCard(item:SavedValuation,reload:()->Unit){
    val context=androidx.compose.ui.platform.LocalContext.current;val scope=rememberCoroutineScope();var menu by remember{mutableStateOf(false)};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")}
    Card(colors=CardDefaults.cardColors(containerColor=HistCard),shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth()){
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text(item.itemSummary,fontSize=18.sp,fontWeight=FontWeight.Black);Text("${item.itemType.uppercase()} • ${item.status.uppercase()}",color=HistYellow,fontSize=11.sp,fontWeight=FontWeight.Bold)};Box{TextButton(onClick={menu=true}){Text("Manage")};DropdownMenu(expanded=menu,onDismissRequest={menu=false}){listOf("quoted","bought","sold","passed").forEach{s->DropdownMenuItem(text={Text("Mark ${s.replaceFirstChar{it.uppercase()}}")},onClick={menu=false;busy=true;scope.launch{runCatching{ValuationHistoryManager.updateStatus(context,item.id,s,boughtPrice=if(s=="bought")item.askingPrice else null,soldPrice=if(s=="sold")item.marketValue else null)}.onFailure{error=it.message?:"Update failed"};busy=false;reload()}})};DropdownMenuItem(text={Text("Delete")},onClick={menu=false;busy=true;scope.launch{runCatching{ValuationHistoryManager.delete(context,item.id)}.onFailure{error=it.message?:"Delete failed"};busy=false;reload()}})}}}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Metric("Ask",moneyHist(item.askingPrice));Metric("Market",moneyHist(item.marketValue));Metric("Max buy",moneyHist(item.maxBuy))}
            if(item.status=="sold"||item.actualProfit!=null) Text("Actual profit ${moneyHist(item.actualProfit)}",color=Color(0xFF57E389),fontWeight=FontWeight.Bold)
            if(item.confidence.isNotBlank())Text("Confidence: ${item.confidence}",color=Color.LightGray,fontSize=12.sp)
            if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error,fontSize=12.sp)
            if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}
@Composable private fun Metric(label:String,value:String){Column{Text(label,color=Color.Gray,fontSize=10.sp);Text(value,fontWeight=FontWeight.Bold)}}
