from pathlib import Path

# This pre-build guard protects the current primary pricing structure:
# Categories contains laptops, desktops, mobile phones and gaming consoles.
# General Buys / GP is a dashboard navigation destination. Console Pricing remains available
# inside Categories but is intentionally not duplicated as a home-screen shortcut.
#
# Keep this guard semantic rather than formatting-sensitive: Android 16 adaptive layout work may
# legitimately wrap the route switch in Row/AdaptiveContentFrame without changing route behavior.
dashboard = Path(__file__).resolve().parent / 'app' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'hub' / 'DashboardActivity.kt'
categories = Path(__file__).resolve().parent / 'app' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'hub' / 'CategoriesPricingScreen.kt'
phones = Path(__file__).resolve().parent / 'app' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'hub' / 'MobilePhonePricingScreen.kt'
phone_catalog = Path(__file__).resolve().parent / 'app' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'hub' / 'MobilePhoneDeviceCatalog.kt'

texts = {
    'DashboardActivity.kt': dashboard.read_text(encoding='utf-8'),
    'CategoriesPricingScreen.kt': categories.read_text(encoding='utf-8'),
    'MobilePhonePricingScreen.kt': phones.read_text(encoding='utf-8'),
    'MobilePhoneDeviceCatalog.kt': phone_catalog.read_text(encoding='utf-8'),
}

required = {
    'DashboardActivity.kt': (
        'Page.Laptop -> CategoriesPricingScreen()',
        'Page.Desktop -> ConsolePricingScreen()',
        'GP("General Buys", MorleyIcons.Money)',
        'BottomDestination.GP -> { showMenu = false; page = Page.GP }',
        'private fun ParityHome(onGp: () -> Unit)',
        'NavCard(MorleyIcons.Money, "General Buys / GP", "A / B / C / Luxury buying targets", onGp)',
    ),
    'CategoriesPricingScreen.kt': (
        'PricingCategory.LAPTOPS -> LaptopGuidedScreen()',
        'PricingCategory.DESKTOPS -> Desktop()',
        'PricingCategory.MOBILE_PHONES -> MobilePhonePricingScreen()',
        'PricingCategory.GAMING_CONSOLES -> ConsolePricingScreen()',
    ),
    'MobilePhonePricingScreen.kt': (
        'featuredPhoneBrands = listOf("Apple", "Samsung", "Google", "OnePlus", "Xiaomi")',
        'Search all mobile phones',
        'MobilePhoneDeviceCatalog.search(globalQuery)',
        'MobilePhoneDeviceCatalog.models(brand)',
        'MobilePhoneDeviceCatalog.pricedEntry(it)',
        'MobilePhonePricingCatalog.gradeBuyPercent',
        'MobilePhonePhoto(',
    ),
    'MobilePhoneDeviceCatalog.kt': (
        'ExpandedMobilePhoneCatalog.models',
        'fun search(query: String, limit: Int = 80)',
        'Existing Morley price-sheet records are always authoritative',
    ),
}

# Android 16 adaptive integration contract. These markers ensure the route assertions above are
# still exercised inside the live adaptive dashboard rather than being satisfied by dead code.
adaptive_required = (
    'val adaptiveSize = morleyAdaptiveSize()',
    'AdaptiveBackHandler(enabled = showMenu || page != Page.Home)',
    'if (adaptiveSize == MorleyAdaptiveSize.Compact) CompactDashboardNavigation(page, showMenu, ::selectDestination)',
    'if (adaptiveSize != MorleyAdaptiveSize.Compact)',
    'MorleyAdaptiveNavigation(size = adaptiveSize, items = adaptiveNavItems, compact = {})',
    'AdaptiveContentFrame',
)

forbidden = {
    'DashboardActivity.kt': (
        'HISTORY("History", MorleyIcons.History)',
        'NavCard(MorleyIcons.Categories, "Categories", "Laptops, desktops, mobile phones and gaming consoles", onCategories)',
        'NavCard(MorleyIcons.Console, "Console Pricing", "PS4, PS5, Xbox and Nintendo grade pricing", onConsole)',
    ),
}

missing = []
for file_name, needles in required.items():
    text = texts[file_name]
    missing.extend(f'{file_name}: {needle}' for needle in needles if needle not in text)

missing.extend(
    f'DashboardActivity.kt adaptive contract: {needle}'
    for needle in adaptive_required
    if needle not in texts['DashboardActivity.kt']
)

present_forbidden = []
for file_name, needles in forbidden.items():
    text = texts[file_name]
    present_forbidden.extend(f'{file_name}: {needle}' for needle in needles if needle in text)

if missing:
    raise SystemExit('Pricing navigation drifted; missing: ' + ' | '.join(missing))
if present_forbidden:
    raise SystemExit('Removed navigation shortcuts returned: ' + ' | '.join(present_forbidden))

print('Adaptive dashboard, Categories, GP navigation, global mobile search, imagery, and priced/unpriced phone flows aligned')
