# Morley Admin Native Authority Parity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Morley Admin desktop/mobile web expose the same user-visible authority as the current native Admin app, removing legacy web-only write ownership from read-only native workspaces.

**Architecture:** Replace the parity shell's legacy `admin/app.js` owner with a narrower `admin-native-parity-core.js` that owns full-access data loading/rendering and only native-approved user/config mutations. Add a dedicated read-only Catalogue runtime, retain proven specialized support/invite/notification modules where native exposes the same capability, and stop loading pricing/release/legacy control write owners.

**Tech Stack:** Vanilla JavaScript/HTML/CSS, Supabase JS v2, Node `node:test`, Kotlin native Admin source as parity reference, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-09-14-admin-native-authority-parity-design.md`

## Global Constraints

- Native Admin is the user-visible authority contract.
- Do not change Auth, RLS, Supabase schema, Guardian authority, pricing formulas or production data.
- Staff remains support-only.
- Catalogue, Staff alerts and Release are read-only in parity web.
- User delete, force-signout and display-name editing are not exposed in parity web.
- Safe Controls may write only maintenance enabled/message and OTA enabled.
- Native Android source stays unchanged; do not mint a new Admin APK identity.
- TDD RED must precede production changes.

---

### Task 1: Lock the native authority contract in tests

**Files:**
- Modify: `tests/admin-web-app-parity.test.mjs`
- Modify: `tests/admin-auth-recovery-contract.test.mjs`

**Interfaces:**
- Consumes: current native source files and authorized web loader/template/runtime.
- Produces: source-contract assertions for loader ownership and permitted workspace actions.

- [ ] **Step 1: Add failing loader ownership assertions**

Require full-access `workspace.html` to load `admin-native-parity-core.js` and `catalogue-readonly-parity.js`, and to omit `app.js`, `pricing-management.js`, `release-control.js`, and `control-governance.js` from the authorized parity script list.

- [ ] **Step 2: Add failing authority assertions**

Assert that parity source does not emit/bind `force_signout`, `delete`, display-name edits, announcement publishing, release publishing, pricing updates or device-only notification targets; assert safe controls preserve only maintenance/message/OTA writes.

- [ ] **Step 3: Run the protected Admin contract at RED**

Run through the PR-triggered Admin Android Least-Privilege Gate. Expected: FAIL on the new parity ownership/authority assertions while unrelated security/quality checks remain unchanged.

- [ ] **Step 4: Commit the test-only RED state**

Commit only tests/spec/plan before production runtime changes.

---

### Task 2: Create the full-access native parity core

**Files:**
- Create: `admin/admin-native-parity-core.js`
- Modify: `admin/workspace.html`
- Modify: `admin/workspace-template.html`

**Interfaces:**
- Consumes: `window.sb`, `window.__morleyAdminAuthContext`.
- Produces: global compatibility helpers/state `$`, `esc`, `me`, `myProfile`, `config`, `managedUsers`, plus `refreshAll`, `loadUsers`, `loadDevices`, `loadConfig`, `loadAudit`, `loadMetrics`, `loadNotifications`, `loadAnnouncements` for existing allowed modules and PR #2072 Refresh behavior.

- [ ] **Step 1: Implement authenticated core bootstrap**

Read the immutable auth context, fail closed unless role is Admin/Manager, populate identity header state, and bind guarded sign-out.

- [ ] **Step 2: Implement snapshot loaders**

Use existing Supabase tables and bounded limits matching native snapshot intent. Each loader handles its own status/error without crashing other workspaces.

- [ ] **Step 3: Render users with native-approved controls only**

Render role and enable/disable actions under existing role policy. Do not generate display-name edit, delete or force-signout controls. Keep `managedUsers` for policy checks.

- [ ] **Step 4: Render devices, announcements, audit and metrics read-only**

Retain current useful status/adoption information without adding mutations.

- [ ] **Step 5: Render native Safe Controls contract**

Expose maintenance mode, maintenance message and OTA enabled only. Save through existing `admin_set_config` with preserved unknown feature flags rather than replacing the whole object.

- [ ] **Step 6: Wire full-access loader to the new core**

Replace `app.js` in `workspace.html` with the new core using a fresh cache key. Keep Staff loader unchanged.

- [ ] **Step 7: Run focused parity/auth tests**

Expected: core/loader assertions pass; read-only Catalogue task may still remain RED until Task 3.

---

### Task 3: Make Catalogue genuinely read-only

**Files:**
- Create: `admin/catalogue-readonly-parity.js`
- Modify: `admin/workspace-template.html`
- Modify: `admin/workspace.html`

**Interfaces:**
- Consumes: `window.sb`, `window.__morleyAdminAuthContext`.
- Produces: `window.MorleyAdminCatalogueParity.refresh()` and read-only active catalogue rendering.

- [ ] **Step 1: Replace legacy pricing-editor markup in the Catalogue workspace**

Add status, refresh button and list container only. No price/source/authoritative/active editor fields or save button.

- [ ] **Step 2: Implement native-equivalent catalogue query**

Query `device_catalog` for active rows, order category/brand/model_name, cap at 250, and render brand/model/category/model-number/storage details.

- [ ] **Step 3: Remove `pricing-management.js` from parity loader**

Do not load the write owner in the authorized parity shell.

- [ ] **Step 4: Run parity and pricing boundary tests**

Expected: Catalogue authority assertions pass and protected pricing tests remain green because backend/pricing source is unchanged.

---

### Task 4: Remove Staff Alerts and Release write ownership

**Files:**
- Modify: `admin/workspace-template.html`
- Modify: `admin/workspace.html`
- Modify: `admin/admin-native-parity-core.js`

**Interfaces:**
- Consumes: snapshot announcements/config/devices already loaded by the core.
- Produces: read-only announcement and release/adoption views.

- [ ] **Step 1: Replace Staff Alerts composer with read-only list**

Retain `annList`/status presentation but remove title/body/audience fields and publish button.

- [ ] **Step 2: Replace Release mutation controls with read-only release facts**

Show current release, minimum supported release and device adoption. Remove save/publish/force-update controls.

- [ ] **Step 3: Remove `release-control.js` from parity loader**

No release-policy write owner executes in parity web.

- [ ] **Step 4: Verify no announcement/release mutation strings remain in authorized parity ownership**

Expected: tests prove no `announcements.insert`, release `admin_set_config`, `publishAnnBtn`, `saveReleaseBtn` or force-update input is owned by the parity runtime.

---

### Task 5: Align notifications and user access with native

**Files:**
- Modify: `admin/targeted-notifications.js`
- Modify: `admin/admin-user-access-parity.js`
- Modify: `tests/admin-web-app-parity.test.mjs`

**Interfaces:**
- Consumes: globals exported by `admin-native-parity-core.js` plus auth context.
- Produces: native-equivalent notification targets and user/invite/password flows.

- [ ] **Step 1: Remove device-only notification target generation**

Keep all/admin/manager/staff audiences and enabled user targets. Native does not expose installation/device targeting.

- [ ] **Step 2: Keep invite/provision/reset flows and non-native action scrub as defense-in-depth**

Do not broaden Manager privileges. Keep existing backend functions/RPCs and password handling unchanged.

- [ ] **Step 3: Run notification/user governance tests**

Expected: native target/role assertions pass; Admin Device Governance and Support Governance remain green.

---

### Task 6: Retire legacy control ownership from the parity loader

**Files:**
- Modify: `admin/workspace.html`
- Modify: `tests/morley-ecosystem-contract.test.mjs`
- Modify: `tests/admin-web-app-parity.test.mjs`

**Interfaces:**
- Consumes: new core/catalogue ownership.
- Produces: one authoritative parity loader with no legacy runtime re-entry.

- [ ] **Step 1: Remove `control-governance.js` from parity loader**

The new core owns native Safe Controls and must not dynamically reintroduce `admin-v2.js`.

- [ ] **Step 2: Update ecosystem contracts to the new cache keys/runtime owners**

Assert the parity core/catalogue owners and absence of legacy write modules.

- [ ] **Step 3: Verify JavaScript syntax for every changed/new runtime**

Expected: `new Function(...)` source checks and repository syntax gate pass.

---

### Task 7: Full verification and protected merge

**Files:**
- Review all changed files only; no opportunistic refactors.

**Interfaces:**
- Consumes: completed implementation.
- Produces: verified protected PR candidate.

- [ ] **Step 1: Review exact diff against the spec**

Confirm no Auth/RLS/schema/Guardian/pricing/native Android changes and no hidden legacy write owner remains loaded.

- [ ] **Step 2: Require green exact-head checks**

Require Admin Android Least-Privilege, Admin Support Governance, Admin Device Governance, Admin Control Integration, Repository Security Audit, B&L Morley Quality Gate, Full Feature Contract Audit, Web Release Smoke Checks, UI PR Checklist Gate, Private Distribution Readiness, Admin OTA Release Safety and Morley Ultimate Parity Gate.

- [ ] **Step 3: Check branch freshness against `main`**

If behind, reconcile with a normal merge and rerun exact-head checks; never force-push/rebase protected history.

- [ ] **Step 4: Merge only the verified exact head through repository protections**

Use expected-head SHA. Do not bypass required checks or protected approval boundaries.

- [ ] **Step 5: Verify merged SHA on `main`**

Confirm the merge commit exists and the parity loader on `main` references the new runtime/cache keys.
