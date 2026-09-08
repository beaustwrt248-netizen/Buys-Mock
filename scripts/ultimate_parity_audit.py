from pathlib import Path
import json
import sys

ROOT = Path(__file__).resolve().parents[1]
errors = []

def read(path):
    p = ROOT / path
    if not p.is_file():
        errors.append(f"Missing required file: {path}")
        return ""
    return p.read_text(encoding="utf-8")

def require(text, needle, label):
    if needle not in text:
        errors.append(f"Missing {label}: {needle}")

def forbid(text, needle, label):
    if needle in text:
        errors.append(f"Retired {label} remains: {needle}")

index = read("index.html")
web_parity = read("ultimate-parity.js")
web_css = read("morley-app-parity-v2.css")
android_theme = read("android/app/src/main/java/com/buysloans/hub/MorleyVisualTheme.kt")
android_help = read("android/app/src/main/java/com/buysloans/hub/HelpGuideActivity.kt")
dashboard = read("android/app/src/main/java/com/buysloans/hub/DashboardActivity.kt")
categories = read("android/app/src/main/java/com/buysloans/hub/CategoriesPricingScreen.kt")
phones = read("android/app/src/main/java/com/buysloans/hub/MobilePhonePricingScreen.kt")
phone_catalog = read("android/app/src/main/java/com/buysloans/hub/MobilePhonePricingCatalog.kt")
mobile_live_catalog = read("android/app/src/main/java/com/buysloans/hub/MobilePhoneDeviceCatalog.kt")
console_catalog = read("android/app/src/main/java/com/buysloans/hub/ConsolePricingCatalog.kt")
console_ui = read("android/app/src/main/java/com/buysloans/hub/ComputerConsolePricingScreens.kt")
live_pricing = read("android/app/src/main/java/com/buysloans/hub/LiveDevicePricing.kt")
manifest = read("android/app/src/main/AndroidManifest.xml")
nfc_logic = read("android/app/src/main/java/com/buysloans/hub/NfcScanLogic.kt")
nfc_activity = read("android/app/src/main/java/com/buysloans/hub/NfcScannerActivity.kt")
guardian_html = read("admin/guardian.html")
guardian_js = read("admin/guardian.js")
guardian_health = read("admin/guardian-health.js")
menu_js = read("more-menu-v2.js")

for label in ('CATEGORIES("Categories", MorleyIcons.Categories)', 'GP("General Buys", MorleyIcons.Money)', "General Buys / GP"):
    require(dashboard, label, "Android primary navigation")
for label in ("Categories → Laptops", "Categories → Desktops", "Categories → Mobile Phones", "Categories → Gaming Consoles", "General Buys / GP", "More → Support", "NFC", "Valuation"):
    require(android_help, label, "Android Help/FAQ content")
for retired in ("Computer Pricing", "Console Pricing", "Menu → Report an Issue", "Menu → Updates", "Menu → Backup & Data"):
    forbid(android_help, retired, "Android Help/FAQ navigation")
for label in ("Categories", "Laptops", "Desktops", "Mobile Phones", "Gaming Consoles", "General Buys / GP", "More → Support", "More → Updates", "NFC", "Valuation"):
    require(web_parity, label, "web Help/FAQ content")
for retired in ("Computer Pricing", "Console Pricing", "Menu → Report an Issue", "Menu → Updates", "Menu → Backup & Data"):
    forbid(web_parity, retired, "web Help/FAQ navigation")
for token in ("#f5f7f4", "#ffffff", "#167a5a", "#1c2b26", "#cedbd5", "color-scheme:light"):
    require(web_parity.lower(), token.lower(), "web Help light-theme token")
for retired in ("#080b0d", "#101619", "#151d20", "color-scheme:dark"):
    forbid(web_parity.lower(), retired.lower(), "web Help dark-theme token")
require(index, "ultimate-parity.js?v=2", "web parity bootstrap")
require(dashboard, "Page.Laptop -> CategoriesPricingScreen()", "Categories pricing route")
require(dashboard, "Page.Desktop -> ConsolePricingScreen()", "separate Console Pricing route")
require(dashboard, "MorleyIcons.Categories", "Categories vector icon")
for adaptive in (
    "val adaptiveSize = morleyAdaptiveSize()",
    "AdaptiveBackHandler(enabled = showMenu || page != Page.Home)",
    "MorleyAdaptiveNavigation(size = adaptiveSize, items = adaptiveNavItems, compact = {})",
    "AdaptiveContentFrame",
):
    require(dashboard, adaptive, "Android adaptive dashboard integration")
forbid(dashboard, "BottomDestination.HISTORY", "History bottom navigation")
forbid(dashboard, "NavCard(MorleyIcons.Console", "Console Pricing Home shortcut")
for label in ("Laptops", "Desktops", "Mobile Phones", "Gaming Consoles"):
    require(categories, label, f"Categories subcategory {label}")
require(categories, "MobilePhonePricingScreen()", "Mobile Phones route")
for label in ("Apple iPhone", "Samsung Galaxy", "Select Storage Capacity", "Select Condition Grade"):
    require(phones, label, f"mobile pricing UI {label}")
for label in ('"A" to 0.70', '"B" to 0.50', '"C" to 0.30'):
    require(phone_catalog, label, f"mobile pricing policy {label}")
require(mobile_live_catalog, 'filter { it.category == "mobile_phone" }', "live mobile catalogue source")
require(mobile_live_catalog, "LiveDevicePricing.find", "live mobile authoritative price source")

# Canonical data, not compiled device names/prices, defines current console and mobile coverage.
require(console_catalog, 'filter { it.category == "console" }', "live console catalogue source")
require(console_catalog, "LiveDevicePricing.find", "live console authoritative price source")
require(console_catalog, "device.family", "canonical console family/series source")
require(console_ui, "Search all consoles", "console global search")
require(console_ui, "Price to be added", "safe unpriced console state")
require(console_catalog, "fun buyPrice(entry: ConsoleDeviceEntry", "unpriced console buy boundary")
forbid(console_catalog, "private val catalogueSeed", "compiled console catalogue")
read("android/app/src/test/java/com/buysloans/hub/ConsolePricingCatalogTest.kt")
require(live_pricing, "catalog_sync_state?id=eq.1&select=revision", "native catalogue revision reconciliation")

for token in ("0xFFF5F7F4", "0xFFFFFFFF", "0xFF167A5A", "0xFF1C2B26", "0xFFCEDBD5"):
    require(android_theme, token, "Android Morley theme token")
require(android_theme, "lightColorScheme", "Android light color scheme")
web_light = read("morley-light-web.css")
require(index, "morley-light-web.css?v=2", "web light theme layer")
for token in ("#f5f7f4", "#ffffff", "#167a5a", "#1c2b26", "#cedbd5"):
    if token.lower() not in web_light.lower():
        errors.append(f"Web Morley theme token missing: {token}")
for retired in ("0xFF16C7FF", "0xFF2684FF", "0xFF030712", "0xFF0B1528"):
    forbid(android_help, retired, "Help-only blue/cyan theme token")

require(manifest, 'android.permission.NFC', "NFC permission")
require(manifest, 'android.hardware.nfc', "NFC hardware declaration")
require(manifest, 'android:required="false"', "optional NFC hardware policy")
require(manifest, '.NfcScannerActivity', "NFC scanner activity")
for needle in ("NfcCapability.UNAVAILABLE", "NfcCapability.DISABLED", "NfcCapability.READY", "shouldAccept", "parseWellKnown"):
    require(nfc_logic, needle, "NFC logic guard")
require(nfc_activity, "NfcScanLogic", "NFC activity logic integration")
read("android/app/src/test/java/com/buysloans/hub/NfcScanLogicTest.kt")
read("android/app/src/test/java/com/buysloans/hub/TestBuyNfcEvidenceBoundaryTest.kt")

for path in (
    "android/app/src/test/java/com/buysloans/hub/Valuation3FinalSmokeTest.kt",
    "android/app/src/test/java/com/buysloans/hub/ValuationDecisionEngineTest.kt",
    "android/app/src/test/java/com/buysloans/hub/Valuation3FinancialInvariantTest.kt",
    "android/app/src/test/java/com/buysloans/hub/Valuation3RegressionSentinelTest.kt",
    "android/app/src/test/java/com/buysloans/hub/Valuation3CompletionRegressionTest.kt",
):
    read(path)

require(dashboard, "NavigationBarItem", "Android bottom navigation")
read("android/app/src/main/java/com/buysloans/hub/MorleyIcons.kt")
for path in (
    "android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml",
    "android/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml",
    "android/app/src/main/res/drawable/ic_notification.xml",
    "more-menu-v2.css",
):
    read(path)
for action in ("deals", "inventory", "sales", "scanner", "account", "privacy", "backup", "notifications", "display", "help", "updates", "report", "legal", "about"):
    require(menu_js, f"'{action}'", f"web menu action {action}")

require(guardian_html, "guardian-health.js?v=1", "Guardian health telemetry bootstrap")
for needle in ("Human approval for code changes", "Emergency kill switch", "guarded_auto"):
    require(guardian_html, needle, "Guardian control surface")
for needle in ("guardian_set_controls", "guardian_decide_incident"):
    require(guardian_js, needle, "Guardian audited RPC")
for needle in ("human-controlled", "Stale open incidents", "kill_switch", "requires_approval"):
    require(guardian_health, needle, "Guardian health safety signal")

combined = "\n".join((android_help, dashboard, categories, phones, console_ui, web_parity, guardian_html))
for typo in ("Valution", "Consol Pricing", "Macbook", "signout everywhere", "Seller ask is the price quoted by the seller seller"):
    if typo in combined:
        errors.append(f"Copy-quality sentinel found: {typo}")

for evidence_path in sorted((ROOT / "nova").glob("catalogue-*.json")):
    try:
        evidence = json.loads(evidence_path.read_text(encoding="utf-8"))
    except Exception as exc:
        errors.append(f"Invalid Nova catalogue evidence JSON {evidence_path.name}: {exc}")
        continue
    if "execution_authorized" in evidence and evidence["execution_authorized"] is not False:
        errors.append(f"Nova catalogue evidence must not authorize production execution: {evidence_path.name}")

if errors:
    print("ULTIMATE PARITY AUDIT FAILED", file=sys.stderr)
    for e in errors:
        print(f"- {e}", file=sys.stderr)
    raise SystemExit(1)

print("Ultimate parity audit passed: current Categories/GP/More navigation, light Help/FAQ, live console/mobile catalogues, NFC, valuation coverage, icons/menu, Guardian safety contracts and Nova catalogue execution boundaries are aligned.")
