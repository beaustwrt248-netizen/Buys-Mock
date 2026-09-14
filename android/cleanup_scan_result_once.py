from pathlib import Path
p=Path(__file__).resolve().parent/'app/src/main/java/com/buysloans/hub/DeviceLensActivity.kt'
s=p.read_text(encoding='utf-8')
b='''            OutlinedButton(onClick = pricing, modifier = Modifier.fillMaxWidth()) {
                Text("Compare Prices", fontWeight = FontWeight.Bold)
            }
'''
count=s.count(b)
if count==2:
    s=s.replace(b+b,b,1)
elif count!=1:
    raise SystemExit(f'Expected one or two Compare Prices blocks, found {count}')
required=['Position the device within the frame','Scan Device','Auto','Barcode','Serial Number','ScanFrameOverlay','Analysing Device','Detecting model','Checking specifications','Identifying condition','Searching market data','Keep the device in frame for the best results.','Scan Result','Full Specifications','Condition Assessment','Market Value','Compare Prices','New Scan']
missing=[x for x in required if x not in s]
if missing: raise SystemExit(f'Missing required scanner markers: {missing}')
if s.count(b)!=1: raise SystemExit('Expected exactly one Compare Prices action after cleanup')
p.write_text(s,encoding='utf-8')
print('Scanner result action count verified and cleaned')
