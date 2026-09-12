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
admin_login = read("android/adminapp/src/main/java/com/buysloans/admin/AdminLoginActivity.kt")
admin_session_store = read("android/adminapp/src/main/java/com/buysloans/admin/AdminSessionStore.kt")
admin_dashboard = read("android/adminapp/src/main/java/com/buysloans/admin/AdminNativeDashboard.kt")
admin_api = read("android/adminapp/src/main/java/com/buysloans/admin/AdminApi.kt")
update_manager = read("android/app/src/main/java/com/buysloans/hub/UpdateManager.kt")
update_activity = read("android/app/src/main/java/com/buysloans/hub/UpdateActivity.kt")
morley_application = read("android/app/src/main/java/com/buysloans/hub/MorleyApplication.kt")
ota_policy = read("android/app/src/main/java/com/buysloans/hub/OtaFeaturePolicy.kt")
build_gradle = read("android/app/build.gradle")
ota_manifest_text = read("ota/latest.json")
ota_migration = read("supabase/migrations/20260829192000_enable_ota_feature_control.sql")

# Browser Admin remains an independent client and must keep its own complete auth and control surface.
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
require("turnstile.render(challengeHost" in login_security, "Admin web Turnstile is not rendered directly on the login surface")
require("challenges.cloudflare.com/turnstile/v0/api.js?render=explicit" in login_security, "Admin web Turnstile API source is missing")
require("challengeWatchdog" in login_security, "Admin web Turnstile loading is not bounded by a recovery watchdog")
require("loadUsers" in admin_app and "admin-user-control" in admin_app, "Accounts/user-control wiring is incomplete")
require("loadDevices" in admin_app and "app_version" in admin_app, "Device/version visibility wiring is incomplete")
require("ticketReplyBtn" in support_tickets and "support_ticket_messages" in support_tickets, "Support reply wiring is incomplete")
require("assigned_to" in support_tickets and "ticketPriority" in support_tickets, "Support assignment/priority controls are incomplete")
require("notification_jobs" in admin_app and "send-admin-notification" in admin_app, "Notification delivery wiring is incomplete")
require("announcements" in admin_app and "publishAnnBtn" in admin_app, "Announcement publishing wiring is incomplete")
require("admin_audit_log" in audit_triage or "admin_audit_log" in admin_app, "Audit-log read path is missing")
require("redeem-app-invite" in invites or "app_invites" in invites, "Invite governance wiring is incomplete")
require("target_installation_id" in targeted_notifications or "notifTarget" in targeted_notifications, "Targeted notification wiring is incomplete")

# Android Admin is now a native authenticated client. The browser Admin is not its workspace,
# session store, logout route or navigation fallback.
require("AdminSessionStore.current()" in admin_activity, "Android Admin does not read the native session owner")
require("AdminSessionStore.hasAuthorizedSession()" in admin_activity, "Android Admin does not verify the native session before workspace entry")
require("AdminNativeDashboard" in admin_activity, "Android Admin does not enter the native Compose dashboard")
for forbidden in [
    "WebView",
    "evaluateJavascript",
    "loadUrl(",
    "installNativeAdminSession",
    "native-logout",
    "AdminWebParityPolicy",
    "EXTRA_ACCESS_TOKEN",
    "EXTRA_REFRESH_TOKEN",
]:
    require(forbidden not in admin_activity, f"Authenticated Android Admin workspace still contains retired browser handoff symbol: {forbidden}")

require("AdminSessionStore.set(session)" in admin_login, "Native Admin login does not install the authorized Android session")
require("Intent(this, AdminActivity::class.java)" in admin_login, "Native Admin login does not navigate to AdminActivity")
store_index = admin_login.find("AdminSessionStore.set(session)")
navigation_index = admin_login.find("Intent(this, AdminActivity::class.java)")
require(store_index >= 0 and navigation_index > store_index, "Native Admin session must be installed before workspace navigation")
require("putExtra(AdminActivity.EXTRA_ACCESS_TOKEN" not in admin_login, "Admin access token is still transferred through an Intent")
require("putExtra(AdminActivity.EXTRA_REFRESH_TOKEN" not in admin_login, "Admin refresh token is still transferred through an Intent")
require(admin_login.count("addJavascriptInterface(") == 1, "Admin Android must have exactly one JavaScript bridge, scoped to Turnstile login")
require('"AndroidBridge"' in admin_login, "Admin Turnstile bridge identity is missing")
require("domStorageEnabled = false" in admin_login, "Admin Turnstile WebView must not enable DOM storage")
require("allowFileAccess = false" in admin_login, "Admin Turnstile WebView must disable file access")
require("allowContentAccess = false" in admin_login, "Admin Turnstile WebView must disable content access")

require("private var active: AdminSession?" in admin_session_store, "Native Admin session store is missing its process-scoped session")
require("fun clear()" in admin_session_store, "Native Admin session store cannot clear sign-in state")
require('active?.accessToken = ""' in admin_session_store, "Native Admin sign-out does not blank the active access token")
require('active?.refreshToken = ""' in admin_session_store, "Native Admin sign-out does not blank the active refresh token")

for workspace in ["Overview", "Support", "Health", "Guardian", "Notifications", "Users & devices", "Staff alerts", "Controls", "Audit", "Release"]:
    require(f'"{workspace}"' in admin_dashboard, f"Native Android Admin workspace missing: {workspace}")
for component in [
    "SupportOperationsPanel",
    "GuardianPanel",
    "ManualNotificationPanel",
    "UserManagementPanel",
    "AuditTimelinePanel",
    "MaintenanceNativePanel",
    "AdminReleasePanel",
    "DeviceSummaryPanel",
]:
    require(component in admin_dashboard, f"Native Android Admin component is missing: {component}")
require("AdminAppAccessPolicy.canReadFullSnapshot(session)" in admin_dashboard, "Native Android Admin full workspaces are not role gated")
require("NATIVE CONTROL MODE" in admin_dashboard, "Native Android Admin identity marker is missing")
require("AdminApi.load(session)" in admin_dashboard, "Native Android Admin does not load data directly through AdminApi")
require("AdminSessionStore" not in admin_dashboard, "Dashboard must not become a second session owner")
require("WebView" not in admin_dashboard and "evaluateJavascript" not in admin_dashboard, "Native Android Admin dashboard contains web runtime code")

# Native API paths and privileged controls stay backend/RLS governed.
require("updateMaintenanceConfig" in admin_api, "Android Admin maintenance write path is missing")
require("updateSupportTicket" in admin_api, "Android Admin ticket update path is missing")
require("sendSupportReply" in admin_api, "Android Admin support reply path is missing")
require("loadSupportNotes" in admin_api, "Android Admin internal-note read path is missing")
require("admin_audit_log" in admin_api, "Android Admin audit read path is missing")
require("grant_type=refresh_token" in admin_api, "Android Admin refresh-token path is missing")

# Morley customer OTA: normal main is exact-match. A release PR may be exactly one monotonic
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
        print(" -", error)
    sys.exit(1)

print("Admin Control integration audit PASSED")
print("Verified web Admin independence, native Android session ownership/workspaces, privileged API contracts and OTA governance.")
