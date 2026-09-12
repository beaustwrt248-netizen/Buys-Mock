# Nova Knowledge Platform Expansion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a scalable hybrid lexical + semantic knowledge platform for Nova with evidence, provenance, continuous ingestion and safe fallback.

**Architecture:** Keep Supabase/PostgreSQL as the authoritative store. Add normalized source/chunk/evidence records, deterministic idempotent ingestion, pgvector-backed semantic retrieval behind a provider boundary, and rank fusion with existing full-text search. Preserve existing knowledge and protected approval boundaries.

**Tech Stack:** TypeScript/JavaScript repository services, Supabase PostgreSQL, PostgreSQL full-text search, pgvector, existing repository test/security gates.

**Spec:** `docs/superpowers/specs/2026-09-13-nova-knowledge-platform-expansion-design.md`

## Global Constraints
- Never push directly to `main`.
- Production schema/RLS/function changes are high risk and require Beau's explicit conversational approval for the specific ready PR before merge/deployment.
- Preserve existing `nova_knowledge_items` data.
- Retrieval must continue working when embeddings are unavailable.
- Sensitive identifiers must not be copied into general-purpose knowledge chunks.
- Every material retrieved item must retain provenance and confidence/freshness metadata.

---

### Task 1: Knowledge schema and migration contract

**Files:**
- Create: a timestamped Supabase migration following the repository's existing migration naming/location convention.
- Test: extend the repository's schema/security contract tests covering Nova tables and RLS.

**Interfaces:**
- Produces normalized records for `nova_knowledge_sources`, `nova_knowledge_chunks`, `nova_knowledge_evidence`, and ingestion-run state.
- Produces a vector column/index only after enabling the supported `vector` extension in the migration.

- [ ] Write failing schema-contract tests asserting additive tables, source identity/content hash/version fields, provenance metadata, embedding status and indexes.
- [ ] Run the focused schema/security tests and confirm they fail before implementation.
- [ ] Add an additive migration; do not delete or rewrite existing knowledge rows.
- [ ] Add/verify RLS consistent with existing Nova authorization patterns; no anonymous write path.
- [ ] Run focused schema/security tests and confirm they pass.
- [ ] Commit the schema task independently.

### Task 2: Deterministic ingestion and deduplication

**Files:**
- Create focused Nova knowledge ingestion modules adjacent to the existing Nova knowledge service.
- Test ingestion modules in the repository's established unit-test location.

**Interfaces:**
- `normalizeKnowledgeDocument(input)` -> normalized source document.
- `chunkKnowledgeDocument(document)` -> deterministic chunks.
- `ingestKnowledgeDocument(document)` -> inserted/updated/unchanged counts plus source/version identifiers.

- [ ] Write failing tests proving identical re-ingestion creates no duplicate chunks.
- [ ] Write failing tests proving changed content versions only affected chunks and preserves provenance.
- [ ] Write failing tests proving sensitive identifier fields are excluded from general chunks.
- [ ] Implement normalization, stable hashing, deterministic chunking and upsert/version logic.
- [ ] Run focused tests and confirm all ingestion cases pass.
- [ ] Commit the ingestion task independently.

### Task 3: Internal Morley knowledge adapters

**Files:**
- Create adapters for catalogue, Guardian learning/incidents, support, and repository-approved documentation/release metadata.
- Test each adapter with representative fixtures.

**Interfaces:**
- Each adapter emits the normalized document contract from Task 2 with domain, source type, authority/trust, timestamps and provenance.

- [ ] Write failing adapter tests for catalogue, Guardian, support and project/release knowledge.
- [ ] Ensure absent inventory/sales/valuation data emits no fabricated documents.
- [ ] Implement adapters using authoritative fields only.
- [ ] Add domain/source counters to ingestion results.
- [ ] Run adapter and ingestion tests.
- [ ] Commit the adapter task independently.

### Task 4: Hybrid retrieval and rank fusion

**Files:**
- Create/modify the Nova knowledge retrieval service and its tests.

**Interfaces:**
- `searchNovaKnowledge(query, options)` returns ranked chunks with provenance, domain, lexical score, semantic score when available, confidence and freshness.

- [ ] Write failing tests for lexical-only fallback.
- [ ] Write failing tests for semantic + lexical rank fusion and domain/status/trust filters.
- [ ] Write failing tests ensuring stale/low-authority evidence cannot silently outrank stronger evidence solely through semantic similarity.
- [ ] Implement lexical candidate retrieval, vector candidate retrieval and deterministic rank fusion.
- [ ] Implement graceful vector failure/absence fallback without failing the Nova answer path.
- [ ] Run focused retrieval tests.
- [ ] Commit the retrieval task independently.

### Task 5: Embedding provider boundary and backfill controls

**Files:**
- Create an embedding provider abstraction, worker/backfill service and tests using a deterministic fake provider.

**Interfaces:**
- `embedKnowledgeChunks(chunkIds, provider)` records embedding model/version/status without coupling ingestion to a single vendor.
- Backfill accepts bounded batches and is resumable/idempotent.

- [ ] Write failing tests for successful embedding, provider failure, retry state and resumable batches.
- [ ] Implement provider boundary and fake provider for tests.
- [ ] Implement bounded backfill with status/error bookkeeping.
- [ ] Ensure ingestion remains successful when embedding generation fails.
- [ ] Run focused tests.
- [ ] Commit the embedding task independently.

### Task 6: Explainability, health metrics and stale-knowledge reporting

**Files:**
- Extend Nova health/diagnostic surfaces or add focused knowledge-health service modules following existing patterns.
- Add tests.

**Interfaces:**
- Health reports indexed chunks by domain/source, embedding coverage/failures, stale counts, ingestion timestamps and retrieval latency without exposing secrets/sensitive identifiers.

- [ ] Write failing tests for health metrics and provenance returned with search results.
- [ ] Implement counters/health queries and Explain-mode metadata.
- [ ] Ensure metrics distinguish indexed knowledge from live authoritative business records.
- [ ] Run focused tests.
- [ ] Commit the observability task independently.

### Task 7: Regression, security and rollout verification

**Files:**
- Modify only existing quality/security contract files needed to register the new tests; do not weaken gates.
- Update Nova platform documentation with rollout/backfill operator notes.

**Interfaces:**
- Produces validation evidence and rollback instructions for the PR.

- [ ] Run all focused Nova knowledge tests.
- [ ] Run repository security, RLS/schema, feature-contract, parity and quality gates applicable to the changed areas.
- [ ] Verify no existing `nova_knowledge_items` mutation/destructive migration is present.
- [ ] Verify vector-disabled fallback tests pass.
- [ ] Document migration/backfill ordering and rollback behavior.
- [ ] Commit validation/docs independently.

### Task 8: High-risk pull request handoff

**Files:** none beyond PR metadata.

**Interfaces:**
- Produces a reviewable PR that remains unmerged pending explicit conversational approval.

- [ ] Rebase/update the branch from current `main` if necessary and rerun affected checks.
- [ ] Open a PR describing the problem, architecture, files changed, risk class, tests, migration/backfill steps, known limitations and rollback.
- [ ] Mark the PR **High risk — human approval required** because it contains production database/schema changes.
- [ ] Do not merge or deploy the migration until Beau explicitly approves that specific ready PR/head in conversation.