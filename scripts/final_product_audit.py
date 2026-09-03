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
admin_build = need('android/adminapp/build.gradle', "versionCode 21", "versionName '0.1.20'", 'applyAdminMorleyPalette')
dashboard = need(
    'android/app/src/main/java/com/buysloans/hub/DashboardActivity.kt',
    'CATEGORIES("Categories", MorleyIcons.Categories)',
    'GP("General Buys", MorleyIcons.Money)',
    'General Buys / GP',
    'Page.Laptop -> CategoriesPricingScreen()',
    'Page.Desktop -> ConsolePricingScreen()',
    'val adaptiveSize = morleyAdaptiveSize()',
    'AdaptiveBackHandler(enabled = showMenu || page != Page.Home)',
    'MorleyAdaptiveNavigation(size = adaptiveSize, items = adaptiveNavItems, compact = {})',
    'AdaptiveContentFrame',
    'Admin mode',
    'consumeWindowInsets',
)
categories = need(
    'android/app/src/main/java/com/buysloans/hub/CategoriesPricingScreen.kt',
    'Laptops',
    'Desktops',
    'Mobile Phones',
    'Gaming Consoles',
    'PricingCategory.LAPTOPS -> LaptopGuidedScreen()',
    'PricingCategory.DESKTOPS -> Desktop()',
    'PricingCategory.MOBILE_PHONES -> MobilePhonePricingScreen()',
    'PricingCategory.GAMING_CONSOLES -> ConsolePricingScreen()',
)
need(
    'android/app/src/main/java/com/buysloans/hub/MobilePhonePricingScreen.kt',
    'Apple iPhone',
    'Samsung Galaxy',
    'Select Storage Capacity',
    'Select Condition Grade',
    'Quick Summary',
    'Grade Guide',
)
need(
    'android/app/src/main/java/com/buysloans/hub/MobilePhonePricingCatalog.kt',
    'Galaxy S25 Ultra',
    'Galaxy Z Fold 7',
    '"A" to 0.70',
    '"B" to 0.50',
    '"C" to 0.30',
)
need(
    'android/app/src/main/java/com/buysloans/hub/ComputerConsolePricingScreens.kt',
    'Console Pricing',
    'Search all consoles',
    'Price to be added',
)
need(
    'android/app/src/main/java/com/buysloans/hub/ConsolePricingCatalog.kt',
    'Sony PS5 Pro',
    'Sony PS5 Slim Digital',
    'Nintendo DSi XL',
    'Game Boy Advance SP',
    'fun buyPrice(entry: ConsoleDeviceEntry',
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
    'android/app/src/main/java/com/buysloans/hub/TemporaryPasswordGateActivity.kt',
    'must_change_password',
    'Change temporary password',
    'Change password & continue',
)
need(
    'android/adminapp/src/main/java/com/buysloans/admin/TeamInvitePanel.kt',
    'Temporary password',
    'Skip email verification',
    'Create account with temporary password',
)
need(
    'index.html',
    'product-parity-v3.js',
    'ultimate-parity.js',
    'web-admin-mode.js',
    'mobile-parity-v3.css',
    'morley-light-web.css?v=2',
    'mobile-layout-fix.js',
)
need('morley-light-web.css', 'color-scheme:light', '#f5f7f4', '#167a5a', '#1c2b26', '#morleyWebAuth')
need(
    'mobile-parity-v3.css',
    '@media(max-width:760px)',
    'grid-template-columns:repeat(4,minmax(0,1fr))',
    '.modal.open,.morley-menu-dialog.open,.msw-dialog.open',
)
need('mobile-layout-fix.js', "home|laptop|general|settings", 'morley-mobile-overlay-active')
need('product-parity-v3.js', 'Computer Pricing', 'Console Pricing', 'Laptop / MacBook', 'Desktop / Gaming PC')
need('more-menu-v2.js', 'How-to Guide & FAQ')
need('admin/guardian-health.js', 'guardian')
need('android/app/src/main/AndroidManifest.xml', 'android.permission.NFC', 'TemporaryPasswordGateActivity')

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

# Primary navigation goes through Categories. Console Pricing remains a category route, not a
# separate bottom-nav/Home shortcut. GP intentionally replaces History in primary navigation.
if 'Page.Laptop -> LaptopGuidedScreen()' in dashboard or 'Page.Desktop -> Desktop()' in dashboard:
    errors.append('DashboardActivity.kt: split Laptop/Desktop primary routes remain')
if 'BottomDestination.HISTORY' in dashboard:
    errors.append('DashboardActivity.kt: retired History bottom navigation remains')
if 'NavCard(MorleyIcons.Console' in dashboard:
    errors.append('DashboardActivity.kt: Console Pricing Home shortcut must remain removed')
if 'NavCard(MorleyIcons.Categories' in dashboard:
    errors.append('DashboardActivity.kt: Categories Home shortcut must remain removed')
if 'MobilePhoneCategoryPlaceholder' in categories:
    errors.append('CategoriesPricingScreen.kt: retired Mobile Phones placeholder remains')

if errors:
    raise SystemExit('\n'.join(errors))
print('Final Morley product audit passed: adaptive Categories contains laptops, desktops, mobile phones and gaming consoles; GP is primary navigation; series-first mobile/console catalogues preserve pricing boundaries.')
