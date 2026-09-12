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
admin_user_control = read("supabase/functions/admin-user-control/index.ts")
session_revoke_migration = read("supabase/migrations/20260912123000_admin_revoke_user_sessions.sql")

workspace_scripts = [
    "user-management-policy.js",
    "app.js",
    "auth-boundary.js",
    "release-control.js",
    "targeted-notifications.js",
    "invites.js",
    "download-invites.js",
    "support-tickets.js",
    "audit-triage.js",
    "pricing-management.js",
    "admin-v2.js",
    "control-governance.js",
    "admin-home.js",
]

require('id="adminTurnstileFrame"' in auth_html, "Admin browser auth document is missing its isolated Turnstile frame")
require("browser-auth-bootstrap.js?v=1" in auth_html, "Admin browser auth bootstrap is missing")
require("login-security.js?v=11" in auth_html, "Admin browser auth controller cache key is stale")
for script in workspace_scripts:
    require(f'src="{script}' not in auth_html and f"src='{script}" not in auth_html, f"Logged-out Admin auth document executes workspace script: {script}")
    require(script in workspace_html, f"Authorized Admin workspace loader is missing: {script}")
require("from('profiles')" in browser_auth and "['admin','manager'].includes(profile.role)" in browser_auth, "Admin browser auth bootstrap does not verify privileged profile authorization")
require("workspace.html?auth=" in browser_auth, "Admin browser auth bootstrap does not enter the isolated workspace after authorization")
require("client.auth.getSession()" in workspace_html, "Admin workspace does not independently verify the Supabase session")
require("from('profiles')" in workspace_html and "!['admin','manager'].includes(profile.role)" in workspace_html, "Admin workspace does not independently enforce privileged profile authorization")
require("workspace-template.html?v=2" in workspace_html, "Admin workspace does not load the preserved workspace template")
require("for(const entry of scripts)await loadScript(entry)" in workspace_html, "Admin workspace scripts are not loaded behind the authorization gate")
require("login-security.js" not in workspace_html, "Authenticated Admin workspace must not start a second browser Turnstile flow")
require('id="appView"' in workspace_template and 'id="logoutBtn"' in workspace_template, "Admin workspace template is incomplete")

# Supabase admin.signOut expects a logged-in JWT, not a target user UUID. The
# server-side user-control path must revoke target sessions through the locked
# service-role database helper instead of feeding a UUID into JWT parsing.
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

# Preserve the full existing governance, native-Admin, OTA and backend checks. The
# only compatibility adjustment is that browser workspace controls now live in
# workspace-template.html instead of the logged-out authentication document.
core_path = ROOT / "scripts/admin_control_integration_audit_core.py"
source = core_path.read_text(encoding="utf-8")
needle = 'admin_html = read("admin/index.html")'
replacement = needle + '\nadmin_workspace_html = read("admin/workspace-template.html")\nadmin_surface_html = admin_html + admin_workspace_html'
if needle not in source:
    print("Admin Control integration audit FAILED")
    print(" - core audit browser surface hook is missing")
    sys.exit(1)
source = source.replace(needle, replacement, 1)
source = source.replace(' in admin_html, f"Admin web', ' in admin_surface_html, f"Admin web')
source = source.replace(' in admin_html, "Admin web', ' in admin_surface_html, "Admin web')
exec(compile(source, str(core_path), "exec"), {"__name__": "__main__", "__file__": str(core_path)})
