# Nova Knowledge Maintenance Loop Design

## Goal

Turn Nova's deployed hybrid knowledge platform into a continuously maintained system: ingest safe internal knowledge deltas, drain pending semantic embeddings in bounded batches, expose health, and recover safely from transient failures without weakening existing user/admin authorization.

## Current production baseline

- `nova_knowledge_sources`: 4,696 active sources, all currently in the `catalogue` domain.
- `nova_knowledge_chunks`: 4,696 active chunks; all 4,696 currently have `embedding_status = 'pending'`.
- `nova_knowledge_health()` reports zero stale sources and zero ingestion failures.
- `nova-knowledge-ingest` is admin/JWT protected and currently supports only `ingest_internal`.
- `nova-orchestrator` v9 already attempts `gte-small` query embeddings and falls back to lexical retrieval if embeddings are unavailable.
- pgvector 0.8.2 and `pg_net` are installed in production.

## Architecture

Add a dedicated `nova-knowledge-maintenance` Edge Function rather than weakening `nova-knowledge-ingest` authentication. The maintenance function is an internal worker: it accepts a server-held maintenance token, never an end-user/admin impersonation token, and performs only bounded maintenance actions. It uses the existing service-role database client internally and does not expose service-role credentials in responses, logs, database rows, or client code.

A scheduled database job invokes the worker through `pg_net`. The invocation credential is read server-side from Supabase Vault at execution time; the migration never contains a literal secret value. The worker can also be invoked manually by trusted operators through the same internal credential for controlled verification.

## Maintenance cycle

Each scheduled cycle performs two independently bounded stages.

1. **Internal ingestion:** call the existing safe adapter logic for catalogue, Guardian, support, inventory, sales, and valuation deltas. Existing adapter privacy rules remain authoritative: support free text, user/assignee IDs, raw diagnostics, IMEI/serial identifiers, tokens, and secrets are excluded; empty operational datasets produce no fabricated knowledge.
2. **Embedding:** claim at most 20 active chunks whose embedding state is `pending`, plus eligible transient `error` rows whose retry time has elapsed. Generate `gte-small` 384-dimensional embeddings and update only embedding-related columns. Source text, source metadata, revisions, trust, confidence, and protected action rules are not modified.

The stages fail independently. Ingestion failure must not erase or block already-searchable lexical knowledge. Embedding failure must not prevent Nova answers because lexical retrieval remains the fallback.

## Retry and concurrency rules

- Maximum embedding batch: 20 chunks per invocation.
- Maximum ingestion batch per adapter: 50 source rows per invocation.
- Use a database claim RPC with `FOR UPDATE SKIP LOCKED` so overlapping maintenance invocations cannot process the same chunks concurrently.
- A claimed chunk records an attempt timestamp and attempt count.
- Transient embedding failures return the chunk to retryable state with exponential backoff capped at 24 hours.
- Permanent/invalid embedding results remain `error` and are visible in health telemetry; they are never silently marked ready.
- A ready chunk is idempotent and is not re-embedded unless its knowledge revision creates a new pending chunk.

## Authentication and secret handling

The maintenance function must reject requests without the exact internal maintenance token. The token is supplied as an Edge Function secret and separately stored in Vault for cron invocation. No authenticated browser role receives direct table/RPC access. Existing RLS and `revoke ... from anon, authenticated` rules remain unchanged.

The scheduled SQL retrieves only the named Vault secret at runtime and sends it in a dedicated maintenance header. SQL migrations contain the Vault secret name, not the secret value. The worker never accepts arbitrary SQL, table names, URLs, or function names from the request.

## Database changes

Add only maintenance state needed for safe retries and scheduling:

- `embedding_attempt_count integer not null default 0`
- `embedding_last_attempt_at timestamptz`
- `embedding_next_retry_at timestamptz`
- `embedding_last_error_code text`

Add a service-role-only `nova_claim_embedding_chunks(limit)` RPC using `SECURITY INVOKER` and row locking. Extend `nova_knowledge_health()` with retryable/error/attempt information while keeping it content-free.

Enable/schedule the maintenance cron only after the worker and migration are deployed. Rollback unschedules the named cron job; lexical retrieval continues to work with existing data even if the worker is disabled.

## Scheduling

Run the maintenance worker every minute while there is backlog. One invocation processes at most 20 embeddings, so the initial 4,696-chunk backlog requires at least 235 successful batches. Once caught up, the same bounded schedule handles new chunks incrementally. The worker should exit quickly with `processed = 0` when there is no work.

Internal ingestion runs less frequently inside the worker (for example, once per hour per adapter) using persisted ingestion-run timestamps/checkpoints; embedding maintenance can continue every minute independently.

## Observability

Return and record content-free counters only:

- pending / ready / error embedding counts
- embedding coverage
- claimed / processed / failed / deferred counts
- latest maintenance timestamp
- latest successful ingestion per adapter
- retry backlog

Do not log chunk content or embedding vectors.

## Testing

Add contract tests that prove:

- worker has no user/admin impersonation path;
- batch limits cannot exceed 20 embeddings / 50 ingestion rows;
- secret values are not present in migration/source;
- cron SQL reads the named Vault secret rather than embedding credentials;
- claim RPC uses `FOR UPDATE SKIP LOCKED`, is `SECURITY INVOKER`, and remains service-role-only;
- ready chunks are not reclaimed;
- failures preserve lexical-searchable chunk content;
- health output contains counters, not knowledge content;
- existing Nova protected-action and RLS contracts remain unchanged.

Repository security, parity, quality, feature-contract, Nova guard, email-contract, and admin-integration gates must pass on the final head.

## Rollout and rollback

1. Merge guarded PR only after explicit approval of its exact head.
2. Apply additive maintenance migration.
3. Deploy `nova-knowledge-maintenance` with JWT verification enabled in addition to the internal maintenance-token check.
4. Store the maintenance token as an Edge Function secret and Vault secret through protected configuration tooling; never commit it.
5. Schedule the cron job.
6. Run one manual bounded maintenance invocation and verify health counters.
7. Allow cron to drain the backlog while monitoring errors/coverage.

Rollback: unschedule the cron job and disable/rollback the maintenance Edge Function. Existing chunks, FTS search, and orchestrator lexical fallback remain intact; no knowledge content deletion is required.