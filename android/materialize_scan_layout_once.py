from pathlib import Path

path = Path(__file__).resolve().parent / "app" / "src" / "main" / "java" / "com" / "buysloans" / "hub" / "DeviceLensActivity.kt"
source = path.read_text(encoding="utf-8")

replacements = [
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
if frame_box not in source:
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
source = source.replace(frame_box, guide + frame_box, 1)

after_frame = frame_box + '''

        if (cameraError.isNotBlank()) {'''
modes = frame_box + '''

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
if after_frame not in source:
    raise SystemExit("Camera frame tail anchor not found")
source = source.replace(after_frame, modes, 1)

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
if result_anchor not in source:
    raise SystemExit("Results action insertion anchor not found")
source = source.replace(result_anchor, result_replacement, 1)

required = [
    "Position the device within the frame",
    "Scan Device",
    "Auto",
    "Barcode",
    "Serial Number",
    "ScanFrameOverlay",
    "Analysing Device",
    "Detecting model",
    "Checking specifications",
    "Identifying condition",
    "Searching market data",
    "Keep the device in frame for the best results.",
    "Scan Result",
    "Full Specifications",
    "Condition Assessment",
    "Market Value",
    "Compare Prices",
    "New Scan",
]
missing = [marker for marker in required if marker not in source]
if missing:
    raise SystemExit(f"Missing markers after materialization: {missing}")

path.write_text(source, encoding="utf-8")
print("Materialized approved Morley AI Scan Device layout into DeviceLensActivity.kt")
