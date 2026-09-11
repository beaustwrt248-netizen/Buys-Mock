# Morley Ecosystem Autopilot State

Last reconciled: 2026-09-11 18:05 AWST
Main SHA: `78adf2dbb81e163827e6461a8601fb9869b3f373`

This file is a non-sensitive continuity ledger for autonomous Morley maintenance. It records verified repository state, active work lanes, blockers and the next safe actions. It must be reconciled against live GitHub/project evidence before each update. Gumtree is intentionally excluded.

## Production / release snapshot

- Morley Buys Android OTA is now published at `2.15.89` / versionCode `133`; `ota/latest.json` points to release `v2.15.89` and records SHA-256 `b4b9058122a0c32f402ca028d846f67e76dd129a43567df0d0811594a79aca22`.
- PR #1352 merged the exact verified 2.15.89 OTA metadata from current main after bot-authored PR #1351 was blocked by GitHub `action_required`; #1351 was closed as superseded rather than bypassing protections.
- PR #1349 remains the Device Lens core pricing-evidence safeguard on main: unresolved photo-quality or cross-photo consistency warnings block live market pricing before research begins.
- Morley Vision reusable staff-review UI remains merged from PR #1342; full live Device Lens wiring of Confirm / Not Damage review decisions is still unfinished.
- Nova OpenRouter completion-parameter/diagnostic compatibility fix remains merged from PR #1343.
- Morley Admin live OTA metadata is verified at `0.1.34` / versionCode `35` with SHA-256 `9e336299e146609dfd889f7c2cc1fa0e85744901d2b939de37b3a3a4c5fe4997` and source SHA `b2d996fc83875843176c95010f7e8d3d98a67012`.

## Active workstreams and impact/risk ranking

Scoring is relative and evidence-led: impact/release/security/data-quality benefit increase priority; effort/dependency/protected-boundary risk reduce autonomous suitability.

| Rank | Lane | Impact | Risk | Current state | Next safe action |
|---|---|---:|---:|---|---|
| 1 | Morley Vision / Device Lens | 10 | 4 | Core pricing evidence gate is live and 2.15.89 release chain is reconciled; reusable staff review panel is not yet wired into the live results/damage flow | Integrate Confirm / Not Damage review state into live Device Lens on a narrow branch with regression, accessibility and degraded-state coverage |
| 2 | Release/integration stabilisation | 10 | 3 | 2.15.89/133 OTA metadata is published and verified on main | Re-check current-main build/security/parity/release health after new Android changes; preserve exact signed artifact/checksum and version monotonicity |
| 3 | Nova multi-model quality/reliability | 9 | 4 | Bounded orchestrator and current completion parameter merged | Add provider/routing evaluation fixtures, latency/fallback diagnostics and non-sensitive cost/quality scorecard |
| 4 | Admin reliability / parity | 9 | 5 | Admin 0.1.34/35 OTA feed verified; Samsung WebView focus repair merged and published | Compare app/web/Admin feature contracts and prioritise read-only parity gaps/regression tests; auth/security changes remain approval-gated |
| 5 | Catalogue/live-sync/data integrity | 9 | 5 | Existing side-effect-free catalogue integrity gate checks categories, carrier-as-brand, model identifiers, storage, 3G-only records and duplicate variants | Extend read-only scorecard/coverage for image completeness, conflicting identifiers and cross-surface divergence without destructive reconciliation |
| 6 | Valuation 3.0 / Test & Buy / inventory lifecycle | 8 | 5 | active programme | Audit contract/test coverage, invalid lifecycle transitions and pricing evidence boundaries before new behavior |
| 7 | Backup/recovery readiness | 8 | 6 | evidence required | Verify freshness/coverage and perform only isolated non-destructive restore-readiness checks where supported |
| 8 | Technical debt / branch hygiene | 5 | 2 | stale bot OTA PR #1351 closed as superseded; historical branches remain | Identify only clearly obsolete merged/superseded branches; never delete ambiguous branches automatically |

## Dependency / protected-boundary map

- Web, Android and Admin depend on shared catalogue and pricing semantics.
- Device Lens live pricing depends on `MorleyVisionPolicy.pricingBlockReason` through `DeviceInspectionClient.livePricing`; warning-state changes affect whether market research is permitted but do not grant pricing authority.
- Nova depends on catalogue/search/market contracts plus external AI provider behavior; model output remains advisory.
- Guardian incident/repair behavior is approval protected.
- Supabase schema/RLS/auth, secrets, privileged roles, production-destructive operations, signing credentials and protected pricing approval policy require explicit human approval.
- OTA release metadata must remain version-monotonic and match the exact signed artifact/checksum identity.
- Shared API/schema/catalogue field changes require consumer mapping and contract tests across affected app/web/Admin/Nova surfaces.

## Critical invariants

1. OTA `versionCode` is monotonic and metadata references the exact intended signed artifact/checksum.
2. Every material `android/app` source change mints a fresh release identity exactly one source version ahead of the published Morley OTA before release promotion.
3. Canonical catalogue identity must not silently collapse distinct Australian/regional hardware variants.
4. Model numbers/specifications are evidence-backed; unknown values remain unverified rather than guessed.
5. Suggested valuation/pricing must not become authoritative when identity, photo quality, cross-photo consistency or market evidence is unresolved.
6. Inventory/Test & Buy lifecycle transitions must remain valid and traceable; impossible state transitions are audit findings, not auto-repair targets.
7. App/web/Admin/Nova catalogue visibility and shared contract fields should not diverge silently.
8. Authentication/authorization/RLS and Guardian approval boundaries must not be weakened by autonomous work.
9. Ambiguous production data is never auto-deleted, merged or destructively rewritten.

## Failure-pattern record

- 2026-09-11 — Bot-authored OTA PR #1351 had all Actions in `action_required`. Proven recovery: create a current-main repository-owner PR containing the exact already-generated release metadata, allow normal checks to run, close the blocked bot PR as superseded, and merge only after all required gates pass. Do not bypass or manually override protections.
- 2026-09-11 — Release identity merge race after PR #1349. The Vision pricing safeguard merged to main while source still reported 2.15.88/132; a later version bump landed on the already-merged feature branch rather than main. Proven recovery: branch from actual post-merge main, advance only release identity to exactly 2.15.89/133, validate, then publish the exact signed artifact metadata.
- 2026-09-11 — Device Lens audit found the reusable staff-review policy blocked quality/consistency evidence in UI state while core `MorleyVisionPolicy.pricingBlockReason` only blocked weak identity. PR #1349 moved photo-quality/cross-photo blocks into the core gate and added regression coverage. Full staff-review UI wiring remains separate follow-up work.
- 2026-09-11 — Morley Vision PR #1342 compile failure. Symptom: Kotlin `VisionDamageReview` redeclaration plus unresolved properties. Root cause: duplicate review model name across policy and UI layers. Proven fix: remove the duplicate UI data class and reuse the canonical policy model with `VisionReviewState`.
- 2026-09-11 — OTA integration audit blocked when Android source version was two releases ahead of published OTA. Proven fix: publish the exact prior green release metadata before advancing another candidate; treat source/OTA distance greater than one as a release-chain fault.
- 2026-09-11 — Nova multi-model timeout/provider compatibility lane. Proven direction: bounded provider/fusion budgets, truthful diagnostics and current provider completion parameter; preserve advisory-only authority.

## Completion / verification policy

A lane is not marked Done merely because code merged. Applicable CI/security/parity/contracts, release/deployment evidence, regression coverage, accessibility/performance/degraded-state implications, rollback/recovery implications and this ledger must be reconciled first.

## Current blockers / approvals

- Full live Device Lens staff-review integration is not Done: the reusable review panel exists on main but current `DeviceLensActivity` results/damage flow does not yet render it or persist staff damage decisions.
- Protected changes (auth/RLS/secrets/destructive production data/privileged roles/workflow security/signing/Guardian approval policy) must be prepared and validated but not autonomously merged/deployed.

## Next autonomous checkpoint

1. Start the separate Device Lens live staff-review integration from current main with no pricing-authority expansion.
2. Add regression coverage proving every detected damage region requires explicit Confirm / Not Damage review before staff completion.
3. Reconcile current-main Android release identity after that behavior change before any subsequent OTA promotion.
4. Continue independent read-only catalogue/parity/Nova evaluation work while Android CI is running.
5. Re-check Admin 0.1.34 focus/recovery health and backup-readiness evidence without weakening auth/security boundaries.
