# Nova Knowledge Maintenance Pipeline Design

## Goal

Make Nova's production knowledge platform self-maintaining without weakening user-facing authentication or protected action boundaries. The pipeline must continuously ingest safe internal knowledge deltas and drain pending semantic embeddings in bounded, observable batches while preserving lexical search as a permanent fallback.

## Current Production Baseline

- `nova_knowledge_sources`: 4,696 active sources.
- `nova_knowledge_chunks`: 4,696 active chunks.
- All 4,696 chunks currently have `embedding_status = 'pending'`.
- `nova_knowledge_health()` reports zero stale sources, zero ingestion failures, and 0% embedding coverage.
- Existing `nova-knowledge-ingest` is admin-session authenticated and supports only `ingest_internal`.
- `nova-orchestrator` v9 is live and can consume `nova_search_knowledge_chunks`, with lexical fallback when embeddings are absent.

## Architecture

Add a dedicated internal maintenance path rather than weakening `nova-knowledge-ingest` authentication.

### 1. `nova-knowledge-maintenance` Edge Function

A new Edge Function dedicated to trusted internal maintenance work. It must not be callable by ordinary browser/app users. It validates a dedicated maintenance secret from an authorization header before any work and uses the service role only server-side.

Supported action: `run`.

Per invocation it performs, in order:
1. bounded safe internal ingestion via existing database/application contracts where possible;
2. selects a bounded batch of pending or retryable failed chunks;
3. generates `gte-small` embeddings in the Edge Runtime;
4. writes embedding, provider/model/dimensions/status timestamps per chunk;
5. records failures without blocking subsequent chunks;
6. returns bounded operational counts only, never source content or secrets.

The function must be idempotent and safe to invoke repeatedly.

### 2. Scheduling

Use Supabase Cron + `pg_net` for recurring maintenance calls. Store the maintenance credential in Supabase Vault and read it only inside the scheduled SQL invocation. Never hard-code credentials into migrations, repository files, browser clients, or logs.

Initial cadence: every 5 minutes. Each run is deliberately small so maintenance cannot monopolize database or Edge Function resources.

### 3. Embedding Batch Rules

- Maximum 20 chunks per invocation.
- Eligible: `status = 'active'` and `embedding_status IN ('pending','failed')`.
- Retry failed chunks only when the previous failure is old enough to avoid a hot loop.
- `gte-small` is expected to return 384 dimensions.
- Successful chunk state: `embedding_status='ready'`, `embedding_provider='supabase-ai'`, `embedding_model='gte-small'`, `embedding_dimensions=384`, `embedding_error=null`, `embedded_at=now()`.
- Failed chunk state: `embedding_status='failed'`, bounded `embedding_error`, no fabricated embedding.
- Existing ready embeddings are never regenerated unless their content hash/version changes and ingestion marks the replacement chunk pending.

### 4. Internal Ingestion

Maintenance must extend Nova beyond catalogue-only knowledge while respecting existing privacy rules:
- catalogue/device evidence: safe structured device fields;
- Guardian incidents/learning: summaries, outcomes, confidence, approval state; no secrets;
- support: structured issue patterns only; exclude user IDs, free-text customer descriptions, raw diagnostics and assignee identifiers;
- inventory/sales/valuation: aggregate-only and only when rows exist; never fabricate metrics.

The existing admin-only ingester remains available for manual runs and retains its current authentication model. The new maintenance worker may share pure adapter/helper modules but must not impersonate a user or bypass the manual endpoint's auth checks.

## Security

- Never expose `SUPABASE_SERVICE_ROLE_KEY` or the maintenance secret to clients.
- The maintenance function is internal-only and rejects absent/invalid maintenance credentials before doing database work.
- New SQL helpers default to `SECURITY INVOKER`; no public `SECURITY DEFINER` bypasses.
- Existing RLS/service-role boundaries on Nova knowledge tables remain unchanged.
- Vault stores only the maintenance credential; migration text contains only the Vault lookup name.
- Logs must not include secrets, raw source documents, embeddings, or private support content.
- Protected Morley actions remain outside this subsystem: pricing approvals, Guardian repair execution, user/role mutation, releases/deployments, destructive deletes, support sends and GitHub merge/release authority.

## Observability

Extend `nova_knowledge_health()` or add a companion health surface to expose only aggregated maintenance state:
- active sources/chunks;
- pending/ready/failed embeddings and coverage percentage;
- latest maintenance run time/status;
- recent maintenance failures;
- sources by domain;
- latest successful ingestion timestamp.

Do not expose source content, credentials or private identifiers.

## Failure Behaviour

- Embedding/provider failure: mark only the affected chunk failed, continue the bounded batch, preserve lexical search.
- Internal ingestion failure: record run failure/partial status; embedding work may continue only if database health is otherwise intact.
- Cron/Vault failure: no client impact; existing orchestrator continues using lexical retrieval.
- Missing maintenance secret: function returns 401/403 and performs no work.

## Rollout

1. Land tests and maintenance function on isolated branch.
2. Land additive migration for maintenance-run state + cron/Vault wiring.
3. Run repository security/quality/parity/Nova gates.
4. Open high-risk PR and stop for explicit approval before merge/deploy because this introduces a service-role/Vault/cron trust boundary.
5. After approval, merge, apply migration, deploy function with JWT verification enabled where compatible with the internal caller; otherwise rely on the dedicated maintenance secret only if the platform-level JWT requirement would prevent cron invocation and this exact configuration is reviewed explicitly.
6. Verify production health and embedding coverage movement.

## Definition of Done

- A bounded internal maintenance run can ingest safe internal deltas and embed pending chunks without a user session.
- No browser/client receives privileged credentials.
- Repeated runs are idempotent and resumable.
- Lexical fallback remains operational at all times.
- Security/quality/parity/Nova guards pass or intentionally fail closed for the new trust boundary.
- Production merge/deploy happens only after explicit approval of the exact ready PR/head.