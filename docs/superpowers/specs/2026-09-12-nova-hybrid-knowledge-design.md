# Nova Hybrid Knowledge Foundation Design

## Goal

Dramatically expand Nova's usable knowledge without weakening its existing approval, security, pricing, deployment, or Guardian boundaries.

Nova currently stores thousands of catalogue-oriented knowledge items and retrieves them with PostgreSQL full-text search. This change establishes a scalable hybrid knowledge layer so catalogue, Guardian, support, release, pricing, business, operational, and researched knowledge can be searched by both exact wording and semantic meaning.

## Architecture

### Preserve the current knowledge item API

`nova_knowledge_items` remains the durable parent record and the existing `nova-knowledge` actions remain compatible. Existing records are not replaced or deleted.

### Add chunk-level retrieval

Add `nova_knowledge_chunks` as a service-role-only child table. Each chunk belongs to one parent knowledge item and stores:

- ordered chunk index;
- normalized chunk text and content hash;
- generated full-text search vector;
- optional semantic embedding;
- embedding model and timestamp;
- source/provenance metadata;
- active state and timestamps.

Existing active knowledge items are backfilled as chunks so the hybrid layer is useful immediately before a full embedding backfill completes.

### Add provenance and freshness

The parent knowledge item gains first-class fields for source URI, source update time, freshness expiry, confidence, and authority weight. These fields supplement existing metadata and allow retrieval to favor high-confidence, authoritative, fresh evidence while still retaining older reference material.

### Semantic embeddings

Use Nova's existing `OPENROUTER_API_KEY` with OpenRouter's OpenAI-compatible embeddings endpoint. Default model: `openai/text-embedding-3-small`, 1536 dimensions. The model is configurable with `NOVA_EMBEDDING_MODEL`.

Embedding failure must not make knowledge unusable. Writes continue with lexical chunks, and hybrid search falls back to lexical ranking when a query embedding is unavailable.

### Hybrid retrieval

Add a service-role-only SQL RPC that combines:

1. PostgreSQL full-text ranking over chunk text.
2. pgvector semantic ranking over chunk embeddings.
3. Reciprocal Rank Fusion (RRF).
4. confidence, source authority, and freshness multipliers.

The RPC must be `SECURITY INVOKER`, inaccessible to `anon` and `authenticated`, and executable only through trusted server-side code.

### Chunk lifecycle

On knowledge create/update/restore, the Edge Function rebuilds chunks deterministically. Chunk content uses paragraph-aware splitting with bounded overlap. Content hashes allow deterministic replacement and future deduplication. Archiving a parent deactivates its chunks; restoring rebuilds/reactivates them.

### Operational ingestion

The foundation exposes an internal/admin ingestion action that can upsert normalized knowledge records with a stable external key. Initial adapters target existing Morley sources that are already available in Supabase, such as Guardian lessons/incidents, support cases, catalogue audit findings, release knowledge, and future pricing/business outcomes.

Ingestion is idempotent: the same external key updates the existing knowledge item when source content changes instead of creating endless duplicates.

## Security and protected boundaries

- Existing admin-only `nova-knowledge` authentication remains unchanged.
- Tables remain RLS-enabled and inaccessible directly to `anon`/`authenticated`.
- No service-role key is exposed to clients.
- Hybrid SQL functions use `SECURITY INVOKER`, not `SECURITY DEFINER`.
- Knowledge remains advisory and cannot override live source-of-truth data, approval policies, auth/RLS, pricing approvals, deployments, or Guardian protected-repair policy.
- Permanent deletion remains disabled through Nova.
- This migration is high-risk under `NOVA_AUTONOMOUS_DEVELOPMENT.md`; preparing the migration and PR is allowed, but applying/merging it requires explicit conversational approval for the ready PR/head.

## Failure handling

- Missing embedding provider configuration: lexical search remains available.
- Embedding request failure: record remains searchable lexically and can be embedded later.
- Invalid or mismatched embedding dimensions: semantic branch is skipped rather than returning fabricated results.
- Stale sources remain identifiable through `fresh_until` and are down-ranked after expiry.
- Ingestion conflicts are resolved by stable external keys and content hashes; protected source records are never mutated by ingestion.

## Verification

Regression coverage must prove that:

- vector support and chunk schema are declared;
- RLS/revokes/service-role grants remain present;
- hybrid retrieval supports lexical fallback and RRF;
- existing knowledge is backfilled without deleting parent items;
- the Edge Function preserves existing actions and permanent-delete protection;
- new create/update/archive/restore paths maintain chunks;
- ingestion uses a stable external key and cannot grant client table access;
- no protected action or approval boundary is weakened.
