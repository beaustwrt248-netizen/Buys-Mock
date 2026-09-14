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


auth_html = read("admin/index.html")
workspace_html = read("admin/workspace.html")
workspace_template = read("admin/workspace-template.html")
native_core = read("admin/admin-native-parity-core.js")
catalogue_runtime = read("admin/catalogue-readonly-parity.js")
parity_runtime = read("admin/admin-app-parity.js")
user_access_runtime = read("admin/admin-user-access-parity.js")
login_security = read("admin/login-security.js")
turnstile_html = read("admin/turnstile.html")
release_control = read("admin/release-control.js")
support_tickets = read("admin/support-tickets.js")
targeted_notifications = read("admin/targeted-notifications.js")
invites = read("admin/invites.js")
audit_triage = read("admin/audit-triage.js")
admin_activity = read("android/adminapp/src/main/java/com/buysloans/admin/AdminActivity.kt")
admin_login = read("android/adminapp/src/main/java/com/buysloans/admin/AdminLoginActivity.kt")
captcha_challenge = read("android/adminapp/src/main/java/com/buysloans/admin/CaptchaChallenge.kt")
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

# Browser Admin now mirrors native authority instead of acting as an independent
# superset. Read-only native workspaces must remain read-only on web, while the
# approved shared operations retain their backend/RLS-governed write paths.
for workspace in ["overview", "support", "catalogue", "health", "guardian", "notifications", "users-devices", "staff-alerts", "controls", "audit", "release"]:
    require(f'data-workspace="{workspace}"' in workspace_template, f"Admin web navigation missing native workspace: {workspace}")
    require(f'data-workspace-panel="{workspace}"' in workspace_template, f"Admin web panel missing native workspace: {workspace}")
for element_id in [
    "metricUsers", "metricDevices", "metricVersion", "metricQueued", "usersList", "devicesList",
    "ticketsList", "ticketDetail", "catalogueList", "catalogueRefreshBtn", "featureFlags",
    "maintenanceMessage", "otaEnabled", "saveControlsBtn", "releaseSummary", "releaseAdoption",
    "notifTitle", "notifBody", "queueNotifBtn", "annList", "auditList",
]:
    require(f'id="{element_id}"' in workspace_template, f"Admin web native-parity control missing: {element_id}")
for forbidden in [
    "pricingSaveBtn", "pricingEditor", "publishAnnBtn", "annTitle", "annBody", "annAudience",
    "saveReleaseBtn", "forceUpdate", "data-display-name", "data-name-save",
]:
    require(forbidden not in workspace_template, f"Admin web exposes authority not present in native Admin: {forbidden}")

require("admin-native-parity-core.js" in workspace_html, "Admin web does not load the native-parity core")
require("catalogue-readonly-parity.js" in workspace_html, "Admin web does not load the read-only Catalogue owner")
for forbidden in ["['adminCoreApp','app.js", "pricing-management.js", "release-control.js", "control-governance.js"]:
    require(forbidden not in workspace_html, f"Admin web still executes retired browser authority owner: {forbidden}")
require("loadUsers" in native_core and "admin-user-control" in native_core, "Native-parity account/user-control wiring is incomplete")
require("loadDevices" in native_core and "app_version" in native_core, "Native-parity device/version visibility wiring is incomplete")
require("admin_set_config" in native_core and "admin_ota_enabled" in native_core, "Native-parity maintenance/OTA control wiring is incomplete")
require("current_release" in native_core and "minimum_supported_version" in native_core, "Native-parity release summary wiring is incomplete")
require("device_catalog" in catalogue_runtime, "Read-only Catalogue source wiring is incomplete")
require("reset_password" in user_access_runtime and "create_user" in user_access_runtime, "Native-parity user access wiring is incomplete")
require('data-user-action="force_signout"' not in user_access_runtime and 'data-user-action="delete"' not in user_access_runtime, "Native-parity user access exposes destructive legacy actions")
require("ticketReplyBtn" in support_tickets and "support_ticket_messages" in support_tickets, "Support reply wiring is incomplete")
require("assigned_to" in support_tickets and "ticketPriority" in support_tickets, "Support assignment/priority controls are incomplete")
require("send-admin-notification" in targeted_notifications and "notification_jobs" in targeted_notifications, "Notification delivery wiring is incomplete")
require("audience:all" in targeted_notifications and "user:" in targeted_notifications, "Native-parity notification targeting is incomplete")
require("device:" not in targeted_notifications and "target_installation_id" not in targeted_notifications, "Native-parity web notifications still expose device targeting")
require("admin_audit_log" in audit_triage or "admin_audit_log" in native_core, "Audit-log read path is missing")
require("redeem-app-invite" in invites or "app_invites" in invites, "Invite governance wiring is incomplete")
require("supportOnly" in parity_runtime, "Admin parity navigation no longer enforces staff support-only mode")

require("captchaToken:token" in login_security, "Admin web login is not submitting the Turnstile token")
require('id="adminTurnstileFrame"' in auth_html, "Admin web isolated Turnstile frame is missing")
require("turnstile.html?v=8&browser=1" in login_security, "Admin web isolated challenge URL is missing")
require("MAX_BOOTSTRAP_RETRIES=2" in login_security, "Admin web challenge bootstrap retry bound is missing")
require("bootstrapTimer=setTimeout" in login_security, "Admin web challenge loading is not bounded by a recovery watchdog")
require("Security check unavailable. Tap here to retry." in login_security, "Admin web Turnstile failure has no visible retry state")
require("event.source===frame.contentWindow" in login_security, "Admin web isolated bridge source validation is missing")
require("event.origin===window.location.origin" in login_security, "Admin web isolated bridge origin validation is missing")
require("payload.source!=='morley-turnstile'" in login_security, "Admin web isolated bridge identity validation is missing")
require("payload.type==='token'&&payload.value" in login_security, "Admin web isolated token bridge is missing")
require("adminTurnstileWidget" not in login_security, "Admin web must not reintroduce direct top-level Turnstile ownership")
require("function startFallback" not in login_security, "Admin web must not reintroduce the failed dual-transport challenge stack")
require("MAX_API_ATTEMPTS=3" in turnstile_html, "Isolated Turnstile page API retry bound is missing")
require("challenges.cloudflare.com/turnstile/v0/api.js?render=explicit" in turnstile_html, "Isolated Turnstile page does not load the canonical explicit API")
require("callback:function(token)" in turnstile_html, "Isolated Turnstile token callback is missing")
require("script.onerror" in turnstile_html, "Isolated Turnstile script failure recovery is missing")
require("window.parent.postMessage" in turnstile_html and "source:'morley-turnstile'" in turnstile_html, "Isolated Turnstile page does not retain its scoped delivery bridge")
require("setTimeout(()=>post(type,token),150)" in turnstile_html and "setTimeout(()=>post(type,token),600)" in turnstile_html, "Isolated Turnstile token relay does not tolerate browser timing races")

# Android Admin is a native authenticated client. The browser Admin is not its workspace,
# session store, logout route or navigation fallback.
require("AdminSessionStore.current()" in admin_activity, "Android Admin does not read the native session owner")
require("AdminSessionStore.hasAuthorizedSession()" in admin_activity, "Android Admin does not verify the native session before workspace entry")
require("AdminNativeDashboard" in admin_activity, "Android Admin does not enter the native Compose dashboard")
for forbidden in [
    "WebView", "evaluateJavascript", "loadUrl(", "installNativeAdminSession", "native-logout",
    "AdminWebParityPolicy", "EXTRA_ACCESS_TOKEN", "EXTRA_REFRESH_TOKEN",
]:
    require(forbidden not in admin_activity, f"Authenticated Android Admin workspace still contains retired browser handoff symbol: {forbidden}")

require("AdminSessionStore.set(session)" in admin_login, "Native Admin login does not install the authorized Android session")
require("Intent(this, AdminActivity::class.java)" in admin_login, "Native Admin login does not navigate to AdminActivity")
store_index = admin_login.find("AdminSessionStore.set(session)")
navigation_index = admin_login.find("Intent(this, AdminActivity::class.java)")
require(store_index >= 0 and navigation_index > store_index, "Native Admin session must be installed before workspace navigation")
require("putExtra(AdminActivity.EXTRA_ACCESS_TOKEN" not in admin_login, "Admin access token is still transferred through an Intent")
require("putExtra(AdminActivity.EXTRA_REFRESH_TOKEN" not in admin_login, "Admin refresh token is still transferred through an Intent")
require("CaptchaChallenge(" in admin_login, "Native Admin login is not using the scoped Turnstile component")

require(captcha_challenge.count("addJavascriptInterface(") == 1, "Admin Android must have exactly one Turnstile JavaScript bridge")
require('"AndroidBridge"' in captcha_challenge, "Admin Turnstile bridge identity is missing")
require("domStorageEnabled = false" in captcha_challenge, "Admin Turnstile WebView must not enable DOM storage")
require("allowFileAccess = false" in captcha_challenge, "Admin Turnstile WebView must disable file access")
require("allowContentAccess = false" in captcha_challenge, "Admin Turnstile WebView must disable content access")
require("databaseEnabled = false" in captcha_challenge, "Admin Turnstile WebView must disable database storage")
require("MIXED_CONTENT_NEVER_ALLOW" in captcha_challenge, "Admin Turnstile WebView must forbid mixed content")
require("addJavascriptInterface" not in admin_activity and "addJavascriptInterface" not in admin_dashboard, "Privileged native Admin workspace must not own a JavaScript bridge")

require("private var active: AdminSession?" in admin_session_store, "Native Admin session store is missing its process-scoped session")
require("fun clear()" in admin_session_store, "Native Admin session store cannot clear sign-in state")
require('active?.accessToken = ""' in admin_session_store, "Native Admin sign-out does not blank the active access token")
require('active?.refreshToken = ""' in admin_session_store, "Native Admin sign-out does not blank the active refresh token")

for workspace in ["Overview", "Support", "Catalogue", "Health", "Guardian", "Notifications", "Users & devices", "Staff alerts", "Controls", "Audit", "Release"]:
    require(f'"{workspace}"' in admin_dashboard, f"Native Android Admin workspace missing: {workspace}")
for component in [
    "SupportOperationsPanel", "CatalogueNativePanel", "GuardianPanel", "ManualNotificationPanel",
    "UserManagementPanel", "AuditTimelinePanel", "MaintenanceNativePanel", "AdminReleasePanel", "DeviceSummaryPanel",
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
require("admin_ota_enabled" in native_core, "Native-parity Admin does not expose the approved OTA enable control")
# Dormant legacy release-control code remains security-checked but must not be loaded.
require("otaEnabled" in release_control and "saveOtaEnabled" in release_control, "Dormant release-control compatibility path lost OTA safeguards")
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
print("Verified isolated browser auth, native-authority web parity, native Android session ownership/workspaces, privileged API contracts and OTA governance.")
