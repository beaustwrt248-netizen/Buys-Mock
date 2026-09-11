# Morley Ecosystem Autopilot State

Last reconciled: 2026-09-11 17:08 AWST
Main SHA: `3dc0bcbe9089fa8080e87d101f74d5f77be1d7b8`

This file is a non-sensitive continuity ledger for autonomous Morley maintenance. It records verified repository state, active work lanes, blockers and the next safe actions. It must be reconciled against live GitHub/project evidence before each update. Gumtree is intentionally excluded.

## Production / release snapshot

- Morley Buys Android OTA: `2.15.88` / versionCode `132`; metadata points to release `v2.15.88` with SHA-256 recorded in `ota/latest.json`.
- Morley Vision staff-review UI and pricing evidence safeguards are merged via PR #1342.
- Nova OpenRouter completion-parameter/diagnostic compatibility fix is merged via PR #1343.
- Morley Admin latest confirmed repository release line: `0.1.33` (PR #1340 / preceding recovery work #1332).
- Current active safety repair: PR #1349, branch `fix/vision-pricing-evidence-gates`, based on main `3dc0bcbe9089fa8080e87d101f74d5f77be1d7b8`; head before ledger update `3a8f57de5eb17376f14b0c2096245f7c9406e144`.

## Active workstreams and impact/risk ranking

Scoring is relative and evidence-led: impact/release/security/data-quality benefit increase priority; effort/dependency/protected-boundary risk reduce autonomous suitability.

| Rank | Lane | Impact | Risk | Current state | Next safe action |
|---|---|---:|---:|---|---|
| 1 | Morley Vision / Device Lens | 10 | 4 | PR #1349 closes a core pricing-evidence bypass: unresolved photo-quality or cross-photo consistency warnings now block live pricing before market research | Validate PR #1349 through Android/security/parity/contract checks; then integrate the reusable staff Confirm / Not Damage review panel into the live Device Lens flow on a separate narrow branch |
| 2 | Release/integration stabilisation | 10 | 3 | 2.15.88 OTA published; recent Nova compatibility repair merged | Continue current-main release/security/parity/contract health checks; repair only evidence-backed non-protected failures |
| 3 | Nova multi-model quality/reliability | 9 | 4 | bounded orchestrator + current completion parameter merged | Add provider/routing evaluation fixtures, explicit latency/fallback diagnostics and non-sensitive cost/quality scorecard |
| 4 | Admin reliability / parity | 9 | 5 | 0.1.33 recovery baseline published | Compare app/web/Admin feature contracts; prioritize read-only parity gaps and regression tests; auth/security changes remain approval-gated |
| 5 | Catalogue/live-sync/data integrity | 9 | 5 | ongoing | Build read-only invariants/scorecard for duplicates, identifiers, image coverage and app/web/Admin/Nova divergence; do not destructively reconcile ambiguous production data |
| 6 | Valuation 3.0 / Test & Buy / inventory lifecycle | 8 | 5 | active programme | Audit contract/test coverage, invalid lifecycle transitions and pricing evidence boundaries before new behavior |
| 7 | Backup/recovery readiness | 8 | 6 | evidence required | Verify freshness/coverage and perform only isolated non-destructive restore-readiness checks where supported |
| 8 | Technical debt / branch hygiene | 5 | 2 | repository contains many historical non-protected branches | Identify only clearly obsolete merged/superseded branches; never delete ambiguous branches automatically |

## Dependency / protected-boundary map

- Web, Android and Admin depend on shared catalogue and pricing semantics.
- Device Lens live pricing depends on `MorleyVisionPolicy.pricingBlockReason` through `DeviceInspectionClient.livePricing`; warning-state changes therefore affect whether market research is permitted, but do not grant pricing authority.
- Nova depends on catalogue/search/market contracts plus external AI provider behavior; model output remains advisory.
- Guardian incident/repair behavior is approval protected.
- Supabase schema/RLS/auth, secrets, privileged roles, production-destructive operations, signing credentials and protected pricing approval policy require explicit human approval.
- OTA release metadata must remain version-monotonic and match exact signed artifact/checksum identity.
- Shared API/schema/catalogue field changes require consumer mapping and contract tests across affected app/web/Admin/Nova surfaces.

## Critical invariants

1. OTA `versionCode` is monotonic and metadata references the exact intended signed artifact/checksum.
2. Canonical catalogue identity must not silently collapse distinct Australian/regional hardware variants.
3. Model numbers/specifications are evidence-backed; unknown values remain unverified rather than guessed.
4. Suggested valuation/pricing must not become authoritative when identity, photo quality, cross-photo consistency or market evidence is unresolved.
5. Inventory/Test & Buy lifecycle transitions must remain valid and traceable; impossible state transitions are audit findings, not auto-repair targets.
6. App/web/Admin/Nova catalogue visibility and shared contract fields should not diverge silently.
7. Authentication/authorization/RLS and Guardian approval boundaries must not be weakened by autonomous work.
8. Ambiguous production data is never auto-deleted, merged or destructively rewritten.

## Failure-pattern record

- 2026-09-11 — Device Lens integration audit found that the reusable staff-review policy blocked quality/consistency evidence in UI state, while core `MorleyVisionPolicy.pricingBlockReason` only blocked weak identity. Because `DeviceInspectionClient.livePricing` consumes the core gate, unresolved image-quality or cross-photo warnings could still reach live market pricing. Repair in PR #1349 moves these fail-safe blocks into the core pricing gate and adds regression coverage. Full staff-review UI wiring remains separate follow-up work.
- 2026-09-11 — Morley Vision PR #1342 compile failure. Symptom: Kotlin `VisionDamageReview` redeclaration plus unresolved properties. Root cause: duplicate review model name across policy and UI layers. Proven fix: remove the duplicate UI data class and reuse the canonical policy model with `VisionReviewState`. Regression lesson: compile the integrated model graph after adding UI-layer state wrappers; avoid duplicate domain type names.
- 2026-09-11 — OTA integration audit blocked when Android source version was two releases ahead of published OTA. Root cause: pending OTA metadata promotion. Proven fix: publish exact prior green release metadata before advancing the next release candidate. Regression lesson: treat OTA/source version distance greater than one as a release-chain fault.
- 2026-09-11 — Nova multi-model timeout/provider compatibility lane. Proven direction: bounded provider/fusion budgets, truthful diagnostics and current provider completion parameter; preserve advisory-only authority.

## Completion / verification policy

A lane is not marked Done merely because code merged. Applicable CI/security/parity/contracts, release/deployment evidence, regression coverage, accessibility/performance/degraded-state implications, rollback/recovery implications and this ledger must be reconciled first.

## Current blockers / approvals

- PR #1349 is low/medium-risk fail-safe tightening and requires normal protected CI/merge conditions; no special auth/RLS/Guardian/security-boundary approval is introduced by this change.
- Full live Device Lens staff-review integration is not yet Done: the reusable review panel exists on main but the current `DeviceLensActivity` results/damage flow does not render it or persist staff damage decisions.
- Protected changes (auth/RLS/secrets/destructive production data/privileged roles/workflow security/signing/Guardian approval policy) must be prepared and validated but not autonomously merged/deployed.

## Next autonomous checkpoint

1. Inspect PR #1349 checks and repair any evidence-backed failures.
2. Confirm the core pricing block returns no suggested resale path for unresolved photo quality/consistency evidence.
3. After #1349 is green/merged, create a separate narrow Device Lens live-review integration branch with regression coverage.
4. While CI runs, continue independent read-only catalogue/parity/evaluation work.
5. Reconcile release/OTA state and update this ledger only with verified evidence.
