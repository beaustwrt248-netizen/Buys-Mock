from pathlib import Path

main = Path(__file__).resolve().parent / 'app' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'hub' / 'MainActivity.kt'
text = main.read_text(encoding='utf-8')

# AppRuntime.context now resolves directly from MorleyApplication.instance, so
# Activity lifecycle order can never leave pricing with an uninitialised context.
old_activity = 'class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);AppRuntime.context=applicationContext;window.statusBarColor=android.graphics.Color.rgb(17,17,17);window.navigationBarColor=android.graphics.Color.rgb(17,17,17);setContent{App()}}}'
new_activity = 'class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);window.statusBarColor=android.graphics.Color.rgb(17,17,17);window.navigationBarColor=android.graphics.Color.rgb(17,17,17);setContent{App()}}}'

old_request = 'private suspend fun request(query:String,limit:Int=40):JSONObject=withContext(Dispatchers.IO){val c=(URL(API).openConnection() as HttpURLConnection).apply{requestMethod="POST";connectTimeout=15000;readTimeout=20000;doOutput=true;setRequestProperty("Content-Type","application/json")};val body=JSONObject().put("query",query).put("limit",limit).put("australiaOnly",true).put("mode","device").toString();c.outputStream.use{it.write(body.toByteArray())};val code=c.responseCode;val text=(if(code in 200..299)c.inputStream else c.errorStream).bufferedReader().use{it.readText()};val root=JSONObject(text);if(code !in 200..299||!root.optBoolean("success"))throw IllegalStateException(root.optString("error","HTTP $code"));root}'
new_request = 'private suspend fun request(query:String,limit:Int=40):JSONObject=withContext(Dispatchers.IO){val token=AuthManager.validAccessToken(AppRuntime.context);val c=(URL(API).openConnection() as HttpURLConnection).apply{requestMethod="POST";connectTimeout=15000;readTimeout=20000;doOutput=true;setRequestProperty("Content-Type","application/json");setRequestProperty("apikey",BuildConfig.SUPABASE_PUBLISHABLE_KEY);setRequestProperty("Authorization","Bearer $token")};try{val body=JSONObject().put("query",query).put("limit",limit).put("australiaOnly",true).put("mode","device").toString();c.outputStream.use{it.write(body.toByteArray())};val code=c.responseCode;val stream=if(code in 200..299)c.inputStream else c.errorStream;val text=stream?.bufferedReader()?.use{it.readText()}.orEmpty();val root=runCatching{JSONObject(text.ifBlank{"{}"})}.getOrElse{JSONObject()};if(code==401){throw IllegalStateException("Your session has expired. Please sign in again.")};if(code !in 200..299||!root.optBoolean("success"))throw IllegalStateException(root.optString("error","Pricing search failed ($code)"));root}finally{c.disconnect()}}'

changed = False
if old_activity in text:
    text = text.replace(old_activity, new_activity)
    changed = True

if old_request in text:
    text = text.replace(old_request, new_request)
    changed = True
elif new_request not in text:
    raise SystemExit('Could not locate primary pricing request for auth migration')

if changed:
    main.write_text(text, encoding='utf-8')
    print('Secured primary pricing API request with lifecycle-safe app context')
else:
    print('Primary pricing auth migration already applied')
