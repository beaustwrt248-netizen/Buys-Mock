# Morley Ecosystem Autopilot State

Last reconciled: 2026-09-12 02:02 AWST
Main SHA at reconciliation: `1274393d2aeb5c36d36a9f4caf08cf0dfb587768`

This is the non-sensitive continuity ledger for Morley ecosystem maintenance. Gumtree is intentionally excluded.

## Production / release snapshot

- Morley Buys Android source and OTA are aligned at `2.15.94`, versionCode `138`.
- `ota/latest.json` points to `B-and-L-Morley-2.15.94.apk` with SHA-256 `eee917054620f7ffacad64c7c895eb8c750ac93dfd48535e1e30e99f048d4e34`.
- Morley Admin source/release handoff advanced to `0.1.36`, versionCode `37`; PR #1402 merged the exact signed OTA metadata from source SHA `f3b0ed11320766cd07bb5a75c79dd9852a96f8d5`.
- PR #1376 is merged. The Admin mobile More menu is now mounted inside `#appView`, marked privileged, and backed by fail-closed auth-boundary CSS/JS. The prior stale #1369 containment concern is therefore resolved on current main.
- PR #1374 remains the browser/mobile-web challenge lifecycle repair; PR #1377 remains the native Morley Buys challenge-bootstrap repair.
- Nova production-intelligence work from #1355 remains on main with multi-model routing, bounded retry/circuit-breaking, cost metadata, mode selection, Vision damage regions and operational telemetry contracts.
- Main now also includes guarded automatic low-risk PR review/auto-merge from #1403. Sensitive workflow/security/auth/RLS/Guardian boundaries remain human-gated by Morley policy and must not be treated as independently approved by the repository-owner identity.

## Production-first health triage

### Repository / deployment

- Current main: `1274393d2aeb5c36d36a9f4caf08cf0dfb587768` (`ci: add guarded automatic PR review and auto-merge (#1403)`).
- No open pull requests were present at reconciliation.
- GitHub Pages deployment for current main completed successfully.
- The current-main live desktop navigation check completed successfully.
- Other current-main checks were still running when reconciled; do not infer completion until their exact-head results are available.

### Admin APK / OTA release chain

- The Admin APK build for source SHA `f3b0ed11320766cd07bb5a75c79dd9852a96f8d5` passed unit tests, lint, stable signing verification and checksum verification.
- Its workflow run reported failure in the metadata-publication job even though generated PR #1402 subsequently received owner approval and merged as `113136486349a8ae95f6115f2067c19fc787e3f0`.
- Therefore Admin 0.1.36 metadata is on main; the historical workflow failure is a release-automation reliability signal, not evidence that the signed 0.1.36 artifact or merged metadata failed verification.
- Do not change GitHub workflow security or release permissions autonomously. Any workflow repair remains protected and requires explicit approval before merge/deploy.

### Authentication / UI

- Current Admin source mounts `#adminMoreMenu` inside authenticated `#appView` and tags it `data-admin-privileged-nav=true`.
- `admin/auth-boundary.css` fail-closes `#appView`, the mobile More menu and privileged navigation while unauthenticated.
- The earlier ledger statement that pre-auth body-mounted More navigation still required a fresh candidate is obsolete and must not be used for task selection.

### Catalogue / sync — last verified service evidence

Read-only production audit carried forward from the latest verified service pass:
- `device_catalog`: 1,873 total rows; 1,756 active.
- Active rows missing brand/model/source URL: 0.
- Active rows missing image reference: 6.
- Active rows with no storage options: 59.
- Duplicate active model-number groups: 16; preserve legitimate regional/hardware variants and do not collapse automatically.
- `catalog_sync_state`: revision 258; latest observed event was a `device_catalog` INSERT at `2026-09-11 16:31:46 UTC`.

### Inventory integrity — last verified service evidence

- orphan inventory -> catalogue references: 0.
- negative acquired/expected-sale prices: 0.
- retired timestamps with incompatible lifecycle status: 0.

### Guardian — last verified service evidence

- 14 incidents were not yet resolved/closed/verified: 2 high, 11 medium, 1 low.
- 5 unresolved incidents required approval and had no `approved_at` evidence.
- Guardian repair/decision authority remains human-approval protected; no autonomous repair/approval action is authorised.

### Backup / recovery — last verified service evidence

- Latest recorded user-encrypted Google Drive backup remained `ready`, format v1, AES-256-GCM, 701 bytes, created `2026-09-07 13:47:28 UTC`.
- `restored_at` remained null; restore readiness cannot be claimed green.
- No server-side token substitution, key mutation, credential change, destructive restore or authorization bypass is permitted.

### Nova quality / cost evidence — last verified service evidence

- `nova_ai_runs` exists with provider/mode/model, latency, token, cost, success/degraded and failure metadata fields.
- Latest verified query contained zero production rows, so production latency/cost/fallback drift cannot be inferred.
- Continue static/evaluation coverage and record telemetry conclusions only from real approved evidence.

### Supabase protected drift evidence

- Latest advisor evidence included 12 `RLS enabled, no policy` informational findings, 13 authenticated-user `SECURITY DEFINER` warnings, and leaked-password protection disabled.
- These touch protected auth/RLS/privilege boundaries. No autonomous schema, policy, function privilege or Auth configuration change is authorised.
- Latest performance evidence included 45 unused-index notices and one duplicate Nova index warning; index removal is a protected production-schema change and is not authorised autonomously.

## Impact / risk ranking

| Rank | Lane | Impact | Risk | Current state | Next safe action |
|---|---|---:|---:|---|---|
| 1 | Production/release stabilisation | 10 | 4 | Buys OTA is aligned at 2.15.94; Admin 0.1.36 metadata merged, but its publisher workflow reported failure before the PR completed | Correlate exact failure mode with current workflow behavior; prepare protected workflow repair only if still reproducible |
| 2 | Production login verification | 10 | 5 | Web/native challenge repairs and Admin containment are on main | Verify live read-only/synthetic behavior where supported; patch only demonstrated regressions |
| 3 | Backup/recovery readiness | 10 | 6 | Last verified user-encrypted backup is stale and no restore is recorded | Re-check only through existing authorised user flow; no token/key bypass or destructive restore |
| 4 | Nova multi-model quality | 9 | 5 | Multi-model contracts merged; production telemetry still lacked samples at last verification | Strengthen static/evaluation coverage and guardrails without inventing production metrics |
| 5 | Catalogue/data quality | 9 | 4 | 6 image gaps, 59 storage gaps, 16 model-number collision groups at last verified pass | Manufacturer-first evidence review; never guess or destructively collapse variants |
| 6 | Guardian incident hygiene | 9 | 8 | 14 unresolved at last verified pass; 5 approval-gated | Read-only correlation/triage only; repairs/decisions remain human gated |
| 7 | Supabase security drift | 9 | 8 | Protected advisor findings remain evidence-only | Build consumer/intent evidence map; explicit approval required for RLS/Auth/privilege changes |
| 8 | Valuation/Test & Buy/inventory lifecycle | 8 | 5 | Latest inventory invariants were clean | Continue contract/regression audit and preserve pricing-authority boundaries |
| 9 | Technical debt / capacity | 5 | 3 | Historical branches/check volume remain large; schema/index cleanup is ambiguous | Prefer narrow evidence-backed cleanup; no ambiguous branch/index deletion |

## Dependency / protected-boundary map

- Web, Android and Admin share catalogue, pricing and session semantics.
- Device Lens pricing depends on complete identity/photo/storage evidence and guarded pricing authority; staff verification supplies evidence but does not grant pricing authority.
- Nova depends on catalogue/search/market contracts and approved external AI providers; output remains advisory.
- Supabase schema/RLS/auth, secrets, privileged functions/roles, production-destructive operations, signing credentials, GitHub workflow security, protected pricing policy and Guardian approval policy require explicit human approval.
- User-encrypted Drive backup requires valid user Google authorization; autonomous token substitution/bypass is forbidden.
- OTA metadata must remain version-monotonic and match the exact signed artifact/checksum.

## Critical invariants

1. OTA `versionCode` is monotonic and metadata matches the exact signed artifact/checksum.
2. Material Android source changes must mint the next valid release identity before OTA promotion.
3. Catalogue identity must not silently collapse legitimate Australian/regional variants.
4. Device facts/specifications remain evidence-backed; unknown means unverified, never guessed.
5. Suggested valuation/pricing remains blocked when identity/photo consistency/quality/storage evidence is unresolved.
6. Inventory lifecycle transitions remain valid and traceable.
7. App/web/Admin/Nova contracts must not silently diverge.
8. Auth/RLS/Guardian approval boundaries must not be weakened autonomously.
9. Ambiguous production data is never auto-deleted/merged/destructively rewritten.
10. Backup health is not green without freshness, integrity and restore-readiness evidence.
11. Gumtree remains excluded unless Beau explicitly re-enables it.
12. Nova provider changes must not expose prompts, secrets or sensitive production data.
13. Advisor warnings are evidence, not permission for automatic security/schema changes.
14. Repository-owner actions do not count as independent human approval for protected automation/Guardian decisions.

## Failure-pattern / change-correlation record

- 2026-09-12 — Continuity drift: the ledger lagged 23 commits behind main and incorrectly still described Admin pre-auth More containment as unresolved. #1376 had already landed the fail-closed containment. Corrective action: reconcile ledger against current main before selecting work.
- 2026-09-12 — Release-state drift: the ledger still described Buys 2.15.92 while current source/OTA were aligned at 2.15.94/versionCode 138. Corrective action: verify build.gradle and OTA identity together before release task selection.
- 2026-09-12 — Admin 0.1.36 publisher run: APK build/sign/checksum passed; metadata-publication job reported failure, but generated PR #1402 later received owner approval and merged. Treat as automation reliability evidence, not artifact-integrity failure.
- 2026-09-12 — Web/mobile auth regression history: #1374 deferred browser Turnstile until credentials were ready while preserving the mandatory challenge.
- 2026-09-12 — Native Android auth regression history: #1377 detects `AndroidBridge` and starts native Turnstile immediately while preserving Admin deferred startup and the CAPTCHA boundary.
- 2026-09-12 — Nova operational telemetry had zero rows at last verified service pass; do not invent latency/cost/reliability conclusions.

## Current blockers / approvals

- GitHub workflow/repository-security changes are approval-gated.
- Supabase RLS/Auth/`SECURITY DEFINER` privilege changes are approval-gated.
- Guardian repairs/decisions remain approval-gated.
- User-encrypted Drive backup freshness still needs authorised-flow evidence before recovery readiness can be claimed green.
- Destructive catalogue reconciliation remains approval-gated when evidence is ambiguous.
- Nova telemetry lacks production samples at last verification; this is an evidence limitation, not permission to fabricate conclusions.

## Next autonomous checkpoint

1. Reconcile current-main checks after they settle and confirm production/OTA/deployment status on the exact head.
2. Diagnose whether the Admin metadata publisher failure remains reproducible after #1403; do not alter workflow security autonomously.
3. Audit Nova provider/routing static contracts and evaluation coverage while production telemetry is absent.
4. Verify live login/challenge behavior using non-destructive/synthetic evidence where supported.
5. Continue manufacturer-first catalogue evidence review for the six image gaps, 59 storage gaps and 16 model-number collision groups from the last verified service pass.
6. Correlate unresolved Guardian incidents against recent merges/deployments without applying protected repairs.
7. Re-check authorised user-backup freshness when the approved Google Drive flow produces new evidence.
