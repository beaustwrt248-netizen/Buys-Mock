# Morley Ecosystem Contract-First Hardening Design

Date: 2026-09-12
Status: Approved architecture; implementation pending plan approval

## Objective

Harden Morley Admin, Morley Buys, Nova and Guardian around their existing shared Morley Core contracts without duplicating authoritative data or privileged functionality. The work prioritises regression safety, Admin/web parity, realtime catalogue consistency, Guardian/Nova reliability, and evidence-based performance improvements.

## Architectural principles

1. Morley Core remains the single source of truth for catalogue, pricing, identity/RBAC, media, audit/events, search, integrations, notifications and realtime events.
2. Morley Admin Android remains a privileged shell around the live `/admin/` workspace rather than becoming a second independent Admin implementation.
3. Admin parity is enforced as a contract: privileged web capabilities that are intended for Admin must remain reachable and functional through the Android shell, subject to the same RBAC and Guardian controls.
4. Guardian remains Nova's independent security/governance enforcement layer. Protected actions stay human-gated, auditable and server-authorised.
5. Catalogue synchronisation is shared infrastructure. No Admin-specific authoritative catalogue copy is introduced.
6. Security and performance advisor warnings are investigated according to intended access patterns; findings are not silenced by weakening controls or deleting infrastructure without evidence.

## Workstream 1: Admin parity and regression safety

### Goal
Ensure the Android Admin app stays functionally aligned with the live Admin website while avoiding feature duplication.

### Design
- Treat the live `/admin/` web workspace as the feature surface rendered inside the privileged Android shell.
- Keep native responsibilities narrow: authentication bootstrap, trusted-origin navigation, session injection, external-link isolation, native logout, update/recovery handling and device-specific lifecycle concerns.
- Define an Admin parity contract covering navigation, filters, permissions, catalogue administration, pricing controls, support operations, system health, Nova/Guardian controls, audit views and any other privileged workspace capability exposed by the web Admin.
- Add regression checks that verify the shell always loads the current Admin workspace using its versioned freshness parameter and cannot navigate privileged sessions to untrusted origins.
- Add compatibility checks for native-auth session injection and logout so web changes cannot silently strand Android users in loading loops.
- Avoid native clones of web filters, catalogue editors or permission controls unless a capability genuinely requires native device APIs.

### Failure handling
- If session bootstrap cannot complete, fail closed to native login rather than exposing a partially authenticated Admin workspace.
- If the live Admin route is unavailable, surface a recoverable error state rather than loading cached privileged content indefinitely.
- Preserve the recovery APK/update path as a separate operational escape hatch.

## Workstream 2: Shared catalogue and realtime resilience

### Goal
Make catalogue, price, media and device changes converge quickly and reliably across Morley Buys, Morley Admin and Nova without introducing competing stores.

### Design
- Keep the existing shared realtime catalogue sync as the only authoritative client synchronisation path.
- Add explicit freshness state for catalogue consumers: last successful snapshot, realtime subscription state, last applied event/version and resync status.
- On reconnect, visibility resume, auth refresh or detected event gap, perform a bounded authoritative resync before declaring the client current.
- Make event application idempotent so duplicate realtime deliveries cannot corrupt local presentation state.
- Coalesce bursts of catalogue events and refresh only affected views/data where practical.
- Ensure additions, removals, price changes, image/media changes and relevant metadata changes invalidate the same shared client caches.
- Add consistency tests proving that Admin mutations become observable by Morley Buys/Nova through Morley Core rather than through Admin-specific replication.

### Failure handling
- Realtime disconnection does not cause destructive local assumptions; consumers retain the last known snapshot with a stale/reconnecting state.
- Resync failures use bounded retry/backoff and remain visible to diagnostics.
- No client is allowed to invent conflict resolution for protected catalogue/pricing data; the server-authoritative state wins.

## Workstream 3: Guardian and Nova reliability

### Goal
Improve autonomous diagnostics and repair reliability while preserving approval boundaries and server-side authority.

### Design
- Keep Guardian as the enforcement layer behind Nova rather than exposing a separate competing product surface.
- Preserve human approval for protected repairs/actions and ensure approvals are recorded in the audit trail.
- Strengthen Guardian diagnostic source mapping so browser/runtime errors are attributed to the actual generating source when possible rather than opaque containers such as `about:srcdoc`.
- Ensure stale fallback runtime artefacts cannot override current deployed sources.
- Add regression coverage for previously observed runtime failures, including undefined runtime references and stale srcdoc/fallback behaviour.
- Separate diagnostic collection, repair proposal generation, approval decision and repair execution into auditable stages.
- Treat privileged Guardian RPCs as server-authorised operations: inspect each SECURITY DEFINER function's internal authorisation before changing grants.

### Security constraints
- Do not authorise privileged actions from mutable user metadata.
- Do not grant Nova broader write/repair authority to reduce friction.
- Do not bypass Guardian from Admin.
- Fail closed when an approval record, role check or target provenance cannot be verified.

## Workstream 4: Supabase hardening

### Goal
Resolve real backend security weaknesses while preserving intentional service-only designs.

### Design
- Review every SECURITY DEFINER function currently executable by `authenticated` users and classify it as public-authenticated, privileged-admin, Guardian-internal or service-only.
- For privileged functions, require an explicit trusted server-side role/admin check and restrict EXECUTE grants to the narrowest viable role.
- Review RLS-enabled tables with no policies. If they are intentionally service-only, keep client access closed and verify Data API grants do not expose them. If client access is required, add specific ownership/admin policies rather than blanket authenticated access.
- Enable leaked-password protection for Supabase Auth if the connected project configuration supports it.
- Re-run security advisors after migrations and require no newly introduced high-severity findings.

## Workstream 5: Performance and index hygiene

### Goal
Improve performance using evidence rather than advisor-count optimisation.

### Design
- Confirm the duplicate `nova_ai_runs` indexes are definitionally identical and dependency-safe; remove only the redundant copy through a migration.
- Keep the remaining currently-unused indexes until query/workload evidence shows they are harmful or redundant.
- Instrument or inspect hot paths before changing indexes used by rare Admin, audit, support, Guardian or Nova workflows.
- Measure client-side catalogue refresh behaviour and reduce unnecessary full re-renders/refetches before attempting schema-level optimisation.

## Contract and CI gates

Implementation changes will be accepted only when the following gates pass:

- Admin Android unit tests and lint.
- Admin trusted-origin/navigation and session-bootstrap regression tests.
- Admin web parity/feature-contract checks.
- Catalogue mutation/realtime convergence and reconnect/resync tests.
- Guardian protected-action approval and source-attribution regression tests.
- Permission/RBAC tests for privileged Admin and Guardian operations.
- Supabase security advisor rerun after relevant migrations.
- Existing repository security, parity, quality and feature-contract workflows.
- Signed APK signer/checksum verification for release builds.

No implementation PR is merged while a required check is failing.

## Delivery strategy

Deliver the implementation as small, reviewable changes from current `main`, ordered to reduce risk:

1. Add/strengthen contracts and failing regression tests first.
2. Fix Admin shell/session/freshness issues exposed by those tests.
3. Harden shared catalogue realtime recovery and consistency.
4. Harden Guardian diagnostics and protected-operation boundaries.
5. Apply narrowly scoped Supabase security migrations after authorisation review.
6. Remove only the confirmed duplicate index and perform measured performance tuning.
7. Run the complete gate suite, publish the next signed Admin APK only after all required checks pass, and preserve rollback/recovery paths.

## Non-goals

- Rewriting Morley Core.
- Building a second native implementation of the Admin website.
- Creating Admin-specific authoritative catalogue/pricing stores.
- Removing Guardian approval requirements.
- Mass-removing unused indexes.
- Broadening RLS or RPC permissions merely to make advisor warnings disappear.
- Unrelated UI redesigns or speculative refactors.

## Success criteria

The work is complete when:

- Android Admin exposes the intended live Admin feature set without duplicated business logic and without stale-cache/login-loop regressions.
- Catalogue, price and media changes converge across Admin, Morley Buys and Nova through shared Morley Core sync with tested reconnect/resync behaviour.
- Guardian diagnostics correctly trace supported runtime errors to source and protected repairs remain human-approved and auditable.
- Privileged Supabase RPC/table access matches intended roles and service boundaries, with security findings reduced only through justified fixes.
- Performance changes are supported by evidence and do not remove safety-critical operational indexes.
- All required repository and release gates pass before merge/release.
