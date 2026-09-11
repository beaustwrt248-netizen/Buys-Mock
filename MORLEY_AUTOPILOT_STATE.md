# Morley Ecosystem Autopilot State

Last reconciled: 2026-09-12 01:27 AWST
Main SHA at reconciliation: `fb71285772483a2fa155e3ffe7a11e534393248d`

This is the non-sensitive continuity ledger for Morley ecosystem maintenance. Gumtree is intentionally excluded.

## Production / release snapshot

- Published Morley Buys Android OTA remains `2.15.92` / versionCode `136`. The published GitHub asset and `ota/latest.json` still match SHA-256 `df5ee0c4e7068a0c5ed37a831e2a3582e46696d018a975d6910a1bb3c6cd36f2`.
- Native authentication behavior moved beyond the 2.15.92 source after the Turnstile bootstrap repairs. PR #1387 is now merged as `fb71285772483a2fa155e3ffe7a11e534393248d`, advancing Android source identity to `2.15.93` / versionCode `137` without changing OTA metadata or claiming a release.
- Exact-head #1387 checks passed before merge: Repository Security Audit, Full Feature Contract Audit, B&L Morley Quality Gate, Morley Ultimate Parity Gate, Morley OTA Version Policy, Android PR Read-Only Validation, Build B&L Morley APK and associated integration/safety contracts.
- The post-merge main build for 2.15.93 is running. Official release/OTA promotion remains pending exact signed-artifact/signature/checksum evidence and the protected release-approval boundary.
- PR #1374 repaired browser/mobile challenge lifecycle. PR #1377 and the later default-on Turnstile bootstrap repair removed the native AndroidBridge timing dependency while keeping Admin deferred on `about:blank` and keeping CAPTCHA mandatory.
- The mobile-first full-screen stock-image login carousel workstream is already merged via PR #1297. It rotates cached catalogue imagery with broken-image fallback, reduced-motion behavior, responsive safe-area handling, accessible form semantics and a verified packaged video fallback.
- Nova production intelligence from #1355 is on main. Multi-model routing, bounded retry/circuit breaking, cost metadata, native model mode selection, speech input and Vision damage regions are present. Gumtree-expanding experimental work was removed before merge.
- Protected Admin pre-auth navigation-containment repair PR #1376 remains open and validated. It keeps privileged mobile More navigation inside authenticated `#appView` and fails closed pre-auth. It must not be merged/deployed without Beau's explicit approval.

## Live production-first health triage

### Authentication / UI

- Current native/web Turnstile source has multiple regression fixes merged after the supplied Samsung failures, including default-on native challenge bootstrap and removal of duplicate completed-security messaging.
- Published Android OTA is still 2.15.92, so the latest native release candidate is not considered deployed until 2.15.93 is signed, published and OTA metadata is verified.
- Admin Android already uses `LOAD_NO_CACHE`, clears legacy cache and loads a versioned fresh Admin URL. The remaining protected Admin issue is pre-auth privileged-navigation containment in #1376.

### Catalogue / sync — freshly verified

- `device_catalog`: 1,873 total / 1,756 active.
- Active rows missing brand/model/source URL: 0.
- Active rows with blank/null `image_reference_url`: 0, improved from 6 earlier in this run. This means reference coverage is populated; it does **not** prove every reference is a directly renderable image asset. The login carousel preloader must continue skipping non-image/broken references safely.
- Active rows with no storage options: 58, improved from 59 earlier in this run.
- Duplicate active model-number groups: 16 at the last verified collision audit; preserve legitimate regional/hardware variants and never collapse automatically.
- `catalog_sync_state` advanced to revision 260. Latest observed event was an UPDATE at `2026-09-11 17:11:49 UTC`.
- The remaining Motorola `razr 70 plus` / XT2659-3 storage gap remains deliberately unfilled because accessible first-party material did not provide dependable capacity evidence. Unknown remains preferable to guessing.

### Inventory integrity — freshly verified

- Orphan inventory -> catalogue references: 0.
- Negative acquired/expected-sale prices: 0.
- Retired timestamps with incompatible lifecycle status: 0.

### Guardian — freshly verified

- 14 incidents remain outside resolved/closed/verified states: 2 high, 11 medium, 1 low.
- 5 unresolved incidents require approval and have no approval timestamp.
- Most recent high incident evidence includes repeated HTTP 500s from `user-security-center` (16 occurrences, approved previously but failed) and `user-sync-state` (14 occurrences, proposed and unapproved), last seen around `2026-09-11 15:52 UTC`.
- Read-only diagnosis confirmed both deployed Edge Functions are ACTIVE and their required backing tables/RPC currently exist. No Guardian repair/decision was applied; code-changing Guardian repairs remain human gated.

### Backup / recovery — freshly verified

- `morley-google-drive-backup-daily` is active on `0 19 * * *`; the latest observed daily run (2026-09-10 19:00 UTC) succeeded, as did the prior two runs.
- `morley-recovery-health-hourly` is active and recent hourly runs are succeeding.
- Latest user-encrypted Drive backup remains `ready`, AES-256-GCM, format v1, 701 bytes, created `2026-09-07 13:47:28 UTC`; `restored_at` is null.
- Global scheduler health and stale per-user backup freshness remain separate conditions. Refresh requires the existing authorised user Google flow; no token substitution, key mutation or destructive restore is permitted.

### Nova quality / cost evidence

- `nova_ai_runs` currently contains zero production rows, so no production cost/latency/fallback trend can be claimed yet.
- Static source confirms an admin-only orchestrator with Auto/GPT/Gemini/Claude/Consensus routing, bounded retry/circuit breakers and a default `NOVA_MAX_REQUEST_COST_USD` of $0.25.
- Current main added stronger measured-cost guardrail regression coverage. Continue tests/evaluations until approved real traffic produces telemetry; do not invent benchmark results.

### Supabase security / performance drift — evidence only

- Security advisors: 12 RLS-enabled/no-policy INFO findings, 13 authenticated-callable `SECURITY DEFINER` WARN findings, and leaked-password protection disabled warning.
- Performance advisors: 45 unused-index notices and one duplicate-index warning on `nova_ai_runs`.
- No auth/RLS/function privilege/index changes were made. These are protected production boundaries and need intent/consumer evidence plus explicit approval where applicable.

## Active workstreams and impact/risk ranking

| Rank | Lane | Impact | Risk | Current state | Next safe action |
|---|---|---:|---:|---|---|
| 1 | Morley Android release stabilisation | 10 | 4 | 2.15.93/137 identity merged; post-merge signed build evidence pending; OTA still 2.15.92 | Verify main build signature/artifact/checksum; prepare release approval evidence; do not publish OTA autonomously |
| 2 | Admin auth containment | 10 | 8 | PR #1376 validated and open | Hold for Beau explicit approval; re-check current-main compatibility before any approved merge/deploy |
| 3 | Production login verification | 10 | 5 | Latest native/web challenge fixes are on main; published APK remains older | Verify signed 2.15.93 candidate and physical-device behavior after publication; patch only demonstrated regressions |
| 4 | Backup/recovery readiness | 10 | 6 | Global jobs healthy; per-user backup stale; no restore evidence | Refresh only through authorised user flow; isolated restore-readiness evidence when safely available |
| 5 | Nova multi-model quality | 9 | 5 | Routing/cost guardrails merged; production telemetry empty | Expand evidence-backed evaluations/contract checks; compare real telemetry only when samples exist |
| 6 | Catalogue/data quality | 9 | 4 | 0 blank image-reference fields, 58 storage gaps, 16 model-number collision groups | Manufacturer-first verification; separately audit directly renderable image coverage; never guess |
| 7 | Guardian incident hygiene | 9 | 8 | 14 unresolved; two recent high HTTP-500 patterns; 5 unapproved | Read-only correlation/diagnosis; any repair/decision stays approval-gated |
| 8 | Supabase security drift | 9 | 8 | Advisor warnings remain | Build consumer/intent map; no protected RLS/Auth/privilege changes without approval |
| 9 | Valuation/Test & Buy/inventory lifecycle | 8 | 5 | Core invariants clean; Device Lens storage verification released in 2.15.92 | Continue regression/contract audit; preserve protected pricing authority |
| 10 | Technical debt / capacity | 5 | 3 | Unused/duplicate index evidence and large historical branch set | Prefer narrow evidence-backed cleanup; no ambiguous branch/schema/data deletion |

## Dependency / protected-boundary map

- Web, Android and Admin share catalogue, pricing and session/auth semantics.
- Device Lens pricing requires complete identity/photo/storage evidence and guarded pricing authority; staff verification adds evidence only.
- Nova depends on catalogue/search/model-provider contracts and remains advisory.
- Supabase schema/RLS/auth, privileged functions/roles, secrets, production-destructive operations, protected pricing policy, signing/release credentials, GitHub workflow security and Guardian approval policy require explicit human approval.
- User Drive backup freshness depends on valid user Google authorization.
- OTA metadata must remain version-monotonic and match the exact signed release artifact/checksum.

## Critical invariants

1. OTA `versionCode` is monotonic and metadata matches the exact signed artifact/checksum.
2. Material Android source changes mint the next release identity before promotion.
3. A version bump on main is not itself a release; signed artifact and OTA publication require separate evidence.
4. Catalogue identity never silently collapses legitimate regional/hardware variants.
5. Device facts/specifications remain evidence-backed; unknown means unverified, never guessed.
6. Pricing/valuation remains blocked when required evidence is unresolved.
7. Inventory lifecycle transitions remain valid and traceable.
8. App/web/Admin/Nova contracts must not silently diverge.
9. Auth/RLS/Guardian/security approval boundaries must not be weakened autonomously.
10. Ambiguous production data is never auto-deleted/merged/destructively rewritten.
11. Backup health is not green without freshness, integrity and restore-readiness evidence.
12. Gumtree remains excluded unless Beau explicitly re-enables it.

## Failure-pattern / change-correlation record

- 2026-09-12 — Release identity race: repeated 2.15.93 candidates became stale while auth/Nova work advanced main. Proven safe response: close superseded candidates, validate the same narrow bump from a later main, and merge only after exact-head checks. PR #1387 is the successful identity merge.
- 2026-09-12 — Native Turnstile bootstrap: initial AndroidBridge-dependent repair still had a timing risk. Current main uses default-on bootstrap for real challenge navigation while preserving deferred Admin about:blank behavior.
- 2026-09-12 — Duplicate challenge success messaging was removed with regression coverage.
- 2026-09-12 — Catalogue scorecard improved during the run: blank/null image-reference fields 6 -> 0 and active missing-storage rows 59 -> 58; reference population is not equivalent to verified direct-image loadability.
- 2026-09-12 — Guardian correlation: recent high 500 incidents point to `user-security-center` and `user-sync-state`; deployed source/backing DB objects exist, so root cause is not simply a missing current table/function. Continue evidence collection rather than applying an old generic repair.
- 2026-09-12 — Nova telemetry remains empty; do not invent cost/latency trends. Static measured-cost guardrail coverage was strengthened on main.
- 2026-09-11 — Device Lens storage dead-end fixed/released in #1370 / 2.15.92 with staff-confirmed storage and explicit no-guess behavior.
- 2026-09-11 — Global backup scheduler health and stale user-authorised Drive backup are distinct; never bypass Google authorization to refresh it.

## Current blockers / approvals

- **PR #1376 requires Beau's explicit approval** before merge/deployment because it changes the privileged Admin pre-authentication security boundary.
- Official Morley Buys 2.15.93 release/OTA publication remains unapproved until the main signed build produces exact artifact/signature/checksum evidence; release/signing promotion is protected.
- Supabase RLS/Auth/`SECURITY DEFINER` privilege changes are approval-gated.
- Guardian repairs/decisions remain approval-gated.
- Per-user encrypted Drive backup freshness still needs the authorised Google flow.
- Destructive/ambiguous catalogue reconciliation remains approval-gated.

## Next autonomous checkpoint

1. Verify the post-merge 2.15.93 main build through regression, lint, release build, signer verification, packaged login-video identity and uploaded artifact/checksum; prepare, but do not autonomously publish, official OTA/release.
2. Keep #1376 unmerged pending explicit approval and re-check it against current main before any approved action.
3. Continue read-only Guardian diagnosis for the two recent high 500 patterns and correlate against function deployments/schema/config evidence.
4. Audit renderable catalogue image coverage separately from populated reference URLs; continue manufacturer-first storage/model verification.
5. Continue Nova evaluation and cost/fallback regression coverage while production telemetry remains empty.
6. Re-check user Drive backup freshness/recovery evidence through the authorised flow.
