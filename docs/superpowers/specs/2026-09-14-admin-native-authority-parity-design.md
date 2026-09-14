# Morley Admin Native Authority Parity Design

## Problem

The rebuilt Morley Admin web/mobile-web shell mirrors the native workspace names but still loads the legacy all-in-one `admin/app.js` runtime and several write-oriented legacy modules. That creates authority drift: some web workspaces expose mutations that the current native Admin app deliberately keeps read-only.

Confirmed mismatches on current `main`:

- **Catalogue:** native `CatalogueNativePanel` is explicitly read-only and loads active `device_catalog` rows. Web maps Catalogue to the legacy pricing editor and loads `pricing-management.js`, which can write authoritative prices and activation state.
- **Staff alerts:** native `AnnouncementPanel` is read-only. Web maps Staff alerts to a legacy announcement composer whose handler inserts new announcements.
- **Release:** native `AdminReleasePanel` is explicitly read-only. Web loads release controls that can write `current_release`, `minimum_supported_version`, force-update policy and OTA configuration.
- **User access:** native Admin exposes role changes, enable/disable, invites and temporary-password flows, but not delete, force-signout or display-name editing. Web currently generates those legacy controls and then removes only delete/force-signout after render; display-name editing remains broader than native.
- **Safe controls:** native Admin writes only maintenance mode/message and OTA enabled state. Web must preserve exactly that writable set while treating unrelated feature flags as read-only.

The issue is architectural: parity presentation is layered on top of a legacy runtime whose responsibilities combine data loading, rendering and broader mutation authority.

## Goal

Make the authorized Morley Admin web/mobile-web workspace enforce the same user-visible authority contract as the current native Admin app while preserving existing backend authorization, RLS, audit, Guardian and protected release boundaries.

## Non-goals

- Do not remove or weaken backend RPC/function authorization.
- Do not change Supabase Auth, RLS, schema or production data.
- Do not alter native Android Admin behavior in this project.
- Do not change pricing formulas or protected pricing authority.
- Do not delete historical legacy modules solely for cleanup; they may remain in the repository if another explicit surface still owns them.
- Do not mint a new Admin APK release identity unless native Android source changes.

## Authority contract

### Staff

Staff remains support-only. The browser must load only the support-only runtime plus the shared parity/auth/support modules. It must not initialize full snapshot, catalogue, user, release, pricing, control, audit or notification modules.

### Admin and Manager

The web parity shell exposes the same workspace classes as native:

- Overview — read-only snapshot summary.
- Support — native-equivalent support operations.
- Catalogue — read-only active `device_catalog` list; refresh allowed; no pricing editor or pricing writes.
- Production health — read-only metrics and error/device health.
- Guardian — existing protected Guardian control centre; no authority expansion.
- Notifications — manual notification send for Admin/Manager, matching native targeting rules.
- Users & devices — read-only devices; user role enable/disable according to native policy; invites/provisioning and Admin temporary-password reset; no delete, force-signout or display-name edit.
- Staff alerts — read-only announcement list; no publish composer.
- Safe controls — maintenance enabled/message and OTA enabled only; other feature flags may be displayed read-only if useful.
- Audit — read-only audit history.
- Release — read-only verified release identity and rollout/adoption health; no release/support-policy publication control.

## Architecture

### 1. Replace the parity shell's legacy all-in-one runtime

Create `admin/admin-native-parity-core.js` as the browser data/render owner for the full-access parity shell. It will reuse the existing authenticated `window.sb` and `window.__morleyAdminAuthContext` established by `workspace.html`.

The core will provide a small compatibility surface required by already-proven specialized modules:

- global `$` and `esc` helpers;
- `me`, `myProfile`, `config`, `managedUsers` state;
- read/refresh functions `refreshAll`, `loadUsers`, `loadDevices`, `loadConfig`, `loadAudit`, `loadMetrics`, `loadNotifications`, `loadAnnouncements`.

It will not bind handlers for pricing writes, announcement publishing, release-policy mutation, delete, force-signout or display-name editing.

`refreshAll()` remains the one full-access browser refresh owner so the Refresh repair merged in PR #2072 keeps a single data reload path.

### 2. Native-contract renderers

The parity core renders the shared snapshot using native-equivalent constraints:

- Users: role and enable/disable controls only when native policy allows them. No name editor, delete or force-signout controls are emitted at all.
- Devices: read-only.
- Config: maintenance/message/OTA state only as writable controls; unrelated feature flags remain read-only.
- Announcements: read-only list.
- Release: read-only current/minimum release facts plus rollout/adoption information.
- Audit/metrics: read-only.

Create `admin/catalogue-readonly-parity.js` for the native Catalogue contract. It queries `device_catalog` with `active = true`, orders by category/brand/model_name and caps at 250 rows, matching `AdminApi.loadCatalogue`.

### 3. Preserve specialized allowed modules

Keep modules whose mutations are also present natively and whose current authorization contracts are already tested:

- `admin-user-access-parity.js` for invites/provisioning/password reset, adjusted only if it assumes legacy user actions.
- `targeted-notifications.js` for Admin/Manager manual notifications, while preserving native-supported audience/user targets. Device-only targeting must be removed from the parity UI because native does not expose it.
- `support-tickets.js` and support-only runtime.
- Guardian/auth/audit support modules that do not broaden authority.
- `download-invites.js` only where its existing UI is part of the native-equivalent user/invite workflow.

### 4. Stop loading legacy write owners in the parity shell

`admin/workspace.html` full-access script list must no longer load:

- `app.js`;
- `pricing-management.js`;
- `release-control.js`;
- legacy announcement publishing ownership through the old app runtime;
- `control-governance.js` if its behavior reintroduces legacy `admin-v2.js` or broader feature-flag ownership.

The parity shell instead loads the native parity core, read-only catalogue runtime, and narrow allowed modules.

This is a loader/ownership boundary, not merely visual hiding.

## Data flow

1. `browser-auth-bootstrap.js` verifies the browser session and role.
2. `workspace.html` re-verifies session/profile and creates immutable `window.__morleyAdminAuthContext`.
3. Staff loads only support-only scripts.
4. Admin/Manager loads `admin-native-parity-core.js` first, then allowed specialized modules and `admin-app-parity.js` navigation.
5. `refreshAll()` fetches the full parity snapshot and updates read-only/writable native-contract views.
6. Specialized mutation modules call existing protected Supabase RPC/functions; backend authorization remains authoritative.
7. Catalogue refresh is independent and read-only, matching native `loadCatalogue`.

## Error handling

- Every read surface must render a bounded status/error instead of failing the entire shell.
- Refresh continues to disable concurrent clicks and surface failure in `adminLiveStatus`.
- Missing required runtime functions fail closed rather than silently falling back to legacy modules.
- Staff never falls through to full-access initialization.
- No mutation should be attempted from a workspace native treats as read-only.

## Testing strategy

TDD must establish RED before implementation.

Extend `tests/admin-web-app-parity.test.mjs` / `tests/admin-auth-recovery-contract.test.mjs` with source contracts proving:

- full-access loader uses `admin-native-parity-core.js` and does not load `app.js`, `pricing-management.js`, `release-control.js` or `control-governance.js`;
- Catalogue renderer is read-only and contains no pricing/update action;
- Staff Alerts contains no announcement publishing inputs/button/handler;
- Release contains no save/publish controls or `admin_set_config` release writes;
- user rendering does not emit delete, force-signout or display-name edit controls;
- Safe Controls writes only maintenance/message/OTA fields;
- manual notification targets match native: audiences + user, not device-only;
- Staff remains support-only;
- Refresh still delegates to the single full-access `refreshAll()` owner and Staff support loader.

Run repository security, Admin least-privilege, support/device governance, Admin Control Integration, Full Feature Contract, Web Release Smoke, UI checklist, Quality and Ultimate Parity gates before merge.

## Release and deployment

This is web runtime/parity work only. Native Android source remains unchanged, so Admin 0.1.42/versionCode 43 remains the native release unless a later native repair is required. Changed web assets receive fresh cache keys. Deployment/live authenticated validation follows the protected merge and web deployment path.
