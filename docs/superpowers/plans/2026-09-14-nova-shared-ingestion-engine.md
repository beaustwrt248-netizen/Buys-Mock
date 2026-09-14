# Nova Shared Ingestion Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace duplicated Nova internal knowledge persistence with one shared engine while preserving endpoint-specific auth, privacy, attribution, source-selection and scheduler behavior.

**Architecture:** Add `supabase/functions/nova-knowledge/internal_ingestion.mjs` as the only owner of knowledge-item/source/chunk persistence. Manual ingestion and scheduled maintenance keep their own request, auth, source-fetching, run-tracking and error-policy code and pass explicit actor attribution into the shared engine.

**Tech Stack:** Supabase Edge Functions, ES modules, Node test runner, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-09-14-nova-shared-ingestion-engine-design.md`

## Global Constraints

- Keep `MAX_EMBEDDING_BATCH = 4` and `MAX_INGEST_BATCH = 4`.
- Keep `MORLEY_BACKUP_SECRET` plus `morley_backup_scheduler_secret_matches` scheduler authorization.
- Keep Admin bearer-token/profile-role authorization in manual ingestion.
- Keep optional-source handling for `inventory_items`, `sales_records`, and `valuation_quotes` local to maintenance; do not change privileges.
- Keep support `privacy_mode: "structured_pattern_only"` and do not add free-form support text.
- Keep `managed_by: "nova_internal_adapter"`, `internal:${adapterKey}`, legacy catalogue adoption and existing revision/chunk behavior.
- Manual actor attribution remains `authorized.user.id`; maintenance actor attribution remains `null`.
- No migrations, RLS/Auth changes, Guardian authority changes, scheduler cadence changes, or production-data changes.

---

### Task 1: Enforce RED in CI

**Files:**
- Create: `tests/nova_internal_ingestion_contract.test.mjs`
- Create: `.github/workflows/nova-shared-ingestion-contract.yml`

**Produces:** A PR check that runs the shared-ingestion contract before implementation.

- [ ] Add the contract requiring `internal_ingestion.mjs`, `createInternalIngestionEngine`, use from both callers, manual `actorId: authorized.user.id`, maintenance `actorId: null`, and no local definitions of `hasLegacyCatalogueCoverage`, `upsertSourceAndChunks`, or `persistDocument`.
- [ ] Assert the shared module preserves `nova_internal_adapter`, `internal:${adapterKey}`, legacy catalogue adoption, revisions, chunk supersession, pending embeddings and all four outcomes.
- [ ] Assert endpoint policy stays outside the shared module: Admin role/pagination in manual; scheduler secret, 4x4 ceilings, optional-source handling and redacted diagnostics in maintenance; support privacy in `internal_adapters.mjs`.
- [ ] Add a PR workflow that checks out code and runs:

```bash
node --test tests/nova_internal_ingestion_contract.test.mjs tests/nova_knowledge_maintenance_contract.test.mjs
```

- [ ] Verify the workflow fails because the shared module does not yet exist / persistence is still duplicated.

---

### Task 2: Extract the shared persistence engine

**Files:**
- Create: `supabase/functions/nova-knowledge/internal_ingestion.mjs`

**Produces:**

```js
export function createInternalIngestionEngine({ admin }) {
  return { persistDocument };
}
```

with:

```js
persistDocument(entry, { actorId = null } = {})
```

- [ ] Import `chunkKnowledgeDocument`, `normalizeKnowledgeDocument`, `sanitizeMetadata`, and `sha256Hex` from `knowledge_core.mjs`.
- [ ] Move the existing field-selection constant and `clean`, `authorityFor`, `validIso` persistence utilities into the shared module.
- [ ] Move `hasLegacyCatalogueCoverage(sourceIdentity)` unchanged in behavior.
- [ ] Move revision snapshot logic and set `changed_by: actorId ?? null`.
- [ ] Move source/chunk upsert logic, preserving source key `internal:${adapterKey}`, confidence/timestamps, sanitized metadata, active-chunk supersession and `embedding_status: "pending"`.
- [ ] Implement `persistDocument` with the existing lookup, unchanged refresh, update and create paths. Use `actorId ?? null` for audit fields.
- [ ] Return exactly `adopted`, `skipped`, `updated`, or `created`.

---

### Task 3: Convert manual ingestion

**Files:**
- Modify: `supabase/functions/nova-knowledge-ingest/index.ts`

- [ ] Import `createInternalIngestionEngine` from `../nova-knowledge/internal_ingestion.mjs`.
- [ ] Construct `const ingestion = createInternalIngestionEngine({ admin });` after the existing client.
- [ ] Remove only persistence-specific imports/helpers now owned by the shared module.
- [ ] Keep CORS, Admin auth, adapter selection, limit/offset pagination, source fetching, run tracking and response behavior unchanged.
- [ ] Replace the local persistence call with:

```ts
const outcome = await ingestion.persistDocument(entry, { actorId: authorized.user.id });
```

- [ ] Preserve `adopted_count` and skipped-count behavior.

---

### Task 4: Convert scheduled maintenance

**Files:**
- Modify: `supabase/functions/nova-knowledge-maintenance/index.ts`
- Modify: `tests/nova_knowledge_maintenance_contract.test.mjs`

- [ ] Import and construct the shared engine.
- [ ] Remove only duplicated persistence helpers/imports.
- [ ] Keep `MAX_EMBEDDING_BATCH = 4`, `MAX_INGEST_BATCH = 4`, `normalizeMaintenanceError`, `isOptionalSourcePermissionError`, `authorized`, `safeDocuments`, and `embedPending` unchanged in responsibility.
- [ ] Replace the local persistence call with:

```ts
const result = await ingestion.persistDocument(entry, { actorId: null });
```

- [ ] Treat `adopted` as the existing maintenance skipped outcome.
- [ ] Update maintenance contract assertions so identity/revision persistence is required in the shared module while maintenance is required to import/use it with null attribution.

---

### Task 5: Verify GREEN and protected boundaries

- [ ] Run the dedicated shared-ingestion workflow and focused tests; require PASS.
- [ ] Require support adapter privacy `structured_pattern_only` to remain present.
- [ ] Require Admin role enforcement and manual pagination to remain present.
- [ ] Require scheduler secret authorization, 4x4 ceilings, optional-source behavior and error redaction to remain present.
- [ ] Inspect the diff for absence of migrations, privilege changes, Auth/RLS changes, scheduler cadence changes, Guardian changes and production-data mutation.
- [ ] Require repository security, quality, Nova PR Guard and parity checks on the exact head.

---

### Task 6: Finish the branch

- [ ] Update PR #/metadata with RED and GREEN evidence and link issue #1878.
- [ ] Review changed files and PR comments for demonstrated defects only.
- [ ] Mark ready only after exact-head checks are green.
- [ ] Squash-merge without bypassing required checks.
- [ ] Verify `main` contains the shared module plus both caller imports/attribution.
- [ ] Close #1878 with merged SHA and verification evidence.
