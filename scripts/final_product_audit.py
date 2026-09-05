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


def reject(path: str, text: str, *tokens: str):
    for token in tokens:
        if token in text:
            errors.append(f'{path}: deprecated token remains {token!r}')


def semver(value: str):
    match = re.fullmatch(r'(\d+)\.(\d+)\.(\d+)(?:[-+].*)?', value.strip())
    return tuple(map(int, match.groups())) if match else None


def validate_release_version(path: str, build_text: str, manifest_path: str, product: str):
    code_match = re.search(r'\bversionCode\s+(\d+)', build_text)
    name_match = re.search(r"\bversionName\s+['\"]([^'\"]+)['\"]", build_text)
    if not code_match or not name_match:
        errors.append(f'{path}: could not parse release version')
        return

    source_code = int(code_match.group(1))
    source_name = name_match.group(1).strip()
    manifest = json.loads((ROOT / manifest_path).read_text(encoding='utf-8'))
    published_code = int(manifest.get('versionCode', 0))
    published_name = str(manifest.get('versionName', '')).strip()

    if source_code not in {published_code, published_code + 1}:
        errors.append(
            f'{path}: source versionCode {source_code} must equal published {product} {published_code} '
            f'or be exactly its next release-ready code {published_code + 1}'
        )

    source_semver = semver(source_name)
    published_semver = semver(published_name)
    if source_semver is None or published_semver is None:
        errors.append(f'{product} versionName values must be semantic versions')
    elif source_code == published_code and source_name != published_name:
        errors.append(f'{path}: versionName must match published {product} when versionCode matches')
    elif source_code == published_code + 1 and source_semver <= published_semver:
        errors.append(f'{path}: next release versionName must be newer than published {product}')


build = need('android/app/build.gradle', "namespace 'com.buysloans.hub'")
main_activity = need(
    'android/app/src/main/java/com/buysloans/hub/MainActivity.kt',
    'AuthManager.validAccessToken(MorleyApplication.instance)',
    'setRequestProperty("Authorization","Bearer $token")',
    'BuildConfig.SUPABASE_PUBLISHABLE_KEY',
    'Your secure session has expired. Sign in again.',
)
reject('android/app/build.gradle', build, 'securePricingAuth', 'secure_pricing_auth.py')
if (ROOT / 'android/secure_pricing_auth.py').exists():
    errors.append('android/secure_pricing_auth.py: obsolete build-time source rewriter still exists')

admin_build = need(
    'android/adminapp/build.gradle',
    "namespace 'com.buysloans.admin'",
    "tasks.matching { it.name == 'preReleaseBuild' }.configureEach { dependsOn tasks.named('verifyReleaseSigning') }",
)
reject(
    'android/adminapp/build.gradle',
    admin_build,
    'applyAdminMorleyPalette',
    'apply_admin_morley_palette.py',
)
if (ROOT / 'android/apply_admin_morley_palette.py').exists():
    errors.append('android/apply_admin_morley_palette.py: obsolete build-time source rewriter still exists')

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

validate_release_version('android/app/build.gradle', build, 'ota/latest.json', 'Morley OTA')
validate_release_version('android/adminapp/build.gradle', admin_build, 'admin/admin-update.json', 'Admin update')

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
print('Final Morley product audit passed: adaptive Categories contains laptops, desktops, mobile phones and gaming consoles; GP is primary navigation; series-first mobile/console catalogues preserve pricing boundaries; checked-in pricing authentication and Admin builds are deterministic; Morley and Admin release identities remain publication-safe.')
