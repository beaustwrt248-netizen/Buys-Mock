# Morley Desktop Parity Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every visible Morley Buys desktop navigation item and quick action resolve honestly and reliably to an existing production capability, with live shell state, safe responsive lifecycle, regression coverage, and verified deployment.

**Architecture:** Keep `morley-desktop-rebuild.js` as a progressive-enhancement shell over the existing Morley runtime. Replace ad-hoc route aliases with a centralized capability resolver, refresh only desktop-owned state, and preserve legacy Home nodes/listeners through the existing park/restore lifecycle. Do not duplicate pricing, auth, catalogue, inventory, scanner, notification, Guardian, or mobile business logic.

**Tech Stack:** Static HTML/CSS/JavaScript, existing Morley browser runtime, Node.js `node:test` contract tests, GitHub Actions, GitHub Pages.

**Spec:** `docs/superpowers/specs/2026-09-13-morley-desktop-parity-hardening-design.md`

## Global Constraints

- Desktop enhancement boundary remains `>=1000px`.
- `html.morley-physical-phone` remains excluded from the desktop rebuild.
- Existing mobile/physical-phone presentation remains authoritative below 1000px.
- Reuse existing Morley functions, sections, selectors, storage and events; do not duplicate business logic.
- Do not rewrite pricing algorithms, authentication, Supabase schemas, Guardian approval/protected-repair behavior, or hosting.
- A visible desktop control must never silently masquerade as an unrelated feature.
- Preserve live legacy Home DOM nodes/listeners across responsive transitions.
- All new icon-only or ambiguous controls require accessible names and keyboard-reachable behavior.
- Required release gates remain security audit, UI consistency/checklist, full feature contract, web smoke, layout contract, quality gate, ultimate parity, restore point, applicable email contract, deploy workflow, post-deploy smoke and live desktop contract.

---

### Task 1: Capability resolver and honest navigation

**Files:**
- Modify: `morley-desktop-rebuild.js`
- Modify: `tests/morley-desktop-rebuild-contract.test.mjs`

**Interfaces:**
- Consumes: existing `window.morleyDesktopGo(section)`, `window.show(section)`, existing section IDs and runtime globals.
- Produces: `capabilities`, `resolveCapability(name)`, `openCapability(name, options)`, used by sidebar, dashboard, topbar and later tasks.

- [ ] **Step 1: Write failing capability-contract tests**

Add assertions requiring `capabilities`, `resolveCapability`, and `openCapability`; require explicit entries for `search`, `scanner`, `catalogue`, `inventory`, `sales`, `trade`, `price`, `ai`, `reports`, `settings`, `notifications`, `account`, and `support`. Add negative assertions preventing `ai:'home'` and `reports:'sales'` route aliases.

```js
test('desktop capabilities resolve explicitly instead of masquerading as unrelated routes', async () => {
  const source = await read('morley-desktop-rebuild.js');
  assert.match(source, /const capabilities=/);
  assert.match(source, /function resolveCapability/);
  assert.match(source, /function openCapability/);
  for (const name of ['search','scanner','catalogue','inventory','sales','trade','price','ai','reports','settings','notifications','account','support']) {
    assert.ok(source.includes(`${name}:`), `missing capability: ${name}`);
  }
  assert.doesNotMatch(source, /ai:'home'/);
  assert.doesNotMatch(source, /reports:'sales'/);
});
```

- [ ] **Step 2: Run the focused test and verify it fails**

Run: `node --test tests/morley-desktop-rebuild-contract.test.mjs`

Expected: FAIL because the current file has `routeMap` aliases and no capability resolver.

- [ ] **Step 3: Implement the minimal resolver**

In `morley-desktop-rebuild.js`, replace feature-level `routeMap` use with a capability table whose entries declare preferred runtime functions/selectors/sections and a truthful fallback. `openCapability` must try callable globals first, then existing DOM actions, then known existing sections; missing optional capability must fall back to an explicitly labeled composite/settings/dashboard path without throwing.

- [ ] **Step 4: Route sidebar and generic actions through the resolver**

Keep `Dashboard` as Home, but send Search & Scan, Catalogue, Stock, Sales, Trade In / Buy, Price Check, AI Insights, Reports and Settings through capability names. Preserve `syncActive()` by mapping active section IDs back to the owning desktop nav key.

- [ ] **Step 5: Run focused tests**

Run: `node --test tests/morley-desktop-rebuild-contract.test.mjs`

Expected: PASS.

- [ ] **Step 6: Commit**

Commit message: `fix: resolve Morley desktop capabilities explicitly`

---

### Task 2: Search, scanner, catalogue and category parity

**Files:**
- Modify: `morley-desktop-rebuild.js`
- Modify: `tests/morley-desktop-rebuild-contract.test.mjs`

**Interfaces:**
- Consumes: `openCapability`, existing global-search candidate fields, scanner and catalogue runtime actions.
- Produces: explicit Search & Scan dual-path behavior and truthful category targets.

- [ ] **Step 1: Write failing search/scanner/category tests**

Require Search & Scan to expose both search and scanner actions. Require `globalSearch()` to retain the query handoff. Assert Tablets, Smartwatches, Headphones and Cameras are not mapped to `mobilePhones` or generic buy routes merely to make the buttons clickable.

```js
test('search and device categories use truthful desktop capabilities', async () => {
  const source = await read('morley-desktop-rebuild.js');
  assert.match(source, /Search & Scan/);
  assert.match(source, /openCapability\(['"]scanner/);
  assert.match(source, /openCapability\(['"]search/);
  assert.match(source, /morleyDesktopGlobalSearch/);
  assert.doesNotMatch(source, /\['Tablets','mobilePhones'/);
  assert.doesNotMatch(source, /\['Smartwatches','general'/);
  assert.doesNotMatch(source, /\['Headphones','general'/);
  assert.doesNotMatch(source, /\['Cameras','general'/);
});
```

- [ ] **Step 2: Run focused tests and verify failure**

Run: `node --test tests/morley-desktop-rebuild-contract.test.mjs`

Expected: FAIL on current category aliases and missing dual-path Search & Scan behavior.

- [ ] **Step 3: Implement Search & Scan composite behavior**

Make the nav entry open the existing universal search while presenting a desktop-owned scan affordance that invokes the real scanner capability. Keep topbar Scan Device and hero Scan Device on the same scanner adapter.

- [ ] **Step 4: Make category cards truthful**

For each category, resolve a real existing category/section/filter if present. If the runtime lacks a dedicated category, route to Catalogue/Categories with the category query/filter supplied rather than to an unrelated device class. Keep More on the real Categories/Catalogue workflow.

- [ ] **Step 5: Verify focused tests pass**

Run: `node --test tests/morley-desktop-rebuild-contract.test.mjs`

Expected: PASS.

- [ ] **Step 6: Commit**

Commit message: `fix: harden desktop search scan and category parity`

---

### Task 3: AI Insights, Reports, notifications, account and support

**Files:**
- Modify: `morley-desktop-rebuild.js`
- Modify: `tests/morley-desktop-rebuild-contract.test.mjs`

**Interfaces:**
- Consumes: capability resolver, existing runtime globals/sections/storage/DOM state.
- Produces: explicit AI/report behavior, live notification/account state, explicit support behavior.

- [ ] **Step 1: Write failing shell-control tests**

Require notification/profile/support controls to call named capabilities; prohibit hard-coded `<i>3</i>` notification state; require AI and Reports to have explicit adapters/panels/actions rather than Home/Sales aliases.

```js
test('desktop shell controls expose real state and explicit behavior', async () => {
  const source = await read('morley-desktop-rebuild.js');
  assert.match(source, /refreshDesktopState/);
  assert.match(source, /openCapability\(['"]notifications/);
  assert.match(source, /openCapability\(['"]account/);
  assert.match(source, /openCapability\(['"]support/);
  assert.doesNotMatch(source, /class="mdr-notify"[^>]*>[\s\S]*?<i>3<\/i>/);
  assert.doesNotMatch(source, /ai:'home'/);
  assert.doesNotMatch(source, /reports:'sales'/);
});
```

- [ ] **Step 2: Run focused tests and verify failure**

Run: `node --test tests/morley-desktop-rebuild-contract.test.mjs`

Expected: FAIL on static notification count/profile behavior/current aliases.

- [ ] **Step 3: Implement AI and Reports ownership**

Discover and invoke a real existing AI/report runtime capability where available. If no dedicated AI page exists, render an explicitly titled desktop Insights panel derived only from existing pricing/inventory/sales data. Reports must invoke report/export/reporting behavior and must not navigate to Sales merely because Sales contains related data.

- [ ] **Step 4: Implement live notification/account/support state**

Add desktop-owned badge/name elements with stable IDs. `refreshDesktopState()` reads discoverable existing notification count and signed-in identity without inventing values; hide the badge when no count is available. Profile opens the existing account/settings/auth control in that priority order. Help opens an existing help/support/diagnostics capability or clearly falls back to Settings.

- [ ] **Step 5: Run focused tests**

Run: `node --test tests/morley-desktop-rebuild-contract.test.mjs`

Expected: PASS.

- [ ] **Step 6: Commit**

Commit message: `fix: connect desktop insights reports and shell controls`

---

### Task 4: Live dashboard state and accessible presentation

**Files:**
- Modify: `morley-desktop-rebuild.js`
- Modify: `morley-desktop-rebuild.css`
- Modify: `tests/morley-desktop-rebuild-contract.test.mjs`

**Interfaces:**
- Consumes: `refreshDesktopState`, existing KPI/activity DOM and storage values.
- Produces: refreshable desktop-owned KPI/activity/summary nodes and accessible controls.

- [ ] **Step 1: Write failing live-state/accessibility tests**

Require stable IDs/data attributes for desktop KPI values and badge/account state; require `aria-label`/visible labels on icon-only controls; require decorative stock overview to be labeled as a snapshot or generated from live values rather than presented as historical truth.

- [ ] **Step 2: Run focused tests and verify failure**

Run: `node --test tests/morley-desktop-rebuild-contract.test.mjs`

Expected: FAIL until refresh targets and honest snapshot labeling exist.

- [ ] **Step 3: Make dashboard-owned values refreshable**

Update only nodes inside `#morleyDesktopDashboard`/`#morleyDesktopRebuildShell`. Re-read existing `#dUnits`, `#dCost`, `#dSales`, `#dProfit`, `#dSaleCount`, `#dDeals`, `#recent`, and `#apiStatus` after route/mutation/runtime events. Never replace parked legacy nodes.

- [ ] **Step 4: Make overview presentation honest and accessible**

Label synthetic visual bars as a current stock/value snapshot, or calculate them from current values. Ensure collapsed-sidebar controls retain accessible names/tooltips and keyboard focus remains visible in CSS.

- [ ] **Step 5: Run focused tests**

Run: `node --test tests/morley-desktop-rebuild-contract.test.mjs`

Expected: PASS.

- [ ] **Step 6: Commit**

Commit message: `fix: refresh and label desktop dashboard state`

---

### Task 5: Responsive lifecycle idempotence

**Files:**
- Modify: `morley-desktop-rebuild.js`
- Modify: `tests/morley-desktop-rebuild-contract.test.mjs`

**Interfaces:**
- Consumes: `parkLegacyHome`, `restoreLegacyHome`, `installShell`, `teardown`, `refreshDesktopState`.
- Produces: idempotent listener/observer lifecycle across repeated desktop/mobile transitions.

- [ ] **Step 1: Write failing lifecycle contract tests**

Require a single boot/listener guard, a retained/disconnectable MutationObserver reference, no repeated anonymous global event installation, and preservation of the existing park/restore node-moving contract.

- [ ] **Step 2: Run focused tests and verify failure where lifecycle ownership is implicit**

Run: `node --test tests/morley-desktop-rebuild-contract.test.mjs`

Expected: FAIL until lifecycle ownership is explicit.

- [ ] **Step 3: Implement idempotent lifecycle ownership**

Use module-level state for booted/listener/observer ownership. Repeated `installShell()` calls must not add duplicate listeners. `teardown()` removes only desktop-owned DOM/classes and leaves shared runtime state intact. Observer callbacks may refresh state but must not recreate the shell recursively.

- [ ] **Step 4: Preserve physical-phone and sub-1000 exclusions**

Keep the current `DESKTOP=1000`, `morley-physical-phone`, CSS media-query and `restoreLegacyHome()` contracts unchanged in behavior.

- [ ] **Step 5: Run focused tests**

Run: `node --test tests/morley-desktop-rebuild-contract.test.mjs`

Expected: PASS.

- [ ] **Step 6: Commit**

Commit message: `fix: make desktop responsive lifecycle idempotent`

---

### Task 6: Full regression and repository gates

**Files:**
- Modify only if a failing test identifies a real regression: relevant source/test files.

**Interfaces:**
- Consumes: completed Tasks 1-5.
- Produces: a branch that passes focused and repository-wide contracts without bypasses.

- [ ] **Step 1: Run desktop contract suite**

Run: `node --test tests/morley-desktop-rebuild-contract.test.mjs`

Expected: PASS, zero failures.

- [ ] **Step 2: Run repository JavaScript/test commands used by existing web gates**

Use the exact commands declared in `.github/workflows/web-smoke.yml`, `.github/workflows/web-desktop-live-contract.yml`, UI consistency/checklist, security, feature-contract, quality and parity workflows. Do not substitute weaker local commands.

Expected: all applicable commands PASS.

- [ ] **Step 3: Fix any regression test-first**

For each failure, reproduce it with the narrowest existing/focused test, make the minimal production fix, rerun the narrow test, then rerun the affected gate. Do not weaken or delete a gate to obtain green status.

- [ ] **Step 4: Verify branch diff scope**

Compare `hardening/morley-desktop-parity-20260913` with `main`. Expected changes are the approved spec/plan, desktop rebuild JS/CSS, and desktop regression tests only unless a gate proves another file must change.

- [ ] **Step 5: Commit any gate-derived fixes**

Commit message format: `fix: address desktop parity gate regression`

---

### Task 7: PR, protected review, deployment and live verification

**Files:**
- PR metadata only unless review/gates find a defect.

**Interfaces:**
- Consumes: fully green hardening branch.
- Produces: reviewed merge candidate and verified production deployment.

- [ ] **Step 1: Open PR to `main`**

Title: `Harden Morley Buys desktop functionality parity`

PR body must include the repository-required UI/mobile checklist evidence, explicit >=1000px scope, physical-phone exclusion, test commands/results, and protected-boundary statement.

- [ ] **Step 2: Wait for and inspect all required PR checks**

Required applicable checks include Repository Security Audit, UI PR Checklist Gate, Full Feature Contract Audit, Web Release Smoke Checks, Morley Restore Point Capture, Morley UI Consistency, Web No-Blue / Morley Layout Contract, B&L Morley Quality Gate, Morley Ultimate Parity Gate, and any other required check GitHub reports for the head SHA.

Expected: all required checks green; no bypass.

- [ ] **Step 3: Address review/check failures on the branch**

Use the same test-first process as Task 6 and require fresh green checks after every fix.

- [ ] **Step 4: Merge only through protected repository rules**

Do not bypass human approval or protected merge requirements. If GitHub requires the repository owner to press Merge, stop only for that protected action.

- [ ] **Step 5: Verify merged commit deployment**

Confirm `Deploy B&L Morley Web` succeeds for the merged SHA, including release readiness audit, syntax checks, static bundle build, Pages configuration/upload/deploy, and post-deploy smoke tests.

- [ ] **Step 6: Verify live desktop contract**

Confirm `Web Desktop Live Contract` succeeds against the same merged SHA. Verify the live contract covers deployed navigation ownership and no post-merge regression is reported.

- [ ] **Step 7: Report completion only with fresh evidence**

State the merged SHA and the successful deployment/live-contract run statuses. If any check remains queued/running/skipped unexpectedly, report that state instead of declaring completion.
