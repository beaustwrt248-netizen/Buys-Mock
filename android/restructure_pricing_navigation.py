from pathlib import Path

# This pre-build guard protects the current primary pricing structure:
# Categories contains laptops, desktops, mobile phones and gaming consoles.
# Console Pricing remains available as a direct primary shortcut.
dashboard = Path(__file__).resolve().parent / 'app' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'hub' / 'DashboardActivity.kt'
categories = Path(__file__).resolve().parent / 'app' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'hub' / 'CategoriesPricingScreen.kt'
phones = Path(__file__).resolve().parent / 'app' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'hub' / 'MobilePhonePricingScreen.kt'

texts = {
    'DashboardActivity.kt': dashboard.read_text(encoding='utf-8'),
    'CategoriesPricingScreen.kt': categories.read_text(encoding='utf-8'),
    'MobilePhonePricingScreen.kt': phones.read_text(encoding='utf-8'),
}

required = {
    'DashboardActivity.kt': (
        'Page.Laptop->CategoriesPricingScreen()',
        'Page.Desktop->ConsolePricingScreen()',
        'private fun ParityHome(onCategories:()->Unit,onConsole:()->Unit,onGp:()->Unit)',
        'NavCard(MorleyIcons.Categories,"Categories","Laptops, desktops, mobile phones and gaming consoles",onCategories)',
        'NavCard(MorleyIcons.Console,"Console Pricing","PS4, PS5, Xbox and Nintendo grade pricing",onConsole)',
        'NavCard(MorleyIcons.Money,"General Buys / GP","A / B / C / Luxury buying targets",onGp)',
    ),
    'CategoriesPricingScreen.kt': (
        'PricingCategory.LAPTOPS -> LaptopGuidedScreen()',
        'PricingCategory.DESKTOPS -> Desktop()',
        'PricingCategory.MOBILE_PHONES -> MobilePhonePricingScreen()',
        'PricingCategory.GAMING_CONSOLES -> ConsolePricingScreen()',
    ),
    'MobilePhonePricingScreen.kt': (
        'APPLE(MobilePhonePricingCatalog.APPLE, "Apple iPhone")',
        'SAMSUNG(MobilePhonePricingCatalog.SAMSUNG, "Samsung Galaxy")',
        'MobilePhonePricingCatalog.gradeBuyPercent',
    ),
}

missing = []
for file_name, needles in required.items():
    text = texts[file_name]
    missing.extend(f'{file_name}: {needle}' for needle in needles if needle not in text)

if missing:
    raise SystemExit('Pricing navigation drifted; missing: ' + ' | '.join(missing))

print('Categories, direct Console Pricing, and mobile phone pricing navigation aligned')
