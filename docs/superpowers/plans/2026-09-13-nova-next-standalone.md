# Nova Next Standalone Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a completely new, isolated Nova successor from scratch that reproduces the approved dark blue/purple reference layout, reaches functional parity with current Nova, and can later be promoted to become the primary Nova without rewriting the app.

**Architecture:** `nova-next/` is a standalone application surface with zero imports from the existing `nova/` frontend/runtime. It may call the same approved Nova/Morley backend contracts, but it must preserve the existing Admin-only authentication, Guardian enforcement, audit, pricing, release and protected-action boundaries. Development and production identities are separated so the new app can coexist with current Nova during evaluation and later be promoted intentionally.

**Tech Stack:** Standards-based HTML/CSS/JavaScript for the initial shell to match the current hosting model, Vitest-compatible pure modules for behavior tests where available, PWA manifest/service-worker isolation, existing Supabase/Edge Function contracts only through a dedicated adapter layer, Android wrapper/config added as a later promotion-ready phase.

**Spec:** `docs/nova-next/DESIGN_SPEC.md`

## Global Constraints

- Do not modify or import any file under the existing `nova/` application surface.
- Do not change production Supabase schema, RLS, Edge Functions, secrets, deployment, signing, OTA, Guardian approval policy, pricing approval logic or user roles during bootstrap.
- Current Nova remains operational and untouched while Nova Next is built and tested.
- The new UI must preserve the approved reference visual language: deep navy/black background, white text, blue/purple gradients, rounded/glassy cards, minimalist icons, friendly mobile-first layout and smooth transitions.
- Primary bottom navigation is exactly: `Home`, `Chat`, `Tools`, `Tasks`, `More`.
- Side drawer destinations include: `Home`, `Chat`, `Tools`, `Tasks`, `Projects`, `Knowledge Base`, `Files`, `Automation`, `Calendar`, `Integrations`, `Settings`, `Help & Support`.
- A feature does not count as migrated until its data source, permissions, failure states, evidence/observability and regression coverage are connected.
- Guardian remains independently authoritative for protected operations; Nova Next cannot disable, bypass, weaken, self-approve or silently expand Guardian authority.

---

### Task 1: Isolated application shell and reference-layout contract

**Files:**
- Create: `docs/nova-next/DESIGN_SPEC.md`
- Create: `nova-next/tests/layout-contract.test.mjs`
- Create: `nova-next/src/navigation.mjs`
- Create: `nova-next/index.html`
- Create: `nova-next/styles.css`
- Create: `nova-next/app.js`

**Interfaces:**
- Produces: `PRIMARY_NAV`, `DRAWER_NAV`, `resolveRoute(route)` from `nova-next/src/navigation.mjs`.
- Consumes: no existing Nova frontend code.

- [ ] **Step 1: Write the failing navigation/layout contract test**

```js
import assert from 'node:assert/strict';
import { PRIMARY_NAV, DRAWER_NAV, resolveRoute } from '../src/navigation.mjs';

assert.deepEqual(PRIMARY_NAV, ['Home', 'Chat', 'Tools', 'Tasks', 'More']);
assert.equal(DRAWER_NAV.includes('Knowledge Base'), true);
assert.equal(DRAWER_NAV.includes('Help & Support'), true);
assert.equal(resolveRoute('home'), 'home');
assert.equal(resolveRoute('unknown'), 'home');
```

- [ ] **Step 2: Run the test and verify RED**

Run: `node nova-next/tests/layout-contract.test.mjs`

Expected: fail because `../src/navigation.mjs` does not exist.

- [ ] **Step 3: Implement the minimal navigation contract**

```js
export const PRIMARY_NAV = Object.freeze(['Home', 'Chat', 'Tools', 'Tasks', 'More']);
export const DRAWER_NAV = Object.freeze([
  'Home', 'Chat', 'Tools', 'Tasks', 'Projects', 'Knowledge Base', 'Files',
  'Automation', 'Calendar', 'Integrations', 'Settings', 'Help & Support'
]);
const VALID = new Set(['home', 'chat', 'tools', 'tasks', 'more', 'projects', 'knowledge', 'files', 'automation', 'calendar', 'integrations', 'settings', 'help']);
export function resolveRoute(route) {
  const normalized = String(route || '').trim().toLowerCase().replace(/\s+/g, '-');
  return VALID.has(normalized) ? normalized : 'home';
}
```

- [ ] **Step 4: Build the shell to match the approved reference**

Create a single-page shell with splash, login, Home, side drawer, Chat, Tools, Tasks, Projects, Settings and completion/transition states. Use semantic buttons, `aria-current`, keyboard-closeable drawer, reduced-motion support and responsive safe-area spacing.

- [ ] **Step 5: Run the navigation test and verify GREEN**

Run: `node nova-next/tests/layout-contract.test.mjs`

Expected: exit 0.

- [ ] **Step 6: Commit**

```bash
git add docs/nova-next nova-next
git commit -m "feat: bootstrap isolated Nova Next app shell"
```

### Task 2: Route state, drawer behavior and smooth transitions

**Files:**
- Create: `nova-next/tests/router.test.mjs`
- Create: `nova-next/src/router.mjs`
- Modify: `nova-next/app.js`
- Modify: `nova-next/styles.css`

**Interfaces:**
- Consumes: `resolveRoute(route)`.
- Produces: `createRouter({ onRoute })`, with `go(route)`, `current()`, `subscribe(listener)`.

- [ ] **Step 1: Write a failing router test**

```js
import assert from 'node:assert/strict';
import { createRouter } from '../src/router.mjs';

const seen = [];
const router = createRouter({ onRoute: route => seen.push(route) });
router.go('chat');
assert.equal(router.current(), 'chat');
router.go('not-real');
assert.equal(router.current(), 'home');
assert.deepEqual(seen, ['chat', 'home']);
```

- [ ] **Step 2: Run RED**

Run: `node nova-next/tests/router.test.mjs`

Expected: fail because `router.mjs` is missing.

- [ ] **Step 3: Implement router state**

Implement history/hash-aware routing without full page reload, preserve current panel scroll position, close the drawer after route selection and update `aria-current`.

- [ ] **Step 4: Add transitions**

Use transform/opacity transitions under 240ms, skip animation for `prefers-reduced-motion`, avoid layout-shifting animations and keep bottom navigation fixed.

- [ ] **Step 5: Run GREEN**

Run: `node nova-next/tests/router.test.mjs && node nova-next/tests/layout-contract.test.mjs`

Expected: exit 0.

- [ ] **Step 6: Commit**

```bash
git add nova-next
git commit -m "feat: add Nova Next navigation and motion"
```

### Task 3: Authentication boundary adapter

**Files:**
- Create: `nova-next/tests/auth-policy.test.mjs`
- Create: `nova-next/src/auth-policy.mjs`
- Create: `nova-next/src/adapters/auth-adapter.js`
- Modify: `nova-next/app.js`
- Create: `docs/nova-next/SECURITY_BOUNDARY.md`

**Interfaces:**
- Produces: `canEnterNovaNext(profile)` and `createAuthAdapter(config)`.
- `canEnterNovaNext(profile)` returns true only when `profile.role === 'admin' && profile.is_enabled === true`.

- [ ] **Step 1: Write failing authorization tests**

```js
import assert from 'node:assert/strict';
import { canEnterNovaNext } from '../src/auth-policy.mjs';

assert.equal(canEnterNovaNext({ role: 'admin', is_enabled: true }), true);
assert.equal(canEnterNovaNext({ role: 'admin', is_enabled: false }), false);
assert.equal(canEnterNovaNext({ role: 'manager', is_enabled: true }), false);
assert.equal(canEnterNovaNext({ role: 'staff', is_enabled: true }), false);
assert.equal(canEnterNovaNext(null), false);
```

- [ ] **Step 2: Run RED**

Run: `node nova-next/tests/auth-policy.test.mjs`

Expected: fail because the module is missing.

- [ ] **Step 3: Implement the policy and adapter boundary**

The browser adapter may use only public/publishable credentials and authenticated bearer tokens. No service-role key, AI provider key, password or privileged credential may exist in static source. Interactive password sign-in remains Turnstile-gated when wired to production.

- [ ] **Step 4: Keep protected app data locked until verified**

The authenticated shell stays hidden until both Supabase user validation and enabled Admin profile validation succeed. Sign-out clears the app session and returns to login.

- [ ] **Step 5: Run GREEN**

Run: `node nova-next/tests/auth-policy.test.mjs`

Expected: exit 0.

- [ ] **Step 6: Commit**

```bash
git add nova-next docs/nova-next
git commit -m "feat: add Nova Next admin auth boundary"
```

### Task 4: Capability registry and feature-parity map

**Files:**
- Create: `nova-next/tests/capability-registry.test.mjs`
- Create: `nova-next/src/capabilities.mjs`
- Create: `docs/nova-next/FEATURE_PARITY.md`
- Modify: `nova-next/app.js`

**Interfaces:**
- Produces: `CAPABILITIES` and `capabilitiesForSection(section)`.

- [ ] **Step 1: Write failing parity tests**

```js
import assert from 'node:assert/strict';
import { CAPABILITIES } from '../src/capabilities.mjs';

for (const id of [
  'conversation', 'web-research', 'camera-vision', 'catalogue-intelligence',
  'pricing-intelligence', 'support-intelligence', 'guardian-analysis',
  'release-readiness', 'bug-triage', 'business-intelligence', 'memory',
  'voice', 'scenario-planning', 'multi-step-jobs', 'proactive-alerts'
]) {
  assert.equal(CAPABILITIES.some(item => item.id === id), true, `missing ${id}`);
}
```

- [ ] **Step 2: Run RED**

Run: `node nova-next/tests/capability-registry.test.mjs`

Expected: fail because registry is missing.

- [ ] **Step 3: Implement registry**

Each capability record includes: `id`, `label`, `section`, `status`, `risk`, `requiresAuth`, `adapter`, `evidenceRequired`.

- [ ] **Step 4: Render Tools/More/Knowledge from registry**

No duplicated hard-coded feature lists in UI. Existing capability labels are reorganised into the approved reference information architecture.

- [ ] **Step 5: Run GREEN**

Run all current Nova Next tests.

- [ ] **Step 6: Commit**

```bash
git add nova-next docs/nova-next
git commit -m "feat: add Nova Next feature parity registry"
```

### Task 5: Backend adapter isolation and protected-action policy

**Files:**
- Create: `nova-next/tests/action-policy.test.mjs`
- Create: `nova-next/src/action-policy.mjs`
- Create: `nova-next/src/adapters/nova-api.js`
- Create: `docs/nova-next/ADAPTER_CONTRACTS.md`

**Interfaces:**
- Produces: `classifyAction(action)` and `mayAutoExecute(action)`.

- [ ] **Step 1: Write failing policy tests**

```js
import assert from 'node:assert/strict';
import { mayAutoExecute } from '../src/action-policy.mjs';

for (const action of ['pricing-write', 'guardian-repair-approve', 'deploy', 'ota-publish', 'role-change', 'destructive-delete']) {
  assert.equal(mayAutoExecute(action), false, action);
}
assert.equal(mayAutoExecute('catalogue-audit-read'), true);
```

- [ ] **Step 2: Run RED**

Run: `node nova-next/tests/action-policy.test.mjs`

- [ ] **Step 3: Implement fail-closed policy**

Unknown actions return false. Protected writes remain human-gated. Guardian unavailability or incomplete evidence never becomes permission.

- [ ] **Step 4: Implement narrow adapter surface**

Adapters expose only explicitly supported reads/preparation actions and return structured `{ ok, data, evidence, error }` results. The UI never reaches protected tables directly.

- [ ] **Step 5: Run GREEN**

Run all Nova Next tests.

- [ ] **Step 6: Commit**

```bash
git add nova-next docs/nova-next
git commit -m "feat: add fail-closed Nova Next action adapters"
```

### Task 6: Promotion-ready web/PWA identity

**Files:**
- Create: `nova-next/manifest.webmanifest`
- Create: `nova-next/service-worker.js`
- Create: `nova-next/tests/promotion-config.test.mjs`
- Create: `nova-next/src/promotion-config.mjs`
- Create: `docs/nova-next/PROMOTION_RUNBOOK.md`

**Interfaces:**
- Produces: `CHANNELS = { development, production }` and `promotionChecklist()`.

- [ ] **Step 1: Write failing promotion configuration test**

```js
import assert from 'node:assert/strict';
import { CHANNELS } from '../src/promotion-config.mjs';
assert.notEqual(CHANNELS.development.appId, CHANNELS.production.appId);
assert.equal(CHANNELS.production.requiresExplicitPromotion, true);
```

- [ ] **Step 2: Run RED**

Run: `node nova-next/tests/promotion-config.test.mjs`

- [ ] **Step 3: Implement isolated development identity**

Use a distinct PWA scope/cache namespace and future Android development application ID. Production values remain declarative placeholders that cannot activate automatically.

- [ ] **Step 4: Write the promotion runbook**

Require parity pass, auth/security verification, Guardian boundary verification, data compatibility, signing-key/package-id verification, release rollback point and explicit human approval before swapping primary Nova routing/deployment.

- [ ] **Step 5: Run GREEN**

Run all Nova Next tests.

- [ ] **Step 6: Commit**

```bash
git add nova-next docs/nova-next
git commit -m "feat: make Nova Next promotion ready"
```

### Task 7: Verification and first review PR

**Files:**
- Create: `docs/nova-next/BOOTSTRAP_VALIDATION.md`

**Interfaces:**
- Consumes all previous tasks.
- Produces bootstrap validation evidence and a reviewable PR.

- [ ] **Step 1: Run all Nova Next unit/contract tests**

Run: `for f in nova-next/tests/*.test.mjs; do node "$f"; done`

Expected: every test exits 0.

- [ ] **Step 2: Static isolation audit**

Search `nova-next/` for imports/URLs that point to local `../nova/`, service-role credentials, provider secrets or direct protected-table access. Expected: no violations.

- [ ] **Step 3: Reference-layout review**

Verify splash, login, dashboard, side menu, Chat, Tools, Tasks, Projects, Settings and smooth-transition state against the approved `Nova AI Mobile App UI Mockup.png` reference.

- [ ] **Step 4: Record validation evidence**

Document tests, known intentionally-unwired backend adapters, risk classification and rollback (`delete nova-next/` before any production routing exists).

- [ ] **Step 5: Open a bootstrap PR**

PR must explicitly state that current `nova/` production code is untouched and that no production auth/schema/deployment/signing change is included.
