from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
errors = []

def need(path: str, *tokens: str):
    text = (ROOT / path).read_text(encoding='utf-8')
    for token in tokens:
        if token not in text:
            errors.append(f'{path}: missing {token!r}')
    return text

build = need('android/app/build.gradle', "versionCode 59", "versionName '2.15.13'")
admin_build = need('android/adminapp/build.gradle', "versionCode 18", "versionName '0.1.17'", 'applyAdminMorleyPalette')
dashboard = need('android/app/src/main/java/com/buysloans/hub/DashboardActivity.kt', 'Computer Pricing', 'Console Pricing', 'General buys & GP', 'Admin mode')
need('android/app/src/main/java/com/buysloans/hub/AdminModePolicy.kt', 'admin', 'manager')
need('index.html', 'product-parity-v3.js', 'ultimate-parity.js', 'web-admin-mode.js')
need('more-menu-v2.js', 'How-to Guide & FAQ')
need('admin/guardian-health.js', 'guardian')
need('android/app/src/main/AndroidManifest.xml', 'android.permission.NFC')

for bad in ('TODO', 'FIXME', 'HACK'):
    for path in [
        ROOT / 'android/app/src/main/java/com/buysloans/hub',
        ROOT / 'android/adminapp/src/main/java/com/buysloans/admin',
    ]:
        for file in path.glob('*.kt'):
            if bad in file.read_text(encoding='utf-8'):
                errors.append(f'{file.relative_to(ROOT)}: contains {bad}')

if 'Page.Laptop->ComputerPricingScreen()' not in dashboard or 'Page.Desktop->ConsolePricingScreen()' not in dashboard:
    errors.append('DashboardActivity.kt: primary pricing routes drifted')

if errors:
    raise SystemExit('\n'.join(errors))
print('Final Morley product audit passed')
