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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class UpdateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(17,17,17)
        window.navigationBarColor = android.graphics.Color.rgb(17,17,17)
        val versionName = intent.getStringExtra("versionName").orEmpty()
        val apkUrl = intent.getStringExtra("apkUrl").orEmpty()
        setContent { UpdateScreen(versionName, apkUrl) { finish() } }
    }
}

@Composable
private fun UpdateScreen(versionName:String, apkUrl:String, close:()->Unit) {
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
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("B&L Morley $versionName")
            .setDescription("Downloading app update")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "B-and-L-Morley-$versionName.apk")
        downloadId = dm.enqueue(request)
        status = "Downloading update…"
        failed = false
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
                        status = "Download complete — verifying APK…"
                        delay(350)
                        installUri = dm.getUriForDownloadedFile(downloadId)
                        status = if (installUri != null) "Ready to install" else "APK verification failed"
                        failed = installUri == null
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

    MaterialTheme(colorScheme = darkColorScheme(primary=Color(0xFFFFD400),background=Color(0xFF111111),surface=Color(0xFF222222))) {
        Surface(color=Color(0xFF111111),modifier=Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
                Text("B&L Morley Update",fontSize=28.sp,fontWeight=FontWeight.Black)
                Text(if(versionName.isBlank()) "New version" else versionName,color=Color(0xFFFFD400),fontSize=20.sp,fontWeight=FontWeight.Bold)
                Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF222222)),shape=RoundedCornerShape(22.dp),modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        Text(status,fontWeight=FontWeight.Black,fontSize=20.sp)
                        LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth().height(10.dp))
                        Text("${(progress*100).toInt()}%${if(details.isNotBlank())" • $details" else ""}",color=Color.LightGray)
                        Text("Android will show the system installer when the APK is ready. Keep B&L Morley open until the download finishes.",color=Color.Gray,fontSize=13.sp)
                    }
                }
                installUri?.let { uri ->
                    Button(onClick={
                        status = "Opening Android installer…"
                        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri,"application/vnd.android.package-archive")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    },modifier=Modifier.fillMaxWidth().height(56.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFFFD400),contentColor=Color.Black)) {
                        Text("Install Update",fontWeight=FontWeight.Black)
                    }
                    Text("After installation, reopen B&L Morley. The app will confirm the new version and offer a restart/relaunch message if needed.",color=Color.LightGray,fontSize=13.sp)
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
