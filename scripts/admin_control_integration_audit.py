#!/usr/bin/env python3
from __future__ import annotations

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


auth_html = read("admin/index.html")
browser_auth = read("admin/browser-auth-bootstrap.js")
workspace_html = read("admin/workspace.html")
workspace_template = read("admin/workspace-template.html")
parity_runtime = read("admin/admin-app-parity.js")
native_core = read("admin/admin-native-parity-core.js")
catalogue_runtime = read("admin/catalogue-readonly-parity.js")
support_runtime = read("admin/admin-support-only-runtime.js")
user_access_runtime = read("admin/admin-user-access-parity.js")
notification_runtime = read("admin/targeted-notifications.js")
admin_user_control = read("supabase/functions/admin-user-control/index.ts")
session_revoke_migration = read("supabase/migrations/20260912122323_admin_revoke_user_sessions.sql")

full_access_scripts = [
    "user-management-policy.js",
    "admin-native-parity-core.js",
    "catalogue-readonly-parity.js",
    "admin-app-parity.js",
    "admin-user-access-parity.js",
    "auth-boundary.js",
    "targeted-notifications.js",
    "download-invites.js",
    "support-tickets.js",
    "audit-triage.js",
]
support_only_scripts = [
    "admin-support-only-runtime.js",
    "admin-app-parity.js",
    "auth-boundary.js",
    "support-tickets.js",
]
retired_runtime_owners = ["app.js", "release-control.js", "pricing-management.js", "control-governance.js"]
legacy_visual_loaders = ["admin-v2.js", "admin-home.js", "invites.js"]

require('id="adminTurnstileFrame"' in auth_html, "Admin browser auth document is missing its isolated Turnstile frame")
require("browser-auth-bootstrap.js?v=2" in auth_html, "Admin browser auth bootstrap is missing")
require("login-security.js?v=11" in auth_html, "Admin browser auth controller cache key is stale")
for script in full_access_scripts + support_only_scripts + retired_runtime_owners + legacy_visual_loaders:
    require(f'src="{script}' not in auth_html and f"src='{script}" not in auth_html, f"Logged-out Admin auth document executes workspace script: {script}")
for script in full_access_scripts:
    require(script in workspace_html, f"Authorized full-access Admin workspace loader is missing: {script}")
for script in support_only_scripts:
    require(script in workspace_html, f"Authorized support-only Admin workspace loader is missing: {script}")
for script in retired_runtime_owners:
    require(script not in workspace_html, f"Native-parity Admin loader still executes retired authority owner: {script}")
for script in legacy_visual_loaders:
    executable_marker = f"['adminV2Script','{script}" if script == "admin-v2.js" else f"['adminHome','{script}" if script == "admin-home.js" else f"['adminInvites','{script}"
    require(executable_marker not in workspace_html, f"Legacy Admin visual/runtime loader must stay disabled: {script}")

require("from('profiles')" in browser_auth, "Admin browser auth bootstrap does not verify the profile")
require("FULL_ACCESS_ROLES=['admin','manager']" in browser_auth, "Admin browser auth bootstrap is missing full-access roles")
require("ENTRY_ROLES=[...FULL_ACCESS_ROLES,'staff']" in browser_auth, "Admin browser auth bootstrap does not mirror native staff support entry")
require("ENTRY_ROLES.includes(profile.role)" in browser_auth, "Admin browser auth bootstrap does not enforce native entry roles")
require("workspace.html?auth=" in browser_auth, "Admin browser auth bootstrap does not enter the isolated workspace after authorization")

require("client.auth.getSession()" in workspace_html, "Admin workspace does not independently verify the Supabase session")
require("from('profiles')" in workspace_html, "Admin workspace does not independently verify the profile")
require("FULL_ACCESS_ROLES=['admin','manager']" in workspace_html, "Admin workspace is missing full-access roles")
require("ENTRY_ROLES=[...FULL_ACCESS_ROLES,'staff']" in workspace_html, "Admin workspace does not mirror native staff support entry")
require("ENTRY_ROLES.includes(profile.role)" in workspace_html, "Admin workspace does not enforce native entry roles")
require("profile.role==='staff'" in workspace_html, "Admin workspace does not split staff into support-only mode")
require("supportOnly?supportOnlyScripts:fullAccessScripts" in workspace_html, "Admin workspace does not isolate the support-only runtime")
require("workspace-template.html?v=2" in workspace_html, "Admin workspace does not load the preserved workspace template")
require("for(const entry of scripts)await loadScript(entry)" in workspace_html, "Admin workspace scripts are not loaded behind the authorization gate")
require("login-security.js" not in workspace_html, "Authenticated Admin workspace must not start a second browser Turnstile flow")

require('data-admin-shell="app-parity"' in workspace_template, "Admin workspace template is not using the native-parity shell")
require('id="appView"' in workspace_template and 'id="logoutBtn"' in workspace_template, "Admin workspace template is incomplete")
for workspace in ["overview", "support", "catalogue", "health", "guardian", "notifications", "users-devices", "staff-alerts", "controls", "audit", "release"]:
    require(f'data-workspace="{workspace}"' in workspace_template, f"Admin parity navigation is missing workspace: {workspace}")
    require(f'data-workspace-panel="{workspace}"' in workspace_template, f"Admin parity panel is missing workspace: {workspace}")
for forbidden in ["pricingSaveBtn", "publishAnnBtn", "saveReleaseBtn", "forceUpdate", "data-display-name", "data-name-save"]:
    require(forbidden not in workspace_template, f"Admin parity template exposes retired browser authority: {forbidden}")

require("supportOnly" in parity_runtime and "name!='support'" in parity_runtime.replace('!==', '!='), "Admin parity runtime does not force staff into Support")
require("tab-tickets" in parity_runtime, "Admin parity runtime does not expose the staff Support panel")
require("support-only" in support_runtime, "Admin support-only runtime is missing its least-privilege marker")
require("secureUserAction('set_role'" in native_core and "admin_set_config" in native_core, "Admin native-parity core is missing approved write boundaries")
require("device_catalog" in catalogue_runtime, "Admin Catalogue read-only runtime is missing its source boundary")
require("reset_password" in user_access_runtime and "create_user" in user_access_runtime, "Admin native-parity user access controls are incomplete")
require('data-user-action="force_signout"' not in user_access_runtime and 'data-user-action="delete"' not in user_access_runtime, "Admin native-parity user access still exposes destructive legacy actions")
require("audience:all" in notification_runtime and "user:" in notification_runtime, "Admin notifications are missing native audience/user targeting")
require("device:" not in notification_runtime and "target_installation_id" not in notification_runtime, "Admin notifications still expose device-installation targeting")

# Supabase admin.signOut expects a logged-in JWT, not a target user UUID. The
# server-side user-control path may retain revocation support for backend/native
# compatibility even though the parity UI intentionally does not expose it.
require("admin.auth.admin.signOut(targetUser" not in admin_user_control, "Admin user control passes a target UUID to Supabase JWT signOut")
require("admin_revoke_user_sessions" in admin_user_control, "Admin user control is missing target-session revocation")
require("delete from auth.sessions" in session_revoke_migration.lower(), "Session revocation helper does not remove target auth sessions")
require("revoke all on function public.admin_revoke_user_sessions(uuid) from authenticated" in session_revoke_migration.lower(), "Authenticated clients can execute the privileged session revocation helper")
require("grant execute on function public.admin_revoke_user_sessions(uuid) to service_role" in session_revoke_migration.lower(), "Service role cannot execute the session revocation helper")

if errors:
    print("Admin Control integration audit FAILED")
    for error in errors:
        print(" -", error)
    sys.exit(1)

# Preserve the broader native-Admin, OTA and backend checks. The core audit is
# now itself native-authority aware, so no browser-surface source rewriting is
# needed here.
core_path = ROOT / "scripts/admin_control_integration_audit_core.py"
exec(compile(core_path.read_text(encoding="utf-8"), str(core_path), "exec"), {"__name__": "__main__", "__file__": str(core_path)})
