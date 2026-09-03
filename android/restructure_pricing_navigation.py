from pathlib import Path

# This pre-build guard protects the current primary pricing structure:
# Categories contains laptops, desktops, mobile phones and gaming consoles.
# General Buys / GP is a bottom-navigation destination. Console Pricing remains available
# inside Categories but is intentionally not duplicated as a home-screen shortcut.
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
        'Page.Laptop->CategoriesPricingScreen()',
        'Page.Desktop->ConsolePricingScreen()',
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

present_forbidden = []
for file_name, needles in forbidden.items():
    text = texts[file_name]
    present_forbidden.extend(f'{file_name}: {needle}' for needle in needles if needle in text)

if missing:
    raise SystemExit('Pricing navigation drifted; missing: ' + ' | '.join(missing))
if present_forbidden:
    raise SystemExit('Removed navigation shortcuts returned: ' + ' | '.join(present_forbidden))

print('Categories, GP bottom navigation, global mobile search, imagery, and priced/unpriced phone flows aligned')
