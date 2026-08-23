package com.buysloans.hub

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
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

data class Listing(
    val title:String,
    val price:Double,
    val source:String,
    val url:String,
    val condition:String,
    val matchScore:Int,
    val matchLabel:String,
    val matchReasons:String
)

data class MarketResult(
    val newPrice:Double,
    val usedPrice:Double,
    val googleCount:Int,
    val ebayCount:Int,
    val estimated:Boolean,
    val googleItems:List<Listing>,
    val ebayItems:List<Listing>
)

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
    NativeButton(if(busy)"Searching…" else "Analyse Laptop",onClick={
        busy=true; status="Searching live market…"; scope.launch{ runCatching{ market(model,40,"device") }.onSuccess{ result=it; status="${it.googleCount} Google new offers • ${it.ebayCount} eBay used matches" }.onFailure{ status=it.message?:"Search failed" };busy=false }
    },enabled=!busy&&model.isNotBlank())
    Text(status,color=Color.LightGray)
    result?.let{ r->
        val used=if(r.usedPrice>0)r.usedPrice else r.newPrice*.58; val max=used*.7; val a=ask.toDoubleOrNull()?:0.0; val verdict=if(a<=0)"PRICE READY" else if(a<=max)"BUY" else if(a<=max*1.1)"NEGOTIATE" else "PASS"
        MetricGrid(listOf("NEW RETAIL" to r.newPrice,"USED VALUE" to used,"MAX BUY" to max))
        Verdict(verdict)
        EvidenceSection(r, if(r.ebayCount==0) "Estimated used value (${(58).toInt()}% of verified new retail; no exact used comps)" else "Exact/filtered used comparables")
    }
}

@Composable
fun DesktopScreen()=Screen("🖥 Desktop / Gaming PC"){
    var specs by remember{ mutableStateOf("") }; var ask by remember{ mutableStateOf("") }; var margin by remember{ mutableStateOf("30") }; var busy by remember{ mutableStateOf(false) }; var status by remember{ mutableStateOf("Paste full OEM model/specs") }; var result by remember{ mutableStateOf<MarketResult?>(null) }; val scope=rememberCoroutineScope()
    NativeField("Seller asking price",ask,{ask=it},single=true)
    NativeField("Target gross profit %",margin,{margin=it},single=true)
    NativeField("Paste PC specs",specs,{specs=it})
    NativeButton(if(busy)"Searching…" else "Detect + Price All",onClick={
        busy=true;status="Searching exact whole-system market…";scope.launch{runCatching{market(specs,40,"device")}.onSuccess{result=it;status="OEM whole-device valuation • ${it.googleCount} Google new • ${it.ebayCount} eBay used"}.onFailure{status=it.message?:"Search failed"};busy=false}
    },enabled=!busy&&specs.isNotBlank())
    Text(status,color=Color.LightGray)
    result?.let{r->
        val used=if(r.usedPrice>0)r.usedPrice else r.newPrice*.65
        val gp=(margin.toDoubleOrNull()?:30.0)/100.0
        val max=used*(1-gp)
        val a=ask.toDoubleOrNull()?:0.0
        val verdict=if(a<=0)"PRICE READY" else if(a<=max)"BUY" else if(a<=max*1.1)"NEGOTIATE" else "PASS"
        MetricGrid(listOf("NEW PARTS" to r.newPrice,"USED VALUE" to used,"MAX BUY" to max,"AVERAGE RESULT" to used))
        Verdict(verdict)
        EvidenceSection(r, if(r.ebayCount==0) "Estimated used value (65% of verified new retail; no exact used comps)" else "Exact/filtered used comparables")
    }
}

@Composable
fun EvidenceSection(r:MarketResult,usedBasis:String){
    CardBlock(
        if(r.ebayCount==0)"ESTIMATED USED VALUE" else "VALUATION EVIDENCE",
        "${r.googleCount} Google new offers • ${r.ebayCount} eBay used matches\nUsed basis: $usedBasis\nRetail basis: Google Shopping filtered whole-device offers"
    )
    Text("eBay AU used comparables",color=Color.White,fontSize=22.sp,fontWeight=FontWeight.Black)
    if(r.ebayItems.isEmpty()) Text("No reliable exact-model results found.",color=Color.LightGray)
    else r.ebayItems.take(12).forEach{ ListingCard(it) }
    Spacer(Modifier.height(4.dp))
    Text("Google Shopping AU new offers",color=Color.White,fontSize=22.sp,fontWeight=FontWeight.Black)
    if(r.googleItems.isEmpty()) Text("No reliable filtered new offers found.",color=Color.LightGray)
    else r.googleItems.take(12).forEach{ ListingCard(it) }
}

@Composable
fun ListingCard(item:Listing){
    val context= LocalContext.current
    Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF1B1B1B)),shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth()){
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Text(money(item.price),color=Color.White,fontSize=21.sp,fontWeight=FontWeight.Black)
                if(item.url.isNotBlank()) TextButton(onClick={ runCatching{context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))} }){Text("Open")}
            }
            Text(item.title,color=Color(0xFFE2E2E2),fontSize=16.sp,lineHeight=21.sp)
            if(item.source.isNotBlank()) Text(item.source,color=Color.Gray,fontSize=14.sp)
            if(item.condition.isNotBlank()) Text(item.condition,color=Color.LightGray,fontSize=13.sp)
            if(item.matchScore>0 || item.matchLabel.isNotBlank()){
                val badge=listOfNotNull(
                    item.matchScore.takeIf{it>0}?.let{"$it%"},
                    item.matchLabel.takeIf{it.isNotBlank()},
                    item.matchReasons.takeIf{it.isNotBlank()}
                ).joinToString(" · ")
                Surface(color=Color(0xFF171717),shape=RoundedCornerShape(999.dp),border=androidx.compose.foundation.BorderStroke(1.dp,Color(0xFF555555))){
                    Text(badge,Modifier.padding(horizontal=10.dp,vertical=6.dp),color=if(item.matchScore>=72)Color(0xFF72E79B) else CashYellow,fontSize=12.sp,fontWeight=FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GpScreen()=Screen("$ GP Calculator"){
    var sale by remember{ mutableStateOf("") };var gp by remember{ mutableStateOf("30") };val s=sale.toDoubleOrNull()?:0.0;val p=gp.toDoubleOrNull()?:30.0;val cost=s*(1-p/100);val profit=s-cost
    NativeField("Sale price",sale,{sale=it},single=true);NativeField("Target GP %",gp,{gp=it},single=true);MetricGrid(listOf("SALE PRICE" to s,"MAX COST" to cost,"TARGET PROFIT" to profit));CardBlock("MARGIN","${p.toInt()}% gross profit target")
}

@Composable fun MoreScreen()=Screen("⚙ More"){
    CardBlock("B&L MORLEY","Native Android beta 2.0.1")
    CardBlock("STATUS","No WebView. Native Compose UI with direct live-pricing API access and native valuation evidence.")
    CardBlock("FALLBACK","The previous v1.0.1 WebView APK remains available until this native build finishes validation.")
}

@Composable fun NativeField(label:String,value:String,onChange:(String)->Unit,single:Boolean=false){OutlinedTextField(value=value,onValueChange=onChange,label={Text(label)},modifier=Modifier.fillMaxWidth(),singleLine=single,colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=CashYellow,focusedLabelColor=CashYellow))}
@Composable fun NativeButton(text:String,onClick:()->Unit,enabled:Boolean=true){Button(onClick=onClick,enabled=enabled,modifier=Modifier.fillMaxWidth().height(56.dp),colors=ButtonDefaults.buttonColors(containerColor=CashYellow,contentColor=Color.Black),shape=RoundedCornerShape(16.dp)){Text(text,fontWeight=FontWeight.Black,fontSize=17.sp)}}
@Composable fun CardBlock(title:String,body:String){Card(colors=CardDefaults.cardColors(containerColor=Card),shape=RoundedCornerShape(22.dp),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(title,color=CashYellow,fontWeight=FontWeight.Black);Text(body,color=Color(0xFFD0D0D0),lineHeight=22.sp)}}}
@Composable fun Verdict(v:String){Card(colors=CardDefaults.cardColors(containerColor=Card),modifier=Modifier.fillMaxWidth()){Text(v,Modifier.padding(18.dp),color=when(v){"BUY"->Color(0xFF57E389);"PASS"->Color(0xFFFF6B6B);else->CashYellow},fontSize=25.sp,fontWeight=FontWeight.Black)}}
@Composable fun MetricGrid(items:List<Pair<String,Double>>){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){items.chunked(2).forEach{row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){row.forEach{(k,v)->Card(colors=CardDefaults.cardColors(containerColor=Card),modifier=Modifier.weight(1f)){Column(Modifier.padding(16.dp)){Text(k,color=Color.Gray,fontSize=12.sp,fontWeight=FontWeight.Bold);Text(money(v),color=Color.White,fontSize=25.sp,fontWeight=FontWeight.Black)}}};if(row.size==1)Spacer(Modifier.weight(1f))}}}}

private fun money(v:Double):String=NumberFormat.getCurrencyInstance(Locale("en","AU")).apply{maximumFractionDigits=0}.format(v)

private fun parseListings(obj:JSONObject?):List<Listing>{
    if(obj==null)return emptyList()
    val arr=obj.optJSONArray("items")?: JSONArray()
    val out= mutableListOf<Listing>()
    for(i in 0 until arr.length()){
        val x=arr.optJSONObject(i)?:continue
        val mq=x.optJSONObject("matchQuality")
        val reasons=mq?.optJSONArray("reasons")?.let{a->(0 until a.length()).mapNotNull{a.optString(it).takeIf(String::isNotBlank)}.joinToString(" + ")}?:""
        val p=listOf("deliveredPrice","price","itemPrice").asSequence().map{x.optDouble(it,0.0)}.firstOrNull{it>0}?:0.0
        out+=Listing(
            title=x.optString("title",x.optString("name","Untitled result")),
            price=p,
            source=x.optString("seller",x.optString("store",x.optString("source",x.optString("merchant","")))),
            url=x.optString("url",x.optString("link",x.optString("itemWebUrl",""))),
            condition=x.optString("condition",""),
            matchScore=mq?.optInt("score",0)?:0,
            matchLabel=mq?.optString("label","")?:"",
            matchReasons=reasons
        )
    }
    return out
}

private suspend fun market(query:String,limit:Int,mode:String):MarketResult=withContext(Dispatchers.IO){
    val conn=(URL(API).openConnection() as HttpURLConnection).apply{requestMethod="POST";connectTimeout=15000;readTimeout=20000;doOutput=true;setRequestProperty("Content-Type","application/json")}
    val body=JSONObject().put("query",query).put("limit",limit).put("australiaOnly",true).put("mode",mode).toString();conn.outputStream.use{it.write(body.toByteArray())}
    val code=conn.responseCode;val text=(if(code in 200..299)conn.inputStream else conn.errorStream).bufferedReader().use{it.readText()};val root=JSONObject(text);if(code !in 200..299||!root.optBoolean("success"))throw IllegalStateException(root.optString("error","HTTP $code"))
    fun price(o:JSONObject?,vararg keys:String):Double{if(o==null)return 0.0;for(k in keys){val v=o.optDouble(k,0.0);if(v>0)return v};return 0.0}
    val g=root.optJSONObject("google");val e=root.optJSONObject("ebay");val gp=g?.optJSONObject("pricing");val ep=e?.optJSONObject("pricing")
    MarketResult(
        price(gp,"competitiveLow","typicalNew"),
        price(ep,"typicalUsed"),
        g?.optInt("analysedListings",0)?:0,
        e?.optInt("analysedListings",0)?:0,
        ep?.optBoolean("estimatedFromNew",false)?:false,
        parseListings(g),
        parseListings(e)
    )
}
