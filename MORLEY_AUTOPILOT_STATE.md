# Morley Ecosystem Autopilot State

Last reconciled: 2026-09-11 17:12 AWST
Main SHA: `292317277bec5d607787de4971012adae0b8eca4`

This file is a non-sensitive continuity ledger for autonomous Morley maintenance. It records verified repository state, active work lanes, blockers and next safe actions. It must be reconciled against live GitHub/project evidence before each update. Gumtree is intentionally excluded.

## Production / release snapshot

- Published Morley Buys Android OTA remains `2.15.88` / versionCode `132`; metadata points to release `v2.15.88` with its recorded SHA-256.
- PR #1349 merged the Device Lens core pricing-evidence safeguard into `main`: unresolved photo-quality or cross-photo consistency warnings now block live market pricing before research begins.
- Because #1349 merged before its attempted release-identity follow-up commit landed, `main` still identified as `2.15.88` / `132` immediately after that merge. A clean current-main release-identity repair is therefore active to advance source to `2.15.89` / `133` while published OTA remains `132`.
- Morley Vision reusable staff-review UI remains merged from PR #1342; full live Device Lens wiring of its Confirm / Not Damage decisions remains unfinished.
- Nova OpenRouter completion-parameter/diagnostic compatibility fix remains merged from PR #1343.
- Admin release evidence is being treated conservatively: repository history contains newer Admin release tags/branches than the previously recorded 0.1.33 baseline, but the authoritative live Admin OTA/feed location has not yet been re-verified in this run, so no newer Admin production version is claimed here.

## Active workstreams and impact/risk ranking

Scoring is relative and evidence-led: impact/release/security/data-quality benefit increase priority; effort/dependency/protected-boundary risk reduce autonomous suitability.

| Rank | Lane | Impact | Risk | Current state | Next safe action |
|---|---|---:|---:|---|---|
| 1 | Release/integration stabilisation | 10 | 3 | Clean current-main repair advances Morley Android source from 2.15.88/132 to 2.15.89/133 after #1349 merge race | Run OTA/version, Android, security, quality, parity and artifact checks; merge only when normal protections permit; verify exact signed artifact before OTA promotion |
| 2 | Morley Vision / Device Lens | 10 | 4 | Core pricing bypass closed on main by #1349; reusable staff review panel still not rendered/persisted by live Device Lens results flow | After release identity is stable, wire Confirm / Not Damage review state into Device Lens on a separate narrow branch with regression coverage |
| 3 | Nova multi-model quality/reliability | 9 | 4 | Bounded orchestrator and current completion parameter merged | Add provider/routing evaluation fixtures, latency/fallback diagnostics and non-sensitive cost/quality scorecard |
| 4 | Admin reliability / parity | 9 | 5 | Authoritative live release feed needs re-verification before ledger claims a newest production version | Reconcile read-only Admin OTA/release evidence, then compare app/web/Admin feature contracts |
| 5 | Catalogue/live-sync/data integrity | 9 | 5 | ongoing | Build/read current invariants and scorecard for duplicates, identifiers, image coverage and app/web/Admin/Nova divergence; do not destructively reconcile ambiguous production data |
| 6 | Valuation 3.0 / Test & Buy / inventory lifecycle | 8 | 5 | active programme | Audit contract/test coverage, invalid lifecycle transitions and pricing evidence boundaries before new behavior |
| 7 | Backup/recovery readiness | 8 | 6 | evidence required | Verify freshness/coverage and perform only isolated non-destructive restore-readiness checks where supported |
| 8 | Technical debt / branch hygiene | 5 | 2 | repository contains many historical branches | Identify only clearly obsolete merged/superseded branches; never delete ambiguous branches automatically |

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

- 2026-09-11 — Release identity merge race after PR #1349. The Vision pricing safeguard merged to main while source still reported 2.15.88/132; an attempted version bump landed on the already-merged feature branch instead of main. Correct recovery: create a fresh branch from the actual post-merge main and advance only the release identity to exactly 2.15.89/133, then re-run release gates. Regression lesson: once a PR merges, never assume later head commits belong to that merged state; re-read main and restage release identity from current main.
- 2026-09-11 — Device Lens audit found the reusable staff-review policy blocked quality/consistency evidence in UI state while core `MorleyVisionPolicy.pricingBlockReason` only blocked weak identity. PR #1349 moved photo-quality/cross-photo blocks into the core gate and added regression coverage. Full staff-review UI wiring remains separate follow-up work.
- 2026-09-11 — Morley Vision PR #1342 compile failure. Symptom: Kotlin `VisionDamageReview` redeclaration plus unresolved properties. Root cause: duplicate review model name across policy and UI layers. Proven fix: remove the duplicate UI data class and reuse the canonical policy model with `VisionReviewState`.
- 2026-09-11 — OTA integration audit blocked when Android source version was two releases ahead of published OTA. Proven fix: publish the exact prior green release metadata before advancing another candidate; treat source/OTA distance greater than one as a release-chain fault.
- 2026-09-11 — Nova multi-model timeout/provider compatibility lane. Proven direction: bounded provider/fusion budgets, truthful diagnostics and current provider completion parameter; preserve advisory-only authority.

## Completion / verification policy

A lane is not marked Done merely because code merged. Applicable CI/security/parity/contracts, release/deployment evidence, regression coverage, accessibility/performance/degraded-state implications, rollback/recovery implications and this ledger must be reconciled first.

## Current blockers / approvals

- Morley 2.15.89/133 is a release candidate only until required checks and exact signed-artifact evidence complete; do not claim OTA publication or production rollout before that evidence exists.
- Full live Device Lens staff-review integration is not Done: the reusable review panel exists on main but current `DeviceLensActivity` results/damage flow does not yet render it or persist staff damage decisions.
- Protected changes (auth/RLS/secrets/destructive production data/privileged roles/workflow security/signing/Guardian approval policy) must be prepared and validated but not autonomously merged/deployed.

## Next autonomous checkpoint

1. Validate the clean 2.15.89/133 current-main release-identity repair through all applicable checks.
2. Verify build/signature/artifact identity before any OTA metadata promotion.
3. Reconcile authoritative Admin OTA/release feed location and newest verified live Admin version.
4. Start the separate Device Lens live staff-review integration only after release identity is stable.
5. Continue independent read-only catalogue/parity/evaluation work while CI is running.
