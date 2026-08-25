from pathlib import Path

main = Path(__file__).resolve().parent / 'app' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'hub' / 'MainActivity.kt'
text = main.read_text(encoding='utf-8')

old_activity = 'class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);AppRuntime.context=applicationContext;window.statusBarColor=android.graphics.Color.rgb(17,17,17);window.navigationBarColor=android.graphics.Color.rgb(17,17,17);setContent{App()}}}'
new_activity = 'class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);window.statusBarColor=android.graphics.Color.rgb(17,17,17);window.navigationBarColor=android.graphics.Color.rgb(17,17,17);setContent{App()}}}'
if old_activity in text:
    text = text.replace(old_activity, new_activity)

new_request = 'private suspend fun request(query:String,limit:Int=40):JSONObject=withContext(Dispatchers.IO){val token=AuthManager.validAccessToken(AppRuntime.context);val resolved=runCatching{LaptopModelCatalog.resolve(AppRuntime.context,query)}.getOrNull();val searchQuery=resolved?.canonicalQuery?.takeIf{it.isNotBlank()}?:query;val c=(URL(API).openConnection() as HttpURLConnection).apply{requestMethod="POST";connectTimeout=15000;readTimeout=20000;doOutput=true;setRequestProperty("Content-Type","application/json");setRequestProperty("apikey",BuildConfig.SUPABASE_PUBLISHABLE_KEY);setRequestProperty("Authorization","Bearer $token")};try{val body=JSONObject().put("query",searchQuery).put("limit",limit).put("australiaOnly",true).put("mode","device").toString();c.outputStream.use{it.write(body.toByteArray())};val code=c.responseCode;val stream=if(code in 200..299)c.inputStream else c.errorStream;val responseText=stream?.bufferedReader()?.use{it.readText()}.orEmpty();val root=runCatching{JSONObject(responseText.ifBlank{"{}"})}.getOrElse{JSONObject()};if(resolved!=null&&resolved.canonicalQuery!=query){root.put("catalogOriginalQuery",query);root.put("catalogCanonicalQuery",resolved.canonicalQuery);root.put("catalogModel",resolved.modelName?:"");root.put("catalogModelNumber",resolved.modelNumber?:"")};if(code==401){throw IllegalStateException("Your session has expired. Please sign in again.")};if(code !in 200..299||!root.optBoolean("success"))throw IllegalStateException(root.optString("error","Pricing search failed ($code)"));root}finally{c.disconnect()}}'

lines = text.splitlines()
indices = [i for i, line in enumerate(lines) if line.startswith('private suspend fun request(query:String,limit:Int=40):JSONObject=withContext(Dispatchers.IO)')]
if len(indices) != 1:
    raise SystemExit(f'Expected exactly one primary pricing request, found {len(indices)}')
lines[indices[0]] = new_request
main.write_text('\n'.join(lines) + '\n', encoding='utf-8')
print('Secured primary pricing API request and enabled laptop catalogue resolution')
