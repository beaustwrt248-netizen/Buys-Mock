# Nova Hybrid Knowledge Foundation Implementation Plan

## Scope

Deliver the first production-ready foundation for dramatically larger Nova knowledge: chunked storage, pgvector embeddings, hybrid semantic/keyword retrieval, provenance/freshness ranking, idempotent ingestion hooks, and regression coverage. Do not apply the production migration or merge this high-risk PR without Beau's explicit approval of the ready PR/head.

## Tasks

1. **Regression contract first**
   - Add `scripts/test_nova_hybrid_knowledge_contract.py` before production changes.
   - Assert required migration, RLS/revokes, hybrid/RRF behavior, chunk lifecycle hooks, embedding endpoint/model configuration, idempotent ingestion, and permanent-delete protection.
   - Verify the contract fails against the pre-feature branch state for the expected missing-foundation reason.

2. **Database foundation**
   - Add a new Supabase migration after the existing Nova knowledge migration.
   - Enable `vector` without version pinning.
   - Add parent provenance/freshness/confidence fields.
   - Add `nova_knowledge_chunks` with FTS + vector columns, indexes, RLS, revokes, and service-role grants.
   - Backfill existing active knowledge as lexical chunks.
   - Add a `SECURITY INVOKER` service-role-only hybrid RPC using lexical rank + semantic rank + RRF and confidence/authority/freshness weighting.

3. **Edge Function hybrid retrieval**
   - Preserve current `nova-knowledge` actions and admin-only authorization.
   - Add OpenRouter embedding generation using `OPENROUTER_API_KEY` and configurable `NOVA_EMBEDDING_MODEL` (default `openai/text-embedding-3-small`).
   - Add deterministic paragraph-aware chunking.
   - Maintain chunks on create/update/archive/restore.
   - Add `hybrid-search`, falling back cleanly to lexical search if embeddings are unavailable.

4. **Idempotent ingestion foundation**
   - Support a stable `external_key` in knowledge metadata/source contract.
   - Add an admin-only `ingest` action that creates or revises an existing item keyed by `(category, source_type, external_key)` without duplicating unchanged content.
   - Preserve revision snapshots when an existing ingested item changes.

5. **Validation**
   - Re-run the new regression contract and relevant existing security/privacy/feature contracts where executable.
   - Inspect the migration for RLS, client revokes, `SECURITY INVOKER`, non-destructive backfill, and no direct production mutation.
   - Open a high-risk PR with rollback notes and explicitly leave merge/apply pending.

## Follow-on adapters after foundation

Once the foundation is approved/applied, separate bounded follow-on PRs can continuously ingest Guardian lessons/incidents, support resolutions, catalogue audit evidence, release history, project decisions, pricing research, and eventually inventory/sales/valuation outcomes. Those adapters use the stable ingestion contract rather than adding new retrieval architectures.
