#!/usr/bin/env python3
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
errors: list[str] = []


def read(path: str) -> str:
    p = ROOT / path
    if not p.exists():
        errors.append(f"missing required file: {path}")
        return ""
    return p.read_text(encoding="utf-8")


def require(condition: bool, message: str) -> None:
    if not condition:
        errors.append(message)


def semantic_version(value: str) -> tuple[int, int, int] | None:
    match = re.fullmatch(r"(\d+)\.(\d+)\.(\d+)(?:[-+].*)?", value.strip())
    return tuple(map(int, match.groups())) if match else None


admin_html = read("admin/index.html")
admin_app = read("admin/app.js")
login_security = read("admin/login-security.js")
release_control = read("admin/release-control.js")
support_tickets = read("admin/support-tickets.js")
targeted_notifications = read("admin/targeted-notifications.js")
invites = read("admin/invites.js")
audit_triage = read("admin/audit-triage.js")
admin_activity = read("android/adminapp/src/main/java/com/buysloans/admin/AdminActivity.kt")
admin_web_policy = read("android/adminapp/src/main/java/com/buysloans/admin/AdminWebParityPolicy.kt")
admin_api = read("android/adminapp/src/main/java/com/buysloans/admin/AdminApi.kt")
update_manager = read("android/app/src/main/java/com/buysloans/hub/UpdateManager.kt")
update_activity = read("android/app/src/main/java/com/buysloans/hub/UpdateActivity.kt")
morley_application = read("android/app/src/main/java/com/buysloans/hub/MorleyApplication.kt")
ota_policy = read("android/app/src/main/java/com/buysloans/hub/OtaFeaturePolicy.kt")
build_gradle = read("android/app/build.gradle")
ota_manifest_text = read("ota/latest.json")
ota_migration = read("supabase/migrations/20260829192000_enable_ota_feature_control.sql")

# Web Admin Control: every visible section must have both markup and a functional implementation hook.
web_sections = {
    "overview": ["metricUsers", "metricDevices", "metricVersion", "metricQueued"],
    "users": ["usersList", "createInviteBtn", "invitesList"],
    "devices": ["devicesList"],
    "tickets": ["ticketsList", "ticketDetail", "ticketReplyBtn"],
    "controls": ["featureFlags", "maintenanceMessage", "saveControlsBtn"],
    "release": ["releaseName", "releaseCode", "releaseSha", "releaseUrl", "saveReleaseBtn"],
    "notify": ["notifTitle", "notifBody", "queueNotifBtn", "notificationList"],
    "announce": ["annTitle", "annBody", "publishAnnBtn", "annList"],
    "audit": ["auditSearch", "auditActionFilter", "auditRefreshBtn", "auditList"],
}
for section, ids in web_sections.items():
    require(f'data-tab="{section}"' in admin_html, f"Admin web tab missing: {section}")
    require(f'id="tab-{section}"' in admin_html, f"Admin web panel missing: {section}")
    for element_id in ids:
        require(f'id="{element_id}"' in admin_html, f"Admin web {section} control missing: {element_id}")

require("captchaToken:token" in login_security, "Admin web login is not submitting the Turnstile token")
require("loadUsers" in admin_app and "admin-user-control" in admin_app, "Accounts/user-control wiring is incomplete")
require("loadDevices" in admin_app and "app_version" in admin_app, "Device/version visibility wiring is incomplete")
require("ticketReplyBtn" in support_tickets and "support_ticket_messages" in support_tickets, "Support reply wiring is incomplete")
require("assigned_to" in support_tickets and "ticketPriority" in support_tickets, "Support assignment/priority controls are incomplete")
require("notification_jobs" in admin_app and "send-admin-notification" in admin_app, "Notification delivery wiring is incomplete")
require("announcements" in admin_app and "publishAnnBtn" in admin_app, "Announcement publishing wiring is incomplete")
require("admin_audit_log" in audit_triage or "admin_audit_log" in admin_app, "Audit-log read path is missing")
require("redeem-app-invite" in invites or "app_invites" in invites, "Invite governance wiring is incomplete")
require("target_installation_id" in targeted_notifications or "notifTarget" in targeted_notifications, "Targeted notification wiring is incomplete")

# Android Admin Control: either the legacy native dashboard must implement each
# section directly, or the app must use the canonical HTTPS Admin surface behind
# a hardened path/origin boundary. The latter intentionally makes web Admin the
# single functional implementation so filters, permissions and Guardian changes
# cannot drift between browser and APK releases.
web_parity_shell = (
    "WebView" in admin_activity
    and "AdminWebParityPolicy.HOME_URL" in admin_activity
    and "AdminWebParityPolicy.isTrustedAdminUrl" in admin_activity
    and 'HOME_URL = "https://buyshub.me/admin/"' in admin_web_policy
    and 'private const val ADMIN_HOST = "buyshub.me"' in admin_web_policy
    and 'uri.scheme.equals("https"' in admin_web_policy
    and '(path == "/admin" || path.startsWith("/admin/"))' in admin_web_policy
    and "MIXED_CONTENT_NEVER_ALLOW" in admin_activity
    and "allowFileAccess = false" in admin_activity
    and "allowContentAccess = false" in admin_activity
)

if web_parity_shell:
    require("javaScriptEnabled = true" in admin_activity, "Android Admin parity shell must enable the canonical Admin JavaScript application")
    require("domStorageEnabled = true" in admin_activity, "Android Admin parity shell must enable DOM storage for web authentication state")
    require("setAcceptCookie(true)" in admin_activity, "Android Admin parity shell must enable required authentication cookies")
else:
    for tab in ["Health", "Tickets", "Staff alerts", "Users & devices", "Controls", "Audit", "Release"]:
        require(f'"{tab}"' in admin_activity, f"Android Admin tab missing: {tab}")
    require("UserManagementPanel" in admin_activity, "Android Admin user-management panel is missing")

# Native Admin APIs remain covered because they are still part of the package and
# provide the legacy/fallback implementation and independently tested policies.
require("updateMaintenanceConfig" in admin_api, "Android Admin maintenance write path is missing")
require("updateSupportTicket" in admin_api, "Android Admin ticket update path is missing")
require("sendSupportReply" in admin_api, "Android Admin support reply path is missing")
require("loadSupportNotes" in admin_api, "Android Admin internal-note read path is missing")
require("admin_audit_log" in admin_api, "Android Admin audit read path is missing")

# OTA: normal main is exact-match. A release PR may be exactly one monotonic
# versionCode ahead while the signed manifest still points to the last release.
try:
    ota_manifest = json.loads(ota_manifest_text)
except Exception as exc:
    ota_manifest = {}
    errors.append(f"OTA manifest is invalid JSON: {exc}")

version_code_match = re.search(r"\bversionCode\s+(\d+)", build_gradle)
version_name_match = re.search(r"\bversionName\s+['\"]([^'\"]+)['\"]", build_gradle)
require(version_code_match is not None, "Android versionCode could not be parsed")
require(version_name_match is not None, "Android versionName could not be parsed")
if version_code_match and version_name_match:
    source_code = int(version_code_match.group(1))
    source_name = version_name_match.group(1)
    ota_code = int(ota_manifest.get("versionCode", 0) or 0)
    ota_name = str(ota_manifest.get("versionName", ""))
    if source_code == ota_code:
        require(source_name == ota_name, "OTA versionName does not match Android source for the same versionCode")
    elif source_code == ota_code + 1:
        source_semver = semantic_version(source_name)
        ota_semver = semantic_version(ota_name)
        require(source_semver is not None and ota_semver is not None, "Pending release versions must use semantic x.y.z names")
        if source_semver is not None and ota_semver is not None:
            require(source_semver > ota_semver, "Pending Android release versionName must advance beyond the published OTA version")
    else:
        require(False, f"Android source versionCode {source_code} must equal published OTA {ota_code} or be exactly one pending release ahead")
require(bool(re.fullmatch(r"[0-9a-fA-F]{64}", str(ota_manifest.get("sha256", "")))), "OTA SHA-256 is missing or invalid")
require(str(ota_manifest.get("apkUrl", "")).startswith("https://github.com/beaustwrt248-netizen/Buys-Mock/releases/download/"), "OTA APK URL is not a trusted repository release URL")
require("UpdateCheckScheduler.schedule(this)" in morley_application, "Automatic OTA scheduling is not enabled at application startup")
require("OtaFeaturePolicy.isEnabled()" in update_manager, "UpdateManager does not honor the Admin OTA enable flag")
require("otaEnabled" in ota_policy, "Morley OTA feature policy does not consume otaEnabled")
require("otaEnabled" in ota_migration and "admin_audit_log" in ota_migration, "OTA enable flag is not governed and audited by migration")
require("otaEnabled" in release_control and "saveOtaEnabled" in release_control, "Admin Release section does not expose the OTA checkbox")
require("sha256OfUri" in update_activity and "APK integrity check failed" in update_activity, "OTA installer does not verify the downloaded APK checksum")
require("isTrustedApkUrl" in update_manager and "isValidSha256" in update_manager, "OTA metadata security validation is incomplete")

# Remote config least privilege: only maintenance and OTA switches can change inside feature_flags.
require("only maintenanceMode, maintenanceMessage and otaEnabled may be changed remotely" in ota_migration, "Remote-config allowlist is broader than expected")
require("private.is_admin_or_manager()" in ota_migration, "Remote-config mutation is not Admin/Manager gated")
require("revoke execute on function private.admin_set_config_impl(text,jsonb) from authenticated" in ota_migration, "Private remote-config implementation is directly executable by authenticated clients")

if errors:
    print("Admin Control integration audit FAILED")
    for error in errors:
        print(f" - {error}")
    sys.exit(1)

print("Admin Control integration audit PASSED")
print("Verified sections: overview, accounts, devices, tickets, controls, release/OTA, notifications, announcements, audit, Android Admin parity.")
