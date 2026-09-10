package com.buysloans.hub

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.Executor
import kotlin.math.min
import kotlin.math.round

private val LensBlue = Color(0xFF0878F9)
private val LensBlueDark = Color(0xFF063868)
private val LensNavy = Color(0xFF032A4F)
private val LensBg = Color(0xFFF5F8FC)
private val LensText = Color(0xFF102033)
private val LensMuted = Color(0xFF617083)
private val LensBorder = Color(0xFFD8E2EE)
private val LensDanger = Color(0xFFE53935)
private val LensDangerSoft = Color(0xFFFFE9E8)
private val LensSuccess = Color(0xFF14B86E)
private val LensSuccessSoft = Color(0xFFE7F8EF)

private enum class LensStep {
    CAPTURE_FRONT,
    CAPTURE_BACK,
    ANALYSING,
    RESULTS,
    DAMAGE,
    DETAILS,
    CONDITION,
    PRICING,
    ADD_STOCK,
    COMPLETE
}

class DeviceLensActivity : ComponentActivity() {
    private var cameraGranted by mutableStateOf(false)
    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        cameraGranted = it
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        window.statusBarColor = android.graphics.Color.rgb(3, 42, 79)
        window.navigationBarColor = android.graphics.Color.WHITE

        setContent {
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    primary = LensBlue,
                    secondary = LensBlueDark,
                    background = LensBg,
                    surface = Color.White
                )
            ) {
                DeviceLensFlow(
                    cameraGranted = cameraGranted,
                    requestCamera = { cameraPermission.launch(Manifest.permission.CAMERA) },
                    close = { finish() }
                )
            }
        }
    }
}

@Composable
private fun DeviceLensFlow(
    cameraGranted: Boolean,
    requestCamera: () -> Unit,
    close: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(LensStep.CAPTURE_FRONT) }
    var frontPhoto by remember { mutableStateOf<File?>(null) }
    var backPhoto by remember { mutableStateOf<File?>(null) }
    var inspection by remember { mutableStateOf<DeviceInspection?>(null) }
    var pricing by remember { mutableStateOf<LivePricingResult?>(null) }
    var busyPricing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf("") }
    var sellPrice by remember { mutableStateOf("") }
    var stockNumber by remember { mutableStateOf("") }
    var confirmedCondition by remember { mutableStateOf("") }
    var analysisAttempt by remember { mutableStateOf(0) }

    fun reset() {
        runCatching { frontPhoto?.delete() }
        runCatching { backPhoto?.delete() }
        frontPhoto = null
        backPhoto = null
        inspection = null
        pricing = null
        busyPricing = false
        error = ""
        purchasePrice = ""
        sellPrice = ""
        stockNumber = ""
        confirmedCondition = ""
        analysisAttempt = 0
        step = LensStep.CAPTURE_FRONT
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { frontPhoto?.delete() }
            runCatching { backPhoto?.delete() }
        }
    }

    LaunchedEffect(step, frontPhoto, backPhoto, analysisAttempt) {
        if (step == LensStep.ANALYSING && frontPhoto != null && backPhoto != null) {
            error = ""
            runCatching {
                DeviceInspectionClient.inspect(context, frontPhoto!!, backPhoto!!)
            }.onSuccess {
                inspection = it
                confirmedCondition = gradeLabel(it.conditionGrade)
                step = LensStep.RESULTS
            }.onFailure {
                error = it.message ?: "The two photos could not be analysed."
            }
        }
    }

    fun openPricing() {
        val current = inspection ?: return
        step = LensStep.PRICING
        if (pricing != null || busyPricing) return
        busyPricing = true
        scope.launch {
            runCatching { DeviceInspectionClient.livePricing(context, current) }
                .onSuccess {
                    pricing = it
                    it.conditionAdjustedResale?.let { value ->
                        if (sellPrice.isBlank()) sellPrice = roundToFive(value).toInt().toString()
                    }
                }
                .onFailure { error = "Live pricing unavailable: ${it.message ?: "unknown error"}" }
            busyPricing = false
        }
    }

    when (step) {
        LensStep.CAPTURE_FRONT -> CaptureStep(
            cameraGranted = cameraGranted,
            requestCamera = requestCamera,
            title = "Photo 1 of 2",
            instruction = "Take a clear photo of the front",
            side = "FRONT",
            close = close,
            onCaptured = {
                frontPhoto = it
                step = LensStep.CAPTURE_BACK
            }
        )
        LensStep.CAPTURE_BACK -> CaptureStep(
            cameraGranted = cameraGranted,
            requestCamera = requestCamera,
            title = "Photo 2 of 2",
            instruction = "Take a clear photo of the back",
            side = "BACK",
            close = close,
            onBack = {
                runCatching { frontPhoto?.delete() }
                frontPhoto = null
                step = LensStep.CAPTURE_FRONT
            },
            onCaptured = {
                backPhoto = it
                step = LensStep.ANALYSING
            }
        )
        LensStep.ANALYSING -> AnalyseScreen(
            frontPhoto = frontPhoto,
            backPhoto = backPhoto,
            error = error,
            retry = {
                if (frontPhoto != null && backPhoto != null) {
                    error = ""
                    analysisAttempt++
                }
            },
            retake = {
                runCatching { frontPhoto?.delete() }
                runCatching { backPhoto?.delete() }
                frontPhoto = null
                backPhoto = null
                error = ""
                step = LensStep.CAPTURE_FRONT
            },
            close = close
        )
        LensStep.RESULTS -> inspection?.let { result ->
            ResultsScreen(
                inspection = result,
                frontPhoto = frontPhoto!!,
                backPhoto = backPhoto!!,
                damageDetails = { step = LensStep.DAMAGE },
                deviceDetails = { step = LensStep.DETAILS },
                condition = { step = LensStep.CONDITION },
                pricing = ::openPricing,
                addStock = { step = LensStep.ADD_STOCK },
                retake = {
                    frontPhoto?.delete()
                    backPhoto?.delete()
                    frontPhoto = null
                    backPhoto = null
                    inspection = null
                    pricing = null
                    step = LensStep.CAPTURE_FRONT
                },
                close = close
            )
        } ?: reset()
        LensStep.DAMAGE -> inspection?.let { result ->
            DamageDetailsScreen(
                inspection = result,
                frontPhoto = frontPhoto!!,
                backPhoto = backPhoto!!,
                back = { step = LensStep.RESULTS }
            )
        } ?: reset()
        LensStep.DETAILS -> inspection?.let { result ->
            DeviceDetailsScreen(
                inspection = result,
                pricing = ::openPricing,
                addStock = { step = LensStep.ADD_STOCK },
                back = { step = LensStep.RESULTS }
            )
        } ?: reset()
        LensStep.CONDITION -> inspection?.let { result ->
            ConditionSummaryScreen(
                inspection = result,
                frontPhoto = frontPhoto!!,
                backPhoto = backPhoto!!,
                pricing = ::openPricing,
                addStock = { step = LensStep.ADD_STOCK },
                back = { step = LensStep.RESULTS }
            )
        } ?: reset()
        LensStep.PRICING -> inspection?.let { result ->
            PricingScreen(
                inspection = result,
                pricing = pricing,
                loading = busyPricing,
                error = error,
                useSuggested = {
                    pricing?.conditionAdjustedResale?.let { value ->
                        sellPrice = roundToFive(value).toInt().toString()
                    }
                    step = LensStep.ADD_STOCK
                },
                retry = {
                    pricing = null
                    error = ""
                    openPricing()
                },
                back = { step = LensStep.RESULTS }
            )
        } ?: reset()
        LensStep.ADD_STOCK -> inspection?.let { result ->
            AddStockScreen(
                inspection = result,
                condition = confirmedCondition.ifBlank { gradeLabel(result.conditionGrade) },
                onConditionChange = { confirmedCondition = it },
                purchasePrice = purchasePrice,
                onPurchasePriceChange = { purchasePrice = moneyInput(it) },
                sellPrice = sellPrice,
                onSellPriceChange = { sellPrice = moneyInput(it) },
                stockNumber = stockNumber,
                onStockNumberChange = { stockNumber = it.take(80) },
                submit = {
                    runCatching {
                        WorkspaceStore.addInventory(
                            context = context,
                            name = result.displayName,
                            barcode = stockNumber,
                            cost = purchasePrice.toDoubleOrNull() ?: 0.0,
                            resale = sellPrice.toDoubleOrNull() ?: 0.0,
                            quantity = 1
                        )
                    }.onSuccess {
                        step = LensStep.COMPLETE
                    }.onFailure {
                        error = it.message ?: "The device could not be added."
                    }
                },
                error = error,
                back = { step = LensStep.RESULTS }
            )
        } ?: reset()
        LensStep.COMPLETE -> CompleteScreen(
            name = inspection?.displayName.orEmpty(),
            viewStock = {
                context.startActivity(
                    Intent(context, MenuFeatureActivity::class.java)
                        .putExtra(MenuFeatureActivity.EXTRA_FEATURE, "inventory")
                )
                close()
            },
            scanAnother = ::reset
        )
    }
}

@Composable
private fun CaptureStep(
    cameraGranted: Boolean,
    requestCamera: () -> Unit,
    title: String,
    instruction: String,
    side: String,
    close: () -> Unit,
    onBack: (() -> Unit)? = null,
    onCaptured: (File) -> Unit
) {
    if (!cameraGranted) {
        Box(Modifier.fillMaxSize().background(LensNavy), contentAlignment = Alignment.Center) {
            Column(
                Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(52.dp))
                Text("Camera access", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text(
                    "Morley Buys needs camera access to take the two device photos used for damage analysis.",
                    color = Color.White.copy(alpha = .82f),
                    textAlign = TextAlign.Center
                )
                Button(onClick = requestCamera, colors = ButtonDefaults.buttonColors(containerColor = LensBlue)) {
                    Text("Allow Camera", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = close) { Text("Cancel", color = Color.White) }
            }
        }
        return
    }

    CameraCaptureSurface(
        title = title,
        instruction = instruction,
        side = side,
        close = close,
        back = onBack,
        onCaptured = onCaptured
    )
}

@Composable
private fun CameraCaptureSurface(
    title: String,
    instruction: String,
    side: String,
    close: () -> Unit,
    back: (() -> Unit)?,
    onCaptured: (File) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = context as LifecycleOwner
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var captureBusy by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf("") }
    val mainExecutor: Executor = ContextCompat.getMainExecutor(context)

    DisposableEffect(Unit) {
        onDispose { cameraProvider?.unbindAll() }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    val providerFuture = ProcessCameraProvider.getInstance(ctx)
                    providerFuture.addListener({
                        runCatching {
                            val provider = providerFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(surfaceProvider)
                            }
                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                                .build()
                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                capture
                            )
                            cameraProvider = provider
                            imageCapture = capture
                        }.onFailure {
                            cameraError = it.message ?: "Camera could not start."
                        }
                    }, mainExecutor)
                }
            }
        )

        Box(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = .44f))
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 18.dp, start = 10.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { back?.invoke() ?: close() }) {
                Icon(if (back != null) Icons.Default.ArrowBack else Icons.Default.Close, null, tint = Color.White)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(instruction, color = Color.White.copy(alpha = .86f), fontSize = 14.sp)
            }
            Spacer(Modifier.size(48.dp))
        }

        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth(.76f)
                .aspectRatio(.66f)
        ) {
            CameraCorners()
            Surface(
                color = Color.Black.copy(alpha = .48f),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp)
            ) {
                Text(
                    "Align the $side of the device inside the frame",
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (cameraError.isNotBlank()) {
                Surface(color = LensDanger.copy(alpha = .9f), shape = RoundedCornerShape(12.dp)) {
                    Text(cameraError, Modifier.padding(10.dp), color = Color.White, fontSize = 12.sp)
                }
            }
            Surface(
                onClick = {
                    val capture = imageCapture
                    if (capture == null || captureBusy) return@Surface
                    captureBusy = true
                    cameraError = ""
                    val file = File(context.cacheDir, "morley-device-${System.currentTimeMillis()}-$side.jpg")
                    val options = ImageCapture.OutputFileOptions.Builder(file).build()
                    capture.takePicture(
                        options,
                        mainExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                captureBusy = false
                                cameraProvider?.unbindAll()
                                onCaptured(file)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                captureBusy = false
                                runCatching { file.delete() }
                                cameraError = exception.message ?: "Photo capture failed."
                            }
                        }
                    )
                },
                enabled = !captureBusy && imageCapture != null,
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(5.dp, Color.White.copy(alpha = .55f)),
                modifier = Modifier.size(76.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (captureBusy) CircularProgressIndicator(Modifier.size(34.dp), color = LensBlue, strokeWidth = 3.dp)
                    else Surface(shape = CircleShape, color = Color.White, border = BorderStroke(2.dp, Color.Black.copy(alpha = .18f)), modifier = Modifier.size(60.dp)) {}
                }
            }
        }
    }
}

@Composable
private fun CameraCorners() {
    Canvas(Modifier.fillMaxSize()) {
        val c = Color.White
        val stroke = 4.dp.toPx()
        val segment = 34.dp.toPx()
        drawLine(c, Offset(0f, segment), Offset(0f, 0f), stroke, StrokeCap.Round)
        drawLine(c, Offset(0f, 0f), Offset(segment, 0f), stroke, StrokeCap.Round)
        drawLine(c, Offset(size.width - segment, 0f), Offset(size.width, 0f), stroke, StrokeCap.Round)
        drawLine(c, Offset(size.width, 0f), Offset(size.width, segment), stroke, StrokeCap.Round)
        drawLine(c, Offset(0f, size.height - segment), Offset(0f, size.height), stroke, StrokeCap.Round)
        drawLine(c, Offset(0f, size.height), Offset(segment, size.height), stroke, StrokeCap.Round)
        drawLine(c, Offset(size.width - segment, size.height), Offset(size.width, size.height), stroke, StrokeCap.Round)
        drawLine(c, Offset(size.width, size.height), Offset(size.width, size.height - segment), stroke, StrokeCap.Round)
    }
}

@Composable
private fun AnalyseScreen(
    frontPhoto: File?,
    backPhoto: File?,
    error: String,
    retry: () -> Unit,
    retake: () -> Unit,
    close: () -> Unit
) {
    LensScaffold(title = "Analysing Images", back = close) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                frontPhoto?.let { PlainPhoto(it, Modifier.weight(1f).aspectRatio(.72f)) }
                backPhoto?.let { PlainPhoto(it, Modifier.weight(1f).aspectRatio(.72f)) }
            }
            if (error.isBlank()) {
                Spacer(Modifier.height(4.dp))
                CircularProgressIndicator(color = LensBlue, strokeWidth = 4.dp)
                Text("Analysing both photos…", fontSize = 20.sp, fontWeight = FontWeight.Black, color = LensText)
                AnalysisCheck("Identifying device model")
                AnalysisCheck("Checking condition")
                AnalysisCheck("Detecting damage or cracks")
                AnalysisCheck("Locating visible damage")
                AnalysisCheck("Comparing with the catalogue")
                Text(
                    "Photos are processed for this assessment and are not stored by the inspection service.",
                    color = LensMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            } else {
                ErrorCard(error)
                Button(onClick = retry, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = LensBlue)) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Retry Analysis", fontWeight = FontWeight.Black)
                }
                OutlinedButton(onClick = retake, modifier = Modifier.fillMaxWidth()) { Text("Retake Photos") }
            }
        }
    }
}

@Composable
private fun ResultsScreen(
    inspection: DeviceInspection,
    frontPhoto: File,
    backPhoto: File,
    damageDetails: () -> Unit,
    deviceDetails: () -> Unit,
    condition: () -> Unit,
    pricing: () -> Unit,
    addStock: () -> Unit,
    retake: () -> Unit,
    close: () -> Unit
) {
    LensScaffold(title = "Analysis Results", back = close) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnnotatedPhoto(frontPhoto, inspection.damageRegions.filter { it.photoIndex == 1 }, Modifier.weight(1f).aspectRatio(.72f))
                AnnotatedPhoto(backPhoto, inspection.damageRegions.filter { it.photoIndex == 2 }, Modifier.weight(1f).aspectRatio(.72f))
            }

            if (inspection.damageRegions.isNotEmpty() || inspection.damageFlags.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = LensDangerSoft),
                    border = BorderStroke(1.dp, LensDanger.copy(alpha = .22f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = LensDanger, modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text("!", color = Color.White, fontWeight = FontWeight.Black) }
                        }
                        Column {
                            Text("Damage Detected", color = Color(0xFF9A211F), fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Text("${maxOf(inspection.damageRegions.size, inspection.damageFlags.size)} area(s) of visible damage found", color = Color(0xFF9A211F), fontSize = 12.sp)
                        }
                    }
                }
            } else {
                Card(colors = CardDefaults.cardColors(containerColor = LensSuccessSoft), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, null, tint = LensSuccess)
                        Text("No clear visible damage detected in the two photos.", color = Color(0xFF10683F), fontWeight = FontWeight.Bold)
                    }
                }
            }

            DeviceIdentityCard(inspection)
            if (inspection.damageRegions.isNotEmpty()) {
                FlowDamageChips(inspection.damageRegions)
            } else if (inspection.damageFlags.isNotEmpty()) {
                inspection.damageFlags.take(4).forEach { DamageChip(it) }
            }

            Button(onClick = deviceDetails, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = LensBlue)) {
                Text("View Full Details", fontWeight = FontWeight.Black)
            }
            if (inspection.damageRegions.isNotEmpty()) {
                OutlinedButton(onClick = damageDetails, modifier = Modifier.fillMaxWidth()) { Text("View Damage Close-up") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = condition, modifier = Modifier.weight(1f)) { Text("Condition") }
                OutlinedButton(onClick = pricing, modifier = Modifier.weight(1f)) { Text("Live Pricing") }
            }
            Button(onClick = addStock, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = LensBlueDark)) {
                Text("Add to Stock", fontWeight = FontWeight.Black)
            }
            TextButton(onClick = retake, modifier = Modifier.fillMaxWidth()) { Text("Retake Photos") }
            AiBoundary()
        }
    }
}

@Composable
private fun DamageDetailsScreen(
    inspection: DeviceInspection,
    frontPhoto: File,
    backPhoto: File,
    back: () -> Unit
) {
    var photoIndex by remember { mutableStateOf(inspection.damageRegions.firstOrNull()?.photoIndex ?: 1) }
    val activePhoto = if (photoIndex == 1) frontPhoto else backPhoto
    val regions = inspection.damageRegions.filter { it.photoIndex == photoIndex }

    LensScaffold(title = "Damage Details", back = back) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnnotatedPhoto(activePhoto, regions, Modifier.fillMaxWidth().aspectRatio(.72f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { photoIndex = 1 }, modifier = Modifier.weight(1f), enabled = photoIndex != 1) { Text("Front") }
                OutlinedButton(onClick = { photoIndex = 2 }, modifier = Modifier.weight(1f), enabled = photoIndex != 2) { Text("Back") }
            }
            if (regions.isEmpty()) {
                InfoCard("No marked region on this photo", "No confident damage location was returned for this angle.")
            } else {
                regions.forEach { region ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF192330)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFFFF6A66))
                            Column {
                                Text(region.label, color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
                                Text(
                                    "${region.severity.replaceFirstChar { it.uppercase() }} • ${(region.confidence * 100).toInt()}% location confidence",
                                    color = Color.White.copy(alpha = .72f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
            AiBoundary()
        }
    }
}

@Composable
private fun DeviceDetailsScreen(
    inspection: DeviceInspection,
    pricing: () -> Unit,
    addStock: () -> Unit,
    back: () -> Unit
) {
    LensScaffold(title = "Device Details", back = back) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DeviceIdentityCard(inspection)
            DetailCard {
                DetailRow("Brand", inspection.catalogueMatch?.brand.orEmpty().ifBlank { inspection.brand })
                DetailRow("Model", inspection.catalogueMatch?.modelName.orEmpty().ifBlank { inspection.model })
                DetailRow("Model Number", inspection.catalogueMatch?.modelNumber.orEmpty().ifBlank { inspection.modelNumber })
                DetailRow("Storage", inspection.storage.ifBlank { inspection.catalogueMatch?.storageOptions?.joinToString().orEmpty() })
                DetailRow("Colour", inspection.colour)
                DetailRow("Category", inspection.catalogueMatch?.category.orEmpty())
                DetailRow("Release Year", inspection.catalogueMatch?.releaseYear?.toString().orEmpty())
                DetailRow("Confidence", "${(inspection.confidence * 100).toInt()}%")
            }
            if (inspection.evidence.isNotEmpty()) {
                InfoCard("Visible evidence", inspection.evidence.joinToString("\n• ", prefix = "• "))
            }
            if (inspection.uncertainties.isNotEmpty()) {
                InfoCard("Needs verification", inspection.uncertainties.joinToString("\n• ", prefix = "• "))
            }
            Button(onClick = pricing, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = LensBlue)) {
                Text("Check Live Pricing", fontWeight = FontWeight.Black)
            }
            Button(onClick = addStock, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = LensBlueDark)) {
                Text("Add to Stock", fontWeight = FontWeight.Black)
            }
            AiBoundary()
        }
    }
}

@Composable
private fun ConditionSummaryScreen(
    inspection: DeviceInspection,
    frontPhoto: File,
    backPhoto: File,
    pricing: () -> Unit,
    addStock: () -> Unit,
    back: () -> Unit
) {
    LensScaffold(title = "Condition Summary", back = back) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnnotatedPhoto(frontPhoto, inspection.damageRegions.filter { it.photoIndex == 1 }, Modifier.weight(1f).aspectRatio(.72f))
                AnnotatedPhoto(backPhoto, inspection.damageRegions.filter { it.photoIndex == 2 }, Modifier.weight(1f).aspectRatio(.72f))
            }
            val damaged = inspection.damageRegions.isNotEmpty() || inspection.damageFlags.isNotEmpty()
            Card(
                colors = CardDefaults.cardColors(containerColor = if (damaged) LensDangerSoft else LensSuccessSoft),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Device Condition", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LensMuted)
                    Text(gradeLabel(inspection.conditionGrade), fontSize = 28.sp, fontWeight = FontWeight.Black, color = if (damaged) LensDanger else LensSuccess)
                    Text(inspection.conditionSummary.ifBlank { "Condition is based only on visible evidence in the two photos." }, color = LensText)
                }
            }
            inspection.damageRegions.forEach {
                ConditionRow(it.label, "${it.severity.replaceFirstChar { c -> c.uppercase() }} damage", false)
            }
            if (inspection.damageRegions.isEmpty()) ConditionRow("Visible surfaces", "No confident damage regions", true)
            ConditionRow("Identity", if (inspection.catalogueMatch != null) "Catalogue match found" else "Visual identification only", inspection.catalogueMatch != null)
            ConditionRow("Camera / buttons / ports", "Not confirmed from these two photos", true)
            Button(onClick = pricing, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = LensBlue)) {
                Text("Check Live Pricing", fontWeight = FontWeight.Black)
            }
            OutlinedButton(onClick = addStock, modifier = Modifier.fillMaxWidth()) { Text("Add to Stock") }
            AiBoundary()
        }
    }
}

@Composable
private fun PricingScreen(
    inspection: DeviceInspection,
    pricing: LivePricingResult?,
    loading: Boolean,
    error: String,
    useSuggested: () -> Unit,
    retry: () -> Unit,
    back: () -> Unit
) {
    LensScaffold(title = "Live Price Check", back = back) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DeviceIdentityCard(inspection)
            if (loading) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = LensBlue)
                    Text("Comparing Australian market prices…", fontWeight = FontWeight.Bold)
                }
            } else if (pricing == null) {
                ErrorCard(error.ifBlank { "No live pricing evidence was returned." })
                OutlinedButton(onClick = retry, modifier = Modifier.fillMaxWidth()) { Text("Try Again") }
            } else {
                val money = remember { NumberFormat.getCurrencyInstance(Locale("en", "AU")) }
                pricing.usedMedian?.let {
                    PriceSummary("Used market median", money.format(it), "Current retained Australian used-market listings")
                }
                pricing.conditionAdjustedResale?.let {
                    PriceSummary(
                        "Condition-adjusted guide",
                        money.format(roundToFive(it)),
                        "Applies Morley’s existing condition adjustment to the used-market median"
                    )
                }
                pricing.listings.take(8).forEach { listing ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, LensBorder),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(listing.source, color = LensBlueDark, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                Text(listing.title, maxLines = 2, color = LensText, fontSize = 12.sp)
                            }
                            Text(money.format(listing.price), color = LensText, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Button(
                    onClick = useSuggested,
                    enabled = pricing.conditionAdjustedResale != null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = LensBlue)
                ) { Text("Use Suggested Sell Price", fontWeight = FontWeight.Black) }
                Text(
                    "Live pricing is advisory and may include marketplace asking prices. Staff must verify the item and approve the final purchase and sale prices.",
                    color = LensMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun AddStockScreen(
    inspection: DeviceInspection,
    condition: String,
    onConditionChange: (String) -> Unit,
    purchasePrice: String,
    onPurchasePriceChange: (String) -> Unit,
    sellPrice: String,
    onSellPriceChange: (String) -> Unit,
    stockNumber: String,
    onStockNumberChange: (String) -> Unit,
    submit: () -> Unit,
    error: String,
    back: () -> Unit
) {
    LensScaffold(title = "Add to Catalogue", back = back) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DeviceIdentityCard(inspection)
            Text("Condition", color = LensMuted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Excellent", "Good", "Fair", "Poor", "Parts").forEach { option ->
                    val selected = condition == option
                    Surface(
                        onClick = { onConditionChange(option) },
                        color = if (selected) LensBlue else Color.White,
                        contentColor = if (selected) Color.White else LensText,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, if (selected) LensBlue else LensBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            option,
                            Modifier.padding(vertical = 9.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            OutlinedTextField(
                value = purchasePrice,
                onValueChange = onPurchasePriceChange,
                label = { Text("Purchase Price (A$)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = sellPrice,
                onValueChange = onSellPriceChange,
                label = { Text("Sell Price (A$)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = stockNumber,
                onValueChange = onStockNumberChange,
                label = { Text("Stock Number") },
                placeholder = { Text("Enter manually or scan barcode later") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = buildConditionNotes(inspection),
                onValueChange = {},
                readOnly = true,
                label = { Text("AI condition notes") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            if (error.isNotBlank()) ErrorCard(error)
            Button(
                onClick = submit,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LensBlue)
            ) { Text("Add to Catalogue", fontWeight = FontWeight.Black) }
            AiBoundary()
        }
    }
}

@Composable
private fun CompleteScreen(
    name: String,
    viewStock: () -> Unit,
    scanAnother: () -> Unit
) {
    LensScaffold(title = "MORLEY BUYS", back = null) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(shape = CircleShape, color = LensSuccess, modifier = Modifier.size(92.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(54.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Device Added!", color = LensText, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text(
                "${name.ifBlank { "The device" }} has been added to your stock.",
                color = LensMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 28.dp)
            )
            Button(onClick = viewStock, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = LensBlue)) {
                Text("View in Stock", fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = scanAnother, modifier = Modifier.fillMaxWidth()) { Text("Scan Another Device") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LensScaffold(
    title: String,
    back: (() -> Unit)?,
    content: @Composable () -> Unit
) {
    Scaffold(
        containerColor = LensBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LensNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    if (back != null) {
                        IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                    }
                },
                title = {
                    Text(title, fontWeight = FontWeight.Black, fontSize = 19.sp)
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) { content() }
    }
}

@Composable
private fun AnnotatedPhoto(file: File, regions: List<DamageRegion>, modifier: Modifier = Modifier) {
    val bitmap = remember(file.absolutePath, file.lastModified()) {
        DeviceInspectionClient.decodeDisplayBitmap(file)
    }
    DisposableEffect(bitmap) {
        onDispose { if (!bitmap.isRecycled) bitmap.recycle() }
    }
    Box(
        modifier.background(Color(0xFFE8EDF3), RoundedCornerShape(14.dp))
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Device photo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        Canvas(Modifier.fillMaxSize()) {
            val scale = min(size.width / bitmap.width.toFloat(), size.height / bitmap.height.toFloat())
            val renderedWidth = bitmap.width * scale
            val renderedHeight = bitmap.height * scale
            val left = (size.width - renderedWidth) / 2f
            val top = (size.height - renderedHeight) / 2f
            regions.forEach { region ->
                val x = left + region.x * renderedWidth
                val y = top + region.y * renderedHeight
                val w = region.width * renderedWidth
                val h = region.height * renderedHeight
                drawOval(
                    color = LensDanger,
                    topLeft = Offset(x, y),
                    size = Size(w, h),
                    style = Stroke(width = 4.dp.toPx())
                )
                drawCircle(
                    color = LensDanger,
                    radius = 5.dp.toPx(),
                    center = Offset(x + w, y)
                )
            }
        }
    }
}

@Composable
private fun PlainPhoto(file: File, modifier: Modifier = Modifier) {
    val bitmap = remember(file.absolutePath, file.lastModified()) {
        DeviceInspectionClient.decodeDisplayBitmap(file, 1200)
    }
    DisposableEffect(bitmap) {
        onDispose { if (!bitmap.isRecycled) bitmap.recycle() }
    }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Captured device photo",
        contentScale = ContentScale.Fit,
        modifier = modifier.background(Color(0xFFE8EDF3), RoundedCornerShape(14.dp))
    )
}

@Composable
private fun DeviceIdentityCard(inspection: DeviceInspection) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, LensBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(inspection.displayName, color = LensText, fontWeight = FontWeight.Black, fontSize = 20.sp)
            val model = inspection.catalogueMatch?.modelNumber.orEmpty().ifBlank { inspection.modelNumber }
            if (model.isNotBlank()) Text(model, color = LensText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            val detail = listOf(inspection.storage, inspection.colour).filter { it.isNotBlank() }.joinToString(" • ")
            if (detail.isNotBlank()) Text(detail, color = LensMuted, fontSize = 12.sp)
            if (inspection.catalogueMatch != null) {
                Surface(color = LensSuccessSoft, shape = RoundedCornerShape(999.dp)) {
                    Text(
                        "✓ Match found in catalogue",
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = Color(0xFF0C7044),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            } else {
                Surface(color = Color(0xFFFFF4D9), shape = RoundedCornerShape(999.dp)) {
                    Text(
                        "Visual identification — verify model",
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = Color(0xFF7D5B08),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, LensBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp), content = content)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = LensMuted, fontSize = 12.sp, modifier = Modifier.weight(.9f))
        Text(value, color = LensText, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.4f))
    }
    HorizontalDivider(color = LensBorder.copy(alpha = .7f))
}

@Composable
private fun ConditionRow(label: String, detail: String, good: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = if (good) LensSuccess else LensDanger, modifier = Modifier.size(22.dp)) {
            Box(contentAlignment = Alignment.Center) { Text(if (good) "✓" else "!", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black) }
        }
        Spacer(Modifier.width(10.dp))
        Text(label, Modifier.weight(1f), color = LensText, fontWeight = FontWeight.Bold)
        Text(detail, color = if (good) LensMuted else LensDanger, fontSize = 12.sp)
    }
}

@Composable
private fun AnalysisCheck(text: String) {
    Row(Modifier.fillMaxWidth(.88f), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = LensSuccess, modifier = Modifier.size(20.dp)) {
            Box(contentAlignment = Alignment.Center) { Text("✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black) }
        }
        Spacer(Modifier.width(10.dp))
        Text(text, color = LensText, fontSize = 14.sp)
    }
}

@Composable
private fun FlowDamageChips(regions: List<DamageRegion>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        regions.take(2).forEach { region ->
            Surface(
                color = LensDangerSoft,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, LensDanger.copy(alpha = .2f)),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "✕ ${region.label}",
                    Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    color = LensDanger,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DamageChip(text: String) {
    Surface(color = LensDangerSoft, shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, LensDanger.copy(alpha = .2f))) {
        Text("✕ $text", Modifier.padding(horizontal = 10.dp, vertical = 8.dp), color = LensDanger, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun PriceSummary(title: String, value: String, subtitle: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, LensBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = LensMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(value, color = LensText, fontSize = 27.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = LensMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun InfoCard(title: String, detail: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, LensBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = LensText, fontWeight = FontWeight.Black)
            Text(detail, color = LensMuted, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = LensDangerSoft), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Text(message, Modifier.padding(14.dp), color = Color(0xFF8E201E), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AiBoundary() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF2FC)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "ⓘ AI-powered visual estimate. Damage circles show the areas the model associated with visible damage. Verify the device manually before buying, pricing or grading it.",
            Modifier.padding(12.dp),
            color = LensBlueDark,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    }
}

private fun gradeLabel(grade: String): String = when (grade.uppercase()) {
    "A" -> "Excellent"
    "B" -> "Good"
    "C" -> "Fair"
    "D" -> "Poor"
    "PARTS" -> "Parts"
    else -> "Fair"
}

private fun buildConditionNotes(inspection: DeviceInspection): String {
    val damage = when {
        inspection.damageRegions.isNotEmpty() -> inspection.damageRegions.joinToString("; ") { it.label }
        inspection.damageFlags.isNotEmpty() -> inspection.damageFlags.joinToString("; ")
        else -> "No clear visible damage detected"
    }
    return "$damage. ${inspection.conditionSummary}".trim()
}

private fun moneyInput(value: String): String =
    value.filter { it.isDigit() || it == '.' }.let { clean ->
        val firstDot = clean.indexOf('.')
        if (firstDot < 0) clean.take(8)
        else clean.substring(0, firstDot + 1) + clean.substring(firstDot + 1).replace(".", "").take(2)
    }

private fun roundToFive(value: Double): Double = round(value / 5.0) * 5.0
