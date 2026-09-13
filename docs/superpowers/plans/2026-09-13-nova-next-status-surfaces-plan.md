# Nova Next Automation and Integrations Status Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Automation and Integrations placeholders with truthful capability/status surfaces using existing safe runtime evidence only.

**Architecture:** Add a pure status-model module that classifies capability registry entries into live/read-only/staged/blocked states without granting authority. Extend `workspace-ui.mjs` to render Automation from that model and Integrations from authenticated-session presence plus the existing read-only GitHub broker status method.

**Tech Stack:** Browser ES modules, existing `CAPABILITIES`, existing `featureRuntime.githubStatus()`, Node 22 built-in tests, existing Nova Next card/status UI.

**Spec:** `docs/superpowers/specs/2026-09-13-nova-next-functionality-first-design.md`

## Global Constraints

- No new scheduling, arbitrary action executor, GitHub mutation, Guardian repair, release, OTA, pricing, user-role, or destructive backend action.
- Do not widen `SAFE_CLIENT_FUNCTIONS` for these screens.
- No integration is shown as connected unless runtime evidence supports it.
- Browser capability labels are product/UI status, not server authorization claims.
- Current production `nova/**` remains untouched.

---

### Task 1: Capability status model

**Files:**
- Create: `nova-next/src/capability-status.mjs`
- Create: `nova-next/tests/capability-status.test.mjs`

**Interfaces:**
- Consumes: `CAPABILITIES` records.
- Produces: `AUTOMATION_CAPABILITY_IDS`, `statusForCapability(capability, evidence)`, `automationStatusCards(capabilities, evidence)`.
- Status enum: `live`, `read_only`, `staged`, `blocked`.

- [ ] **Step 1: Write failing classification tests**

```js
import test from 'node:test';
import assert from 'node:assert/strict';
import { statusForCapability, automationStatusCards } from '../src/capability-status.mjs';

const capability = (id, risk='medium') => ({ id, label:id, section:'tasks', risk, requiresAuth:true, adapter:id });

test('protected authority is blocked rather than implied live', () => {
  assert.equal(statusForCapability(capability('release-readiness','high'), { liveIds:new Set(['release-readiness']) }), 'blocked');
  assert.equal(statusForCapability(capability('guardian-analysis','high'), { liveIds:new Set(['guardian-analysis']) }), 'blocked');
});

test('automation cards are truthful when no executor exists', () => {
  const cards = automationStatusCards([
    capability('multi-step-jobs'), capability('proactive-alerts'), capability('scenario-planning','low')
  ], { liveIds:new Set(), readOnlyIds:new Set() });
  assert.deepEqual(cards.map(x => x.status), ['staged','staged','staged']);
});
```

- [ ] **Step 2: Run and verify RED**

Run: `node --test nova-next/tests/capability-status.test.mjs`

Expected: FAIL because module does not exist.

- [ ] **Step 3: Implement explicit policy model**

```js
const BLOCKED_IDS = new Set(['guardian-analysis','release-readiness','pricing-intelligence']);
export const AUTOMATION_CAPABILITY_IDS = Object.freeze(['multi-step-jobs','proactive-alerts','scenario-planning']);

export function statusForCapability(capability, { liveIds = new Set(), readOnlyIds = new Set() } = {}) {
  if (BLOCKED_IDS.has(capability.id)) return 'blocked';
  if (readOnlyIds.has(capability.id)) return 'read_only';
  if (liveIds.has(capability.id)) return 'live';
  return 'staged';
}

export function automationStatusCards(capabilities, evidence = {}) {
  const byId = new Map(capabilities.map(item => [item.id, item]));
  return AUTOMATION_CAPABILITY_IDS.map(id => byId.get(id)).filter(Boolean).map(item => ({
    id: item.id,
    label: item.label,
    risk: item.risk,
    status: statusForCapability(item, evidence)
  }));
}
```

- [ ] **Step 4: Add no-authority regression assertions**

```js
test('status model contains no execution API', async () => {
  const source = await import('node:fs/promises').then(fs => fs.readFile(new URL('../src/capability-status.mjs', import.meta.url), 'utf8'));
  for (const forbidden of ['invoke(', 'fetch(', 'guardian-repair', 'prepareDraft', 'deploy', 'set_role']) {
    assert.equal(source.includes(forbidden), false, forbidden);
  }
});
```

- [ ] **Step 5: Run GREEN and commit**

Run: `node --test nova-next/tests/capability-status.test.mjs`

Expected: PASS.

```bash
git add nova-next/src/capability-status.mjs nova-next/tests/capability-status.test.mjs
git commit -m "feat(nova-next): add truthful capability status model"
```

---

### Task 2: Integration status runtime

**Files:**
- Modify: `nova-next/src/feature-runtime.mjs`
- Create: `nova-next/tests/integration-status.test.mjs`

**Interfaces:**
- Consumes: existing `getAccessToken` closure and `services.github.status()`.
- Produces: `integrationStatus()` returning settled per-card status for `nova_session` and `github_broker`.

- [ ] **Step 1: Write failing partial-failure test**

```js
import test from 'node:test';
import assert from 'node:assert/strict';
import { createFeatureRuntime } from '../src/feature-runtime.mjs';

test('integration status does not fake GitHub connectivity on failure', async () => {
  const edgeClient = { invoke: async name => {
    if (name === 'nova-github') throw new Error('BROKER_DOWN');
    return { ok:true };
  }};
  const runtime = createFeatureRuntime({ getAccessToken: () => 'token', edgeClient });
  const status = await runtime.integrationStatus();
  assert.deepEqual(status.novaSession, { connected:true, label:'Admin session active' });
  assert.equal(status.githubBroker.connected, false);
  assert.equal(status.githubBroker.available, false);
  assert.match(status.githubBroker.error, /BROKER_DOWN/);
});
```

- [ ] **Step 2: Run RED**

Run: `node --test nova-next/tests/integration-status.test.mjs`

Expected: FAIL because `integrationStatus()` does not exist.

- [ ] **Step 3: Implement settled read-only status method**

Add inside `createFeatureRuntime`:

```js
async function integrationStatus() {
  const token = getAccessToken();
  const novaSession = Object.freeze({ connected: Boolean(token), label: token ? 'Admin session active' : 'Not authenticated' });
  try {
    const github = await services.github.status();
    return Object.freeze({
      novaSession,
      githubBroker: Object.freeze({ connected: github?.configured === true, available: true, configured: github?.configured === true, error: '' })
    });
  } catch (error) {
    return Object.freeze({
      novaSession,
      githubBroker: Object.freeze({ connected:false, available:false, configured:false, error:String(error?.message || error) })
    });
  }
}
```

Return `integrationStatus` from the frozen runtime API. Do not add new Edge Function names or actions.

- [ ] **Step 4: Run GREEN plus existing runtime test**

Run: `node --test nova-next/tests/integration-status.test.mjs nova-next/tests/feature-runtime.test.mjs`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add nova-next/src/feature-runtime.mjs nova-next/tests/integration-status.test.mjs
git commit -m "feat(nova-next): add read-only integration status"
```

---

### Task 3: Automation and Integrations UI

**Files:**
- Modify: `nova-next/src/workspace-ui.mjs`
- Modify: `nova-next/index.html`
- Modify: `nova-next/live.css`
- Create: `nova-next/tests/status-surfaces-ui-contract.test.mjs`

**Interfaces:**
- Consumes: `CAPABILITIES`, `automationStatusCards`, `featureRuntime.integrationStatus()`.
- Produces route renderers `renderAutomation()` and `renderIntegrations()`.

- [ ] **Step 1: Write failing route contract**

```js
import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const html = await readFile(new URL('../index.html', import.meta.url), 'utf8');
const source = await readFile(new URL('../src/workspace-ui.mjs', import.meta.url), 'utf8');

test('Automation and Integrations are no longer placeholder-only', () => {
  assert.ok(html.includes('novaNextAutomationList'));
  assert.ok(html.includes('novaNextIntegrationsList'));
  assert.ok(source.includes('renderAutomation'));
  assert.ok(source.includes('renderIntegrations'));
});
```

- [ ] **Step 2: Run RED**

Run: `node --test nova-next/tests/status-surfaces-ui-contract.test.mjs`

Expected: FAIL on missing containers/renderers.

- [ ] **Step 3: Replace placeholder HTML with status shells**

```html
<section class="page" data-route="automation" aria-labelledby="automation-title">
  <div class="page-title-row"><div><h1 id="automation-title">Automation</h1><p>Nova workflow capability status</p></div></div>
  <p class="feature-boundary">Status only. This screen cannot execute Guardian repair, deploy, release, OTA, pricing, role, or arbitrary actions.</p>
  <div id="novaNextAutomationList" class="feature-status-grid"></div>
</section>
```

```html
<section class="page" data-route="integrations" aria-labelledby="integrations-title">
  <div class="page-title-row"><div><h1 id="integrations-title">Integrations</h1><p>Verified connection status</p></div></div>
  <div id="novaNextIntegrationsList" class="feature-status-grid"></div>
</section>
```

- [ ] **Step 4: Implement Automation renderer**

Requirements:
- Build cards from `automationStatusCards(CAPABILITIES, { liveIds:new Set(), readOnlyIds:new Set() })` for this slice because no automation executor is being added.
- Display `Staged` for multi-step jobs, alerts, scenario planning until a real adapter exists.
- Include a compact boundary card stating protected actions remain absent.
- No button that implies Run/Execute/Approve/Deploy.

- [ ] **Step 5: Implement Integrations renderer with partial failure**

Requirements:
- `Nova / Supabase`: Connected only if `integrationStatus().novaSession.connected === true`; otherwise Not authenticated.
- `GitHub broker`: Connected only if status call succeeds and `configured === true`; otherwise Not configured or Unavailable based on `available`.
- GitHub card copy explicitly states `Read-only status in Nova Next`.
- Do not render credential forms, tokens, secret fields, or Connect buttons.
- If GitHub status fails, still render Nova session status.

- [ ] **Step 6: Add status badge styles and run GREEN**

Add `.capability-status`, `.capability-status.live`, `.capability-status.read-only`, `.capability-status.staged`, `.capability-status.blocked` using existing palette variables rather than new hard-coded visual system.

Run: `node --test nova-next/tests/status-surfaces-ui-contract.test.mjs nova-next/tests/capability-status.test.mjs nova-next/tests/integration-status.test.mjs`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add nova-next/src/workspace-ui.mjs nova-next/index.html nova-next/live.css nova-next/tests/status-surfaces-ui-contract.test.mjs
git commit -m "feat(nova-next): add truthful automation and integration status"
```

---

### Task 4: Route wiring and protected-boundary verification

**Files:**
- Modify: `nova-next/src/workspace-ui.mjs`
- Modify: `nova-next/app.js` only if additional constructor dependencies are required.

**Interfaces:**
- Consumes: existing route-changed callback.
- Produces: Automation/Integrations lazy rendering without side effects.

- [ ] **Step 1: Add failing route assertion**

Extend `status-surfaces-ui-contract.test.mjs`:

```js
assert.ok(source.includes("route === 'automation'"));
assert.ok(source.includes("route === 'integrations'"));
```

- [ ] **Step 2: Run RED**

Run: `node --test nova-next/tests/status-surfaces-ui-contract.test.mjs`

Expected: FAIL until route handling exists.

- [ ] **Step 3: Add lazy route handling**

Inside `workspaceUi.routeChanged(route)`:

```js
if (route === 'automation') renderAutomation();
if (route === 'integrations') await renderIntegrations();
```

Keep rendering idempotent or clear/rebuild the target container from fresh status each visit; no background polling is introduced.

- [ ] **Step 4: Run full Nova Next verification**

```bash
node --test nova-next/tests/*.test.mjs
node --check nova-next/app.js
find nova-next/src -type f -name '*.mjs' -print0 | sort -z | xargs -0 -n1 node --check
git diff --name-only main...HEAD | grep '^nova/' && exit 1 || true
git diff main...HEAD -- nova-next/src/safe-client-functions.mjs
```

Expected:
- all tests PASS;
- syntax exits 0;
- no current `nova/**` changes;
- final command has no diff, proving the safe Edge Function allowlist was not widened.

- [ ] **Step 5: Commit**

```bash
git add nova-next/src/workspace-ui.mjs nova-next/app.js nova-next/tests/status-surfaces-ui-contract.test.mjs
git commit -m "feat(nova-next): wire status-only workspace routes"
```
