# Nova post-merge hardening — 2026-09-07

## Live service corrections

Two browser integration gaps were found immediately after the Nova intelligence foundation merge and corrected in production without changing pricing rules, source ranking, authentication, or protected authority boundaries.

### `market-search-v2`

- Production version advanced from 24 to 25.
- Replaced the single GitHub Pages CORS origin with an explicit allow-list for:
  - `https://buyshub.me`
  - `https://www.buyshub.me`
  - `https://beaustwrt248-netizen.github.io`
- Existing JWT validation, eBay/Brave/SerpAPI behaviour, trusted-seller policy, Reddit/editorial exclusion and pricing semantics were preserved.

### `app-pricing-catalogue`

- Production version advanced from 5 to 6.
- Added explicit CORS and `OPTIONS` preflight handling for the same three approved web origins.
- Existing authenticated-user requirement and authoritative base-buy-price selection were preserved.
- No price, approval, condition-grade, catalogue or role rule was changed.

## Guardian analyst hardening

The Nova client Guardian analyst is being hardened on `nova/post-merge-hardening` to:

- treat `resolved`, `ignored`, `cancelled` and `closed` as terminal rather than surfacing ignored incidents as current;
- treat every other state as current, so newly introduced non-terminal Guardian states are not silently omitted;
- paginate the full Guardian incident history rather than cap the live read at 100 rows;
- include the existing `proposed_action` as an advisory safest-path evidence field while preserving all Guardian approval/execution boundaries.

## Nova mode copy

Nova now describes its operating mode as `GUARDED`, not `READ`, because the production `nova-actions` service intentionally allows only narrowly scoped, auditable catalogue review queue/finding writes. Protected pricing approvals/writes, Guardian decisions/repairs, deployments/releases/OTA, role/user changes, destructive deletes and automatic catalogue apply remain prohibited or human-gated.

## Database security verification

The Supabase security-advisor `RLS enabled, no policy` findings were checked rather than silenced. The affected Nova audit/knowledge/learning tables, pricing tables and invite rate-limit table have RLS enabled and no direct `anon` or `authenticated` SELECT/INSERT/UPDATE privileges. This is intentional deny-by-default behaviour; no permissive RLS policy was added.

The flagged SECURITY DEFINER invite, inventory and Guardian control functions were also inspected. They contain internal authenticated role checks. `guardian_report_diagnostic` intentionally accepts authenticated users for sanitized telemetry ingestion. No privilege revocation was applied because the inspected functions already enforce their intended authority boundaries.

## Explicit non-changes

This hardening pass does not alter:

- protected pricing approval or pricing-write authority;
- Guardian approval, repair execution, kill-switch or control authority;
- release/deployment/OTA/signing authority;
- user or role mutation authority;
- destructive production data actions;
- automatic catalogue patch application;
- leaked-password protection account setting.
