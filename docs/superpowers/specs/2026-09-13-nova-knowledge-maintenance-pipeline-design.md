# Nova Knowledge Maintenance Pipeline Design

## Goal

Make Nova's production knowledge platform self-maintaining without weakening user-facing authentication or protected action boundaries. The pipeline continuously ingests safe internal knowledge deltas and drains pending semantic embeddings in bounded, observable batches while preserving lexical search as a permanent fallback.

## Current Production Baseline

- `nova_knowledge_sources`: 4,696 active sources.
- `nova_knowledge_chunks`: 4,696 active chunks.
- All 4,696 chunks currently have `embedding_status = 'pending'`.
- `nova_knowledge_health()` reports zero stale sources, zero ingestion failures, and 0% embedding coverage.
- Existing `nova-knowledge-ingest` is admin-session authenticated and supports `ingest_internal`.
- `nova-orchestrator` v9 is live and consumes `nova_search_knowledge_chunks`, with lexical fallback when embeddings are absent.
- Production embedding states are `pending`, `ready`, `error`, and `skipped`.

## Architecture

Add a dedicated internal maintenance path rather than weakening `nova-knowledge-ingest` authentication.

### 1. `nova-knowledge-maintenance` Edge Function

A new Edge Function dedicated to trusted internal maintenance work. Ordinary browser/app users cannot authenticate to this worker. It validates the existing server-to-server scheduler credential before maintenance work and uses the service role only server-side.

Supported action: `run`.

Per invocation it performs, in order:
1. bounded safe internal ingestion using the same stable identities as the manual ingester;
2. selects a bounded batch of pending or retryable error chunks;
3. generates `gte-small` embeddings in the Edge Runtime;
4. writes embedding, provider/model/dimensions/status timestamps per chunk;
5. records errors without blocking subsequent chunks;
6. returns bounded operational counts only, never source content, vectors, or credentials.

The function is idempotent and safe to invoke repeatedly.

### 2. Scheduling and credential model

Use Supabase Cron + `pg_net` for recurring maintenance calls. The project already has an established scheduler credential stored in Vault as `morley_backup_scheduler_secret` and mirrored to Edge Functions as `MORLEY_BACKUP_SECRET`. The maintenance worker reuses that existing internal scheduler boundary because the connected deployment tooling cannot provision a second Edge Function secret automatically.

The scheduled request uses a distinct `x-maintenance-secret` header while reading the value from Vault only at runtime. No credential value is stored in migrations, repository files, browser clients, or logs. The existing service-role-only `morley_backup_scheduler_secret_matches` RPC remains the fallback verifier; this design adds no new `SECURITY DEFINER` helper.

This shared credential is a deployment constraint, not an expansion of client authority. A future dedicated Nova scheduler credential can replace it when secret-management tooling is available without changing the worker interface.

Initial cadence: every 5 minutes. Each run is deliberately small so maintenance cannot monopolize database or Edge Function resources.

### 3. Embedding batch rules

- Maximum 20 chunks per invocation.
- Eligible: `status = 'active'` and `embedding_status = 'pending'`, followed by retryable `embedding_status = 'error'` rows older than the cooldown.
- Retry error chunks only when the previous attempt is old enough to avoid a hot loop.
- `gte-small` must return 384 dimensions.
- Successful chunk state: `embedding_status='ready'`, `embedding_provider='supabase-ai'`, `embedding_model='gte-small'`, `embedding_dimensions=384`, `embedding_error=null`, `embedded_at=now()`.
- Error chunk state: `embedding_status='error'`, bounded `embedding_error`, no fabricated embedding.
- Existing ready embeddings are never regenerated unless ingestion creates a replacement chunk marked pending.

### 4. Internal ingestion

Maintenance extends Nova beyond catalogue-only knowledge while respecting existing privacy rules:
- catalogue/device evidence: safe structured device fields;
- Guardian incidents/learning: summaries, outcomes, confidence, approval state; no secrets;
- support: structured issue patterns only; exclude user IDs, free-text customer descriptions, raw diagnostics and assignee identifiers;
- inventory/sales/valuation: aggregate-only and only when rows exist; never fabricate metrics.

Scheduled and manual ingestion share the existing `nova_internal_adapter` metadata convention and `internal:<adapter-key>` source identity. Existing catalogue seed coverage is adopted rather than duplicated. Knowledge updates preserve revision snapshots.

The existing admin-only ingester remains available for manual runs and retains its current authentication model. The maintenance worker does not impersonate a user or bypass that endpoint's auth checks.

## Security

- Never expose `SUPABASE_SERVICE_ROLE_KEY` or the scheduler credential to clients.
- The maintenance function is internal-only and rejects absent/invalid `x-maintenance-secret` values before maintenance data reads.
- New SQL uses `SECURITY INVOKER`; no new public `SECURITY DEFINER` bypass is introduced.
- Existing RLS/service-role boundaries on Nova knowledge tables remain unchanged.
- Vault supplies the scheduler value only at runtime; migration text contains only the Vault lookup name.
- Logs must not include secrets, raw source documents, embeddings, or private support content.
- Protected Morley actions remain outside this subsystem: pricing approvals, Guardian repair execution, user/role mutation, releases/deployments, destructive deletes, support sends and GitHub merge/release authority.

## Observability

Extend `nova_knowledge_health()` to expose only aggregated maintenance state:
- active sources/chunks;
- pending/ready/error embeddings and coverage percentage;
- latest maintenance run time/status;
- recent maintenance failures;
- sources by domain;
- latest ingestion timestamp.

Do not expose source content, credentials or private identifiers.

## Failure Behaviour

- Embedding/provider failure: mark only the affected chunk `error`, continue the bounded batch, preserve lexical search.
- Internal ingestion failure: record partial/error counts; embedding work may continue if database health is intact.
- Cron/Vault failure: no client impact; existing orchestrator continues using lexical retrieval.
- Missing/invalid scheduler credential: function returns 401 and performs no maintenance work.
- Environments without the Vault scheduler secret do not create the cron job; the migration remains replayable.

## Rollout

1. Land tests and maintenance function on isolated branch.
2. Land additive migration for maintenance-run state + cron/Vault wiring.
3. Run repository security/quality/parity/Nova gates.
4. Open high-risk PR and stop for explicit approval before merge/deploy because this introduces a service-role/Vault/cron trust boundary.
5. After approval, merge, apply migration, and deploy `nova-knowledge-maintenance` with platform JWT verification disabled because the cron call authenticates with the existing internal scheduler secret; the worker itself fails closed on that credential before maintenance work.
6. Verify production cron registration, maintenance run records, health output, and that embedding coverage begins increasing while lexical fallback remains usable.

## Definition of Done

- A bounded internal maintenance run can ingest safe internal deltas and embed pending chunks without a user session.
- No browser/client receives privileged credentials.
- Repeated runs are idempotent and resumable.
- Lexical fallback remains operational at all times.
- Dedicated maintenance contract passes.
- Standard security/quality/parity checks pass; Nova Guard may intentionally fail closed for this critical trust-boundary change and must not be weakened.
- Production merge/deploy happens only after explicit approval of the exact ready PR/head.