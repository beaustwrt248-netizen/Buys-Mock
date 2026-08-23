package com.buysloans.hub

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

private const val API = "https://ghdhairijqjqivqriigi.supabase.co/functions/v1/ebay-search"
private val CashYellow = Color(0xFFFFD400)
private val CashMaroon = Color(0xFF9E1738)
private val Bg = Color(0xFF111111)
private val Card = Color(0xFF222222)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Bg.toArgbCompat()
        window.navigationBarColor = Bg.toArgbCompat()
        setContent { BLMorleyApp() }
    }
}

private fun Color.toArgbCompat(): Int = android.graphics.Color.argb((alpha*255).toInt(), (red*255).toInt(), (green*255).toInt(), (blue*255).toInt())

enum class Page(val label:String,val icon:String){Home("Home","⌂"),Laptop("Laptop","💻"),Desktop("Desktop","🖥"),GP("GP","$"),More("More","⚙")}

data class MarketResult(val newPrice:Double,val usedPrice:Double,val googleCount:Int,val ebayCount:Int,val estimated:Boolean)

@Composable
fun BLMorleyApp(){
    var page by remember{ mutableStateOf(Page.Home) }
    MaterialTheme(colorScheme = darkColorScheme(primary=CashYellow, secondary=CashMaroon, background=Bg, surface=Card)){
        Scaffold(containerColor=Bg,bottomBar={
            NavigationBar(containerColor=Color(0xFF101010)){
                Page.entries.forEach{ p->
                    NavigationBarItem(selected=page==p,onClick={page=p},icon={Text(p.icon,fontSize=20.sp)},label={Text(p.label)},colors=NavigationBarItemDefaults.colors(indicatorColor=CashYellow.copy(alpha=.25f),selectedIconColor=CashYellow,selectedTextColor=CashYellow))
                }
            }
        }){ pad->
            Box(Modifier.padding(pad).fillMaxSize()){
                when(page){
                    Page.Home->HomeScreen(onLaptop={page=Page.Laptop},onDesktop={page=Page.Desktop},onGP={page=Page.GP})
                    Page.Laptop->LaptopScreen()
                    Page.Desktop->DesktopScreen()
                    Page.GP->GpScreen()
                    Page.More->MoreScreen()
                }
            }
        }
    }
}

@Composable
fun Screen(title:String,content:@Composable ColumnScope.()->Unit){
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        Text("B&L Morley",fontWeight=FontWeight.Black,fontSize=28.sp,color=Color.White)
        Text(title,fontWeight=FontWeight.Bold,fontSize=24.sp,color=CashYellow)
        content()
        Spacer(Modifier.height(12.dp))
    }
}

@Composable fun HomeScreen(onLaptop:()->Unit,onDesktop:()->Unit,onGP:()->Unit)=Screen("Native valuation workspace"){
    CardBlock("WELCOME","Live valuation and buying targets now running as a native Android interface — no WebView.")
    NativeButton("💻 Laptop / MacBook",onLaptop)
    NativeButton("🖥 Desktop / Gaming PC",onDesktop)
    NativeButton("$ GP Calculator",onGP)
    CardBlock("ONLINE PRICING","Native screens connect directly to the existing live pricing API and keep the same backend filtering rules.")
}

@Composable
fun LaptopScreen()=Screen("💻 Laptop / MacBook"){
    var model by remember{ mutableStateOf("") }; var ask by remember{ mutableStateOf("") }; var busy by remember{ mutableStateOf(false) }; var status by remember{ mutableStateOf("Ready") }; var result by remember{ mutableStateOf<MarketResult?>(null) }; val scope=rememberCoroutineScope()
    NativeField("Exact model / clean search",model,{model=it})
    NativeField("Seller asking price",ask,{ask=it},single=true)
    NativeButton(if(busy)"Searching…" else "Analyse Laptop",enabled=!busy&&model.isNotBlank()){
        busy=true; status="Searching live market…"; scope.launch{ runCatching{ market(model,40,"device") }.onSuccess{ result=it; status="${it.googleCount} Google new offers • ${it.ebayCount} eBay used matches" }.onFailure{ status=it.message?:"Search failed" };busy=false }
    }
    Text(status,color=Color.LightGray)
    result?.let{ r->
        val used=if(r.usedPrice>0)r.usedPrice else r.newPrice*.58; val max=used*.7; val a=ask.toDoubleOrNull()?:0.0; val verdict=if(a<=0)"PRICE READY" else if(a<=max)"BUY" else if(a<=max*1.1)"NEGOTIATE" else "PASS"
        MetricGrid(listOf("NEW RETAIL" to r.newPrice,"USED VALUE" to used,"MAX BUY" to max))
        Verdict(verdict)
        CardBlock(if(r.estimated)"ESTIMATED USED VALUE" else "DIRECT MARKET EVIDENCE","Used value based on ${r.ebayCount} eBay used matches and ${r.googleCount} Google new offers.")
    }
}

@Composable
fun DesktopScreen()=Screen("🖥 Desktop / Gaming PC"){
    var specs by remember{ mutableStateOf("") }; var ask by remember{ mutableStateOf("") }; var margin by remember{ mutableStateOf("30") }; var busy by remember{ mutableStateOf(false) }; var status by remember{ mutableStateOf("Paste full OEM model/specs") }; var result by remember{ mutableStateOf<MarketResult?>(null) }; val scope=rememberCoroutineScope()
    NativeField("Seller asking price",ask,{ask=it},single=true)
    NativeField("Target gross profit %",margin,{margin=it},single=true)
    NativeField("Paste PC specs",specs,{specs=it})
    NativeButton(if(busy)"Searching…" else "Detect + Price All",enabled=!busy&&specs.isNotBlank()){
        busy=true;status="Searching exact whole-system market…";scope.launch{runCatching{market(specs,40,"device")}.onSuccess{result=it;status="OEM whole-device valuation • ${it.googleCount} Google new • ${it.ebayCount} eBay used"}.onFailure{status=it.message?:"Search failed"};busy=false}
    }
    Text(status,color=Color.LightGray)
    result?.let{r-> val used=if(r.usedPrice>0)r.usedPrice else r.newPrice*.65;val gp=(margin.toDoubleOrNull()?:30.0)/100.0;val max=used*(1-gp);val a=ask.toDoubleOrNull()?:0.0;val verdict=if(a<=0)"PRICE READY" else if(a<=max)"BUY" else if(a<=max*1.1)"NEGOTIATE" else "PASS";MetricGrid(listOf("NEW PARTS" to r.newPrice,"USED VALUE" to used,"MAX BUY" to max,"AVERAGE RESULT" to used));Verdict(verdict);CardBlock(if(r.ebayCount==0)"ESTIMATED USED VALUE" else "VALUATION EVIDENCE","Whole-device filtering remains on the server so accessories, parts and unrelated keyword collisions stay excluded.")}
}

@Composable
fun GpScreen()=Screen("$ GP Calculator"){
    var sale by remember{ mutableStateOf("") };var gp by remember{ mutableStateOf("30") };val s=sale.toDoubleOrNull()?:0.0;val p=gp.toDoubleOrNull()?:30.0;val cost=s*(1-p/100);val profit=s-cost
    NativeField("Sale price",sale,{sale=it},single=true);NativeField("Target GP %",gp,{gp=it},single=true);MetricGrid(listOf("SALE PRICE" to s,"MAX COST" to cost,"TARGET PROFIT" to profit));CardBlock("MARGIN","${p.toInt()}% gross profit target")
}

@Composable fun MoreScreen()=Screen("⚙ More"){
    CardBlock("B&L MORLEY","Native Android beta 2.0.0")
    CardBlock("STATUS","No WebView. Native Compose UI with direct live-pricing API access.")
    CardBlock("FALLBACK","The previous v1.0.1 WebView APK remains available until this native build finishes validation.")
}

@Composable fun NativeField(label:String,value:String,onChange:(String)->Unit,single:Boolean=false){OutlinedTextField(value=value,onValueChange=onChange,label={Text(label)},modifier=Modifier.fillMaxWidth(),singleLine=single,colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=CashYellow,focusedLabelColor=CashYellow))}
@Composable fun NativeButton(text:String,onClick:()->Unit,enabled:Boolean=true){Button(onClick=onClick,enabled=enabled,modifier=Modifier.fillMaxWidth().height(56.dp),colors=ButtonDefaults.buttonColors(containerColor=CashYellow,contentColor=Color.Black),shape=RoundedCornerShape(16.dp)){Text(text,fontWeight=FontWeight.Black,fontSize=17.sp)}}
@Composable fun CardBlock(title:String,body:String){Card(colors=CardDefaults.cardColors(containerColor=Card),shape=RoundedCornerShape(22.dp),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(title,color=CashYellow,fontWeight=FontWeight.Black);Text(body,color=Color(0xFFD0D0D0),lineHeight=22.sp)}}}
@Composable fun Verdict(v:String){Card(colors=CardDefaults.cardColors(containerColor=Card),modifier=Modifier.fillMaxWidth()){Text(v,Modifier.padding(18.dp),color=when(v){"BUY"->Color(0xFF57E389);"PASS"->Color(0xFFFF6B6B);else->CashYellow},fontSize=25.sp,fontWeight=FontWeight.Black)}}
@Composable fun MetricGrid(items:List<Pair<String,Double>>){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){items.chunked(2).forEach{row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){row.forEach{(k,v)->Card(colors=CardDefaults.cardColors(containerColor=Card),modifier=Modifier.weight(1f)){Column(Modifier.padding(16.dp)){Text(k,color=Color.Gray,fontSize=12.sp,fontWeight=FontWeight.Bold);Text(money(v),color=Color.White,fontSize=25.sp,fontWeight=FontWeight.Black)}}};if(row.size==1)Spacer(Modifier.weight(1f))}}}}

private fun money(v:Double):String=NumberFormat.getCurrencyInstance(Locale("en","AU")).apply{maximumFractionDigits=0}.format(v)

private suspend fun market(query:String,limit:Int,mode:String):MarketResult=withContext(Dispatchers.IO){
    val conn=(URL(API).openConnection() as HttpURLConnection).apply{requestMethod="POST";connectTimeout=15000;readTimeout=20000;doOutput=true;setRequestProperty("Content-Type","application/json")}
    val body=JSONObject().put("query",query).put("limit",limit).put("australiaOnly",true).put("mode",mode).toString();conn.outputStream.use{it.write(body.toByteArray())}
    val code=conn.responseCode;val text=(if(code in 200..299)conn.inputStream else conn.errorStream).bufferedReader().use{it.readText()};val root=JSONObject(text);if(code !in 200..299||!root.optBoolean("success"))throw IllegalStateException(root.optString("error","HTTP $code"))
    fun price(o:JSONObject?,vararg keys:String):Double{if(o==null)return 0.0;for(k in keys){val v=o.optDouble(k,0.0);if(v>0)return v};return 0.0}
    val g=root.optJSONObject("google");val e=root.optJSONObject("ebay");val gp=g?.optJSONObject("pricing");val ep=e?.optJSONObject("pricing");MarketResult(price(gp,"competitiveLow","typicalNew"),price(ep,"typicalUsed"),g?.optInt("analysedListings",0)?:0,e?.optInt("analysedListings",0)?:0,ep?.optBoolean("estimatedFromNew",false)?:false)
}
