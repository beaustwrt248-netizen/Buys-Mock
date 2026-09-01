from pathlib import Path
import json
import re

ROOT = Path(__file__).resolve().parents[1]
errors = []


def need(path: str, *tokens: str):
    text = (ROOT / path).read_text(encoding='utf-8')
    for token in tokens:
        if token not in text:
            errors.append(f'{path}: missing {token!r}')
    return text


def semver(value: str):
    match = re.fullmatch(r'(\d+)\.(\d+)\.(\d+)(?:[-+].*)?', value.strip())
    return tuple(map(int, match.groups())) if match else None


build = need('android/app/build.gradle', "namespace 'com.buysloans.hub'")
admin_build = need('android/adminapp/build.gradle', "versionCode 18", "versionName '0.1.17'", 'applyAdminMorleyPalette')
dashboard = need(
    'android/app/src/main/java/com/buysloans/hub/DashboardActivity.kt',
    'Laptop / MacBook',
    'Desktop / Gaming PC',
    'General Buys / GP',
    'Page.Laptop->LaptopGuidedScreen()',
    'Page.Desktop->Desktop()',
    'Admin mode',
    'consumeWindowInsets',
)
need('android/app/src/main/java/com/buysloans/hub/AdminModePolicy.kt', 'admin', 'manager')
need(
    'android/app/src/main/java/com/buysloans/hub/EmbeddedAdminActivity.kt',
    'Support operations',
    'Save Ticket Controls',
    'Confirm user access change',
)
need(
    'android/app/src/main/java/com/buysloans/hub/EmbeddedAdminClient.kt',
    '/functions/v1/admin-user-control',
    '/rest/v1/support_tickets',
    'updateTicket',
    'updateUserAccess',
)
need(
    'index.html',
    'product-parity-v3.js',
    'ultimate-parity.js',
    'web-admin-mode.js',
    'mobile-parity-v3.css',
    'mobile-layout-fix.js',
)
need(
    'mobile-parity-v3.css',
    '@media(max-width:760px)',
    'grid-template-columns:repeat(4,minmax(0,1fr))',
    '.modal.open,.morley-menu-dialog.open,.msw-dialog.open',
)
need('mobile-layout-fix.js', "home|laptop|desktop|general", 'morley-mobile-overlay-active')
need('more-menu-v2.js', 'How-to Guide & FAQ')
need('admin/guardian-health.js', 'guardian')
need('android/app/src/main/AndroidManifest.xml', 'android.permission.NFC')

code_match = re.search(r'\bversionCode\s+(\d+)', build)
name_match = re.search(r"\bversionName\s+['\"]([^'\"]+)['\"]", build)
if not code_match or not name_match:
    errors.append('android/app/build.gradle: could not parse release version')
else:
    source_code = int(code_match.group(1))
    source_name = name_match.group(1).strip()
    manifest = json.loads((ROOT / 'ota/latest.json').read_text(encoding='utf-8'))
    published_code = int(manifest.get('versionCode', 0))
    published_name = str(manifest.get('versionName', '')).strip()
    if source_code not in {published_code, published_code + 1}:
        errors.append(
            f'android/app/build.gradle: source versionCode {source_code} must equal published OTA {published_code} '
            f'or be exactly its next OTA-ready code {published_code + 1}'
        )
    source_semver = semver(source_name)
    published_semver = semver(published_name)
    if source_semver is None or published_semver is None:
        errors.append('Android/OTA versionName must be semantic versions')
    elif source_code == published_code and source_name != published_name:
        errors.append('Android and OTA versionName must match when versionCode matches')
    elif source_code == published_code + 1 and source_semver <= published_semver:
        errors.append('Next Android release versionName must be newer than the published OTA version')

for bad in ('TODO', 'FIXME', 'HACK'):
    for path in [
        ROOT / 'android/app/src/main/java/com/buysloans/hub',
        ROOT / 'android/adminapp/src/main/java/com/buysloans/admin',
    ]:
        for file in path.glob('*.kt'):
            if bad in file.read_text(encoding='utf-8'):
                errors.append(f'{file.relative_to(ROOT)}: contains {bad}')

if 'Page.Laptop->ComputerPricingScreen()' in dashboard or 'Page.Desktop->ConsolePricingScreen()' in dashboard:
    errors.append('DashboardActivity.kt: legacy primary pricing routes remain')

if errors:
    raise SystemExit('\n'.join(errors))
print('Final Morley product audit passed')
