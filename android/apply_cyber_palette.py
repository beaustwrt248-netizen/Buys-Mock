from pathlib import Path

root = Path(__file__).resolve().parent / 'app' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'hub'
replacements = {
    '0xFFFFD400': '0xFF2F7CFF',
    '0xFFC99A27': '0xFF2F7CFF',
    '0xFFDDB347': '0xFF12C9FF',
    '0xFF111111': '0xFF030712',
    '0xFF222222': '0xFF07172C',
    '0xFF101010': '0xFF041024',
    '0xFF1B1B1B': '0xFF081A31',
    '0xFF171717': '0xFF061327',
    '0xFF303030': '0xFF0A1B33',
    '0xFF2B2B2B': '0xFF0B1C35',
    '0xFF252525': '0xFF09182F',
    '0xFF1E1E1E': '0xFF061326',
    '0xFF57E389': '0xFF25D991',
    '0xFF57D68D': '0xFF25D991',
    'android.graphics.Color.rgb(17,17,17)': 'android.graphics.Color.rgb(3,7,18)',
    'window.statusBarColor=android.graphics.Color.BLACK': 'window.statusBarColor=android.graphics.Color.rgb(3,7,18)',
    'window.navigationBarColor=android.graphics.Color.BLACK': 'window.navigationBarColor=android.graphics.Color.rgb(3,7,18)',
    'window.statusBarColor = android.graphics.Color.BLACK': 'window.statusBarColor = android.graphics.Color.rgb(3,7,18)',
    'window.navigationBarColor = android.graphics.Color.BLACK': 'window.navigationBarColor = android.graphics.Color.rgb(3,7,18)',
}

for path in root.glob('*.kt'):
    text = path.read_text(encoding='utf-8')
    updated = text
    for old, new in replacements.items():
        updated = updated.replace(old, new)
    if updated != text:
        path.write_text(updated, encoding='utf-8')
        print(f'Applied blue/cyan cyber palette: {path.name}')

# Upgrade the simple Laptop screen into the same layered cyber dashboard style as the web app.
main = root / 'MainActivity.kt'
if main.exists():
    text = main.read_text(encoding='utf-8')
    old = '''@Composable fun Laptop()=Screen("💻 Laptop / MacBook"){var q by remember{mutableStateOf("")};var ask by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)};var result by remember{mutableStateOf<MarketResult?>(null)};var status by remember{mutableStateOf("Ready")};val scope=rememberCoroutineScope();Field("Exact model / clean search",q,{q=it});Field("Seller asking price",ask,{ask=it},true);Btn(if(busy)"Searching…" else "Analyse Laptop",{result=null;status="Searching exact model and safe fallbacks…";busy=true;scope.launch{runCatching{market(q)}.onSuccess{result=it;status=summary(it)}.onFailure{status=it.message?:"Search failed"};busy=false}},!busy&&q.isNotBlank());Text(status,color=Color.LightGray);if(!busy)result?.let{Valuation(it,ask,0.30,0.58)}}'''
    new = '''@Composable fun Laptop()=Screen("💻 Laptop Intelligence"){
var q by remember{mutableStateOf("")};var ask by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)};var result by remember{mutableStateOf<MarketResult?>(null)};var status by remember{mutableStateOf("Ready for a model")};val scope=rememberCoroutineScope()
Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF07172C)),border=BorderStroke(1.dp,Color(0xFF235A91)),shape=RoundedCornerShape(24.dp),modifier=Modifier.fillMaxWidth()){
 Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("DEVICE LOOKUP",fontSize=11.sp,fontWeight=FontWeight.Black,color=Color(0xFF12C9FF));Text("Find the exact laptop",fontSize=22.sp,fontWeight=FontWeight.Black);Text("Enter the clean model first, then add the seller's asking price for a live buying decision.",color=Color(0xFF8FA6C6),fontSize=13.sp)
  Field("Exact model / clean search",q,{q=it});Field("Seller asking price",ask,{ask=it},true)
  Button(onClick={result=null;status="Searching live Australian pricing…";busy=true;scope.launch{runCatching{market(q)}.onSuccess{result=it;status=summary(it)}.onFailure{status=it.message?:"Search failed"};busy=false}},enabled=!busy&&q.isNotBlank(),modifier=Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(16.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF2F7CFF),contentColor=Color.White)){Text(if(busy)"Searching market…" else "Analyse Laptop",fontWeight=FontWeight.Black,fontSize=16.sp)}
 }
}
Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
 Surface(modifier=Modifier.weight(1f),color=Color(0xFF061327),border=BorderStroke(1.dp,Color(0xFF235A91)),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(14.dp)){Text("LIVE PRICING",fontSize=10.sp,fontWeight=FontWeight.Bold,color=Color(0xFF8FA6C6));Text(if(busy)"SEARCHING" else "READY",fontSize=17.sp,fontWeight=FontWeight.Black,color=if(busy)Color(0xFF12C9FF) else Color(0xFF25D991))}}
 Surface(modifier=Modifier.weight(1f),color=Color(0xFF061327),border=BorderStroke(1.dp,Color(0xFF235A91)),shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(14.dp)){Text("TARGET GP",fontSize=10.sp,fontWeight=FontWeight.Bold,color=Color(0xFF8FA6C6));Text("30%",fontSize=17.sp,fontWeight=FontWeight.Black,color=Color.White)}}
}
Surface(color=Color(0xFF081A31),border=BorderStroke(1.dp,Color(0xFF143A63)),shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(15.dp)){Text("SYSTEM STATUS",fontSize=10.sp,fontWeight=FontWeight.Black,color=Color(0xFF12C9FF));Spacer(Modifier.height(4.dp));Text(status,color=Color(0xFFDCE9FF),fontWeight=FontWeight.SemiBold)}}
if(result==null&&!busy){Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF061327)),border=BorderStroke(1.dp,Color(0xFF143A63)),shape=RoundedCornerShape(22.dp),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(17.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("HOW IT WORKS",fontSize=11.sp,fontWeight=FontWeight.Black,color=Color(0xFF9B4DFF));Text("1  Detect exact model",fontWeight=FontWeight.Bold);Text("2  Compare eBay AU + Google pricing",fontWeight=FontWeight.Bold);Text("3  Calculate used value + maximum buy",fontWeight=FontWeight.Bold);Text("Exact matches drive the valuation. Similar results are kept separate for safety.",fontSize=12.sp,color=Color(0xFF8FA6C6))}}}
if(!busy)result?.let{Valuation(it,ask,0.30,0.58)}
}'''
    if old in text:
        text = text.replace(old, new)
        main.write_text(text, encoding='utf-8')
        print('Applied cyber Laptop layout redesign')
    elif 'Laptop Intelligence' in text:
        print('Laptop layout redesign already applied')
    else:
        print('WARNING: Laptop layout source pattern not found; leaving screen unchanged')

# Secure paid pricing endpoints behind the same authenticated session used by the app.
if main.exists():
    text = main.read_text(encoding='utf-8')
    old_request = '''private suspend fun request(query:String,limit:Int=40):JSONObject=withContext(Dispatchers.IO){val c=(URL(API).openConnection() as HttpURLConnection).apply{requestMethod="POST";connectTimeout=15000;readTimeout=20000;doOutput=true;setRequestProperty("Content-Type","application/json")};val body=JSONObject().put("query",query).put("limit",limit).put("australiaOnly",true).put("mode","device").toString();c.outputStream.use{it.write(body.toByteArray())};val code=c.responseCode;val text=(if(code in 200..299)c.inputStream else c.errorStream).bufferedReader().use{it.readText()};val root=JSONObject(text);if(code !in 200..299||!root.optBoolean("success"))throw IllegalStateException(root.optString("error","HTTP $code"));root}'''
    secure_request = '''private suspend fun request(query:String,limit:Int=40):JSONObject=withContext(Dispatchers.IO){val token=AuthManager.accessToken(MorleyApplication.instance);if(token.isBlank())throw IllegalStateException("Your secure session has expired. Sign in again.");val c=(URL(API).openConnection() as HttpURLConnection).apply{requestMethod="POST";connectTimeout=15000;readTimeout=20000;doOutput=true;setRequestProperty("Content-Type","application/json");setRequestProperty("Authorization","Bearer $token")};val body=JSONObject().put("query",query).put("limit",limit).put("australiaOnly",true).put("mode","device").toString();c.outputStream.use{it.write(body.toByteArray())};val code=c.responseCode;val responseText=(if(code in 200..299)c.inputStream else c.errorStream)?.bufferedReader()?.use{it.readText()}.orEmpty();if(code==401||code==403)throw IllegalStateException("Your secure session has expired or is not authorised. Sign in again.");val root=if(responseText.isBlank())JSONObject() else JSONObject(responseText);if(code !in 200..299||!root.optBoolean("success"))throw IllegalStateException(root.optString("error","HTTP $code"));root}'''
    if old_request in text:
        main.write_text(text.replace(old_request, secure_request), encoding='utf-8')
        print('Secured Android pricing API requests')
    elif 'setRequestProperty("Authorization","Bearer $token")' in text:
        print('Android pricing API requests already secured')
    else:
        raise SystemExit('Could not locate Android pricing request for authentication hardening')
