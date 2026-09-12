package com.buysloans.hub

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

private val LensBlue = Color(0xFF0878F9)
private val LensBlueDark = Color(0xFF063868)
private val LensNavy = Color(0xFF032A4F)
private val LensBg = Color(0xFFF3F6FA)
private val LensText = Color(0xFF102033)
private val LensMuted = Color(0xFF667487)
private val LensBorder = Color(0xFFD8E2EE)
private val LensDanger = Color(0xFFE53935)
private val LensDangerSoft = Color(0xFFFFE8E7)
private val LensSuccess = Color(0xFF17B66A)
private val LensSuccessSoft = Color(0xFFE7F8EF)

private enum class LensStep {
    CAPTURE_FRONT,
    CAPTURE_BACK,
    ANALYSING,
    REVIEW,
    RESULTS,
    DAMAGE,
    DETAILS,
    CONDITION,
    PRICING,
    REPAIR_DECISION,
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
    var reviewState by remember { mutableStateOf<MorleyVisionReviewState?>(null) }
    var pricing by remember { mutableStateOf<LivePricingResult?>(null) }
    var pricingBusy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf("") }
    var sellPrice by remember { mutableStateOf("") }
    var stockNumber by remember { mutableStateOf("") }
    var confirmedCondition by remember { mutableStateOf("") }
    var analysisAttempt by remember { mutableStateOf(0) }
    var repairDecision by remember { mutableStateOf<MorleyRepairDisposition?>(null) }
    var repairDecisionConfirmed by remember { mutableStateOf(false) }
    var repairedResale by remember { mutableStateOf("") }
    var repairCost by remember { mutableStateOf("") }
    var partsRecovery by remember { mutableStateOf("") }
    var repairDays by remember { mutableStateOf("") }

    fun clearPhotos() {
        runCatching { frontPhoto?.delete() }
        runCatching { backPhoto?.delete() }
        frontPhoto = null
        backPhoto = null
    }

    fun reset() {
        clearPhotos()
        inspection = null
        reviewState = null
        pricing = null
        pricingBusy = false
        error = ""
        purchasePrice = ""
        sellPrice = ""
        stockNumber = ""
        confirmedCondition = ""
        analysisAttempt = 0
        repairDecision = null
        repairDecisionConfirmed = false
        repairedResale = ""
        repairCost = ""
        partsRecovery = ""
        repairDays = ""
        step = LensStep.CAPTURE_FRONT
    }

    fun requireStaffReview(): Boolean {
        if (reviewState?.canCompleteStaffReview == true) return true
        error = "Complete staff verification before continuing to pricing or stock entry."
        step = LensStep.REVIEW
        return false
    }

    fun openRepairDecision() {
        if (!requireStaffReview()) return
        error = ""
        repairDecisionConfirmed = false
        step = LensStep.REPAIR_DECISION
    }

    fun requireRepairDecision(): Boolean {
        if (repairDecisionConfirmed && repairDecision != null) return true
        error = "Repair-or-Buy must be confirmed before adding stock."
        step = LensStep.REPAIR_DECISION
        return false
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
            runCatching { DeviceInspectionClient.inspect(context, frontPhoto!!, backPhoto!!) }
                .onSuccess {
                    inspection = it
                    reviewState = MorleyVisionReviewPolicy.from(it, null)
                    confirmedCondition = gradeLabel(it.conditionGrade)
                    step = LensStep.REVIEW
                }
                .onFailure {
                    error = it.message ?: "The two photos could not be analysed."
                }
        }
    }

    fun openPricing() {
        val current = inspection ?: return
        if (!requireStaffReview()) return
        step = LensStep.PRICING
        if (pricing != null || pricingBusy) return
        pricingBusy = true
        error = ""
        scope.launch {
            runCatching { DeviceInspectionClient.livePricing(context, current) }
                .onSuccess {
                    pricing = it
                    reviewState = MorleyVisionReviewPolicy.from(current, it).copy(
                        damageReviews = reviewState?.damageReviews ?: MorleyVisionReviewPolicy.from(current, it).damageReviews
                    )
                    it.conditionAdjustedResale?.let { value ->
                        if (sellPrice.isBlank()) sellPrice = roundToFive(value).toInt().toString()
                        if (repairedResale.isBlank()) repairedResale = roundToFive(value).toInt().toString()
                    }
                }
                .onFailure { error = "Live pricing unavailable: ${it.message ?: "unknown error"}" }
            pricingBusy = false
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
                    analysisAttempt += 1
                }
            },
            retake = {
                clearPhotos()
                error = ""
                step = LensStep.CAPTURE_FRONT
            },
            cancel = close
        )

        LensStep.REVIEW -> inspection?.let { result ->
            val state = reviewState ?: MorleyVisionReviewPolicy.from(result, pricing).also { reviewState = it }
            VisionReviewScreen(
                inspection = result,
                state = state,
                frontPhoto = frontPhoto!!,
                backPhoto = backPhoto!!,
                error = error,
                onDamageDecision = { regionIndex, decision ->
                    reviewState = MorleyVisionReviewPolicy.decideDamage(
                        reviewState ?: MorleyVisionReviewPolicy.from(result, pricing),
                        regionIndex,
                        decision
                    )
                    error = ""
                },
                continueToResults = {
                    if (reviewState?.canCompleteStaffReview == true) {
                        error = ""
                        step = LensStep.RESULTS
                    }
                },
                retake = {
                    clearPhotos()
                    inspection = null
                    reviewState = null
                    pricing = null
                    error = ""
                    step = LensStep.CAPTURE_FRONT
                },
                close = close
            )
        } ?: reset()

        LensStep.RESULTS -> inspection?.let { result ->
            ResultsScreen(
                inspection = result,
                frontPhoto = frontPhoto!!,
                backPhoto = backPhoto!!,
                damageDetails = { step = LensStep.DAMAGE },
                deviceDetails = { step = LensStep.DETAILS },
                condition = { step = LensStep.CONDITION },
                pricing = ::openPricing,
                addStock = ::openRepairDecision,
                retake = {
                    clearPhotos()
                    inspection = null
                    reviewState = null
                    pricing = null
                    step = LensStep.CAPTURE_FRONT
                },
                close = close
            )
        } ?: reset()

        LensStep.DAMAGE -> inspection?.let { result ->
            DamageDetailsScreen(result, frontPhoto!!, backPhoto!!) { step = LensStep.RESULTS }
        } ?: reset()

        LensStep.DETAILS -> inspection?.let { result ->
            DeviceDetailsScreen(result, pricing = ::openPricing, addStock = ::openRepairDecision) {
                step = LensStep.RESULTS
            }
        } ?: reset()

        LensStep.CONDITION -> inspection?.let { result ->
            ConditionSummaryScreen(result, frontPhoto!!, backPhoto!!, pricing = ::openPricing) {
                step = LensStep.RESULTS
            }
        } ?: reset()

        LensStep.PRICING -> inspection?.let { result ->
            PricingScreen(
                inspection = result,
                pricing = pricing,
                loading = pricingBusy,
                error = error,
                useSuggested = {
                    if (!requireStaffReview()) return@PricingScreen
                    pricing?.conditionAdjustedResale?.let { value ->
                        sellPrice = roundToFive(value).toInt().toString()
                        if (repairedResale.isBlank()) repairedResale = sellPrice
                    }
                    openRepairDecision()
                },
                retry = {
                    pricing = null
                    error = ""
                    openPricing()
                },
                back = { step = LensStep.RESULTS }
            )
        } ?: reset()

        LensStep.REPAIR_DECISION -> inspection?.let { result ->
            val asIsResale = pricing?.conditionAdjustedResale ?: sellPrice.toDoubleOrNull()
            val proposal = MorleyRepairDecisionPolicy.evaluate(
                buyCost = purchasePrice.toDoubleOrNull(),
                resaleAsIs = asIsResale,
                resaleAfterRepair = repairedResale.toDoubleOrNull(),
                repairCost = repairCost.toDoubleOrNull(),
                partsRecoveryValue = partsRecovery.toDoubleOrNull(),
                partsProcessingCost = 0.0,
                minMargin = 0.0,
                repairDays = repairDays.toDoubleOrNull()
            ).copy(staffDecision = repairDecision, confirmed = repairDecisionConfirmed)
            ScanScaffold("Repair or Buy", back = { step = LensStep.RESULTS }) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DeviceIdentityCard(result)
                    if (error.isNotBlank()) ErrorCard(error)
                    MorleyRepairDecisionPanel(
                        state = proposal,
                        buyCost = purchasePrice,
                        onBuyCostChange = { purchasePrice = moneyInput(it); repairDecisionConfirmed = false; error = "" },
                        repairedResale = repairedResale,
                        onRepairedResaleChange = { repairedResale = moneyInput(it); repairDecisionConfirmed = false; error = "" },
                        repairCost = repairCost,
                        onRepairCostChange = { repairCost = moneyInput(it); repairDecisionConfirmed = false; error = "" },
                        partsRecovery = partsRecovery,
                        onPartsRecoveryChange = { partsRecovery = moneyInput(it); repairDecisionConfirmed = false; error = "" },
                        repairDays = repairDays,
                        onRepairDaysChange = { repairDays = moneyInput(it); repairDecisionConfirmed = false; error = "" },
                        onSelect = { repairDecision = it; repairDecisionConfirmed = false; error = "" },
                        onConfirm = {
                            if (repairDecision != null) {
                                repairDecisionConfirmed = true
                                error = ""
                                step = LensStep.ADD_STOCK
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    AiBoundary()
                }
            }
        } ?: reset()

        LensStep.ADD_STOCK -> inspection?.let { result ->
            AddStockScreen(
                inspection = result,
                condition = confirmedCondition.ifBlank { gradeLabel(result.conditionGrade) },
                onConditionChange = { confirmedCondition = it },
                purchasePrice = purchasePrice,
                onPurchasePriceChange = { purchasePrice = moneyInput(it); repairDecisionConfirmed = false },
                sellPrice = sellPrice,
                onSellPriceChange = { sellPrice = moneyInput(it); repairDecisionConfirmed = false },
                stockNumber = stockNumber,
                onStockNumberChange = { stockNumber = it.take(80) },
                submit = {
                    if (!requireStaffReview()) return@AddStockScreen
                    if (!requireRepairDecision()) return@AddStockScreen
                    error = ""
                    runCatching {
                        WorkspaceStore.addInventory(
                            context = context,
                            name = result.displayName,
                            barcode = stockNumber,
                            cost = purchasePrice.toDoubleOrNull() ?: 0.0,
                            resale = sellPrice.toDoubleOrNull() ?: 0.0,
                            quantity = 1
                        )
                    }.onSuccess { step = LensStep.COMPLETE }
                        .onFailure { error = it.message ?: "The device could not be added." }
                },
                error = error,
                back = { step = LensStep.REPAIR_DECISION }
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
                Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(54.dp))
                Text("Camera access", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text(
                    "Morley Buys needs camera access to take the two photos used for device and damage analysis.",
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
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var captureBusy by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf("") }
    var torchEnabled by remember { mutableStateOf(false) }
    val mainExecutor: Executor = ContextCompat.getMainExecutor(context)

    fun importGallery(uri: Uri) {
        captureBusy = true
        cameraError = ""
        runCatching {
            val file = File(context.cacheDir, "morley-device-${System.currentTimeMillis()}-$side-gallery.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: error("The selected photo could not be opened.")
            require(file.length() > 0) { "The selected photo was empty." }
            file
        }.onSuccess {
            captureBusy = false
            cameraProvider?.unbindAll()
            onCaptured(it)
        }.onFailure {
            captureBusy = false
            cameraError = it.message ?: "The selected photo could not be imported."
        }
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(::importGallery)
    }

    DisposableEffect(Unit) {
        onDispose {
            boundCamera?.cameraControl?.enableTorch(false)
            cameraProvider?.unbindAll()
        }
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
                            val preview = Preview.Builder().build().also { it.setSurfaceProvider(surfaceProvider) }
                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                                .build()
                            provider.unbindAll()
                            val camera = provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                capture
                            )
                            cameraProvider = provider
                            boundCamera = camera
                            imageCapture = capture
                        }.onFailure {
                            cameraError = it.message ?: "Camera could not start."
                        }
                    }, mainExecutor)
                }
            }
        )

        Box(
            Modifier.fillMaxWidth().height(128.dp).align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = .42f))
        )

        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { back?.invoke() ?: close() }) {
                Icon(if (back != null) Icons.Default.ArrowBack else Icons.Default.Close, null, tint = Color.White)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Take Photos", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(instruction, color = Color.White.copy(alpha = .88f), fontSize = 13.sp)
            }
            IconButton(onClick = close) { Icon(Icons.Default.Close, null, tint = Color.White) }
        }

        Box(
            Modifier.align(Alignment.Center).fillMaxWidth(.78f).aspectRatio(.68f)
        ) {
            CameraCorners()
        }

        if (cameraError.isNotBlank()) {
            Surface(
                color = LensDanger.copy(alpha = .92f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 132.dp, start = 18.dp, end = 18.dp)
            ) {
                Text(cameraError, Modifier.padding(10.dp), color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }

        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 26.dp, vertical = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CaptureSideControl(
                icon = { Text("▣", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black) },
                label = "Gallery",
                enabled = !captureBusy,
                onClick = { gallery.launch("image/*") }
            )

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
                                boundCamera?.cameraControl?.enableTorch(false)
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
                border = BorderStroke(5.dp, Color.White.copy(alpha = .50f)),
                modifier = Modifier.size(78.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (captureBusy) {
                        CircularProgressIndicator(Modifier.size(34.dp), color = LensBlue, strokeWidth = 3.dp)
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            border = BorderStroke(2.dp, Color.Black.copy(alpha = .20f)),
                            modifier = Modifier.size(60.dp)
                        ) {}
                    }
                }
            }

            CaptureSideControl(
                icon = {
                    Icon(
                        if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        null,
                        tint = Color.White
                    )
                },
                label = "Flash",
                enabled = boundCamera?.cameraInfo?.hasFlashUnit() == true,
                onClick = {
                    val next = !torchEnabled
                    boundCamera?.cameraControl?.enableTorch(next)
                    torchEnabled = next
                }
            )
        }
    }
}

@Composable
private fun CaptureSideControl(
    icon: @Composable () -> Unit,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(72.dp).clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(shape = CircleShape, color = Color.Black.copy(alpha = .54f), modifier = Modifier.size(46.dp)) {
            Box(contentAlignment = Alignment.Center) { icon() }
        }
        Text(label, color = Color.White.copy(alpha = if (enabled) 1f else .48f), fontSize = 12.sp)
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
    cancel: () -> Unit
) {
    ScanScaffold("Analysing Images", cancel) {
        Column(
            Modifier.fillMaxSize().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                frontPhoto?.let { PlainPhoto(it, Modifier.weight(1f).aspectRatio(.76f)) }
                backPhoto?.let { PlainPhoto(it, Modifier.weight(1f).aspectRatio(.76f)) }
            }

            if (error.isBlank()) {
                Spacer(Modifier.height(2.dp))
                Surface(shape = CircleShape, color = Color.White, border = BorderStroke(1.dp, LensBorder), modifier = Modifier.size(58.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CameraAlt, null, tint = LensBlue, modifier = Modifier.size(30.dp))
                    }
                }
                Text("Morley Vision is analysing both photos", color = LensText, fontWeight = FontWeight.Black, fontSize = 18.sp)
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = LensBlue,
                    trackColor = LensBorder
                )
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    AnalysisCheck("Identifying device model")
                    AnalysisCheck("Checking condition")
                    AnalysisCheck("Detecting damage or cracks")
                    AnalysisCheck("Analysing visual details")
                    AnalysisCheck("Comparing with catalogue")
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "AI-powered visual estimate. Staff must verify the model, condition and damage before buying or adding stock.",
                    color = LensMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
                OutlinedButton(onClick = cancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            } else {
                ErrorCard(error)
                Button(onClick = retry, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = LensBlue)) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Retry Analysis", fontWeight = FontWeight.Black)
                }
                OutlinedButton(onClick = retake, modifier = Modifier.fillMaxWidth()) { Text("Retake Photos") }
                TextButton(onClick = cancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun VisionReviewScreen(
    inspection: DeviceInspection,
    state: MorleyVisionReviewState,
    frontPhoto: File,
    backPhoto: File,
    error: String,
    onDamageDecision: (Int, VisionStaffDecision) -> Unit,
    continueToResults: () -> Unit,
    retake: () -> Unit,
    close: () -> Unit
) {
    ScanScaffold("Staff Verification", close) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnnotatedPhoto(frontPhoto, inspection.damageRegions.filter { it.photoIndex == 1 }, Modifier.weight(1f).aspectRatio(.78f))
                AnnotatedPhoto(backPhoto, inspection.damageRegions.filter { it.photoIndex == 2 }, Modifier.weight(1f).aspectRatio(.78f))
            }
            MorleyVisionReviewPanel(
                state = state,
                onDamageDecision = onDamageDecision,
                modifier = Modifier.fillMaxWidth()
            )
            if (!state.canCompleteStaffReview) {
                InfoCard(
                    "Verification required",
                    "Resolve every damage decision and retake photos when identity, storage, photo quality or cross-photo consistency is not verified. Pricing and stock entry remain locked until this review is complete."
                )
            }
            if (error.isNotBlank()) ErrorCard(error)
            Button(
                onClick = continueToResults,
                enabled = state.canCompleteStaffReview,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LensBlue)
            ) {
                Text("Continue to Results", fontWeight = FontWeight.Black)
            }
            OutlinedButton(onClick = retake, modifier = Modifier.fillMaxWidth()) { Text("Retake Photos") }
            AiBoundary()
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
    ScanScaffold("Analysis Results", close) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnnotatedPhoto(frontPhoto, inspection.damageRegions.filter { it.photoIndex == 1 }, Modifier.weight(1f).aspectRatio(.78f))
                AnnotatedPhoto(backPhoto, inspection.damageRegions.filter { it.photoIndex == 2 }, Modifier.weight(1f).aspectRatio(.78f))
            }

            DamageStatusCard(inspection)
            DeviceIdentityCard(inspection)
            DamageChips(inspection)

            Button(onClick = deviceDetails, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = LensBlue)) {
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
private fun DamageStatusCard(inspection: DeviceInspection) {
    val count = max(inspection.damageRegions.size, inspection.damageFlags.size)
    if (count > 0) {
        Card(
            colors = CardDefaults.cardColors(containerColor = LensDangerSoft),
            border = BorderStroke(1.dp, LensDanger.copy(alpha = .20f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = CircleShape, color = LensDanger, modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("!", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp) }
                }
                Column {
                    Text("Damage Detected", color = Color(0xFF98211F), fontWeight = FontWeight.Black, fontSize = 17.sp)
                    Text("$count area${if (count == 1) "" else "s"} of visible damage found", color = Color(0xFF98211F), fontSize = 12.sp)
                }
            }
        }
    } else {
        Card(colors = CardDefaults.cardColors(containerColor = LensSuccessSoft), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Icon(Icons.Default.Check, null, tint = LensSuccess)
                Text("No clear visible damage detected in these photos.", color = Color(0xFF12633E), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DamageChips(inspection: DeviceInspection) {
    val labels = if (inspection.damageRegions.isNotEmpty()) inspection.damageRegions.map { it.label } else inspection.damageFlags
    if (labels.isEmpty()) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.distinct().take(2).forEach { label ->
            Surface(
                color = LensDangerSoft,
                border = BorderStroke(1.dp, LensDanger.copy(alpha = .18f)),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("●  $label", Modifier.padding(horizontal = 10.dp, vertical = 8.dp), color = LensDanger, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
            }
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

    ScanScaffold("Damage Details", back) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnnotatedPhoto(activePhoto, regions, Modifier.fillMaxWidth().aspectRatio(.75f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { photoIndex = 1 }, modifier = Modifier.weight(1f), enabled = photoIndex != 1) { Text("Front") }
                OutlinedButton(onClick = { photoIndex = 2 }, modifier = Modifier.weight(1f), enabled = photoIndex != 2) { Text("Back") }
            }
            if (regions.isEmpty()) {
                InfoCard("No marked region on this photo", "No confident damage location was returned for this angle.")
            } else {
                regions.forEach { region ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF172331)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = LensDanger, modifier = Modifier.size(34.dp)) {
                                Box(contentAlignment = Alignment.Center) { Text("!", color = Color.White, fontWeight = FontWeight.Black) }
                            }
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
    ScanScaffold("Device Details", back) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            if (inspection.evidence.isNotEmpty()) InfoCard("Visible evidence", inspection.evidence.joinToString("\n• ", prefix = "• "))
            if (inspection.uncertainties.isNotEmpty()) InfoCard("Needs verification", inspection.uncertainties.joinToString("\n• ", prefix = "• "))
            Button(onClick = pricing, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = LensBlue)) {
                Text("Check Live Pricing", fontWeight = FontWeight.Black)
            }
            OutlinedButton(onClick = addStock, modifier = Modifier.fillMaxWidth()) { Text("Add to Stock") }
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
    back: () -> Unit
) {
    ScanScaffold("Condition Summary", back) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnnotatedPhoto(frontPhoto, inspection.damageRegions.filter { it.photoIndex == 1 }, Modifier.weight(1f).aspectRatio(.80f))
                AnnotatedPhoto(backPhoto, inspection.damageRegions.filter { it.photoIndex == 2 }, Modifier.weight(1f).aspectRatio(.80f))
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = if (inspection.damageFlags.isEmpty() && inspection.damageRegions.isEmpty()) LensSuccessSoft else LensDangerSoft),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        if (inspection.damageFlags.isEmpty() && inspection.damageRegions.isEmpty()) Icons.Default.Check else Icons.Default.Warning,
                        null,
                        tint = if (inspection.damageFlags.isEmpty() && inspection.damageRegions.isEmpty()) LensSuccess else LensDanger
                    )
                    Column {
                        Text("Device Condition", color = LensText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(gradeLabel(inspection.conditionGrade), color = LensText, fontWeight = FontWeight.Black, fontSize = 24.sp)
                        Text(inspection.conditionSummary, color = LensMuted, fontSize = 12.sp)
                    }
                }
            }

            val issueLabels = (inspection.damageRegions.map { it.label } + inspection.damageFlags).distinct()
            if (issueLabels.isNotEmpty()) {
                issueLabels.take(5).forEach { ConditionRow(it, "Issue detected", false) }
            } else {
                ConditionRow("Screen", "No major issues", true)
                ConditionRow("Back panel", "No major issues", true)
            }
            ConditionRow("Camera", "Verify during staff test", true)
            ConditionRow("Buttons", "Verify during staff test", true)
            ConditionRow("Overall", "Visual assessment complete", true)

            Button(onClick = pricing, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = LensBlue)) {
                Text("Live Price Check", fontWeight = FontWeight.Black)
            }
            AiBoundary()
        }
    }
}

@Composable
private fun ConditionRow(label: String, value: String, good: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = if (good) LensSuccessSoft else LensDangerSoft, modifier = Modifier.size(28.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(if (good) "✓" else "!", color = if (good) LensSuccess else LensDanger, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(label, Modifier.weight(1f), color = LensText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(value, color = if (good) LensMuted else LensDanger, fontSize = 12.sp)
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
    ScanScaffold("Live Price Check", back) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DeviceIdentityCard(inspection)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = LensBlue.copy(alpha = .10f), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                    Text("Buy Price", Modifier.padding(10.dp), color = LensBlue, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                }
                Surface(color = Color.White, border = BorderStroke(1.dp, LensBorder), shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                    Text("Sell Price", Modifier.padding(10.dp), color = LensMuted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }

            when {
                loading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = LensBlue, trackColor = LensBorder)
                    Text("Checking current Australian market evidence…", color = LensMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                pricing != null -> {
                    if (pricing.listings.isEmpty()) {
                        InfoCard("No confirmed listings", "No current Australian listing evidence was returned for this exact identity.")
                    } else {
                        pricing.listings.take(8).forEach { listing -> MarketRow(listing) }
                    }
                    pricing.usedMedian?.let { DetailRow("Used-market median", formatAud(it)) }
                    pricing.conditionAdjustedResale?.let { DetailRow("Condition-adjusted resale guide", formatAud(it)) }
                    if (inspection.damageRegions.isNotEmpty() || inspection.damageFlags.isNotEmpty()) {
                        InfoCard("Damage adjustment", "Visible damage may lower the final buy and resale price. Staff confirmation remains required.")
                    }
                    pricing.conditionAdjustedResale?.let {
                        Button(onClick = useSuggested, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = LensBlue)) {
                            Text("Use Suggested Sell Price", fontWeight = FontWeight.Black)
                        }
                    }
                }
                error.isNotBlank() -> {
                    ErrorCard(error)
                    Button(onClick = retry, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = LensBlue)) {
                        Text("Retry Live Price Check", fontWeight = FontWeight.Black)
                    }
                }
            }
            AiBoundary()
        }
    }
}

@Composable
private fun MarketRow(listing: MarketListing) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, LensBorder), shape = RoundedCornerShape(11.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(8.dp), color = LensBlue.copy(alpha = .10f), modifier = Modifier.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(listing.source.take(2).uppercase(), color = LensBlue, fontWeight = FontWeight.Black, fontSize = 11.sp) }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(listing.source, color = LensText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(listing.title, color = LensMuted, fontSize = 10.sp, maxLines = 1)
            }
            Text(formatAud(listing.price), color = LensText, fontWeight = FontWeight.Black, fontSize = 15.sp)
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
    ScanScaffold("Add to Catalogue", back) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DeviceIdentityCard(inspection)
            ConditionDropdown(condition, onConditionChange)
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
                placeholder = { Text("Auto or enter manually") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = buildInspectionNotes(inspection),
                onValueChange = {},
                readOnly = true,
                label = { Text("AI Notes") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            if (error.isNotBlank()) ErrorCard(error)
            Button(onClick = submit, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = LensBlue)) {
                Text("Add to Catalogue", fontWeight = FontWeight.Black)
            }
            AiBoundary()
        }
    }
}

@Composable
private fun ConditionDropdown(value: String, onValueChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val values = listOf("Excellent", "Good", "Fair", "Poor")
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("Condition", color = LensMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(value.ifBlank { "Select condition" }, Modifier.weight(1f), textAlign = TextAlign.Start, color = LensText)
                Text("⌄", color = LensMuted)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                values.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompleteScreen(name: String, viewStock: () -> Unit, scanAnother: () -> Unit) {
    ScanScaffold("Device Added", back = scanAnother) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(shape = CircleShape, color = LensSuccessSoft, modifier = Modifier.size(92.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, null, tint = LensSuccess, modifier = Modifier.size(48.dp)) }
            }
            Spacer(Modifier.height(18.dp))
            Text("Device Added", color = LensText, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text(name, color = LensMuted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Button(onClick = viewStock, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = LensBlue)) {
                Text("View in Stock", fontWeight = FontWeight.Black)
            }
            OutlinedButton(onClick = scanAnother, modifier = Modifier.fillMaxWidth()) { Text("Scan Another Device") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanScaffold(title: String, back: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(
        containerColor = LensBg,
        topBar = {
            TopAppBar(
                title = { Text(title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LensNavy)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding), content = content)
    }
}

@Composable
private fun DeviceIdentityCard(inspection: DeviceInspection) {
    val match = inspection.catalogueMatch
    val modelNumber = match?.modelNumber.orEmpty().ifBlank { inspection.modelNumber }
    val storage = inspection.storage.ifBlank { match?.storageOptions?.firstOrNull().orEmpty() }
    val colour = inspection.colour
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, LensBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(10.dp), color = LensBg, modifier = Modifier.size(58.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.CameraAlt, null, tint = LensBlueDark, modifier = Modifier.size(28.dp)) }
            }
            Column(Modifier.weight(1f)) {
                Text(inspection.displayName, color = LensText, fontSize = 17.sp, fontWeight = FontWeight.Black)
                if (modelNumber.isNotBlank()) Text(modelNumber, color = LensText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(listOf(storage, colour).filter { it.isNotBlank() }.joinToString(" • "), color = LensMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DetailCard(content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, LensBorder), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 5.dp), content = content)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    if (value.isBlank()) return
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), color = LensMuted, fontSize = 12.sp)
            Text(value, color = LensText, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End)
        }
        HorizontalDivider(color = LensBorder.copy(alpha = .65f))
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, LensBorder), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = LensText, fontWeight = FontWeight.Black, fontSize = 14.sp)
            Text(body, color = LensMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = LensDangerSoft), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(Icons.Default.Warning, null, tint = LensDanger)
            Text(message, color = Color(0xFF8E211F), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AnalysisCheck(label: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Surface(shape = CircleShape, color = LensSuccess, modifier = Modifier.size(22.dp)) {
            Box(contentAlignment = Alignment.Center) { Text("✓", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp) }
        }
        Text(label, color = LensText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PlainPhoto(file: File, modifier: Modifier = Modifier) {
    val bitmap = remember(file.absolutePath, file.lastModified()) { DeviceInspectionClient.decodeDisplayBitmap(file, 1200) }
    DisposableEffect(bitmap) { onDispose { bitmap.recycle() } }
    Surface(shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, LensBorder), color = Color.Black, modifier = modifier) {
        Image(bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
private fun AnnotatedPhoto(file: File, regions: List<DamageRegion>, modifier: Modifier = Modifier) {
    val bitmap = remember(file.absolutePath, file.lastModified()) { DeviceInspectionClient.decodeDisplayBitmap(file, 1600) }
    DisposableEffect(bitmap) { onDispose { bitmap.recycle() } }
    Surface(shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, LensBorder), color = Color.Black, modifier = modifier) {
        Box(Modifier.fillMaxSize()) {
            Image(bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            Canvas(Modifier.fillMaxSize()) {
                val scale = min(size.width / bitmap.width.toFloat(), size.height / bitmap.height.toFloat())
                val displayW = bitmap.width * scale
                val displayH = bitmap.height * scale
                val offsetX = (size.width - displayW) / 2f
                val offsetY = (size.height - displayH) / 2f
                regions.forEach { region ->
                    val left = offsetX + region.x.coerceIn(0f, 1f) * displayW
                    val top = offsetY + region.y.coerceIn(0f, 1f) * displayH
                    val width = max(18.dp.toPx(), region.width.coerceIn(.02f, 1f) * displayW)
                    val height = max(18.dp.toPx(), region.height.coerceIn(.02f, 1f) * displayH)
                    drawOval(
                        color = LensDanger,
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                        style = Stroke(width = 4.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
private fun AiBoundary() {
    Text(
        "AI-powered visual estimate. Verify the device, visible damage, condition and pricing before completing a purchase.",
        color = LensMuted,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

private fun buildInspectionNotes(inspection: DeviceInspection): String {
    val damage = (inspection.damageRegions.map { it.label } + inspection.damageFlags).distinct()
    return buildString {
        append("AI-assisted two-photo inspection")
        if (damage.isNotEmpty()) append(": ${damage.joinToString()}")
        else append(": no clear visible damage detected")
        if (inspection.conditionSummary.isNotBlank()) append(". ${inspection.conditionSummary}")
    }.take(600)
}

private fun gradeLabel(value: String): String = when (value.trim().uppercase()) {
    "A", "EXCELLENT" -> "Excellent"
    "B", "GOOD" -> "Good"
    "C", "FAIR" -> "Fair"
    "D", "POOR" -> "Poor"
    else -> value.trim().ifBlank { "Fair" }.replaceFirstChar { it.uppercase() }
}

private fun moneyInput(value: String): String {
    val cleaned = value.filter { it.isDigit() || it == '.' }
    val firstDot = cleaned.indexOf('.')
    return if (firstDot < 0) cleaned.take(8) else {
        val whole = cleaned.substring(0, firstDot).take(8)
        val decimals = cleaned.substring(firstDot + 1).replace(".", "").take(2)
        "$whole.$decimals"
    }
}

private fun roundToFive(value: Double): Double = round(value / 5.0) * 5.0

private fun formatAud(value: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "AU")).format(value)