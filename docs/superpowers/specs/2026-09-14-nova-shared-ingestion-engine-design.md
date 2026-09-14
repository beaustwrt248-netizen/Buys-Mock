# Nova Shared Ingestion Engine Design

## Purpose

Issue #1878 tracks duplicated Nova internal knowledge persistence between `nova-knowledge-ingest` and `nova-knowledge-maintenance`. Both functions independently implement legacy catalogue adoption, deterministic adapter identity, document normalization, item lookup, refresh/update/create, revision snapshots, source upserts and chunk lifecycle.

The refactor removes only that duplication. Authorization, source fetching, scheduling, privacy, run tracking, embeddings and caller attribution stay endpoint-specific.

## Decision

Create `supabase/functions/nova-knowledge/internal_ingestion.mjs` and export:

```js
export function createInternalIngestionEngine({ admin }) {
  return { persistDocument };
}
```

The engine receives the existing privileged backend client through dependency injection. It does not construct credentials or inspect requests.

`persistDocument(entry, { actorId = null } = {})` accepts:

```js
{
  adapter: string,
  sourceIdentity: string,
  document: object
}
```

and returns `created`, `updated`, `skipped`, or `adopted`.

## Shared responsibilities

The shared engine owns:

- `sha256Hex(adapter + ':' + sourceIdentity)` adapter identity;
- `managed_by: "nova_internal_adapter"` metadata;
- legacy live-catalogue coverage adoption;
- `normalizeKnowledgeDocument`;
- managed-item lookup;
- unchanged-content metadata/source/chunk refresh;
- revision snapshot creation before content-changing updates;
- create/update audit fields using explicit `actorId`;
- source key `internal:${adapterKey}`;
- source metadata sanitization;
- active-chunk supersession and pending-embedding chunk rebuild.

## Manual ingestion boundary

`nova-knowledge-ingest/index.ts` keeps:

- CORS and method handling;
- backend configuration validation;
- bearer-token user authentication;
- enabled Admin-role enforcement;
- `ingest_internal` action validation;
- adapter selection;
- limit/offset pagination;
- source-table queries;
- ingestion run tracking;
- per-document error accounting and response shape.

It calls:

```js
ingestion.persistDocument(entry, { actorId: authorized.user.id })
```

so `created_by`, `updated_by`, and revision `changed_by` remain attributed to the authenticated Admin.

## Maintenance boundary

`nova-knowledge-maintenance/index.ts` keeps:

- scheduler authorization using `MORLEY_BACKUP_SECRET` plus the existing RPC fallback;
- constant-time comparison and bounded auth reason logging;
- `MAX_EMBEDDING_BATCH = 4`;
- `MAX_INGEST_BATCH = 4`;
- maintenance run tracking;
- maintenance-specific source selection;
- optional permission handling for `inventory_items`, `sales_records`, and `valuation_quotes`;
- diagnostic redaction;
- embedding claim/generation/retry/error handling;
- maintenance response shape.

It calls:

```js
ingestion.persistDocument(entry, { actorId: null })
```

so scheduled ingestion keeps null user attribution.

## Privacy and permission invariants

The refactor must not broaden data ingestion.

`adaptSupportTicketRow` remains the support transformation boundary and continues to emit only structured pattern fields: category, status, priority, app version/version code, device model, Android version, and resolved state. Metadata remains `privacy_mode: "structured_pattern_only"`. Free-form support ticket text is not added.

Maintenance optional-source permission failures remain local to the maintenance fetch layer. Permission code `42501` or permission-denied responses for optional inventory, sales, and valuation-quote sources produce an empty source set rather than new privileges or a failed maintenance run.

No RLS, grants, credential configuration, scheduler secret, Auth policy, Guardian approval boundary, production schema, scheduler cadence, or production data is changed.

## Compatibility invariants

Both callers keep one identity space:

- `managed_by = "nova_internal_adapter"`;
- source key `internal:${adapterKey}`;
- identical adapter/source identities map to the same knowledge item;
- legacy catalogue coverage is adopted rather than duplicated;
- unchanged content refreshes freshness metadata without revision increment;
- changed content snapshots the prior revision, increments revision, and rebuilds chunks.

## Error handling

The shared engine throws normalization/storage failures to the caller and does not choose HTTP status codes or log formatting.

Manual ingestion keeps its current per-document catch/count behavior.

Maintenance keeps `normalizeMaintenanceError`, diagnostic redaction, and its fail-safe public error response.

## TDD and verification

A contract test must first fail while persistence remains duplicated. It must require the shared module, both imports, explicit caller attribution, absence of duplicated persistence helpers in the entrypoints, and preservation of identity/revision/chunk behavior.

The contract also protects endpoint boundaries: Admin auth and pagination remain manual-only; scheduler auth, 4x4 limits, optional-source handling and diagnostic redaction remain maintenance-only; support privacy remains structured-pattern-only.

After RED is observed in CI, extract the minimal shared engine, convert both callers, and require focused Nova contracts plus repository security, quality and parity checks on the exact PR head.

## Non-goals

This change does not alter queried source tables, scheduler cadence, embedding model/provider/dimensions, support exposure, Admin authentication, scheduler authorization, database migrations, Guardian authority, backend privileges, or production data.

## Success criteria

Both Edge Functions use one shared persistence implementation; all protected-boundary contracts pass; the exact PR head is green; and no protected production boundary is weakened.