# Nova Knowledge Maintenance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a protected, bounded maintenance loop that continuously ingests safe internal Nova knowledge and drains semantic embedding backlog without weakening user-facing authorization or lexical fallback.

**Architecture:** A dedicated internal Edge Function performs bounded maintenance using the existing service-role backend and Supabase `gte-small` embeddings. Additive SQL supplies concurrency-safe chunk claiming, retry metadata, health counters, and a Vault-backed pg_cron invocation; the existing admin ingester and orchestrator remain independently usable.

**Tech Stack:** Supabase Edge Functions (Deno/TypeScript), Supabase AI `gte-small`, PostgreSQL 17, pgvector 0.8.2, pg_cron, pg_net, Supabase Vault, Node contract tests, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-09-13-nova-knowledge-maintenance-design.md`

## Global Constraints

- Maximum embedding batch is 20 chunks per invocation.
- Maximum internal-ingestion batch is 50 rows per adapter invocation.
- Do not expose service-role or maintenance credentials to clients, logs, responses, source control, or knowledge rows.
- Existing Nova RLS, admin/JWT authentication, protected-action boundaries, and lexical fallback remain intact.
- Migration is additive; no knowledge content deletion or destructive rewrite.
- Production merge/deploy/schedule remains human-gated on the exact ready PR head.

---

### Task 1: Maintenance database contract

**Files:**
- Create: `tests/nova_knowledge_maintenance_contract.test.mjs`
- Create: `supabase/migrations/20260913043000_nova_knowledge_maintenance.sql`

**Interfaces:**
- Produces: `public.nova_claim_embedding_chunks(p_limit integer)` returning bounded claimed chunk rows; extended `public.nova_knowledge_health()` maintenance counters.
- Consumes: existing `public.nova_knowledge_chunks`, `public.nova_knowledge_health()` and service-role-only grants.

- [ ] **Step 1: Write failing contract tests** asserting retry columns are additive, claim RPC clamps to 20, filters active non-ready chunks, uses `FOR UPDATE SKIP LOCKED`, is `SECURITY INVOKER`, revokes anon/authenticated access, and health remains content-free.
- [ ] **Step 2: Run the contract test** with `node --test tests/nova_knowledge_maintenance_contract.test.mjs`; expect failure because the migration does not exist.
- [ ] **Step 3: Implement the additive migration** with retry columns, indexes, claim RPC, service-role grants, and health counters. Do not add `SECURITY DEFINER`, destructive DDL, or client grants.
- [ ] **Step 4: Re-run the contract test** and expect PASS.
- [ ] **Step 5: Commit** `test/feat: add bounded Nova embedding claim contract`.

### Task 2: Bounded maintenance worker

**Files:**
- Create: `supabase/functions/nova-knowledge-maintenance/index.ts`
- Create: `tests/nova_knowledge_maintenance_edge_contract.test.mjs`

**Interfaces:**
- Consumes: `nova_claim_embedding_chunks(20)`, `nova_knowledge_health()`, `Supabase.ai.Session('gte-small')`, service-role client.
- Produces: POST maintenance response `{ ok, claimed, processed, failed, deferred, health }`; no content or vectors in response.

- [ ] **Step 1: Write failing Edge contract tests** requiring a dedicated maintenance-token header, JWT-protected deployment expectation, hard maximum batch 20, `gte-small`, 384-dimension validation, no content/vector logging, and updates limited to embedding/retry columns.
- [ ] **Step 2: Run** `node --test tests/nova_knowledge_maintenance_edge_contract.test.mjs`; expect failure because worker does not exist.
- [ ] **Step 3: Implement minimal worker**: validate `NOVA_KNOWLEDGE_MAINTENANCE_TOKEN` with constant-time comparison, claim up to 20 chunks, embed each with `gte-small`, mark ready on valid 384-vector, otherwise mark retry/error with bounded error code/message. Never accept arbitrary table/RPC names from request.
- [ ] **Step 4: Re-run the Edge contract** and expect PASS.
- [ ] **Step 5: Commit** `feat: add bounded Nova knowledge maintenance worker`.

### Task 3: Scheduled Vault-backed invocation

**Files:**
- Modify: `supabase/migrations/20260913043000_nova_knowledge_maintenance.sql`
- Extend: `tests/nova_knowledge_maintenance_contract.test.mjs`

**Interfaces:**
- Consumes: Vault secret names `nova_knowledge_maintenance_token` and `project_url`; `pg_net`; `pg_cron`.
- Produces: named cron schedule `nova-knowledge-maintenance` invoking the worker once per minute.

- [ ] **Step 1: Extend failing test** to require cron SQL to read `vault.decrypted_secrets`, forbid literal bearer/service-role secrets, use a fixed Edge Function path, and unschedule an existing same-name job before rescheduling idempotently.
- [ ] **Step 2: Run the test** and verify it fails on missing scheduling SQL.
- [ ] **Step 3: Add fixed Vault-backed cron SQL** using `cron.schedule` + `net.http_post`; only the named token and project URL may be loaded from Vault, and the request body is a fixed maintenance action.
- [ ] **Step 4: Re-run contract test** and expect PASS.
- [ ] **Step 5: Commit** `feat: schedule protected Nova knowledge maintenance`.

### Task 4: Safe internal-ingestion cadence

**Files:**
- Modify: `supabase/functions/nova-knowledge-maintenance/index.ts`
- Modify: `supabase/functions/nova-knowledge-ingest/index.ts` only if a reusable internal module is required; otherwise call the existing protected ingester through a fixed server-side route.
- Extend: `tests/nova_knowledge_maintenance_edge_contract.test.mjs`

**Interfaces:**
- Produces: hourly bounded ingestion attempt per adapter with checkpoint continuation; embedding stage remains independent.
- Consumes: existing safe adapter privacy rules and ingestion-run state.

- [ ] **Step 1: Add failing tests** proving ingestion batch is clamped to 50, adapters are an allowlist, support free text/raw diagnostics/user IDs remain excluded, and an ingestion failure cannot prevent embedding processing.
- [ ] **Step 2: Run test** and verify the new assertions fail.
- [ ] **Step 3: Implement hourly bounded ingestion orchestration** using fixed adapter names and persisted run/checkpoint timestamps. Reuse existing safe adapter code rather than duplicating/redesigning privacy logic.
- [ ] **Step 4: Re-run tests** and expect PASS.
- [ ] **Step 5: Commit** `feat: add bounded Nova internal knowledge refresh`.

### Task 5: Verification and guarded handoff

**Files:**
- Update: `docs/superpowers/plans/2026-09-13-nova-knowledge-maintenance.md` checkboxes as tasks complete.
- Update PR body with rollout/rollback evidence.

**Interfaces:**
- Produces: ready high-risk PR; no production mutation until exact-head approval.

- [ ] **Step 1: Run focused tests**:
  - `node --test tests/nova_knowledge_maintenance_contract.test.mjs`
  - `node --test tests/nova_knowledge_maintenance_edge_contract.test.mjs`
  - existing Nova knowledge platform/ingestion/orchestrator contract tests.
- [ ] **Step 2: Inspect final diff** for secrets, destructive SQL, `SECURITY DEFINER`, weakened RLS/auth, unbounded loops, or knowledge-content logging.
- [ ] **Step 3: Reconcile branch with current `main` without force-overwriting unrelated work.**
- [ ] **Step 4: Open PR marked `High risk — human approval required`** and run repository security, quality, parity, feature-contract, Nova guard, email-contract, restore, and admin-integration workflows.
- [ ] **Step 5: Fix all genuine failures** without weakening gates. Document intentional fail-closed guard behavior if the critical-boundary guard remains red.
- [ ] **Step 6: Stop at exact-head approval boundary.** After explicit approval, merge, apply migration, configure secrets through protected tooling, deploy the worker, schedule cron, run one bounded invocation, and verify `nova_knowledge_health()` shows progress with lexical fallback intact.