package com.buysloans.hub

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class UpdateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(3,7,18)
        window.navigationBarColor = android.graphics.Color.rgb(3,7,18)
        val versionName = intent.getStringExtra("versionName").orEmpty()
        val apkUrl = intent.getStringExtra("apkUrl").orEmpty()
        val sha256 = intent.getStringExtra("sha256").orEmpty().lowercase()
        setContent { UpdateScreen(versionName, apkUrl, sha256) { finish() } }
    }
}

private suspend fun sha256OfUri(context:Context, uri:Uri):String = withContext(Dispatchers.IO) {
    val digest=MessageDigest.getInstance("SHA-256")
    context.contentResolver.openInputStream(uri)?.use { input ->
        val buffer=ByteArray(64*1024)
        while(true){
            val count=input.read(buffer)
            if(count<0) break
            if(count>0) digest.update(buffer,0,count)
        }
    } ?: throw IllegalStateException("Downloaded APK could not be opened for verification")
    digest.digest().joinToString(""){"%02x".format(it)}
}

@Composable
private fun UpdateScreen(versionName:String, apkUrl:String, expectedSha256:String, close:()->Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var progress by remember { mutableFloatStateOf(0f) }
    var status by remember { mutableStateOf("Preparing update…") }
    var details by remember { mutableStateOf("") }
    var downloadId by remember { mutableLongStateOf(-1L) }
    var installUri by remember { mutableStateOf<Uri?>(null) }
    var failed by remember { mutableStateOf(false) }

    fun beginDownload() {
        if (apkUrl.isBlank() || downloadId >= 0) return
        if (!UpdateManager.isTrustedApkUrl(apkUrl)) {
            failed=true
            status="Update URL failed security validation"
            return
        }
        if (!UpdateManager.isValidSha256(expectedSha256)) {
            failed=true
            status="Update checksum is missing or invalid"
            return
        }
        val safeVersion=versionName.ifBlank{"update"}.replace(Regex("[^A-Za-z0-9._-]+"),"-")
        val fileName="B-and-L-Morley-$safeVersion.apk"
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.resolve(fileName)?.delete()
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("B&L Morley $versionName")
            .setDescription("Downloading verified app update")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
        downloadId = dm.enqueue(request)
        status = "Downloading update…"
        failed = false
        installUri = null
    }

    LaunchedEffect(Unit) { beginDownload() }

    LaunchedEffect(downloadId) {
        if (downloadId < 0) return@LaunchedEffect
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        while (true) {
            val cursor = dm.query(DownloadManager.Query().setFilterById(downloadId))
            cursor.use {
                if (!it.moveToFirst()) {
                    failed = true
                    status = "Download unavailable"
                    return@LaunchedEffect
                }
                val state = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val done = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                if (total > 0) progress = (done.toFloat() / total.toFloat()).coerceIn(0f,1f)
                fun mb(v:Long)=String.format("%.1f MB",v/1048576.0)
                details = if (total > 0) "${mb(done)} / ${mb(total)}" else if (done > 0) mb(done) else "Starting…"
                when(state) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        progress = 1f
                        status = "Download complete — verifying SHA-256…"
                        val uri=dm.getUriForDownloadedFile(downloadId)
                        if(uri==null){
                            failed=true
                            status="Downloaded APK could not be opened"
                            return@LaunchedEffect
                        }
                        val actual=runCatching{sha256OfUri(context,uri)}.getOrElse{
                            failed=true
                            status="APK checksum verification failed"
                            details=it.message.orEmpty()
                            return@LaunchedEffect
                        }
                        if(!actual.equals(expectedSha256,ignoreCase=true)){
                            failed=true
                            installUri=null
                            status="APK integrity check failed"
                            details="The downloaded file did not match the signed release checksum. Installation has been blocked."
                            dm.remove(downloadId)
                            return@LaunchedEffect
                        }
                        installUri = uri
                        status = "Verified — ready to install"
                        details = "SHA-256 verified • ${if(total>0)mb(total) else "download complete"}"
                        return@LaunchedEffect
                    }
                    DownloadManager.STATUS_FAILED -> {
                        failed = true
                        status = "Download failed"
                        return@LaunchedEffect
                    }
                    DownloadManager.STATUS_PAUSED -> status = "Download paused…"
                    DownloadManager.STATUS_PENDING -> status = "Waiting to start…"
                    else -> status = "Downloading update…"
                }
            }
            delay(300)
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary=Color(0xFF2F7CFF),background=Color(0xFF030712),surface=Color(0xFF07172C))) {
        Surface(color=Color(0xFF030712),modifier=Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                Text("B&L Morley Update",fontSize=28.sp,fontWeight=FontWeight.Black)
                Text(if(versionName.isBlank()) "New version" else versionName,color=Color(0xFF70DFFF),fontSize=20.sp,fontWeight=FontWeight.Bold)
                Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF07172C)),shape=RoundedCornerShape(22.dp),modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        Text(status,fontWeight=FontWeight.Black,fontSize=20.sp)
                        LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth().height(10.dp),color=Color(0xFF12C9FF),trackColor=Color(0xFF0A1B33))
                        Text("${(progress*100).toInt()}%${if(details.isNotBlank())" • $details" else ""}",color=Color(0xFFA7BAD3))
                        Text("The APK is downloaded from the B&L Morley GitHub release and its SHA-256 checksum is verified before Android is allowed to install it.",color=Color(0xFF8FA6C6),fontSize=13.sp)
                    }
                }
                installUri?.let { uri ->
                    Button(onClick={
                        status = "Opening Android installer…"
                        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri,"application/vnd.android.package-archive")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    },modifier=Modifier.fillMaxWidth().height(56.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF2F7CFF),contentColor=Color.White)) {
                        Text("Install Verified Update",fontWeight=FontWeight.Black)
                    }
                    Text("After installation, reopen B&L Morley. The app will confirm the installed version on launch.",color=Color(0xFFA7BAD3),fontSize=13.sp)
                }
                if(failed) {
                    Button(onClick={
                        downloadId = -1L
                        progress = 0f
                        details = ""
                        status = "Preparing retry…"
                        scope.launch { delay(100); beginDownload() }
                    },modifier=Modifier.fillMaxWidth()) { Text("Retry") }
                }
                OutlinedButton(onClick=close,modifier=Modifier.fillMaxWidth()) { Text("Close") }
            }
        }
    }
}
