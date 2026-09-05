#!/usr/bin/env python3
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
failures: list[str] = []


def text(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def require(path: str, *tokens: str) -> str:
    value = text(path)
    for token in tokens:
        if token not in value:
            failures.append(f"{path}: missing privacy contract {token!r}")
    return value


def between(value: str, start: str, end: str, label: str) -> str:
    left = value.find(start)
    right = value.find(end, left + len(start)) if left >= 0 else -1
    if left < 0 or right < 0:
        failures.append(f"Could not isolate {label} for privacy audit")
        return ""
    return value[left:right]


def manifest_element(value: str, element: str, android_name: str) -> str:
    """Return one manifest start tag without leaking into a later component."""
    pattern = rf'<{element}\b(?=[^>]*android:name="{re.escape(android_name)}")[^>]*>'
    match = re.search(pattern, value)
    return match.group(0) if match else ""


web_issue = require(
    "web-issue-report.js",
    "diagnostics_opt_in:includeDiagnostics",
    "Diagnostics and attachments are optional",
    "MAX_ATTACHMENT_BYTES=10*1024*1024",
)
web_diag_builder = between(web_issue, "function buildDiagnostics()", "function buildReport", "web support diagnostics")
for forbidden in ("location.href", "location.search", "location.hash", "document.cookie", "localStorage", "sessionStorage", "access_token", "refresh_token"):
    if forbidden in web_diag_builder:
        failures.append(f"web-issue-report.js: support diagnostics may capture sensitive browser state via {forbidden}")
if "location.origin" not in web_diag_builder or "location.pathname" not in web_diag_builder:
    failures.append("web-issue-report.js: support diagnostics page identifier must exclude query parameters and fragments")

web_diagnostics = text("web-diagnostics.js")
summary_block = between(web_diagnostics, "function summary", "async function copySummary", "copied web diagnostics summary")
for forbidden in ("access_token", "refresh_token", "password", "captcha", "document.cookie"):
    if forbidden in summary_block:
        failures.append(f"web-diagnostics.js: copied diagnostics summary contains forbidden sensitive field {forbidden}")

support_client = require(
    "android/app/src/main/java/com/buysloans/hub/SupportTicketClient.kt",
    "val diagnostics = if (includeDiagnostics) JSONObject().apply",
    "else JSONObject()",
    'put("diagnostics_opt_in", includeDiagnostics)',
    'put("extended_opt_in", true)',
    'put("app_version", if (includeDiagnostics) BuildConfig.VERSION_NAME else JSONObject.NULL)',
    'put("device_model", if (includeDiagnostics)',
)
android_diag = between(
    support_client,
    "val diagnostics = if (includeDiagnostics) JSONObject().apply",
    "val payload = JSONObject().apply",
    "Android opt-in support diagnostics",
)
for forbidden in ("accessToken", "refreshToken", "password", "captchaToken", "inviteCode", "authorization", "cookie"):
    if forbidden.lower() in android_diag.lower():
        failures.append(f"SupportTicketClient.kt: diagnostics include forbidden authentication field {forbidden}")
for field in ("locale", "timezone", "uptime_ms", "hardware", "supported_abis"):
    if f'put("{field}"' not in android_diag:
        failures.append(f"SupportTicketClient.kt: expected extended diagnostic field {field!r} is missing from the opt-in block")

admin_telemetry = require(
    "android/adminapp/src/main/java/com/buysloans/admin/AdminTelemetry.kt",
    "MAX_EVENTS = 20",
    "errorClass",
    "failingScreen",
)
for forbidden in ("throwable.message", "stackTraceToString", "printStackTrace", "accessToken", "refreshToken", "password", "captchaToken", "inviteCode"):
    if forbidden in admin_telemetry:
        failures.append(f"AdminTelemetry.kt: crash telemetry may persist sensitive detail via {forbidden}")

main_manifest = require(
    "android/app/src/main/AndroidManifest.xml",
    'android:allowBackup="false"',
    'android:usesCleartextTraffic="false"',
    'android:name=".EmbeddedAdminActivity" android:exported="false"',
    'android:name=".SupportTicketActivity"',
)
support_activity = manifest_element(main_manifest, "activity", ".SupportTicketActivity")
if not support_activity:
    failures.append("AndroidManifest.xml: could not isolate SupportTicketActivity")
elif 'android:exported="true"' in support_activity:
    failures.append("AndroidManifest.xml: SupportTicketActivity must not be exported")

admin_manifest = require(
    "android/adminapp/src/main/AndroidManifest.xml",
    'android:allowBackup="false"',
    'android:usesCleartextTraffic="false"',
    'android:name=".AdminActivity"',
    'android:name="androidx.core.content.FileProvider"',
    'android:exported="false"',
)
admin_activity = manifest_element(admin_manifest, "activity", ".AdminActivity")
if not admin_activity:
    failures.append("Admin AndroidManifest.xml: could not isolate AdminActivity")
elif 'android:exported="false"' not in admin_activity:
    failures.append("Admin AndroidManifest.xml: AdminActivity must not be exported")
provider = manifest_element(admin_manifest, "provider", "androidx.core.content.FileProvider")
if not provider:
    failures.append("Admin AndroidManifest.xml: could not isolate update FileProvider")
elif 'android:exported="false"' not in provider:
    failures.append("Admin AndroidManifest.xml: update FileProvider must not be exported")

for path in (
    "web-issue-report.js",
    "web-diagnostics.js",
    "android/app/src/main/java/com/buysloans/hub/SupportTicketClient.kt",
    "android/adminapp/src/main/java/com/buysloans/admin/AdminTelemetry.kt",
):
    value = text(path)
    if "http://ghdhairijqjqivqriigi.supabase.co" in value:
        failures.append(f"{path}: cleartext Supabase endpoint detected")

if failures:
    print("PRIVACY AUDIT FAILED")
    for failure in sorted(set(failures)):
        print(f"- {failure}")
    sys.exit(1)

print("PRIVACY AUDIT PASSED: web diagnostics exclude URL query/fragment and authentication material; Android device/runtime diagnostics and diagnostic top-level metadata are explicit opt-in; Admin crash telemetry stays metadata-only and bounded; sensitive Android surfaces remain non-exported; cleartext backend endpoints are blocked.")
