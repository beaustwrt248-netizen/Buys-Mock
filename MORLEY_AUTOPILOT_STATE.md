# Morley Ecosystem Autopilot State

Last reconciled: 2026-09-11 20:00 AWST
Main SHA: `c9be261ca6d130cfafb7d705669edc2fa4cde676`

This is the non-sensitive continuity ledger for Morley ecosystem maintenance. Gumtree is intentionally excluded.

## Production / release snapshot

- Morley Buys Android OTA remains published at `2.15.89` / versionCode `133` with the verified signed artifact/checksum already recorded in `ota/latest.json`.
- PR #1354 is now on main and repairs the Admin Android/WebView login freeze by deferring Turnstile until credential-triggered authentication and preventing pre-auth retry loops.
- Device Lens still has the core pricing-evidence gate from PR #1349 and reusable staff-review UI from PR #1342, but the live `DeviceLensActivity` still bypasses persistence of Confirm / Not Damage decisions.
- Nova production-intelligence PR #1355 is open and green across current CI, but it includes Supabase migration/RLS work and therefore remains explicit-human-approval gated under autopilot policy.

## Live production-first health triage

### Catalogue / sync

Read-only production audit at reconciliation time:
- `device_catalog`: 1,869 total rows; 1,752 active.
- Active rows missing brand/model/source URL: 0 / 0 / 0.
- Active rows missing image reference: 6.
- Active rows with no storage options: 59.
- Duplicate canonical brand+model+model-number groups: 0.
- Duplicate model-number groups: 16; these require evidence review because legitimate regional/hardware variants must not be collapsed automatically.
- `catalog_sync_state`: revision 254; latest recorded change is an UPDATE to `device_catalog` at 2026-09-10 16:10:18 UTC.

### Inventory integrity

Read-only invariants currently pass:
- orphan inventory -> catalogue references: 0.
- negative acquired/expected-sale prices: 0.
- retired timestamps with incompatible lifecycle status: 0.

### Guardian

- 14 incidents are not yet in resolved/closed/verified state: 2 high, 11 medium, 1 low.
- 5 unresolved incidents require approval and do not yet have approval evidence.
- Guardian repair/decision authority remains human-approval protected; no autonomous repair/approval action was taken.

### Backup / recovery

- Recovery health currently contains one open warning: encrypted Google Drive backup is stale.
- The warning has been observed repeatedly and was last observed at 2026-09-11 11:17 UTC.
- `user_drive_backups` contains one recorded backup, created 2026-09-07 13:47 UTC, status `ready`, format v1, AES-256-GCM, 701 bytes, with no recorded restore.
- Backup creation, destructive restore, credential/key changes, or production overwrite remain approval-gated. The next safe recovery action is diagnosis/readiness verification, not destructive repair.

### Supabase security drift

Latest Supabase advisor evidence includes:
- leaked-password protection disabled (warning).
- multiple authenticated-callable `SECURITY DEFINER` functions (warning), including Admin inventory/invite and Guardian control/decision RPCs.
- multiple RLS-enabled tables with no policies (informational), including `nova_ai_runs`; this can be intentional for service-only access but must be reviewed against intended consumers before any change.
- No autonomous RLS/auth/function-privilege changes were made because these are protected security boundaries.

Performance advisor currently reports unused-index candidates only. No index was removed automatically because recent/low-volume workloads can make usage evidence misleading.

## Active workstreams and impact/risk ranking

| Rank | Lane | Impact | Risk | Current state | Next safe action |
|---|---|---:|---:|---|---|
| 1 | Backup/recovery readiness | 10 | 6 | Live stale-backup warning; last recorded backup 2026-09-07; no restore evidence | Read-only diagnosis of backup event/key/health flow and isolate why freshness is not advancing; no destructive restore or key mutation |
| 2 | Morley Vision / Device Lens | 10 | 4 | Review UI/policy exists but live DeviceLensActivity bypasses durable staff damage decisions | Wire Confirm / Not Damage into live flow on a narrow current-main branch with regression/accessibility/degraded-state coverage |
| 3 | Nova production intelligence | 9 | 7 | PR #1355 CI is green; includes schema/RLS migration and deployed-compatible backend changes | Hold merge pending Beau's explicit approval; do not treat repository-owner self-approval as independent approval |
| 4 | Supabase security drift | 9 | 8 | Advisor warnings require intent review | Prepare evidence/consumer map; auth/RLS/SECURITY DEFINER/leaked-password changes require explicit approval |
| 5 | Catalogue/data quality | 9 | 4 | Strong identity/source coverage; 6 image gaps, 59 storage gaps, 16 model-number collision groups | Manufacturer-first evidence review; never guess or destructively collapse regional variants |
| 6 | Guardian incident hygiene | 9 | 8 | 14 unresolved; 5 waiting approval | Read-only triage/correlation only; Guardian code-changing repairs and decisions remain human gated |
| 7 | Release/integration stabilisation | 8 | 3 | Current main includes Admin login repair; Morley OTA remains 2.15.89/133 | Re-run release identity/build/parity checks after next Android behavior change before promotion |
| 8 | Valuation/Test & Buy/inventory lifecycle | 8 | 5 | Core inventory invariants currently clean | Continue contract/regression audit; preserve pricing-authority boundaries |
| 9 | Technical debt / branch hygiene | 5 | 2 | No destructive cleanup required this run | Remove only clearly obsolete/superseded work when evidence is unambiguous |

## Dependency / protected-boundary map

- Web, Android and Admin share catalogue, pricing and session semantics.
- Device Lens pricing depends on `MorleyVisionPolicy.pricingBlockReason` via `DeviceInspectionClient.livePricing`; review state may gate usage but never grants pricing authority.
- Nova depends on catalogue/search/market contracts and external AI providers; output remains advisory.
- Supabase schema/RLS/auth, secrets, privileged functions/roles, production-destructive operations, signing credentials, GitHub workflow security, protected pricing policy and Guardian approval policy require explicit human approval.
- OTA metadata must remain version-monotonic and match the exact signed artifact/checksum.

## Critical invariants

1. OTA `versionCode` is monotonic and metadata matches the exact signed artifact/checksum.
2. Material Android source changes must mint the next valid release identity before OTA promotion.
3. Catalogue identity must not silently collapse legitimate Australian/regional variants.
4. Device facts/specifications remain evidence-backed; unknown means unverified, never guessed.
5. Suggested valuation/pricing must remain blocked when identity/photo consistency/quality/evidence is unresolved.
6. Inventory lifecycle transitions must remain valid and traceable.
7. App/web/Admin/Nova contracts must not silently diverge.
8. Auth/RLS/Guardian approval boundaries must not be weakened autonomously.
9. Ambiguous production data is never auto-deleted/merged/destructively rewritten.
10. Backup health is not considered green without freshness, integrity and restore-readiness evidence.

## Failure-pattern record

- 2026-09-11 — Admin WebView login freeze: Turnstile initialized/retried before credential submission. Fix in PR #1354: defer Turnstile until credential-triggered auth and prevent pre-auth retry loops.
- 2026-09-11 — Bot-authored OTA PR #1351 had Actions in `action_required`. Safe recovery: replace with current-main owner PR for the exact generated metadata, let normal checks run, close blocked bot PR as superseded, never bypass protections.
- 2026-09-11 — Release identity merge race after PR #1349. Safe recovery: branch from actual post-merge main, advance exactly one release identity, validate, then publish matching signed artifact metadata.
- 2026-09-11 — Morley Vision PR #1342 Kotlin redeclaration. Fix: remove duplicate review model and reuse canonical policy model.
- 2026-09-11 — Nova provider timeout/compatibility. Proven direction: bounded provider/fusion budgets, truthful diagnostics, current provider completion parameter and advisory-only authority.

## Current blockers / approvals

- PR #1355 is not eligible for autonomous merge despite green CI because `supabase/migrations/20260911185000_nova_ai_runs.sql` changes a protected schema/RLS boundary. Explicit Beau approval is required.
- Supabase advisor security changes are approval-gated; this ledger records evidence only.
- Backup freshness is currently not healthy; cause is not yet proven and no destructive repair should be attempted.
- Device Lens staff-review integration is still not Done.

## Next autonomous checkpoint

1. Continue read-only backup/recovery diagnosis and verify event/freshness flow without changing keys, credentials or production data.
2. Continue Device Lens live staff-review integration as the highest-value non-protected implementation lane.
3. Hold PR #1355 until explicit approval while preserving its verified green checks.
4. Review the 6 image gaps, 59 storage gaps and 16 model-number collision groups manufacturer-first; do not guess or merge variants.
5. Correlate unresolved Guardian incidents against recent merges/deployments without applying protected repairs.
