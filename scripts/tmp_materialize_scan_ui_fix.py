from pathlib import Path

lens_path = Path('android/app/src/main/java/com/buysloans/hub/DeviceLensActivity.kt')
review_path = Path('android/app/src/main/java/com/buysloans/hub/MorleyVisionReviewUi.kt')
lens = lens_path.read_text(encoding='utf-8')
review = review_path.read_text(encoding='utf-8')

def once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected one anchor, found {count}')
    return text.replace(old, new, 1)

lens = once(
    lens,
    'import androidx.compose.foundation.layout.height\nimport androidx.compose.foundation.layout.padding\n',
    'import androidx.compose.foundation.layout.height\nimport androidx.compose.foundation.layout.navigationBarsPadding\nimport androidx.compose.foundation.layout.padding\n',
    'navigationBarsPadding import',
)

old_header = '''        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { back?.invoke() ?: close() }) {
                Icon(if (back != null) Icons.Default.ArrowBack else Icons.Default.Close, null, tint = Color.White)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
'''
new_header = '''        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (back != null) {
                IconButton(onClick = back) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
'''
lens = once(lens, old_header, new_header, 'single close header')

lens = once(
    lens,
    'Modifier.align(Alignment.Center).fillMaxWidth(.78f).aspectRatio(.68f)',
    'Modifier.align(Alignment.Center).fillMaxWidth(.78f).aspectRatio(.82f)',
    'camera frame ratio',
)
lens = once(
    lens,
    'Modifier.align(Alignment.BottomCenter).padding(bottom = 118.dp)',
    'Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 126.dp)',
    'mode controls inset',
)
lens = once(
    lens,
    'modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 132.dp, start = 18.dp, end = 18.dp)',
    'modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 140.dp, start = 18.dp, end = 18.dp)',
    'camera error inset',
)
lens = once(
    lens,
    'Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 26.dp, vertical = 28.dp)',
    'Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 26.dp, top = 8.dp, bottom = 16.dp)',
    'capture controls inset',
)

start = lens.index('@Composable\nprivate fun VisionReviewScreen(')
end = lens.index('@Composable\nprivate fun ResultsScreen(', start)
replacement = '''@Composable
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
        if (state.requiresCaptureRecovery) {
            var detailsExpanded by remember(state.inspection) { mutableStateOf(false) }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().height(132.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnnotatedPhoto(frontPhoto, inspection.damageRegions.filter { it.photoIndex == 1 }, Modifier.weight(1f).fillMaxHeight())
                    AnnotatedPhoto(backPhoto, inspection.damageRegions.filter { it.photoIndex == 2 }, Modifier.weight(1f).fillMaxHeight())
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = LensDangerSoft),
                    border = BorderStroke(1.dp, LensDanger.copy(alpha = .18f))
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("We couldn't verify this device", color = Color(0xFF8E211F), fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text(
                            "The photos do not provide enough reliable evidence to verify the device identity and condition. Retake clear front and back photos before continuing.",
                            color = Color(0xFF8E211F),
                            fontSize = 13.sp
                        )
                    }
                }

                if (error.isNotBlank()) ErrorCard(error)

                Button(
                    onClick = retake,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LensBlue)
                ) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Retake clear photos", fontWeight = FontWeight.Black)
                }

                TextButton(onClick = { detailsExpanded = !detailsExpanded }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (detailsExpanded) "Hide scan details" else "Why the scan was blocked", fontWeight = FontWeight.Bold)
                }

                if (detailsExpanded) {
                    val recoveryReasons = mutableListOf<String>().apply {
                        if (!state.identityVerified) add("Device identity could not be verified.")
                        addAll(state.qualityWarnings)
                        addAll(state.consistencyWarnings)
                    }.distinct()
                    InfoCard(
                        "Scan details",
                        recoveryReasons.joinToString(" • ").ifBlank { "The scan does not contain enough verified evidence to continue." }
                    )
                }

                Text(
                    "Pricing and stock entry remain locked until a reliable scan passes staff verification.",
                    color = LensMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnnotatedPhoto(frontPhoto, inspection.damageRegions.filter { it.photoIndex == 1 }, Modifier.weight(1f).aspectRatio(.90f))
                    AnnotatedPhoto(backPhoto, inspection.damageRegions.filter { it.photoIndex == 2 }, Modifier.weight(1f).aspectRatio(.90f))
                }
                MorleyVisionReviewPanel(state = state, onDamageDecision = onDamageDecision, modifier = Modifier.fillMaxWidth())
                if (!state.canCompleteStaffReview) {
                    InfoCard(
                        "Verification required",
                        "Complete the remaining staff checks before continuing. Pricing and stock entry stay locked until review is complete."
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
}

'''
lens = lens[:start] + replacement + lens[end:]

review = once(
    review,
    '''    val hasBlockingEvidenceGap: Boolean
        get() = !identityVerified || qualityWarnings.isNotEmpty() || consistencyWarnings.isNotEmpty()

    val unresolvedDamageCount: Int
''',
    '''    val hasBlockingEvidenceGap: Boolean
        get() = !identityVerified || qualityWarnings.isNotEmpty() || consistencyWarnings.isNotEmpty()

    val requiresCaptureRecovery: Boolean
        get() = hasBlockingEvidenceGap

    val unresolvedDamageCount: Int
''',
    'capture recovery policy',
)

lens_path.write_text(lens, encoding='utf-8')
review_path.write_text(review, encoding='utf-8')
