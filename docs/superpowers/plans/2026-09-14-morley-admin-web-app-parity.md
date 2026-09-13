# Morley Admin Web + Mobile-Web App Parity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild Morley Admin web and mobile web to mirror the current Android Admin app workspace model while preserving existing audited backend/auth behavior.

**Architecture:** Keep the existing authenticated browser bootstrap and feature modules, but replace the legacy tab-centric workspace shell with one responsive app-parity shell and controller. Desktop and mobile web share one DOM and behavior model; CSS adapts the same workspaces to sidebar/compact navigation layouts.

**Tech Stack:** Static HTML/CSS/JavaScript, Supabase JS v2, Node `node:test` contract tests, GitHub Pages/Admin Pages deployment.

**Spec:** `docs/superpowers/specs/2026-09-14-morley-admin-web-app-parity-design.md`

## Global Constraints
- Android Admin app is the canonical navigation/workspace source of truth.
- Preserve existing element IDs used by current Admin feature modules unless explicitly migrated with tests.
- Preserve current auth bootstrap, login security, auth boundary and server-side role/policy enforcement.
- Do not create a Guardian/security bypass or broaden remote-control authority.
- One responsive web implementation must serve desktop and mobile web.

---

### Task 1: Add app-parity contract tests

**Files:**
- Create: `tests/admin-web-app-parity.test.mjs`

**Interfaces:**
- Consumes: `admin/workspace-template.html`, `admin/workspace.html`, Android workspace names from `AdminNativeDashboard.kt`.
- Produces: regression contract for required web workspaces, required feature IDs, auth ordering and responsive hooks.

- [ ] **Step 1: Write the failing test** asserting the rebuilt shell exposes `overview`, `support`, `catalogue`, `health`, `guardian`, `notifications`, `users-devices`, `staff-alerts`, `controls`, `audit`, and `release`, plus `data-admin-shell="app-parity"`, mobile navigation hooks, and existing feature IDs.
- [ ] **Step 2: Run** `node --test tests/admin-web-app-parity.test.mjs` and confirm it fails because the legacy shell lacks app-parity workspaces/hooks.
- [ ] **Step 3: Keep the test unchanged while implementing Tasks 2-4.**

### Task 2: Rebuild authenticated workspace shell

**Files:**
- Modify: `admin/workspace-template.html`
- Create: `admin/admin-app-parity.css`

**Interfaces:**
- Consumes: all existing feature module element IDs.
- Produces: unified app-like shell with header, workspace navigation, overview cards and workspace panels.

- [ ] **Step 1:** Replace the legacy horizontal tab shell with semantic app shell markup while retaining required existing IDs inside mapped workspaces.
- [ ] **Step 2:** Add desktop sidebar/app header styling and shared card/metric styles in `admin-app-parity.css`.
- [ ] **Step 3:** Add <=820px responsive rules for compact mobile header, touch targets, single-column cards and scrollable workspace navigation.
- [ ] **Step 4:** Ensure Guardian is a first-class workspace entry while retaining `guardian.html` compatibility.

### Task 3: Add app-parity navigation/controller behavior

**Files:**
- Create: `admin/admin-app-parity.js`
- Modify: `admin/workspace.html`

**Interfaces:**
- Consumes: `[data-workspace]` navigation controls and `[data-workspace-panel]` panels from Task 2.
- Produces: `window.MorleyAdminAppParity.openWorkspace(name)`, active workspace state, overview launcher routing, mobile selector behavior, header title synchronization.

- [ ] **Step 1:** Load `admin-app-parity.css` with the workspace shell.
- [ ] **Step 2:** Load `admin-app-parity.js` after core `app.js` and before optional feature enhancers.
- [ ] **Step 3:** Implement workspace switching without breaking existing feature module tab activation; dispatch the existing tab click where a mapped legacy tab is required.
- [ ] **Step 4:** Keep authenticated context and retry/session-expired behavior unchanged.

### Task 4: Add Production health and native-style overview status

**Files:**
- Modify: `admin/admin-app-parity.js`
- Modify: `admin/workspace-template.html`

**Interfaces:**
- Consumes: existing metric/user/device/ticket DOM and existing loaded data.
- Produces: read-only Production health summary and app-style live-data header status.

- [ ] **Step 1:** Add health metric nodes for support tickets, returned/system errors when available, and registered devices.
- [ ] **Step 2:** Mirror existing metric values into health cards without introducing new privileged queries.
- [ ] **Step 3:** Update live-data state on workspace load/refresh and expose retry/refresh controls through existing safe handlers.

### Task 5: Verify regression contracts

**Files:**
- Test: `tests/admin-web-app-parity.test.mjs`
- Existing tests: `tests/admin-webview-input-focus.test.mjs`, `tests/admin-auth-recovery-contract.test.mjs`, and other Admin web contract tests discovered in repository.

- [ ] **Step 1:** Run `node --test tests/admin-web-app-parity.test.mjs` and confirm PASS.
- [ ] **Step 2:** Run Admin web/auth contract suite and fix only regressions caused by this rebuild.
- [ ] **Step 3:** Verify existing script IDs/elements required by pricing, support, users, release, notifications, announcements, audit and controls remain present.

### Task 6: Branch review and PR

**Files:**
- No production files beyond prior tasks.

- [ ] **Step 1:** Review branch diff for accidental backend/auth/security changes.
- [ ] **Step 2:** Confirm deployment workflow still targets the rebuilt `admin/` output.
- [ ] **Step 3:** Open a PR into `main` with a concise parity/test summary.
- [ ] **Step 4:** Do not merge until required checks pass.