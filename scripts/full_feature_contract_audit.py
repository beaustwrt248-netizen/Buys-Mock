#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
errors = []
checked = []

def read(path: str) -> str:
    p = ROOT / path
    if not p.is_file() or p.stat().st_size == 0:
        errors.append(f"missing required file: {path}")
        return ""
    return p.read_text(encoding="utf-8", errors="ignore")

def require_file(path: str):
    read(path); checked.append(f"file:{path}")

def require_markers(path: str, *markers: str):
    text = read(path)
    for marker in markers:
        if marker not in text: errors.append(f"{path}: missing feature contract marker: {marker}")
    checked.append(f"markers:{path}")

def forbid_markers(path: str, *markers: str):
    text = read(path)
    for marker in markers:
        if marker in text: errors.append(f"{path}: retired feature marker remains: {marker}")
    checked.append(f"forbidden:{path}")

require_file("UI_CHANGE_MASTER_CHECKLIST.md")
require_file("UI_CHANGE_CHECKLIST_LAPTOP_FACTORY_VARIANTS.md")
require_markers("morley-ui-baseline.css", "--morley-ui-font", "#1c2b26", "#167a5a", ".decisionGrid", "#livePricing", "#onlineStatus")
require_markers("morley-ui-baseline.js", "ensureViewport", "normaliseSpelling", "BUYING DECISION", "Seller Ask")
require_markers("mobile-readability-fix.js", "max-width:100vw")
require_markers("web-initial-render-fix.js", "refreshLayout")
require_markers("web-a11y.js", "focus-visible")
require_markers("product-parity-v3.js", "Computer Pricing", "Laptop / MacBook", "Desktop / Gaming PC", "Console Pricing", "General Buys / GP", "Nintendo Switch 2", "A Grade", "B Grade", "C Grade")
require_markers("quick-deal-grade.js", "grade")
for path in ["smart-workspace.js","smart-workspace-plus.js","smart-insights.js","connection-status.js","web-diagnostics.js","web-notification-centre.js","web-issue-report.js","more-menu-v2.js","menu-dialog-a11y.js","secure-pricing.js","deal-workflow.js","desktop-oem.js","desktop-results.js","match-quality.js"]: require_file(path)
require_markers("web-auth.js", "verifyAuthorised")
require_file("signed-in-user.js"); require_file("account-refresh-parity.js")
require_markers("index.html", "morley-ui-baseline.css", "morley-ui-baseline.js", "mobile-readability-fix.js", "web-initial-render-fix.js")
require_markers(".github/workflows/deploy-admin-pages.yml", "web-base.html", "Post-deploy smoke tests")
require_markers("android/app/src/main/java/com/buysloans/hub/MorleyVisualTheme.kt", "MorleyBackground", "MorleySurface", "MorleyAccent", "MorleyTextPrimary", "lightColorScheme")
require_markers("android/app/src/main/java/com/buysloans/hub/DashboardActivity.kt", "CATEGORIES(\"Categories\", MorleyIcons.Categories)", "GP(\"General Buys\", MorleyIcons.Money)", "General Buys / GP", "CategoriesPricingScreen", "ConsolePricingScreen")
forbid_markers("android/app/src/main/java/com/buysloans/hub/DashboardActivity.kt", "NavCard(MorleyIcons.Categories", "NavCard(MorleyIcons.Console")
require_markers("android/app/src/main/java/com/buysloans/hub/CategoriesPricingScreen.kt", "Laptops", "Desktops", "Mobile Phones", "Gaming Consoles", "LaptopGuidedScreen", "MobilePhonePricingScreen")
require_markers("android/app/src/main/java/com/buysloans/hub/MobilePhonePricingScreen.kt", "Apple iPhone", "Samsung Galaxy", "Search all mobile phones", "Select Storage Capacity", "Select Condition Grade", "Quick Summary", "Grade Guide")
require_markers("android/app/src/main/java/com/buysloans/hub/MobilePhonePricingCatalog.kt", "APPLE", "SAMSUNG", "Galaxy S25 Ultra", "Galaxy Z Fold 7", '"A" to 0.70', '"B" to 0.50', '"C" to 0.30')
require_file("android/app/src/main/java/com/buysloans/hub/MobilePhoneDeviceCatalog.kt")
require_file("android/app/src/main/java/com/buysloans/hub/MobilePhonePhoto.kt")
require_file("android/app/src/main/java/com/buysloans/hub/MobilePhonePhotoCatalog.kt")
require_file("android/app/src/main/java/com/buysloans/hub/ComputerConsolePricingScreens.kt")
require_markers("android/app/src/main/java/com/buysloans/hub/LaptopGuidedScreen.kt", "Verified version / model code", "Manufacturer-verified configuration", "Seller Ask", "contentColor = Color.White", "LaptopFactoryVariantCatalog.configurationVerified")
require_markers("android/app/src/main/java/com/buysloans/hub/LaptopFactoryVariantCatalog.kt", "LaptopFactoryProfile", "LaptopFactoryVersion", "exactVariants", "configurationVerified", "canonicalQuery", "CX1400", "CX1405CTA")
for path in ["android/app/src/test/java/com/buysloans/hub/LaptopFactoryVariantCatalogTest.kt","android/app/src/main/java/com/buysloans/hub/SmartWorkspaceSection.kt","android/app/src/main/java/com/buysloans/hub/ComparableEvidenceDecisionAdapter.kt","android/app/src/main/java/com/buysloans/hub/ComparableSalesQuality.kt","android/app/src/main/java/com/buysloans/hub/CompleteValuationDecision.kt","android/app/src/main/java/com/buysloans/hub/ValuationHistoryManager.kt"]: require_file(path)
require_markers("android/app/src/main/java/com/buysloans/hub/AuthActivity.kt", "TURNSTILE", "password")
require_markers("android/app/src/main/java/com/buysloans/hub/SupportTicketActivity.kt", "B&L Morley Support", "New Ticket", "Include diagnostics", "Send Reply")
require_file("android/app/src/main/java/com/buysloans/hub/SupportTicketClient.kt")
require_markers("android/app/build.gradle", "versionCode", "versionName", "verifyReleaseSigning")
require_markers(".github/workflows/ultimate-parity.yml", "NFC and valuation regression tests")
require_markers(".github/workflows/ota-version-policy.yml", "Morley OTA Version Policy")
require_markers(".github/workflows/auto-ota-release.yml", "sha256")
require_markers("ota/latest.json", "versionCode", "versionName", "apkUrl", "sha256")
require_markers("admin/index.html", "inviteName", "First and last name")
require_markers("admin/login-security.js", "turnstile", "credentialsReady", "syncLoginEnabled", "emailInput.checkValidity()", "options:{captchaToken:token}")
for path in ["admin/styles.css","admin/support-tickets.js","admin/audit-triage.js","admin/control-governance.js","admin/user-management-policy.js","admin/invites.js","admin/download-invites.js","admin/targeted-notifications.js","admin/release-control.js"]: require_file(path)
require_markers("admin/app.js", "set_role", "set_display_name", "admin-user-control")
require_markers(".github/workflows/admin-support-governance.yml", "support")
require_markers(".github/workflows/admin-device-governance.yml", "device")
require_markers(".github/workflows/admin-control-integration.yml", "Admin Control")
for workflow in [".github/workflows/quality-gate.yml",".github/workflows/security-audit.yml",".github/workflows/ui-consistency.yml",".github/workflows/ui-pr-checklist-gate.yml",".github/workflows/web-smoke.yml",".github/workflows/ultimate-parity.yml"]: require_file(workflow)
if errors:
    print("Full feature contract audit FAILED:")
    for error in errors: print(f" - {error}")
    sys.exit(1)
print(f"Full feature contract audit passed ({len(checked)} contract groups checked)")
