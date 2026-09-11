# Morley Ecosystem Autopilot State

Last reconciled: 2026-09-12 04:00 AWST
Source of truth for this entry: live GitHub state from `main` and open pull requests.

## Current main

- Main SHA: `99b278de89aaa2c82828962a39b377bea1e9c0e2`
- Latest merged change: `Finalize Admin browser auth and native session handoff (#1417)`
- Main is protected; automation must not push directly to it or bypass repository protections.

## Production-first triage

- Admin authentication/session recovery work from #1417 is on current main.
- Morley Buys OTA 2.15.95 metadata is being republished from current main in PR #1444 after the previous release handoff stalled.
- PR #1444 is mergeable and its currently observed repository security, quality, parity, Admin integration, release-safety, distribution-readiness and feature-contract checks are green. Nova PR Guard is skipped as non-applicable.
- Release publication remains a protected boundary. Do not merge/deploy #1444 autonomously.
- Morley AI assessment engine remains draft in PR #1437 because it contains protected Supabase migration/workflow changes.
- No Gumtree work is in scope.

## Active workstreams and scores

Scoring scale: impact and confidence 1-5 (higher is better); risk, effort and dependency risk 1-5 (higher means more caution/cost). Priority is a qualitative synthesis, not an automatic authorization to cross protected boundaries.

| Workstream | Evidence / identifier | Impact | Risk | Effort | Confidence | Dependency risk | Priority | Current safe action |
| --- | --- | ---: | ---: | ---: | ---: | ---: | --- | --- |
| Morley 2.15.95 OTA metadata recovery | PR #1444 | 5 | 4 | 1 | 5 | 4 | Highest, protected | Keep checks green; require explicit release approval before merge/deploy. |
| Morley AI assessment engine | PR #1437 | 5 | 5 | 4 | 4 | 5 | High, protected | Keep draft; validate core/contracts; no migration/workflow merge without approval. |
| Android catalogue push realtime | PR #1422 | 5 | 3 | 3 | 4 | 4 | High | Reconcile with current main/release sequencing, run contract/build/security checks, avoid duplicate sync work. |
| Unified scanner release identity 2.15.96 | PR #1432 | 4 | 4 | 1 | 4 | 4 | High, release-gated | Rebase/recreate only after release ordering is unambiguous; do not publish ahead of 2.15.95. |
| Admin 0.1.37 OTA metadata | PR #1416 | 4 | 4 | 1 | 4 | 4 | High, release-gated | Reconcile with #1417 and current Admin release identity before any publication action. |
| Durable continuity / triage ledger | this file | 4 | 1 | 1 | 5 | 1 | Immediate safe maintenance | Keep reconciled every run; never treat stale entries as authoritative over live state. |

## Protected blockers / approval requirements

- Release publication, signing/checksum identity, and deployment promotion are approval-gated.
- Auth/authorization/RLS, secrets, privileged roles, destructive production data work, Guardian repair authority, GitHub workflow/repository security and protected pricing policy are approval-gated.
- PR #1437 specifically includes Supabase migration/workflow changes and must remain draft/unmerged until explicitly approved.
- Guardian code-changing repair PRs remain human-approval gated.
- Same-owner actions are not independent approval.

## Lightweight dependency map

- **Morley Buys Android** -> authentication/session, catalogue API/data, Supabase Realtime, valuation/Test & Buy, scanner/NFC, OTA metadata and signed APK identity.
- **Morley website** -> shared catalogue/pricing contracts, auth/session behavior, valuation flows, Nova-facing data contracts and release/static asset integrity.
- **Morley Admin** -> auth/session handoff, privileged role boundaries, support/governance workflows, catalogue/admin contracts and Admin OTA metadata.
- **Nova AI** -> catalogue/data authority, model/provider routing, source verification, support/admin tools, image/camera analysis and protected action boundaries.
- **Guardian** -> runtime diagnostics, Nova diagnostic context, repository repair proposals and human approval boundary for code-changing repair.
- **Supabase** -> schemas, RLS, auth, catalogue state/revisions, realtime publication and storage/data relationships.
- **Release infrastructure** -> Android/Admin version monotonicity, OTA manifests, signed artifact identity, checksums, release assets and CI gates.
- **Backup/recovery** -> production data coverage, retention/integrity evidence and isolated restore readiness.

## Data-integrity invariants

The automation must detect and report before destructive repair when any of these are violated:

- One canonical device identity must not silently collapse legitimate regional/hardware variants.
- Canonical model identifiers must not conflict across records without evidence-backed variant handling.
- Required catalogue fields must not become malformed or silently guessed.
- Catalogue counts must not collapse/spike unexpectedly without an explained import/change.
- App/web/Admin/Nova catalogue views must not silently diverge from the shared authority.
- Inventory lifecycle transitions and stock identifiers must remain internally consistent.
- Pricing/valuation relationships must not become impossible or cross protected approval policy.
- OTA/release version, checksum, artifact URL and release notes must describe the same signed artifact.
- Production records/images must not be auto-deleted, merged or destructively rewritten when ambiguity remains.

## Definition-of-done checkpoint

A work item is not Done until applicable implementation, tests, security/permission boundaries, dependency/contracts, parity, release/deployment evidence, regression coverage, performance/accessibility/degraded-mode considerations, rollback/recovery implications and this ledger are all reconciled.

## Next safe actions

1. Keep #1444 release checks reconciled but do not cross the protected release boundary.
2. Validate/reconcile #1422 against current main and current Supabase/catalogue contracts while release work is gated.
3. Keep #1437 draft while continuing non-protected assessment-core/evaluation work only.
4. Reconcile #1432 and #1416 release ordering against current main before deciding whether either is stale or must be recreated.
5. Expand this ledger with synthetic-production, backup/readiness, data-quality and performance baselines only from measured evidence; never invent telemetry.
