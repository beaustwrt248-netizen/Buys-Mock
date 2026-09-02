from pathlib import Path
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
manifest = read("android/app/src/main/AndroidManifest.xml")
nfc_logic = read("android/app/src/main/java/com/buysloans/hub/NfcScanLogic.kt")
nfc_activity = read("android/app/src/main/java/com/buysloans/hub/NfcScannerActivity.kt")
guardian_html = read("admin/guardian.html")
guardian_js = read("admin/guardian.js")
guardian_health = read("admin/guardian-health.js")
menu_js = read("more-menu-v2.js")

# Shared product navigation and help terminology.
for label in ("Categories", "Console Pricing", "General Buys / GP"):
    require(dashboard, label, "Android primary navigation")
# Help/web retain their descriptive Computer Pricing terminology for the computer-specific feature.
for label in ("Computer Pricing", "Console Pricing", "General Buys / GP", "NFC", "Valuation"):
    require(android_help, label, "Android Help/FAQ content")
for label in ("Computer Pricing", "Console Pricing", "General Buys / GP", "NFC", "Valuation"):
    require(web_parity, label, "web Help/FAQ content")
require(index, "ultimate-parity.js?v=1", "web parity bootstrap")
require(dashboard, "Page.Laptop->CategoriesPricingScreen()", "Categories pricing route")
require(dashboard, "Page.Desktop->ConsolePricingScreen()", "separate Console Pricing route")
require(dashboard, "MorleyIcons.Categories", "Categories vector icon")
require(dashboard, "MorleyIcons.Console", "Console vector icon")
for label in ("Laptops", "Desktops", "Mobile Phones", "Gaming Consoles"):
    require(categories, label, f"Categories subcategory {label}")
require(categories, "MobilePhonePricingScreen()", "Mobile Phones route")
for label in ("Apple iPhone", "Samsung Galaxy", "Select Storage Capacity", "Select Condition Grade"):
    require(phones, label, f"mobile pricing UI {label}")
for label in ("Galaxy S25 Ultra", "Galaxy Z Fold 7", '"A" to 0.70', '"B" to 0.50', '"C" to 0.30'):
    require(phone_catalog, label, f"mobile pricing catalog {label}")

# Morley light/emerald theme contract across native and web surfaces.
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

# NFC release contract: optional hardware, permission, activity and guarded parsing.
require(manifest, 'android.permission.NFC', "NFC permission")
require(manifest, 'android.hardware.nfc', "NFC hardware declaration")
require(manifest, 'android:required="false"', "optional NFC hardware policy")
require(manifest, '.NfcScannerActivity', "NFC scanner activity")
for needle in ("NfcCapability.UNAVAILABLE", "NfcCapability.DISABLED", "NfcCapability.READY", "shouldAccept", "parseWellKnown"):
    require(nfc_logic, needle, "NFC logic guard")
require(nfc_activity, "NfcScanLogic", "NFC activity logic integration")
read("android/app/src/test/java/com/buysloans/hub/NfcScanLogicTest.kt")
read("android/app/src/test/java/com/buysloans/hub/TestBuyNfcEvidenceBoundaryTest.kt")

# Valuation engine regression coverage must remain present.
for path in (
    "android/app/src/test/java/com/buysloans/hub/Valuation3FinalSmokeTest.kt",
    "android/app/src/test/java/com/buysloans/hub/ValuationDecisionEngineTest.kt",
    "android/app/src/test/java/com/buysloans/hub/Valuation3FinancialInvariantTest.kt",
    "android/app/src/test/java/com/buysloans/hub/Valuation3RegressionSentinelTest.kt",
    "android/app/src/test/java/com/buysloans/hub/Valuation3CompletionRegressionTest.kt",
):
    read(path)

# Icon/menu loading contract.
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

# Guardian safety posture: ultimate upgrade must not weaken human boundaries.
require(guardian_html, "guardian-health.js?v=1", "Guardian health telemetry bootstrap")
for needle in ("Human approval for code changes", "Emergency kill switch", "guarded_auto"):
    require(guardian_html, needle, "Guardian control surface")
for needle in ("guardian_set_controls", "guardian_decide_incident"):
    require(guardian_js, needle, "Guardian audited RPC")
for needle in ("human-controlled", "Stale open incidents", "kill_switch", "requires_approval"):
    require(guardian_health, needle, "Guardian health safety signal")

# Basic copy-quality sentinels for recurring user-visible mistakes.
combined = "\n".join((android_help, dashboard, categories, phones, web_parity, guardian_html))
for typo in ("Valution", "Consol Pricing", "Macbook", "signout everywhere", "Seller ask is the price quoted by the seller seller"):
    if typo in combined:
        errors.append(f"Copy-quality sentinel found: {typo}")

if errors:
    print("ULTIMATE PARITY AUDIT FAILED", file=sys.stderr)
    for e in errors:
        print(f"- {e}", file=sys.stderr)
    raise SystemExit(1)

print("Ultimate parity audit passed: Categories/Console navigation, mobile price sheets, theme, Help/FAQ, NFC, valuation coverage, icons/menu and Guardian safety contracts are aligned.")
