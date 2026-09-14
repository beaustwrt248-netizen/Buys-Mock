from pathlib import Path
p=Path(__file__).resolve().parent/'app/src/main/java/com/buysloans/hub/DeviceLensActivity.kt'
s=p.read_text(encoding='utf-8')
b='''            OutlinedButton(onClick = pricing, modifier = Modifier.fillMaxWidth()) {
                Text("Compare Prices", fontWeight = FontWeight.Bold)
            }
'''
if s.count(b)!=2:
    raise SystemExit(f'Expected exactly 2 Compare Prices blocks, found {s.count(b)}')
s=s.replace(b+b,b,1)
if s.count(b)!=1:
    raise SystemExit('Compare Prices cleanup did not produce exactly one action')
required=['Position the device within the frame','Scan Device','Auto','Barcode','Serial Number','ScanFrameOverlay','Analysing Device','Detecting model','Checking specifications','Identifying condition','Searching market data','Keep the device in frame for the best results.','Scan Result','Full Specifications','Condition Assessment','Market Value','Compare Prices','New Scan']
missing=[x for x in required if x not in s]
if missing: raise SystemExit(f'Missing required scanner markers: {missing}')
p.write_text(s,encoding='utf-8')
print('Removed duplicate Compare Prices action; scanner markers intact')
