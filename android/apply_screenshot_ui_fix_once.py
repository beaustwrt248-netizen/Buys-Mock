from pathlib import Path

p = Path('android/app/src/main/java/com/buysloans/hub/DeviceLensActivity.kt')
s = p.read_text()

def rep(old, new):
    global s
    if old not in s:
        raise SystemExit('missing anchor: ' + old[:100])
    s = s.replace(old, new, 1)

# Insets imports.
rep('import androidx.compose.foundation.layout.padding\n', 'import androidx.compose.foundation.layout.padding\nimport androidx.compose.foundation.layout.navigationBarsPadding\nimport androidx.compose.foundation.layout.statusBarsPadding\n')

# Camera top bar: respect status inset and remove redundant right-side close action.
rep('.align(Alignment.TopCenter)\n                .padding(top = 18.dp),', '.align(Alignment.TopCenter)\n                .statusBarsPadding()\n                .padding(top = 10.dp),')
rep('''                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(34.dp))
                }
''', '')

# Make frame shorter so it does not collide visually with capture controls.
rep('Modifier.align(Alignment.Center).fillMaxWidth(.78f).aspectRatio(.68f)', 'Modifier.align(Alignment.Center).fillMaxWidth(.74f).aspectRatio(.82f)')

# Mode selector and bottom controls both respect the Android navigation inset.
rep('''            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 116.dp),''', '''            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 112.dp),''')
rep('''                .align(Alignment.BottomCenter)
                .padding(horizontal = 28.dp, vertical = 22.dp),''', '''                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 18.dp),''')

old = '''            MorleyVisionReviewPanel(
                state = reviewState,
                onModelSelected = onModelSelected,
                onConditionSelected = onConditionSelected,
                onDamageDecision = onDamageDecision,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Text(
                "Staff verification is still required before completing the purchase.",
                color = LensInk,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Surface(
                modifier = Modifier.padding(horizontal = 12.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, LensBorder),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Verification required", color = LensInk, fontWeight = FontWeight.Black)
                    Text(
                        "Resolve every damage decision and retake photos whenever identity, storage, photo quality or cross-photo consistency is not verified. Pricing and stock entry remain locked until this review is complete.",
                        color = LensMuted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    )
                }
            }
            Button(
                onClick = onContinue,
                enabled = reviewState.canCompleteStaffReview,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LensBlue),
                shape = RoundedCornerShape(14.dp),
            ) { Text("Continue to Results", fontWeight = FontWeight.Black) }
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) { Text("Retake Photos", fontWeight = FontWeight.Bold) }
'''
new = '''            if (reviewState.selectedModel == null) {
                Surface(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, LensBorder),
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("We couldn't identify this device", color = LensInk, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text(
                            "The photos do not contain enough clear device detail to verify the model or condition. Retake the front and back photos before continuing.",
                            color = LensMuted,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        )
                        Button(
                            onClick = onRetake,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LensBlue),
                            shape = RoundedCornerShape(14.dp),
                        ) { Text("Retake Photos", fontWeight = FontWeight.Black) }
                        Text("Why the scan failed", color = LensInk, fontWeight = FontWeight.Black)
                        Text(
                            "• Device identity could not be verified\n• Photo quality or framing is insufficient\n• Front/back consistency could not be confirmed",
                            color = LensMuted,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                        )
                        Text("View AI details", color = LensBlue, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                MorleyVisionReviewPanel(
                    state = reviewState,
                    onModelSelected = onModelSelected,
                    onConditionSelected = onConditionSelected,
                    onDamageDecision = onDamageDecision,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                Text(
                    "Staff verification is still required before completing the purchase.",
                    color = LensInk,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                Surface(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, LensBorder),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Verification required", color = LensInk, fontWeight = FontWeight.Black)
                        Text(
                            "Resolve every damage decision and retake photos whenever identity, storage, photo quality or cross-photo consistency is not verified. Pricing and stock entry remain locked until this review is complete.",
                            color = LensMuted,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                        )
                    }
                }
                Button(
                    onClick = onContinue,
                    enabled = reviewState.canCompleteStaffReview,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LensBlue),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Continue to Results", fontWeight = FontWeight.Black) }
                OutlinedButton(
                    onClick = onRetake,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Retake Photos", fontWeight = FontWeight.Bold) }
            }
'''
rep(old, new)
p.write_text(s)
