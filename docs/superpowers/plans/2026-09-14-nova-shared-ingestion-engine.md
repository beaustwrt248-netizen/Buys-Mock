# Nova Shared Ingestion Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace duplicated Nova internal knowledge persistence in manual ingestion and scheduled maintenance with one shared engine while preserving all existing authorization, privacy, attribution, source-selection and scheduler boundaries.

**Architecture:** Add `supabase/functions/nova-knowledge/internal_ingestion.mjs` as the only owner of internal knowledge persistence. Both Edge Functions keep their existing request/auth/source-fetch/run-tracking policy and call the shared engine with explicit actor attribution (`user.id` for manual Admin ingestion, `null` for maintenance).

**Tech Stack:** Supabase Edge Functions, TypeScript/JavaScript ES modules, Supabase JS client, Node test runner contract tests, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-09-14-nova-shared-ingestion-engine-design.md`

## Global Constraints

- Keep `MAX_EMBEDDING_BATCH = 4` and `MAX_INGEST_BATCH = 4` unchanged.
- Keep scheduler authorization on `MORLEY_BACKUP_SECRET` plus `morley_backup_scheduler_secret_matches` fallback unchanged.
- Keep Admin-only bearer-token/profile-role authorization for `nova-knowledge-ingest` unchanged.
- Keep optional permission handling local to maintenance for `inventory_items`, `sales_records` and `valuation_quotes`; do not add grants.
- Keep support ingestion `privacy_mode: "structured_pattern_only"`; do not add free-form support ticket text.
- Keep internal identity `managed_by: "nova_internal_adapter"` and source key `internal:${adapterKey}` unchanged.
- Keep legacy live-catalogue adoption behavior unchanged.
- Keep manual actor attribution to the authenticated Admin user and maintenance actor attribution null.
- Do not add migrations, change RLS/Auth, change Guardian approval boundaries, change scheduler cadence, or mutate production data.

---

### Task 1: Add a failing shared-engine contract

**Files:**
- Modify: `tests/nova_knowledge_maintenance_contract.test.mjs`
- Create: `tests/nova_internal_ingestion_contract.test.mjs`

**Interfaces:**
- Consumes: current `nova-knowledge-ingest/index.ts`, `nova-knowledge-maintenance/index.ts`, `internal_adapters.mjs`.
- Produces: repository contracts requiring `createInternalIngestionEngine({ admin })` and caller-specific attribution.

- [ ] **Step 1: Write the failing shared-engine test**

Create `tests/nova_internal_ingestion_contract.test.mjs` with assertions equivalent to:

```js
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const sharedPath = 'supabase/functions/nova-knowledge/internal_ingestion.mjs';
const manualPath = 'supabase/functions/nova-knowledge-ingest/index.ts';
const maintenancePath = 'supabase/functions/nova-knowledge-maintenance/index.ts';
const adaptersPath = 'supabase/functions/nova-knowledge/internal_adapters.mjs';
const read = (path) => fs.readFileSync(path, 'utf8');

test('manual and scheduled ingestion share one persistence engine', () => {
  assert.equal(fs.existsSync(sharedPath), true, `${sharedPath} must exist`);
  const shared = read(sharedPath);
  const manual = read(manualPath);
  const maintenance = read(maintenancePath);

  assert.match(shared, /export function createInternalIngestionEngine/);
  assert.match(manual, /createInternalIngestionEngine/);
  assert.match(maintenance, /createInternalIngestionEngine/);
  assert.match(manual, /actorId:\s*authorized\.user\.id/);
  assert.match(maintenance, /actorId:\s*null/);

  for (const duplicate of ['hasLegacyCatalogueCoverage', 'upsertSourceAndChunks', 'persistDocument']) {
    assert.doesNotMatch(manual, new RegExp(`(?:async\\s+)?function\\s+${duplicate}\\s*\\(`));
    assert.doesNotMatch(maintenance, new RegExp(`(?:async\\s+)?function\\s+${duplicate}\\s*\\(`));
    assert.match(shared, new RegExp(`(?:async\\s+)?function\\s+${duplicate}\\s*\\(`));
  }
});

test('shared engine preserves internal identity, revision and chunk lifecycle', () => {
  const shared = read(sharedPath);
  assert.match(shared, /managed_by:\s*["']nova_internal_adapter["']/);
  assert.match(shared, /`internal:\$\{adapterKey\}`/);
  assert.match(shared, /generated_from_live_catalogue/);
  assert.match(shared, /nova_knowledge_revisions/);
  assert.match(shared, /status:\s*["']superseded["']/);
  assert.match(shared, /embedding_status:\s*["']pending["']/);
  assert.match(shared, /return\s+["']created["']/);
  assert.match(shared, /return\s+["']updated["']/);
  assert.match(shared, /return\s+["']skipped["']/);
  assert.match(shared, /return\s+["']adopted["']/);
});

test('endpoint-specific auth, privacy and source policy stay outside the shared engine', () => {
  const shared = read(sharedPath);
  const manual = read(manualPath);
  const maintenance = read(maintenancePath);
  const adapters = read(adaptersPath);

  assert.match(manual, /p\.role\s*!==\s*["']admin["']/);
  assert.match(manual, /body\.adapter/);
  assert.match(manual, /body\.offset/);
  assert.match(maintenance, /MORLEY_BACKUP_SECRET/);
  assert.match(maintenance, /MAX_EMBEDDING_BATCH\s*=\s*4/);
  assert.match(maintenance, /MAX_INGEST_BATCH\s*=\s*4/);
  assert.match(maintenance, /optional:\s*true/);
  assert.match(maintenance, /normalizeMaintenanceError/);
  assert.match(adapters, /privacy_mode:\s*["']structured_pattern_only["']/);

  assert.doesNotMatch(shared, /MORLEY_BACKUP_SECRET/);
  assert.doesNotMatch(shared, /x-maintenance-secret/i);
  assert.doesNotMatch(shared, /profiles/);
  assert.doesNotMatch(shared, /support_tickets/);
  assert.doesNotMatch(shared, /inventory_items/);
  assert.doesNotMatch(shared, /sales_records/);
});
```

- [ ] **Step 2: Strengthen maintenance contract attribution assertions**

In `tests/nova_knowledge_maintenance_contract.test.mjs`, update the scheduled-ingestion identity test to additionally require the shared engine import/use and null actor attribution while retaining all existing assertions for `nova_internal_adapter`, `internal:${adapterKey}`, legacy catalogue adoption and revisions.

- [ ] **Step 3: Run focused tests and verify RED**

Run:

```bash
node --test tests/nova_internal_ingestion_contract.test.mjs tests/nova_knowledge_maintenance_contract.test.mjs
```

Expected: FAIL because `internal_ingestion.mjs` does not exist and both entrypoints still define their own persistence helpers.

- [ ] **Step 4: Commit the red tests**

```bash
git add tests/nova_internal_ingestion_contract.test.mjs tests/nova_knowledge_maintenance_contract.test.mjs
git commit -m "test: require shared Nova ingestion persistence"
```

---

### Task 2: Extract the shared persistence engine

**Files:**
- Create: `supabase/functions/nova-knowledge/internal_ingestion.mjs`
- Test: `tests/nova_internal_ingestion_contract.test.mjs`

**Interfaces:**
- Consumes: injected Supabase client `admin`, `normalizeKnowledgeDocument`, `chunkKnowledgeDocument`, `sanitizeMetadata`, `sha256Hex`.
- Produces: `createInternalIngestionEngine({ admin })` returning `persistDocument(entry, { actorId })`.

- [ ] **Step 1: Create shared module imports and utility constants**

Start `internal_ingestion.mjs` with:

```js
import {
  chunkKnowledgeDocument,
  normalizeKnowledgeDocument,
  sanitizeMetadata,
  sha256Hex,
} from './knowledge_core.mjs';

const F = 'id,category,title,content,source_type,source_label,source_filename,mime_type,version_label,trust_level,status,content_hash,revision,metadata,created_by,updated_by,created_at,updated_at';
const clean = (value, max = 220) => String(value ?? '').trim().replace(/\s+/g, ' ').slice(0, max);
const authorityFor = (trust) => trust === 'verified' ? 1 : trust === 'reviewed' ? 0.82 : 0.65;
const validIso = (value) => {
  const raw = clean(value, 80);
  if (!raw) return null;
  const parsed = Date.parse(raw);
  return Number.isFinite(parsed) ? new Date(parsed).toISOString() : null;
};
```

- [ ] **Step 2: Move legacy coverage and snapshot behavior into factory scope**

Inside `createInternalIngestionEngine({ admin })`, define `hasLegacyCatalogueCoverage(sourceIdentity)` and `snap(row, actorId)` using the exact current queries/fields. `snap` must write `changed_by: actorId ?? null`.

- [ ] **Step 3: Move source and chunk lifecycle into factory scope**

Move `upsertSourceAndChunks(row, normalized, adapterKey, refreshOnly)` into the shared factory. Preserve:

```js
source_key: `internal:${adapterKey}`
```

and preserve confidence, observed/stale timestamps, sanitized source metadata, active chunk supersession, `embedding_status: 'pending'`, and the existing upsert conflict key.

- [ ] **Step 4: Implement shared persistDocument**

Implement:

```js
async function persistDocument(entry, { actorId = null } = {}) {
  if (entry.adapter === 'device_catalog' && await hasLegacyCatalogueCoverage(entry.sourceIdentity)) {
    return 'adopted';
  }

  const adapterKey = await sha256Hex(`${entry.adapter}:${entry.sourceIdentity}`);
  const normalized = await normalizeKnowledgeDocument({
    ...entry.document,
    metadata: {
      ...(entry.document.metadata || {}),
      managed_by: 'nova_internal_adapter',
      adapter: entry.adapter,
      adapter_key: adapterKey,
    },
  });

  // Preserve current lookup, unchanged-content refresh, snapshot/update,
  // create and source/chunk calls using actorId for audit fields.
}
```

For unchanged content, preserve current manual behavior of setting `updated_by: actorId ?? null` in the knowledge item refresh. For updates, use `updated_by: actorId ?? null`. For creates, use both `created_by` and `updated_by` as `actorId ?? null`.

- [ ] **Step 5: Export only the factory**

End with:

```js
  return { persistDocument };
}
```

No request/auth/source-fetch/scheduler code belongs in this module.

- [ ] **Step 6: Run shared-module contract**

Run:

```bash
node --test tests/nova_internal_ingestion_contract.test.mjs
```

Expected: still FAIL because callers have not yet imported the engine, but shared-module identity/lifecycle assertions should pass.

- [ ] **Step 7: Commit shared engine**

```bash
git add supabase/functions/nova-knowledge/internal_ingestion.mjs
git commit -m "refactor: extract Nova internal ingestion engine"
```

---

### Task 3: Convert manual Admin ingestion to the shared engine

**Files:**
- Modify: `supabase/functions/nova-knowledge-ingest/index.ts`
- Test: `tests/nova_internal_ingestion_contract.test.mjs`

**Interfaces:**
- Consumes: `createInternalIngestionEngine({ admin })`.
- Produces: manual ingestion calls `persistDocument(entry, { actorId: authorized.user.id })`.

- [ ] **Step 1: Replace persistence-core imports**

Remove direct imports of `chunkKnowledgeDocument`, `normalizeKnowledgeDocument`, `sanitizeMetadata`, and `sha256Hex` from `knowledge_core.mjs` if no longer used by the entrypoint. Add:

```ts
import { createInternalIngestionEngine } from "../nova-knowledge/internal_ingestion.mjs";
```

- [ ] **Step 2: Construct the engine next to the Supabase client**

After `admin` is created:

```ts
const ingestion = createInternalIngestionEngine({ admin });
```

- [ ] **Step 3: Delete duplicated persistence helpers**

Remove entrypoint definitions of `authorityFor`, `validIso`, `snap`, `hasLegacyCatalogueCoverage`, `upsertSourceAndChunks` and `persistDocument` if they are no longer used. Keep `clean`, `bounded`, auth, headers, run tracking, `fetchRows`, `fetchDocuments` and endpoint handling.

- [ ] **Step 4: Route each document through shared persistence with Admin attribution**

Replace:

```ts
const outcome = await persistDocument(entry, authorized.user.id);
```

with:

```ts
const outcome = await ingestion.persistDocument(entry, { actorId: authorized.user.id });
```

Keep `adopted_count` and skipped-count semantics unchanged.

- [ ] **Step 5: Run focused contract**

Run:

```bash
node --test tests/nova_internal_ingestion_contract.test.mjs
```

Expected: still FAIL only on maintenance duplication/use; manual assertions pass.

- [ ] **Step 6: Commit manual conversion**

```bash
git add supabase/functions/nova-knowledge-ingest/index.ts
git commit -m "refactor: route manual Nova ingestion through shared engine"
```

---

### Task 4: Convert scheduled maintenance to the shared engine

**Files:**
- Modify: `supabase/functions/nova-knowledge-maintenance/index.ts`
- Test: `tests/nova_internal_ingestion_contract.test.mjs`
- Test: `tests/nova_knowledge_maintenance_contract.test.mjs`

**Interfaces:**
- Consumes: `createInternalIngestionEngine({ admin })`.
- Produces: maintenance calls `persistDocument(entry, { actorId: null })` while retaining source and scheduler policy.

- [ ] **Step 1: Replace persistence-core imports**

Remove direct `chunkKnowledgeDocument`, `normalizeKnowledgeDocument`, `sanitizeMetadata`, and `sha256Hex` imports from the maintenance entrypoint if no longer used. Add:

```ts
import { createInternalIngestionEngine } from "../nova-knowledge/internal_ingestion.mjs";
```

- [ ] **Step 2: Construct the shared engine**

After the `admin` client:

```ts
const ingestion = createInternalIngestionEngine({ admin });
```

- [ ] **Step 3: Remove duplicated persistence helpers only**

Delete `authorityFor`, `validIso`, `hasLegacyCatalogueCoverage`, `snap`, `upsertSourceAndChunks`, and local `persistDocument` when unused.

Do not remove or alter:

```ts
MAX_EMBEDDING_BATCH = 4
MAX_INGEST_BATCH = 4
normalizeMaintenanceError
isOptionalSourcePermissionError
authorized
safeDocuments
embedPending
```

- [ ] **Step 4: Use null actor attribution**

Inside `ingestInternal`, replace the local call with:

```ts
const result = await ingestion.persistDocument(entry, { actorId: null });
```

Treat `adopted` exactly like the existing maintenance skipped outcome so maintenance response counters remain unchanged.

- [ ] **Step 5: Run focused tests and verify GREEN**

Run:

```bash
node --test tests/nova_internal_ingestion_contract.test.mjs tests/nova_knowledge_maintenance_contract.test.mjs
```

Expected: PASS.

- [ ] **Step 6: Commit maintenance conversion**

```bash
git add supabase/functions/nova-knowledge-maintenance/index.ts tests/nova_knowledge_maintenance_contract.test.mjs
git commit -m "refactor: route Nova maintenance through shared ingestion"
```

---

### Task 5: Verify privacy, authorization and broader Nova knowledge contracts

**Files:**
- No production changes expected unless a regression test exposes a demonstrated mismatch.

**Interfaces:**
- Consumes: completed shared engine and both callers.
- Produces: exact-head evidence that protected behavior is unchanged.

- [ ] **Step 1: Run all Nova knowledge-focused tests**

Run:

```bash
node --test tests/nova*knowledge*.test.mjs tests/*nova*maintenance*.test.mjs
```

Expected: PASS.

- [ ] **Step 2: Run any explicit support/privacy adapter contracts found in the repository**

Run the matching Node tests covering `internal_adapters.mjs`, support privacy, Guardian knowledge ingestion and knowledge normalization. Expected: PASS.

- [ ] **Step 3: Syntax-check modified Edge Functions/modules**

Run the repository-established JavaScript/TypeScript syntax/type validation commands used by CI for Supabase Edge Functions. Expected: PASS.

- [ ] **Step 4: Review diff for boundary creep**

Confirm the diff contains no migrations, grants, RLS changes, scheduler secret changes, batch-size changes, scheduler cadence changes, support free-text fields, Guardian approval changes or production-data writes.

- [ ] **Step 5: Commit any test-only contract adjustments if required**

If focused tests reveal a missing regression assertion but no behavior change, add only the narrow assertion and commit it separately.

---

### Task 6: Open PR and require exact-head protected CI

**Files:**
- PR metadata only.

**Interfaces:**
- Consumes: fully tested branch.
- Produces: reviewable PR linked to issue #1878.

- [ ] **Step 1: Open PR against main**

Title:

```text
refactor: unify Nova internal ingestion persistence
```

Body must state:

- closes #1878;
- shared persistence only;
- Admin and scheduler authorization remain separate;
- maintenance stays 4×4;
- optional sources stay optional without grant changes;
- support privacy remains structured-pattern-only;
- no migration/RLS/Auth/Guardian/production-data changes;
- RED and GREEN TDD evidence.

- [ ] **Step 2: Require exact-head checks**

Verify at minimum the repository security audit, quality gate, Nova PR Guard, Nova knowledge/maintenance contracts, parity gate and any Supabase/function contract gates triggered by the changed paths.

- [ ] **Step 3: Review PR diff and comments**

Resolve only demonstrated issues. Do not broaden the refactor.

- [ ] **Step 4: Merge only when all required checks pass**

Use squash merge with the exact reviewed head SHA. Do not bypass required checks.

- [ ] **Step 5: Verify merged main and close #1878**

Fetch `main` versions of the shared engine and both callers, confirm imports/attribution and preserved limits/auth contracts, then close #1878 with merged SHA and CI evidence.
