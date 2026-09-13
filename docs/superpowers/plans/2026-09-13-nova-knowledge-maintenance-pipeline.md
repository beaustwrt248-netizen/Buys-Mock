# Nova Knowledge Maintenance Pipeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a durable internal Nova maintenance loop that safely ingests approved internal knowledge and drains semantic embedding work in bounded batches without weakening user-facing auth.

**Architecture:** Add a dedicated internal maintenance Edge Function plus additive SQL for run-state/cron integration. Keep existing admin ingestion intact, reuse pure Nova knowledge helpers/adapters, and preserve lexical retrieval as a permanent fallback if semantic maintenance fails.

**Tech Stack:** Supabase Edge Functions (Deno), Supabase AI `gte-small`, PostgreSQL 17, pgvector 0.8.2, pg_cron, pg_net, Vault, GitHub Actions contract/security gates.

**Spec:** `docs/superpowers/specs/2026-09-13-nova-knowledge-maintenance-pipeline-design.md`

## Global Constraints

- Maximum 20 embedding chunks per maintenance invocation.
- Never expose service-role or maintenance credentials to clients/logs/source control.
- Existing RLS/service-role boundaries stay unchanged.
- No public `SECURITY DEFINER` helper is introduced.
- Failed semantic maintenance must not break lexical retrieval.
- Support ingestion excludes customer free text, user/assignee IDs and raw diagnostics.
- No production merge/deploy before explicit approval of the exact ready PR/head.

---

### Task 1: Maintenance security and behavior contracts

**Files:**
- Create: `tests/nova_knowledge_maintenance_contract.test.mjs`
- Test: `tests/nova_knowledge_maintenance_contract.test.mjs`

**Interfaces:**
- Consumes: planned `supabase/functions/nova-knowledge-maintenance/index.ts` and migration file.
- Produces: contract expectations for bounded batch size, credential checks, embedding state transitions, Vault/cron usage, no literal credentials, and preserved RLS/security semantics.

- [ ] **Step 1: Write failing contract tests**

Assert the worker file exists and contains: `gte-small`, a maximum batch of 20, authorization validation before maintenance work, only bounded aggregate response fields, pending/failed eligibility, ready/failed transitions, and no service-role literal. Assert the migration uses Vault lookup by name, schedules a 5-minute cron call, and does not contain credential literals or `SECURITY DEFINER`.

- [ ] **Step 2: Run the contract test and verify RED**

Run: `node --test tests/nova_knowledge_maintenance_contract.test.mjs`
Expected: FAIL because the worker/migration do not yet exist.

- [ ] **Step 3: Commit the red contract**

Commit message: `test: define Nova knowledge maintenance contract`

---

### Task 2: Internal maintenance Edge Function

**Files:**
- Create: `supabase/functions/nova-knowledge-maintenance/index.ts`
- Reuse: `supabase/functions/nova-knowledge/knowledge_core.mjs`
- Reuse: `supabase/functions/nova-knowledge/internal_adapters.mjs`
- Test: `tests/nova_knowledge_maintenance_contract.test.mjs`

**Interfaces:**
- Consumes: `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`, `NOVA_KNOWLEDGE_MAINTENANCE_SECRET`; existing knowledge tables and pure adapters.
- Produces: `POST { action: "run", embedding_limit?: number, ingest_limit?: number }` returning aggregate counts only.

- [ ] **Step 1: Implement strict internal authorization**

Require `Authorization: Bearer <maintenance-secret>` and compare against `NOVA_KNOWLEDGE_MAINTENANCE_SECRET` before database reads. Return 401 for absent/invalid values. Do not accept user JWTs as a substitute.

- [ ] **Step 2: Implement bounded embedding selection**

Select active chunks with `embedding_status in ('pending','failed')`, retrying failed rows only after a cooldown, ordered oldest-first, with `limit <= 20`.

- [ ] **Step 3: Implement per-chunk `gte-small` embedding**

Use `new Supabase.ai.Session('gte-small')`, mean pooling + normalization, require 384 dimensions, write successful state (`ready`, provider/model/dimensions/embedded_at), or bounded error state (`failed`) without aborting the batch.

- [ ] **Step 4: Implement bounded safe ingestion**

Reuse existing safe adapters directly rather than calling the admin-auth endpoint. Process a bounded subset of internal sources and persist only sanitized structured documents. Keep support privacy exclusions intact and emit zero documents for absent operational datasets.

- [ ] **Step 5: Return operational summary only**

Return `embedded_ready`, `embedded_failed`, `ingested_created`, `ingested_updated`, `ingested_skipped`, `duration_ms`; never return chunk text, vectors or credentials.

- [ ] **Step 6: Run contract test and verify GREEN for worker assertions**

Run: `node --test tests/nova_knowledge_maintenance_contract.test.mjs`
Expected: migration-related assertions may still fail; worker assertions pass.

- [ ] **Step 7: Commit worker**

Commit message: `feat: add bounded Nova knowledge maintenance worker`

---

### Task 3: Maintenance run state and cron/Vault migration

**Files:**
- Create: `supabase/migrations/20260913042000_nova_knowledge_maintenance_pipeline.sql`
- Test: `tests/nova_knowledge_maintenance_contract.test.mjs`

**Interfaces:**
- Consumes: pg_cron, pg_net, Vault secret named `nova_knowledge_maintenance_secret`, project URL setting.
- Produces: maintenance-run audit state and scheduled invocation every 5 minutes.

- [ ] **Step 1: Add additive run-state table**

Create `nova_knowledge_maintenance_runs` with status, counts, timestamps and bounded error summary. Enable RLS. Grant table access only to `service_role`; revoke anon/authenticated privileges.

- [ ] **Step 2: Add cron schedule**

Create a single deterministic cron job (unscheduling any previous job with the same name first) on `*/5 * * * *`. Use `pg_net.http_post` to call `/functions/v1/nova-knowledge-maintenance` and obtain the bearer value from Vault at runtime. The migration must contain no secret value.

- [ ] **Step 3: Preserve invoker security**

Avoid new `SECURITY DEFINER` helpers. Any SQL helper must be `SECURITY INVOKER` and explicitly revoke public execution unless needed.

- [ ] **Step 4: Run contract test and verify GREEN**

Run: `node --test tests/nova_knowledge_maintenance_contract.test.mjs`
Expected: PASS.

- [ ] **Step 5: Commit migration**

Commit message: `feat: schedule Nova knowledge maintenance safely`

---

### Task 4: Health and observability integration

**Files:**
- Modify: migration above or add a follow-up migration in the same branch if necessary.
- Test: `tests/nova_knowledge_maintenance_contract.test.mjs`

**Interfaces:**
- Consumes: `nova_knowledge_maintenance_runs`, existing `nova_knowledge_health()`.
- Produces: aggregate health including latest maintenance time/status and recent failure count.

- [ ] **Step 1: Extend health output additively**

Expose maintenance aggregates only: latest run timestamp/status, last 24h failed-run count, pending/ready/failed embeddings and coverage. Do not expose source content or credentials.

- [ ] **Step 2: Verify lexical fallback is untouched**

Confirm `nova_search_knowledge_chunks` remains usable with `p_query_embedding = null` and no migration removes FTS indexes/search vectors.

- [ ] **Step 3: Run contract test**

Run: `node --test tests/nova_knowledge_maintenance_contract.test.mjs`
Expected: PASS.

- [ ] **Step 4: Commit health changes**

Commit message: `feat: expose Nova maintenance health`

---

### Task 5: Repository verification and guarded PR

**Files:**
- Update: `docs/superpowers/plans/2026-09-13-nova-knowledge-maintenance-pipeline.md` checkboxes as tasks complete.
- PR: branch `nova/knowledge-maintenance-pipeline` -> `main`.

**Interfaces:**
- Consumes: all implementation commits.
- Produces: exact ready PR/head for protected approval.

- [ ] **Step 1: Rebase/reconcile with current `main` without forcing unrelated changes**

Use GitHub compare; if current main advanced with non-overlapping files, preserve both histories via a normal merge commit only if required for mergeability.

- [ ] **Step 2: Run/observe repository checks**

Require current-head results for Repository Security Audit, B&L Morley Quality Gate, Morley Ultimate Parity Gate, Full Feature Contract Audit, Nova PR Guard, Restore Point Capture and other triggered checks.

- [ ] **Step 3: Inspect failures exactly**

Fix real code/test failures. Do not weaken Nova PR Guard if it intentionally fails closed on service-role/Vault/cron trust boundaries.

- [ ] **Step 4: Open PR with high-risk handoff**

PR body must state: trust-boundary change, migration details, Vault secret name (not value), cron cadence, function auth model, verification evidence, rollback steps, and **HIGH RISK — HUMAN APPROVAL REQUIRED**.

- [ ] **Step 5: Stop at protected boundary**

Do not merge, apply production migration, create production Vault secret, or deploy the worker until the user explicitly approves the exact ready PR/head.