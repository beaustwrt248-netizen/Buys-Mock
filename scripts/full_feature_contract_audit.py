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
    read(path)
    checked.append(f"file:{path}")


def require_markers(path: str, *markers: str):
    text = read(path)
    for marker in markers:
        if marker not in text:
            errors.append(f"{path}: missing feature contract marker: {marker}")
    checked.append(f"markers:{path}")


# Governance / canonical presentation
require_file("UI_CHANGE_MASTER_CHECKLIST.md")
require_markers("morley-ui-baseline.css", "--morley-ui-font", "#1c2b26", "#167a5a", ".decisionGrid", "#livePricing", "#onlineStatus")
require_markers("morley-ui-baseline.js", "ensureViewport", "normaliseSpelling", "BUYING DECISION", "Seller Ask")
require_markers("mobile-readability-fix.js", "max-width:100vw")
require_markers("web-initial-render-fix.js", "refreshLayout")
require_markers("web-a11y.js", "focus-visible")

# Web product/navigation contracts
require_markers(
    "product-parity-v3.js",
    "Computer Pricing",
    "Laptop / MacBook",
    "Desktop / Gaming PC",
    "Console Pricing",
    "General Buys / GP",
    "Nintendo Switch 2",
    "A Grade",
    "B Grade",
    "C Grade",
)
require_markers("quick-deal-grade.js", "grade")
require_file("smart-workspace.js")
require_file("smart-workspace-plus.js")
require_file("smart-insights.js")
require_file("connection-status.js")
require_file("web-diagnostics.js")
require_file("web-notification-centre.js")
require_file("web-issue-report.js")
require_file("more-menu-v2.js")
require_file("menu-dialog-a11y.js")
require_file("secure-pricing.js")
require_file("deal-workflow.js")
require_file("desktop-oem.js")
require_file("desktop-results.js")
require_file("match-quality.js")

# Web auth/account/release shell
require_markers("web-auth.js", "verifyAuthorised")
require_file("signed-in-user.js")
require_file("account-refresh-parity.js")
require_markers("index.html", "morley-ui-baseline.css", "morley-ui-baseline.js", "mobile-readability-fix.js", "web-initial-render-fix.js")
require_markers(".github/workflows/deploy-admin-pages.yml", "web-base.html", "Post-deploy smoke tests")

# Android canonical theme/navigation/product contracts
require_markers(
    "android/app/src/main/java/com/buysloans/hub/MorleyVisualTheme.kt",
    "MorleyBackground",
    "MorleySurface",
    "MorleyAccent",
    "MorleyTextPrimary",
    "lightColorScheme",
)
require_markers(
    "android/app/src/main/java/com/buysloans/hub/DashboardActivity.kt",
    "Computer Pricing",
    "Console Pricing",
    "General Buys / GP",
    "ComputerPricingScreen",
    "ConsolePricingScreen",
)
require_file("android/app/src/main/java/com/buysloans/hub/ComputerConsolePricingScreens.kt")
require_file("android/app/src/main/java/com/buysloans/hub/SmartWorkspaceSection.kt")
require_file("android/app/src/main/java/com/buysloans/hub/ComparableEvidenceDecisionAdapter.kt")
require_file("android/app/src/main/java/com/buysloans/hub/ComparableSalesQuality.kt")
require_file("android/app/src/main/java/com/buysloans/hub/CompleteValuationDecision.kt")
require_file("android/app/src/main/java/com/buysloans/hub/ValuationHistoryManager.kt")
require_markers("android/app/src/main/java/com/buysloans/hub/AuthActivity.kt", "TURNSTILE", "password")
require_markers("android/app/src/main/java/com/buysloans/hub/SupportTicketActivity.kt", "B&L Morley Support", "New Ticket", "Include diagnostics", "Send Reply")
require_file("android/app/src/main/java/com/buysloans/hub/SupportTicketClient.kt")

# Android build/release/NFC boundaries
require_markers("android/app/build.gradle", "versionCode", "versionName", "verifyReleaseSigning")
require_markers(".github/workflows/ultimate-parity.yml", "NFC and valuation regression tests")
require_markers(".github/workflows/ota-version-policy.yml", "Morley OTA Version Policy")
require_markers(".github/workflows/auto-ota-release.yml", "sha256")
require_markers("ota/latest.json", "versionCode", "versionName", "apkUrl", "sha256")

# Admin Control contracts
require_markers("admin/index.html", "inviteName", "First and last name")
require_markers("admin/login-security.js", "turnstile", "credentialsReady", "syncLoginEnabled", "emailInput.checkValidity()", "options:{captchaToken:token}")
require_file("admin/styles.css")
require_file("admin/support-tickets.js")
require_file("admin/audit-triage.js")
require_file("admin/control-governance.js")
require_file("admin/user-management-policy.js")
require_file("admin/invites.js")
require_file("admin/download-invites.js")
require_file("admin/targeted-notifications.js")
require_file("admin/release-control.js")
require_markers("admin/app.js", "set_role", "set_display_name", "admin-user-control")
require_markers(".github/workflows/admin-support-governance.yml", "support")
require_markers(".github/workflows/admin-device-governance.yml", "device")
require_markers(".github/workflows/admin-control-integration.yml", "Admin Control")

# Existing regression/safety gates must remain present.
for workflow in [
    ".github/workflows/quality-gate.yml",
    ".github/workflows/security-audit.yml",
    ".github/workflows/ui-consistency.yml",
    ".github/workflows/ui-pr-checklist-gate.yml",
    ".github/workflows/web-smoke.yml",
    ".github/workflows/ultimate-parity.yml",
]:
    require_file(workflow)

if errors:
    print("Full feature contract audit FAILED:")
    for error in errors:
        print(f" - {error}")
    sys.exit(1)

print(f"Full feature contract audit passed ({len(checked)} contract groups checked)")
