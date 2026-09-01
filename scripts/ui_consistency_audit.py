#!/usr/bin/env python3
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
errors = []

def require(condition, message):
    if not condition:
        errors.append(message)

def read(path):
    p = ROOT / path
    require(p.is_file() and p.stat().st_size > 0, f"missing required UI file: {path}")
    return p.read_text(encoding="utf-8") if p.is_file() else ""

index = read("index.html")
css = read("morley-ui-baseline.css")
js = read("morley-ui-baseline.js")
light = read("morley-light-web.css")
mobile = read("mobile-readability-fix.js")
initial = read("web-initial-render-fix.js")

require('width=device-width,initial-scale=1,viewport-fit=cover' in index, "outer shell viewport contract changed")
require('morley-light-web.css' in index, "light Morley theme is not loaded")
require('morley-ui-baseline.css' in index, "canonical UI baseline CSS is not loaded")
require('morley-ui-baseline.js' in index, "canonical UI baseline runtime is not loaded")
require('mobile-readability-fix.js' in index, "mobile readability repair is not loaded")
require('web-initial-render-fix.js' in index, "initial render repair is not loaded")

for earlier, later in [
    ('morley-light-web.css', 'morley-ui-baseline.css'),
    ('mobile-layout-fix.js', 'web-initial-render-fix.js'),
    ('web-initial-render-fix.js', 'mobile-readability-fix.js'),
    ('mobile-readability-fix.js', 'morley-ui-baseline.js'),
]:
    require(index.find(earlier) >= 0 and index.find(later) > index.find(earlier), f"load order must keep {later} after {earlier}")

require('color-scheme:light!important' in light, "primary light theme contract changed")
for token in ['#f5f7f4', '#ffffff', '#1c2b26', '#52645d', '#167a5a']:
    require(token in light.lower(), f"primary light theme lost canonical token {token}")
for token in ['--morley-ui-font', 'Inter,system-ui', '#1c2b26', '#167a5a', '.decisionGrid', '#livePricing', '#onlineStatus']:
    require(token in css, f"canonical UI baseline missing contract: {token}")
for token in ['ensureViewport', 'normaliseSpelling', 'markStatus', 'markDecision', 'B&L Morley']:
    require(token in js, f"UI runtime missing consistency rule: {token}")
require('max-width:100vw' in mobile, "mobile scale guard changed")
require('refreshLayout' in initial, "initial layout synchroniser changed")

# Guard the exact display mistakes already seen in production without policing identifiers/data.
ui_files = [p for p in ROOT.glob('*.js')] + [p for p in ROOT.glob('*.html')]
known_typos = re.compile(r'\b(Reccomended|Recommened|Comparibles|Valutation|Availible)\b', re.I)
for path in ui_files:
    if path.name == 'morley-ui-baseline.js':
        continue
    text = path.read_text(encoding='utf-8', errors='ignore')
    match = known_typos.search(text)
    require(match is None, f"known display typo remains in {path.name}: {match.group(0) if match else ''}")

# Canonical top-level product wording must remain stable.
product = read('product-parity-v3.js')
for label in ['Computer Pricing', 'Console Pricing', 'General Buys / GP']:
    require(label in product, f"canonical product label missing: {label}")

# Android must stay on the same light/emerald presentation contract as the web client.
android_theme = read('android/app/src/main/java/com/buysloans/hub/MorleyVisualTheme.kt')
for token in ['lightColorScheme', '0xFFF5F7F4', '0xFFFFFFFF', '0xFF1C2B26', '0xFF52645D', '0xFF167A5A']:
    require(token in android_theme, f"Android Morley theme contract missing: {token}")
support = read('android/app/src/main/java/com/buysloans/hub/SupportTicketActivity.kt')
require('Text("B&L Morley Support", color = Color.White' in support, "Android Support top-bar title contrast regressed")
require('containerColor = Color(0xFF050B16)' in support, "Android Support top-bar surface contract changed")

# Scan user-facing Kotlin strings for the same known spelling errors.
for path in (ROOT / 'android/app/src/main/java/com/buysloans/hub').glob('*.kt'):
    text = path.read_text(encoding='utf-8', errors='ignore')
    match = known_typos.search(text)
    require(match is None, f"known Android display typo remains in {path.name}: {match.group(0) if match else ''}")

if errors:
    print('Morley UI consistency audit FAILED:')
    for error in errors:
        print(f' - {error}')
    sys.exit(1)
print('Morley UI consistency audit passed')
