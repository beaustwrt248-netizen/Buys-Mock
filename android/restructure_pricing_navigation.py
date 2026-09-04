from pathlib import Path

# Pre-build semantic guard for the current pricing navigation and live mobile pricing flow.
dashboard = Path(__file__).resolve().parent / 'app' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'hub' / 'DashboardActivity.kt'
categories = Path(__file__).resolve().parent / 'app' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'hub' / 'CategoriesPricingScreen.kt'
phones = Path(__file__).resolve().parent / 'app' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'hub' / 'MobilePhonePricingScreen.kt'
phone_catalog = Path(__file__).resolve().parent / 'app' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'hub' / 'MobilePhoneDeviceCatalog.kt'
texts = {'DashboardActivity.kt':dashboard.read_text(encoding='utf-8'),'CategoriesPricingScreen.kt':categories.read_text(encoding='utf-8'),'MobilePhonePricingScreen.kt':phones.read_text(encoding='utf-8'),'MobilePhoneDeviceCatalog.kt':phone_catalog.read_text(encoding='utf-8')}
required = {
'DashboardActivity.kt':('Page.Laptop -> CategoriesPricingScreen()','Page.Desktop -> ConsolePricingScreen()','GP("General Buys", MorleyIcons.Money)','BottomDestination.GP -> { showMenu = false; page = Page.GP }','private fun ParityHome(onGp: () -> Unit)','NavCard(MorleyIcons.Money, "General Buys / GP", "A / B / C / Luxury buying targets", onGp)'),
'CategoriesPricingScreen.kt':('PricingCategory.LAPTOPS -> LaptopGuidedScreen()','PricingCategory.DESKTOPS -> Desktop()','PricingCategory.MOBILE_PHONES -> MobilePhonePricingScreen()','PricingCategory.GAMING_CONSOLES -> ConsolePricingScreen()'),
'MobilePhonePricingScreen.kt':('featuredPhoneBrands = listOf("Apple", "Samsung", "Google", "OnePlus", "Xiaomi")','otherPhoneBrands()','Search all mobile phones','MobilePhoneDeviceCatalog.search(globalQuery)','MobilePhoneDeviceCatalog.models(brand)','MobilePhoneDeviceCatalog.pricedEntry(it)','MobilePhonePricingCatalog.gradeBuyPercent','MobilePhonePhoto('),
'MobilePhoneDeviceCatalog.kt':('LiveDevicePricing.catalogue()','.filter { it.category == "mobile_phone" }','fun search(query: String, limit: Int = 80)','LiveDevicePricing.find(','MobilePhonePricingCatalog.entries.firstOrNull')}
adaptive_required=('val adaptiveSize = morleyAdaptiveSize()','AdaptiveBackHandler(enabled = showMenu || page != Page.Home)','if (adaptiveSize == MorleyAdaptiveSize.Compact) CompactDashboardNavigation(page, showMenu, ::selectDestination)','if (adaptiveSize != MorleyAdaptiveSize.Compact)','MorleyAdaptiveNavigation(size = adaptiveSize, items = adaptiveNavItems, compact = {})','AdaptiveContentFrame')
forbidden={'DashboardActivity.kt':('HISTORY("History", MorleyIcons.History)','NavCard(MorleyIcons.Categories, "Categories", "Laptops, desktops, mobile phones and gaming consoles", onCategories)','NavCard(MorleyIcons.Console, "Console Pricing", "PS4, PS5, Xbox and Nintendo grade pricing", onConsole)'),'MobilePhoneDeviceCatalog.kt':('ExpandedMobilePhoneCatalog.models','ImportedMobilePhoneCatalog.entries')}
missing=[]
for file_name,needles in required.items(): missing.extend(f'{file_name}: {needle}' for needle in needles if needle not in texts[file_name])
missing.extend(f'DashboardActivity.kt adaptive contract: {needle}' for needle in adaptive_required if needle not in texts['DashboardActivity.kt'])
present=[]
for file_name,needles in forbidden.items(): present.extend(f'{file_name}: {needle}' for needle in needles if needle in texts[file_name])
if missing: raise SystemExit('Pricing navigation drifted; missing: '+' | '.join(missing))
if present: raise SystemExit('Removed navigation/catalogue sources returned: '+' | '.join(present))
print('Adaptive dashboard, Categories, GP navigation, global mobile search, imagery, and shared live-authoritative priced/unpriced phone flows aligned')
