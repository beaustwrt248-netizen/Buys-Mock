from pathlib import Path

main = Path(__file__).resolve().parent / 'app' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'hub' / 'MainActivity.kt'
text = main.read_text(encoding='utf-8')

old_activity = 'class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);AppRuntime.context=applicationContext;window.statusBarColor=android.graphics.Color.rgb(17,17,17);window.navigationBarColor=android.graphics.Color.rgb(17,17,17);setContent{App()}}}'
new_activity = 'class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);window.statusBarColor=android.graphics.Color.rgb(17,17,17);window.navigationBarColor=android.graphics.Color.rgb(17,17,17);setContent{App()}}}'
if old_activity in text:
    text = text.replace(old_activity, new_activity)

# Keep this task deterministic and idempotent. Newer source may already contain
# a refreshed-token implementation; in that case validate it instead of trying
# to match only the original one-line request body.
secure_request = 'private suspend fun request(query:String,limit:Int=40):JSONObject{val token=AuthManager.validAccessToken(MorleyApplication.instance);if(token.isBlank())throw IllegalStateException("Your secure session has expired. Sign in again.");return withContext(Dispatchers.IO){val c=(URL(API).openConnection() as HttpURLConnection).apply{requestMethod="POST";connectTimeout=15000;readTimeout=20000;doOutput=true;setRequestProperty("Content-Type","application/json");setRequestProperty("apikey",BuildConfig.SUPABASE_PUBLISHABLE_KEY);setRequestProperty("Authorization","Bearer $token")};val body=JSONObject().put("query",query).put("limit",limit).put("australiaOnly",true).put("mode","device").toString();c.outputStream.use{it.write(body.toByteArray())};val code=c.responseCode;val stream=if(code in 200..299)c.inputStream else c.errorStream;val text=stream?.bufferedReader()?.use{it.readText()}.orEmpty();val root=runCatching{JSONObject(text)}.getOrElse{JSONObject().put("error",if(code==401)"Your secure session has expired. Sign in again." else "HTTP $code")};if(code !in 200..299||!root.optBoolean("success"))throw IllegalStateException(root.optString("error","HTTP $code"));root}}'

lines = text.splitlines()
indices = [i for i, line in enumerate(lines) if line.startswith('private suspend fun request(query:String,limit:Int=40):JSONObject')]
if len(indices) != 1:
    raise SystemExit(f'Expected exactly one primary pricing request, found {len(indices)}')
idx = indices[0]
line = lines[idx]
if ('AuthManager.validAccessToken' in line and
    'setRequestProperty("Authorization","Bearer $token")' in line and
    'BuildConfig.SUPABASE_PUBLISHABLE_KEY' in line):
    print('Primary pricing API request already secured')
else:
    lines[idx] = secure_request
    print('Secured primary pricing API request with refreshed authenticated session')

main.write_text('\n'.join(lines) + '\n', encoding='utf-8')
