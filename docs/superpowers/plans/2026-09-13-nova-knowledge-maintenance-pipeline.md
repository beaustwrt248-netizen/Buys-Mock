# Nova Knowledge Maintenance Pipeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a durable internal Nova maintenance loop that safely ingests approved internal knowledge and drains semantic embedding work in bounded batches without weakening user-facing auth.

**Architecture:** Add a dedicated internal maintenance Edge Function plus additive SQL for run-state/cron integration. Keep existing admin ingestion intact, reuse pure Nova knowledge helpers/adapters and the established Vault-backed internal scheduler credential, and preserve lexical retrieval as a permanent fallback if semantic maintenance fails.

**Tech Stack:** Supabase Edge Functions (Deno), Supabase AI `gte-small`, PostgreSQL 17, pgvector 0.8.2, pg_cron, pg_net, Vault, GitHub Actions contract/security gates.

**Spec:** `docs/superpowers/specs/2026-09-13-nova-knowledge-maintenance-pipeline-design.md`

## Global Constraints

- Maximum 20 embedding chunks per maintenance invocation.
- Never expose service-role or scheduler credentials to clients/logs/source control.
- Existing RLS/service-role boundaries stay unchanged.
- Add no new public `SECURITY DEFINER` helper.
- Production embedding failure state is `error`, not `failed`.
- Failed semantic maintenance must not break lexical retrieval.
- Support ingestion excludes customer free text, user/assignee IDs and raw diagnostics.
- Scheduled ingestion must use the same `nova_internal_adapter` / `internal:<adapter-key>` identity space as the manual ingester.
- No production merge/deploy before explicit approval of the exact ready PR/head.

---

### Task 1: Maintenance security and behavior contracts

**Files:**
- Create: `tests/nova_knowledge_maintenance_contract.test.mjs`
- Create: `.github/workflows/nova-knowledge-maintenance-contract.yml`

- [x] **Step 1: Write failing contract tests**

Contract covers: established scheduler credential validation, bounded `gte-small` embedding batch, production `ready/error` states, shared ingestion identities, legacy catalogue adoption, Vault/cron usage, no credential literals, RLS/service-role access, health metrics and lexical fallback preservation.

- [x] **Step 2: Run the contract test and verify RED**

GitHub Actions ran the dedicated contract before worker/migration implementation and failed because those files did not yet exist.

- [x] **Step 3: Commit the red contract and dedicated CI workflow**

---

### Task 2: Internal maintenance Edge Function

**Files:**
- Create: `supabase/functions/nova-knowledge-maintenance/index.ts`
- Reuse: `supabase/functions/nova-knowledge/knowledge_core.mjs`
- Reuse: `supabase/functions/nova-knowledge/internal_adapters.mjs`
- Test: `tests/nova_knowledge_maintenance_contract.test.mjs`

**Interfaces:**
- Consumes: `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`, existing project scheduler secret exposed to Edge Functions as `MORLEY_BACKUP_SECRET`; existing knowledge tables and pure adapters.
- Produces: `POST { action: "run", embedding_limit?: number, ingest_limit?: number }` returning aggregate counts only.

- [x] **Step 1: Implement strict internal authorization**

Require `x-maintenance-secret`. Validate against `MORLEY_BACKUP_SECRET` with constant-time comparison, with fallback to the existing service-role-only `morley_backup_scheduler_secret_matches` RPC. Do not accept a normal user JWT as scheduler authority.

- [x] **Step 2: Implement bounded embedding selection**

Select active pending chunks first, then retryable `error` rows older than the cooldown, oldest-first, total `limit <= 20`.

- [x] **Step 3: Implement per-chunk `gte-small` embedding**

Use `new Supabase.ai.Session('gte-small')`, mean pooling + normalization, require 384 dimensions, write successful `ready` state or bounded `error` state without aborting the batch.

- [x] **Step 4: Implement bounded safe ingestion**

Reuse safe adapters directly. Scheduled and manual ingestion share `managed_by: nova_internal_adapter` and `internal:<adapter-key>` identities. Legacy catalogue seeds are adopted rather than duplicated. Updates snapshot revisions. Support privacy exclusions remain intact and absent operational datasets emit no documents.

- [x] **Step 5: Return operational summary only**

Return embedding/ingestion counts and duration only; never return chunk text, vectors or credentials.

- [x] **Step 6: Verify worker through dedicated contract**

Worker assertions pass on the current implementation head; full dedicated contract passed after migration was added.

---

### Task 3: Maintenance run state and cron/Vault migration

**Files:**
- Create: `supabase/migrations/20260913042000_nova_knowledge_maintenance_pipeline.sql`
- Test: `tests/nova_knowledge_maintenance_contract.test.mjs`

**Interfaces:**
- Consumes: pg_cron, pg_net, existing Vault secret `morley_backup_scheduler_secret`, fixed project Edge Function URL.
- Produces: maintenance-run audit state and scheduled invocation every 5 minutes.

- [x] **Step 1: Add additive run-state table**

Create `nova_knowledge_maintenance_runs` with status, counts, timestamps and bounded error summary. Enable RLS. Revoke public/anon/authenticated access and grant only service-role table access.

- [x] **Step 2: Add cron schedule**

Create a deterministic cron job on `*/5 * * * *`. It calls `/functions/v1/nova-knowledge-maintenance` through `net.http_post`, resolving `morley_backup_scheduler_secret` from Vault at runtime into `x-maintenance-secret`. No secret value is committed. Environments without the Vault secret skip schedule creation.

- [x] **Step 3: Preserve invoker security**

No new `SECURITY DEFINER` helper is introduced. The existing scheduler matcher is reused unchanged.

- [x] **Step 4: Run contract test and verify GREEN**

Dedicated `contract` GitHub check passed on head `4edf3ea6c52050cb4c56ece954ff9900a30615f8`.

---

### Task 4: Health and observability integration

**Files:**
- Included in: `supabase/migrations/20260913042000_nova_knowledge_maintenance_pipeline.sql`

- [x] **Step 1: Extend health output additively**

`nova_knowledge_health()` keeps existing keys and adds latest maintenance time/status plus 24-hour maintenance failure count. Embedding health uses production `pending/ready/error` states.

- [x] **Step 2: Verify lexical fallback is untouched**

Migration does not drop or alter `nova_search_knowledge_chunks`, search vectors, or FTS indexes. Existing orchestrator can continue querying with `p_query_embedding = null`.

- [x] **Step 3: Run contract test**

Dedicated contract is green.

---

### Task 5: Repository verification and guarded PR

**PR:** `nova/knowledge-maintenance-pipeline` -> `main`, draft PR #1830.

- [ ] **Step 1: Reconcile with current `main` without forcing unrelated changes**

Use GitHub compare immediately before readiness. Preserve both histories if main advanced and mergeability requires reconciliation.

- [ ] **Step 2: Run/observe repository checks**

Require current-head results for Repository Security Audit, B&L Morley Quality Gate, Morley Ultimate Parity Gate, Full Feature Contract Audit, maintenance contract, Restore Point Capture and other triggered checks.

- [x] **Step 3: Inspect Nova Guard failure exactly**

Nova PR Guard intentionally fails closed because this PR adds a service-role/Vault/cron trust-boundary change. Do not weaken or bypass that guard.

- [ ] **Step 4: Update and mark PR ready with high-risk handoff**

PR body must state: trust-boundary change, existing shared Vault scheduler credential name (never value), cron cadence, function auth model, verification evidence, rollback steps, and **HIGH RISK — HUMAN APPROVAL REQUIRED**.

- [ ] **Step 5: Stop at protected boundary**

Do not merge, apply production migration, schedule production cron, or deploy the worker until the user explicitly approves the exact ready PR/head.