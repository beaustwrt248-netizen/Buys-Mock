# Morley Ecosystem Autopilot State

Last reconciled: 2026-09-11 21:00 AWST
Main SHA: `09a490a474a0986e88509354e0198e410306296a`

This is the non-sensitive continuity ledger for Morley ecosystem maintenance. Gumtree is intentionally excluded.

## Production / release snapshot

- Morley Buys Android OTA remains published at `2.15.89` / versionCode `133` with the verified signed artifact/checksum already recorded in `ota/latest.json`.
- PR #1354 is now on main and repairs the Admin Android/WebView login freeze by deferring Turnstile until credential-triggered authentication and preventing pre-auth retry loops.
- Device Lens still has the core pricing-evidence gate from PR #1349 and reusable staff-review UI from PR #1342, but the live `DeviceLensActivity` still bypasses persistence of Confirm / Not Damage decisions.
- Nova production-intelligence PR #1355 remains open. Its experimental market-search/pricing-confidence changes were reverted to current main, so it no longer expands Gumtree functionality. It still includes protected `nova_ai_runs` schema/RLS work and therefore remains approval-gated.

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

The backup warning has now been isolated rather than treated as a generic scheduler outage:
- Global `morley-google-drive-backup-daily` cron is active on `0 19 * * *` and its latest observed run (2026-09-10 19:00 UTC) succeeded.
- `morley-recovery-health-hourly` is active on minute 17 and continues to succeed while correctly reporting the separate stale user-encrypted backup finding.
- The open finding is `stale_backup`: encrypted Google Drive backup is stale; threshold 36 hours; last user backup 2026-09-07 13:47 UTC; first warning 2026-09-09 02:17 UTC.
- `user_drive_backups` contains one ready backup, format v1, AES-256-GCM, 701 bytes. Exactly one `backup_created` event, one wrapped backup key and one master key are recorded; no restore is recorded.
- The active `user-google-drive-backup` Edge Function requires both the Morley user JWT and a live `X-Google-Access-Token`, verifies the Google identity, encrypts locally, uploads to Drive appDataFolder and verifies the uploaded envelope before marking ready. This design means the per-user backup cannot be refreshed by the server cron without an approved user Google authorization/token flow.
- Therefore the current stale warning is not evidence that the global cron is broken. The next safe action is to restore/verify user backup freshness through the existing authorised app flow; no key mutation, credential change, destructive restore or bypass is permitted.

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
| 1 | Morley Vision / Device Lens | 10 | 4 | Review UI/policy exists but live DeviceLensActivity bypasses durable staff damage decisions | Wire Confirm / Not Damage into live flow on a narrow current-main branch with regression/accessibility/degraded-state coverage |
| 2 | Backup/recovery readiness | 10 | 6 | Global backup cron is healthy; separate user-encrypted backup is stale because freshness depends on user-authorised Google Drive flow | Verify/refresh via existing authorised app flow; no server-side token bypass, destructive restore or key mutation |
| 3 | Nova production intelligence | 9 | 8 | PR #1355 no longer expands Gumtree; protected schema/RLS telemetry remains | Hold merge pending explicit approval for `nova_ai_runs` schema/RLS; continue validating non-protected Nova routing/voice/Vision changes |
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
- User encrypted Drive backup requires a valid user Google authorization; autonomous server-side token substitution/bypass is forbidden.
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
11. Gumtree work remains excluded unless Beau explicitly re-enables it; existing references are not permission to expand Gumtree functionality.

## Failure-pattern record

- 2026-09-11 — Nova scope reconciliation: PR #1355 mixed unrelated Nova improvements with experimental market-search/pricing-confidence changes that expanded Gumtree processing. Safe correction: restore `market-search-v2` and its Brave contract to current main, remove the market-analysis regression assertions, and keep the protected schema/RLS blocker explicit.
- 2026-09-11 — Admin WebView login freeze: Turnstile initialized/retried before credential submission. Fix in PR #1354: defer Turnstile until credential-triggered auth and prevent pre-auth retry loops.
- 2026-09-11 — Backup triage: global daily backup cron is active/succeeding, while the user-encrypted Drive backup is independently stale. The per-user flow requires a live Google access token and must not be misdiagnosed as a cron outage or bypassed server-side.
- 2026-09-11 — Bot-authored OTA PR #1351 had Actions in `action_required`. Safe recovery: replace with current-main owner PR for the exact generated metadata, let normal checks run, close blocked bot PR as superseded, never bypass protections.
- 2026-09-11 — Release identity merge race after PR #1349. Safe recovery: branch from actual post-merge main, advance exactly one release identity, validate, then publish matching signed artifact metadata.
- 2026-09-11 — Morley Vision PR #1342 Kotlin redeclaration. Fix: remove duplicate review model and reuse canonical policy model.
- 2026-09-11 — Nova provider timeout/compatibility. Proven direction: bounded provider/fusion budgets, truthful diagnostics, current provider completion parameter and advisory-only authority.

## Current blockers / approvals

- PR #1355 is not eligible for autonomous merge because `supabase/migrations/20260911185000_nova_ai_runs.sql` changes a protected schema/RLS boundary. Explicit Beau approval is required.
- The Gumtree expansion previously present in #1355 has been removed; `market-search-v2` and its contract are restored to current main and the PR no longer expands Gumtree functionality.
- Supabase advisor security changes are approval-gated; this ledger records evidence only.
- Per-user encrypted Drive backup freshness is not green. Global backup scheduler is healthy; refreshing the user backup requires the existing authorised Google Drive flow.
- Device Lens staff-review integration is still not Done.

## Next autonomous checkpoint

1. Continue Device Lens live staff-review integration as the highest-value non-protected implementation lane.
2. Hold PR #1355 pending explicit protected schema/RLS approval while validating the remaining non-Gumtree Nova changes.
3. Re-check user-encrypted backup freshness and recovery evidence; never substitute credentials or bypass Google authorization.
4. Review the 6 image gaps, 59 storage gaps and 16 model-number collision groups manufacturer-first; do not guess or merge variants.
5. Correlate unresolved Guardian incidents against recent merges/deployments without applying protected repairs.
