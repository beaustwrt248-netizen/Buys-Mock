# Nova Shared Ingestion Engine Design

## Purpose

Issue #1878 tracks duplicated Nova internal knowledge persistence between `nova-knowledge-ingest` and `nova-knowledge-maintenance`. Both functions independently implement the same persistence lifecycle: legacy catalogue adoption, adapter identity hashing, document normalization, item lookup, refresh/update/create, revision snapshots, knowledge-source upserts, chunk supersession/rebuild and created/updated/skipped outcomes.

The goal is to remove that duplicated persistence implementation without merging the two functions' different authorization, source-fetching, scheduling, privacy or attribution policies.

## Decision

Extract only the shared persistence engine into `supabase/functions/nova-knowledge/internal_ingestion.mjs`.

Do not unify request handlers, source fetching, run tracking, scheduler authorization, Admin authorization, embedding work, pagination, optional-source policy or maintenance diagnostics.

This is intentionally narrower than a general ingestion framework.

## Shared engine responsibilities

The shared module owns:

- deterministic `adapter_key` generation using `sha256Hex(adapter + ':' + sourceIdentity)`;
- `managed_by: "nova_internal_adapter"` metadata identity;
- legacy catalogue adoption checks for active generated catalogue records;
- normalization through `normalizeKnowledgeDocument`;
- lookup of existing managed internal knowledge items;
- metadata-only refresh when content hash is unchanged;
- revision snapshot creation before content-changing updates;
- item update/create operations;
- source upsert using `internal:${adapterKey}`;
- chunk trust/confidence/source refresh for unchanged content;
- superseding prior active chunks for changed content;
- rebuilding active pending-embedding chunks from `chunkKnowledgeDocument`;
- returning one of `created`, `updated`, `skipped` or `adopted`.

The engine accepts the Supabase service-role client as a dependency rather than creating its own client. It also accepts explicit actor attribution so the two callers preserve their current audit semantics.

Proposed public interface:

```js
export function createInternalIngestionEngine({ admin }) {
  return {
    persistDocument(entry, { actorId = null } = {})
  };
}
```

`entry` remains the existing shape:

```js
{
  adapter: string,
  sourceIdentity: string,
  document: object
}
```

## Manual ingestion boundary

`supabase/functions/nova-knowledge-ingest/index.ts` remains responsible for:

- CORS and HTTP method handling;
- service-role configuration validation;
- bearer-token user authentication;
- enabled-profile Admin-role enforcement;
- `ingest_internal` action validation;
- adapter selection;
- `limit` and `offset` pagination;
- ingestion run creation/completion;
- source-table queries used by the manual workflow;
- per-document error accounting;
- caller-visible response shape.

For each document it calls the shared engine with:

```js
persistDocument(entry, { actorId: authorized.user.id })
```

This preserves `created_by`, `updated_by` and revision `changed_by` attribution to the authenticated Admin user.

## Maintenance boundary

`supabase/functions/nova-knowledge-maintenance/index.ts` remains responsible for:

- scheduler-secret authorization using `MORLEY_BACKUP_SECRET` and the scheduler-secret RPC fallback;
- constant-time credential comparison;
- bounded authorization reason logging without credential material;
- `MAX_EMBEDDING_BATCH = 4`;
- `MAX_INGEST_BATCH = 4`;
- maintenance run tracking;
- maintenance-specific safe source selection;
- optional permission handling for `inventory_items`, `sales_records` and `valuation_quotes`;
- structured diagnostic redaction;
- embedding claim, generation, retry and error-state handling;
- maintenance response shape.

For each document it calls the shared engine with a null actor:

```js
persistDocument(entry, { actorId: null })
```

This preserves scheduler-created knowledge with null user attribution.

## Privacy and permission invariants

The refactor must not broaden data ingestion.

The existing adapters remain the sole transformation boundary. In particular, `adaptSupportTicketRow` continues to emit only structured support-pattern fields: category, status, priority, app version/version code, device model, Android version and resolved state. It must continue to mark metadata with `privacy_mode: "structured_pattern_only"`. Free-form support ticket text is not introduced.

Maintenance optional-source behavior from PR #1867 remains local to the maintenance fetch layer. Permission errors (`42501` or permission-denied responses) for optional inventory, sales and valuation-quote sources return an empty source set rather than changing grants or aborting the maintenance run.

No RLS, grants, service-role privileges, scheduler secrets, Auth policy, Guardian approval boundary or production schema are changed by this refactor.

## Identity and compatibility invariants

Both callers must continue sharing one internal identity space:

- `managed_by = "nova_internal_adapter"`;
- source key `internal:${adapterKey}`;
- identical adapter/source identities map to the same knowledge item;
- existing legacy live-catalogue coverage is adopted instead of duplicated;
- unchanged content refreshes metadata/source/chunk freshness without incrementing revision;
- changed content snapshots the previous revision before incrementing revision and replacing chunks.

The shared engine must preserve all currently selected normalized fields and metadata sanitization behavior.

## Error handling

The shared engine throws storage/normalization errors to its caller. It does not decide endpoint response codes or log formatting.

Manual ingestion keeps its current per-document catch-and-count behavior and current top-level error response.

Maintenance keeps `normalizeMaintenanceError`, secret-bearing diagnostic redaction and fail-safe public error responses in `index.ts`.

## Testing strategy

TDD is required.

First, strengthen repository contracts so they fail while duplicated persistence remains. The tests must require:

- the shared module to exist and export `createInternalIngestionEngine`;
- both Edge Functions to import and construct the shared engine;
- duplicate persistence helpers to be absent from both entrypoint files;
- manual ingestion to pass the authenticated Admin user ID as actor attribution;
- maintenance to pass null actor attribution;
- shared engine to preserve `nova_internal_adapter`, `internal:${adapterKey}`, legacy catalogue adoption, revisions, source/chunk lifecycle and all four outcomes;
- maintenance's scheduler auth, 4×4 limits, optional-source handling and diagnostic redaction to remain in the maintenance entrypoint;
- support adapter privacy mode to remain `structured_pattern_only`;
- manual Admin-only auth and adapter/pagination behavior to remain in the manual entrypoint.

After the contract fails for the intended reason, extract the minimal shared implementation, update both callers, and rerun focused plus repository-wide CI gates.

## Non-goals

This work does not:

- change which internal tables are queried;
- increase maintenance batch sizes above 4×4;
- change scheduler cadence;
- alter embedding model/provider/dimensions;
- change support-ticket data exposure;
- change Admin authentication;
- change scheduler authorization;
- add production database migrations;
- alter Guardian repair authority;
- broaden service-role grants;
- deploy or mutate production data directly.

## Success criteria

The refactor is complete when both Edge Functions use one shared persistence implementation, all preserved-boundary contracts pass, focused Nova knowledge tests pass, repository security/quality/parity gates pass on the exact PR head, and no protected production boundary is weakened.