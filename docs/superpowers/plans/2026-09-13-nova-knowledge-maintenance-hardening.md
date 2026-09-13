# Nova Knowledge Maintenance Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Harden the newly landed Nova knowledge maintenance pipeline against concurrent embedding races and privacy drift between manual and scheduled ingestion.

**Architecture:** Keep the production scheduler credential, five-minute cron, maintenance-run telemetry, and existing worker contract. Add a service-role-only `SKIP LOCKED` embedding claim RPC with retry metadata, and extract one shared internal-ingestion engine consumed by both the admin ingester and maintenance worker.

**Tech Stack:** Supabase Edge Functions, PostgreSQL 17, pgvector, Supabase AI `gte-small`, Node contract tests, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-09-13-nova-knowledge-maintenance-pipeline-design.md`

## Global Constraints
- Preserve the existing `morley_backup_scheduler_secret` trust path and five-minute schedule.
- Maximum embedding claim remains 20 chunks.
- Existing admin ingestion remains admin/JWT-gated.
- Support free text, user/assignee IDs and raw diagnostics remain excluded.
- No destructive knowledge mutation or lexical fallback removal.
- Merge/deploy remains human-gated on the exact ready PR head.

---

### Task 1: Concurrency-safe embedding claims
- [x] Add failing hardening contract.
- [x] Add additive retry metadata and `nova_claim_embedding_chunks()` using `FOR UPDATE SKIP LOCKED`.
- [x] Keep RPC `SECURITY INVOKER` and service-role-only.
- [x] Extend health counters with attempts/retry state.

### Task 2: Shared internal ingestion engine
- [x] Extract safe catalogue/Guardian/support/operational adapters and persistence into `_shared/nova_internal_ingestion.mjs`.
- [x] Keep support safe-field selection unchanged.
- [x] Refactor admin `nova-knowledge-ingest` to use the shared engine while preserving admin auth.
- [x] Refactor scheduled maintenance to use the same engine.

### Task 3: Maintenance worker hardening
- [x] Replace racing direct pending/error selects with claim RPC.
- [x] Preserve `gte-small` 384-dimension validation.
- [x] Record bounded retry/backoff metadata on failures.
- [x] Continue embeddings if ingestion fails.

### Task 4: Verification and guarded handoff
- [x] Extend maintenance CI path coverage and run all three focused contracts.
- [ ] Reconcile with latest `main` if it moves.
- [ ] Run repository security, quality, parity, feature-contract, Nova guard, restore, email and admin-integration gates.
- [ ] Fix genuine failures without weakening gates.
- [ ] Mark PR ready and stop at exact-head merge/deploy approval boundary.
