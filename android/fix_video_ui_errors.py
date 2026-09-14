from pathlib import Path

root = Path(__file__).resolve().parent / 'app' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'hub'


def replace(path: Path, replacements: dict[str, str]) -> None:
    text = path.read_text(encoding='utf-8')
    updated = text
    for old, new in replacements.items():
        updated = updated.replace(old, new)
    if updated != text:
        path.write_text(updated, encoding='utf-8')

# The video review exposed two screens that still carried the pre-2.15.8 blue
# palette. Keep this migration presentation-only and deterministic.
for name in ('MenuFeatureActivity.kt', 'NotificationCentreActivity.kt'):
    replace(root / name, {
        'private val MFAccent = Color(0xFF16C7FF)': 'private val MFAccent = MorleyAccent',
        'private val MFStrong = Color(0xFF2684FF)': 'private val MFStrong = MorleyAccentStrong',
        'private val MFBg = Color(0xFF030712)': 'private val MFBg = MorleyBackground',
        'private val MFCard = Color(0xFF0B1528)': 'private val MFCard = MorleySurfaceRaised',
        'private val MFMuted = Color(0xFF8EA6C4)': 'private val MFMuted = MorleyTextSecondary',
        'private val NCAccent = Color(0xFF16C7FF)': 'private val NCAccent = MorleyAccent',
        'private val NCBg = Color(0xFF030712)': 'private val NCBg = MorleyBackground',
        'private val NCCard = Color(0xFF0B1528)': 'private val NCCard = MorleySurfaceRaised',
        'private val NCMuted = Color(0xFF8EA6C4)': 'private val NCMuted = MorleyTextSecondary',
        'android.graphics.Color.rgb(3, 7, 18)': 'android.graphics.Color.rgb(245, 248, 252)',
        'containerColor = Color(0xFF050B16)': 'containerColor = MorleySurfaceSoft',
        'containerColor = if (item.read) NCCard else Color(0xFF0E2038)': 'containerColor = if (item.read) NCCard else MorleyAccentSoft',
        'if (item.body.isNotBlank()) Text(item.body, color = Color.White)': 'if (item.body.isNotBlank()) Text(item.body, color = MorleyTextPrimary)',
        'colors = ButtonDefaults.buttonColors(containerColor = MFStrong)': 'colors = ButtonDefaults.buttonColors(containerColor = MorleyAccentStrong, contentColor = Color.White)',
    })

replace(root / 'MainActivity.kt', {
    'contentColor=if(grade==g)Color(0xFF06251B) else Color.White':
        'contentColor=if(grade==g)Color.White else MorleyTextPrimary',
})

# Keep Smart Workspace actions readable, canonicalise Seller Ask copy, and give
# the Quick Deal verdict card enough bottom breathing room on compact phones.
replace(root / 'SmartWorkspaceSection.kt', {
    'colors = ButtonDefaults.buttonColors(containerColor = SWStrong, contentColor = MorleyTextPrimary)':
        'colors = ButtonDefaults.buttonColors(containerColor = SWStrong, contentColor = androidx.compose.ui.graphics.Color.White)',
    'label = { Text("Seller asking price") }': 'label = { Text("Seller Ask") }',
    'Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {\n                        Text(verdict.first':
        'Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {\n                        Text(verdict.first',
    'if (headroom != null) Text(if (headroom >= 0) "${swMoney(headroom)} below max buy" else "${swMoney(-headroom)} above max buy", color = if (headroom >= 0) SWGood else SWWarn, fontSize = 11.sp)':
        'if (headroom != null) Text(if (headroom >= 0) "${swMoney(headroom)} below max buy" else "${swMoney(-headroom)} above max buy", color = if (headroom >= 0) SWGood else SWWarn, fontSize = 11.sp, modifier = Modifier.padding(bottom = 2.dp))',
})

replace(root / 'TestBuyActivity.kt', {
    'labelColor = Color.White.copy(alpha = .86f),': 'labelColor = MorleyTextPrimary,',
    'selectedLabelColor = Color.White': 'selectedLabelColor = MorleyTextPrimary',
    'labelColor = Color(0xFF1C2B26),': 'labelColor = MorleyTextPrimary,',
    'selectedLabelColor = Color(0xFF1C2B26)': 'selectedLabelColor = MorleyTextPrimary',
    'labelColor = Color(0xFF102A43),': 'labelColor = MorleyTextPrimary,',
    'selectedLabelColor = Color(0xFF102A43)': 'selectedLabelColor = MorleyTextPrimary',
})

history = root / 'ValuationHistoryActivity.kt'
replace(history, {
    'import androidx.compose.foundation.BorderStroke\n': 'import androidx.compose.foundation.BorderStroke\nimport androidx.compose.foundation.background\n',
    'MaterialTheme(colorScheme=lightColorScheme(primary=HistAccent,secondary=HistStrong,background=HistBg,surface=HistCard)){': 'MaterialTheme(colorScheme=lightColorScheme(primary=HistAccent,secondary=HistStrong,background=HistBg,surface=Color.White,onBackground=MorleyTextPrimary,onSurface=MorleyTextPrimary)){',
    'Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){': 'Column(Modifier.fillMaxSize().background(HistBg).verticalScroll(rememberScrollState()).padding(horizontal=18.dp,vertical=20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){',
    'Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){listOf("all","watch","quoted","bought","sold","passed").forEach{s->FilterChip(selected=filter==s,onClick={filter=s},label={Text(s.replaceFirstChar{it.uppercase()},fontSize=9.sp)})}}': 'Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("all","watch","quoted","bought","sold","passed").forEach{s->FilterChip(selected=filter==s,onClick={filter=s},modifier=Modifier.weight(1f),label={Text(s.replaceFirstChar{it.uppercase()},fontSize=9.sp,maxLines=1,softWrap=false)},colors=FilterChipDefaults.filterChipColors(containerColor=Color.White,labelColor=MorleyTextSecondary,selectedContainerColor=MorleyAccentSoft,selectedLabelColor=HistStrong),border=FilterChipDefaults.filterChipBorder(enabled=true,selected=filter==s,borderColor=MorleyBorder,selectedBorderColor=HistAccent,borderWidth=1.dp,selectedBorderWidth=1.dp))}}',
    'if(!loading&&shown.isEmpty())Card(colors=CardDefaults.cardColors(containerColor=HistCard),border=BorderStroke(1.dp,HistAccent.copy(alpha=.18f)),shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth()){Text(if(search.isBlank()&&filter=="all")"No saved valuations yet." else "No valuations match this view.",Modifier.padding(18.dp),color=HistMuted)}': 'if(!loading&&shown.isEmpty())Card(colors=CardDefaults.cardColors(containerColor=Color.White),border=BorderStroke(1.dp,MorleyBorder),shape=RoundedCornerShape(24.dp),modifier=Modifier.fillMaxWidth()){Column(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=34.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text(if(search.isBlank()&&filter=="all")"No saved valuations yet." else "No valuations match this view.",fontSize=18.sp,fontWeight=FontWeight.Black,color=MorleyTextPrimary);Text(if(search.isBlank()&&filter=="all")"Your saved valuations and deals will appear here." else "Try a different status or search term.",color=HistMuted);if(search.isBlank()&&filter=="all")Text("Get started by saving your first valuation.",color=HistAccent,fontWeight=FontWeight.Bold)}}',
    'label={Text("Seller asking price")}': 'label={Text("Seller Ask")}',
})

# Approved Morley AI Scan Device presentation. Keep the existing CameraX,
# inspection, pricing and navigation behaviour; this only changes presentation
# inside DeviceLensActivity and deliberately does not touch DashboardActivity.
lens = root / 'DeviceLensActivity.kt'
lens_text = lens.read_text(encoding='utf-8')
scan_replacements = [
    ('Text("Take Photos", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)',
     'Text("Scan Device", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)'),
    ('private fun CameraCorners()', 'private fun ScanFrameOverlay()'),
    ('CameraCorners()', 'ScanFrameOverlay()'),
    ('val c = Color.White\n        val stroke = 4.dp.toPx()', 'val c = LensBlue\n        val stroke = 4.dp.toPx()'),
    ('ScanScaffold("Analysing Images", cancel)', 'ScanScaffold("Analysing Device", cancel)'),
    ('Text("Morley Vision is analysing both photos", color = LensText, fontWeight = FontWeight.Black, fontSize = 18.sp)',
     'Text("Analysing Device", color = LensText, fontWeight = FontWeight.Black, fontSize = 18.sp)'),
    ('AnalysisCheck("Identifying device model")', 'AnalysisCheck("Detecting model")'),
    ('AnalysisCheck("Checking condition")', 'AnalysisCheck("Checking specifications")'),
    ('AnalysisCheck("Detecting damage or cracks")', 'AnalysisCheck("Identifying condition")'),
    ('AnalysisCheck("Analysing visual details")', 'AnalysisCheck("Searching market data")'),
    ('AnalysisCheck("Comparing with catalogue")', 'AnalysisCheck("Finalising assessment")'),
    ('"AI-powered visual estimate. Staff must verify the model, condition and damage before buying or adding stock."',
     '"Keep the device in frame for the best results. AI findings remain advisory until staff verification is complete."'),
    ('ScanScaffold("Analysis Results", close)', 'ScanScaffold("Scan Result", close)'),
    ('Text("View Full Details", fontWeight = FontWeight.Black)', 'Text("Full Specifications", fontWeight = FontWeight.Black)'),
    ('OutlinedButton(onClick = condition, modifier = Modifier.weight(1f)) { Text("Condition") }',
     'OutlinedButton(onClick = condition, modifier = Modifier.weight(1f)) { Text("Condition Assessment") }'),
    ('OutlinedButton(onClick = pricing, modifier = Modifier.weight(1f)) { Text("Live Pricing") }',
     'OutlinedButton(onClick = pricing, modifier = Modifier.weight(1f)) { Text("Market Value") }'),
    ('TextButton(onClick = retake, modifier = Modifier.fillMaxWidth()) { Text("Retake Photos") }',
     'TextButton(onClick = retake, modifier = Modifier.fillMaxWidth()) { Text("New Scan") }'),
]
for old, new in scan_replacements:
    if old in lens_text:
        lens_text = lens_text.replace(old, new, 1)
    elif new not in lens_text:
        raise SystemExit(f'DeviceLensActivity.kt scan-layout anchor missing: {old[:90]!r}')

frame_anchor = '''        Box(
            Modifier.align(Alignment.Center).fillMaxWidth(.78f).aspectRatio(.68f)
        ) {
            ScanFrameOverlay()
        }

        if (cameraError.isNotBlank()) {'''
frame_replacement = '''        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color.Black.copy(alpha = .62f),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 136.dp)
        ) {
            Text(
                "Position the device within the frame",
                Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            Modifier.align(Alignment.Center).fillMaxWidth(.78f).aspectRatio(.68f)
        ) {
            ScanFrameOverlay()
        }

        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 118.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Auto", "Barcode", "Serial Number").forEachIndexed { index, label ->
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (index == 0) LensBlue else Color.Black.copy(alpha = .58f),
                    border = BorderStroke(1.dp, if (index == 0) LensBlue else Color.White.copy(alpha = .20f))
                ) {
                    Text(
                        label,
                        Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = if (index == 0) FontWeight.Black else FontWeight.SemiBold
                    )
                }
            }
        }

        if (cameraError.isNotBlank()) {'''
if frame_anchor in lens_text:
    lens_text = lens_text.replace(frame_anchor, frame_replacement, 1)
elif 'Position the device within the frame' not in lens_text:
    raise SystemExit('DeviceLensActivity.kt camera frame insertion anchor missing')

result_anchor = '''            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = condition, modifier = Modifier.weight(1f)) { Text("Condition Assessment") }
                OutlinedButton(onClick = pricing, modifier = Modifier.weight(1f)) { Text("Market Value") }
            }
            Button(onClick = addStock, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = LensBlueDark)) {'''
result_replacement = '''            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = condition, modifier = Modifier.weight(1f)) { Text("Condition Assessment") }
                OutlinedButton(onClick = pricing, modifier = Modifier.weight(1f)) { Text("Market Value") }
            }
            OutlinedButton(onClick = pricing, modifier = Modifier.fillMaxWidth()) {
                Text("Compare Prices", fontWeight = FontWeight.Bold)
            }
            Button(onClick = addStock, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = LensBlueDark)) {'''
if result_anchor in lens_text:
    lens_text = lens_text.replace(result_anchor, result_replacement, 1)
elif 'Compare Prices' not in lens_text:
    raise SystemExit('DeviceLensActivity.kt result action insertion anchor missing')

required_scan_markers = (
    'Position the device within the frame', 'Scan Device', 'Auto', 'Barcode',
    'Serial Number', 'ScanFrameOverlay', 'Analysing Device', 'Detecting model',
    'Checking specifications', 'Identifying condition', 'Searching market data',
    'Keep the device in frame for the best results.', 'Scan Result',
    'Full Specifications', 'Condition Assessment', 'Market Value',
    'Compare Prices', 'New Scan',
)
for marker in required_scan_markers:
    if marker not in lens_text:
        raise SystemExit(f'DeviceLensActivity.kt expected scan-layout marker missing: {marker}')
lens.write_text(lens_text, encoding='utf-8')

# Stale update filtering is now authoritative checked-in Kotlin. Keep the
# build-time migration from silently restoring or depending on that behaviour.
store = root / 'NotificationInboxStore.kt'
store_text = store.read_text(encoding='utf-8')
if 'item.versionCode <= BuildConfig.VERSION_CODE' not in store_text:
    raise SystemExit('NotificationInboxStore.kt is missing authoritative stale-update filtering')

print('Applied video-review UI, blue visual consistency, GP contrast, Smart Workspace Quick Deal spacing, Test & Buy, Valuation History and approved Morley AI Scan Device presentation; verified checked-in stale-update filtering')
