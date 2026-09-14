from pathlib import Path

path = Path(__file__).resolve().parent / "app" / "src" / "main" / "java" / "com" / "buysloans" / "hub" / "DeviceLensActivity.kt"
source = path.read_text(encoding="utf-8")

replacements = [
    ('Text("Take Photos", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)', 'Text("Scan Device", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)'),
    ('private fun CameraCorners()', 'private fun ScanFrameOverlay()'),
    ('CameraCorners()', 'ScanFrameOverlay()'),
    ('val c = Color.White\n        val stroke = 4.dp.toPx()', 'val c = LensBlue\n        val stroke = 4.dp.toPx()'),
    ('ScanScaffold("Analysing Images", cancel)', 'ScanScaffold("Analysing Device", cancel)'),
    ('Text("Morley Vision is analysing both photos", color = LensText, fontWeight = FontWeight.Black, fontSize = 18.sp)', 'Text("Analysing Device", color = LensText, fontWeight = FontWeight.Black, fontSize = 18.sp)'),
    ('AnalysisCheck("Identifying device model")', 'AnalysisCheck("Detecting model")'),
    ('AnalysisCheck("Checking condition")', 'AnalysisCheck("Checking specifications")'),
    ('AnalysisCheck("Detecting damage or cracks")', 'AnalysisCheck("Identifying condition")'),
    ('AnalysisCheck("Analysing visual details")', 'AnalysisCheck("Searching market data")'),
    ('AnalysisCheck("Comparing with catalogue")', 'AnalysisCheck("Finalising assessment")'),
    ('"AI-powered visual estimate. Staff must verify the model, condition and damage before buying or adding stock."', '"Keep the device in frame for the best results. AI findings remain advisory until staff verification is complete."'),
    ('ScanScaffold("Analysis Results", close)', 'ScanScaffold("Scan Result", close)'),
    ('Text("View Full Details", fontWeight = FontWeight.Black)', 'Text("Full Specifications", fontWeight = FontWeight.Black)'),
    ('OutlinedButton(onClick = condition, modifier = Modifier.weight(1f)) { Text("Condition") }', 'OutlinedButton(onClick = condition, modifier = Modifier.weight(1f)) { Text("Condition Assessment") }'),
    ('OutlinedButton(onClick = pricing, modifier = Modifier.weight(1f)) { Text("Live Pricing") }', 'OutlinedButton(onClick = pricing, modifier = Modifier.weight(1f)) { Text("Market Value") }'),
    ('TextButton(onClick = retake, modifier = Modifier.fillMaxWidth()) { Text("Retake Photos") }', 'TextButton(onClick = retake, modifier = Modifier.fillMaxWidth()) { Text("New Scan") }'),
]
for old, new in replacements:
    if old in source:
        source = source.replace(old, new, 1)
    elif new not in source:
        raise SystemExit(f"Missing scan-layout anchor: {old[:90]}")

frame_box = '''        Box(
            Modifier.align(Alignment.Center).fillMaxWidth(.78f).aspectRatio(.68f)
        ) {
            ScanFrameOverlay()
        }'''
frame_pos = source.find(frame_box)
if frame_pos < 0:
    raise SystemExit("Camera frame box anchor not found")

guide = '''        Surface(
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

'''
source = source[:frame_pos] + guide + source[frame_pos:]
frame_pos += len(guide)

error_marker = '        if (cameraError.isNotBlank()) {'
error_pos = source.find(error_marker, frame_pos + len(frame_box))
if error_pos < 0:
    raise SystemExit("Camera error marker not found after scan frame")
chips = '''        Row(
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

'''
source = source[:error_pos] + chips + source[error_pos:]

results_start = source.find('ScanScaffold("Scan Result", close)')
if results_start < 0:
    raise SystemExit("Scan Result screen anchor not found")
add_stock_marker = '            Button(onClick = addStock, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = LensBlueDark)) {'
add_stock_pos = source.find(add_stock_marker, results_start)
if add_stock_pos < 0:
    raise SystemExit("Add to Stock action anchor not found")
compare = '''            OutlinedButton(onClick = pricing, modifier = Modifier.fillMaxWidth()) {
                Text("Compare Prices", fontWeight = FontWeight.Bold)
            }
'''
source = source[:add_stock_pos] + compare + source[add_stock_pos:]

required = ["Position the device within the frame", "Scan Device", "Auto", "Barcode", "Serial Number", "ScanFrameOverlay", "Analysing Device", "Detecting model", "Checking specifications", "Identifying condition", "Searching market data", "Keep the device in frame for the best results.", "Scan Result", "Full Specifications", "Condition Assessment", "Market Value", "Compare Prices", "New Scan"]
missing = [marker for marker in required if marker not in source]
if missing:
    raise SystemExit(f"Missing markers after materialization: {missing}")
path.write_text(source, encoding="utf-8")
print("Materialized approved Morley AI Scan Device layout into DeviceLensActivity.kt")
