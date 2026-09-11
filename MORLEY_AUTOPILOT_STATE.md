# Morley Ecosystem Autopilot State

Last reconciled: 2026-09-12 00:57 AWST
Main SHA: `f8957196579832615cd33d5f4f93008c15dac525`

This is the non-sensitive continuity ledger for Morley ecosystem maintenance. Gumtree is intentionally excluded.

## Production / release snapshot

- Morley Buys Android release `2.15.92` is published from merge commit `b89e77a033f46b53c2f6429f0f0bde10c3bf04ee` with asset `B-and-L-Morley-2.15.92.apk` and SHA-256 `df5ee0c4e7068a0c5ed37a831e2a3582e46696d018a975d6910a1bb3c6cd36f2`.
- PR #1370 is merged and closes the Device Lens storage-verification dead-end: staff can confirm an existing storage option or enter a physically verified value, with validated units and explicit no-guess guidance. The change remains session-local and does not weaken the existing fail-closed pricing evidence gate.
- PR #1374 is merged on current `main` and repairs the Morley Buys mobile security-challenge lifecycle. Turnstile is deferred until required credentials are ready, refreshes when credential state changes, and a valid challenge token remains mandatory before sign-in.
- Nova production-intelligence PR #1355 is merged. Multi-model routing, bounded retry/circuit-breaking, cost-budget metadata, native Nova mode selection, speech input, Vision damage-region output and operational telemetry are therefore no longer pending work. Gumtree expansion was removed before merge.
- PR #1369 remains open and is the only open PR found at this reconciliation. It addresses pre-auth Admin navigation containment plus Android Admin WebView cache freshness. Because this work changes an authentication/security boundary, it remains explicit-approval gated even though its observed required checks on head `f2bbd86412093955fff99c512de69c4ffeba594f` are green apart from an older superseded UI checklist run that was followed by a successful run.

## Production-first health triage

### Authentication / UI

- Current main includes PR #1374 mobile challenge-flow repair.
- Open Admin PR #1369 reports a confirmed pre-auth privileged-navigation containment defect and stale WebView-cache divergence. Do not autonomously merge because auth/security boundary changes are protected.
- No protected auth/RLS/role/secret bypass is authorised by this ledger.

### Catalogue / sync

Last read-only production audit carried forward from the 2026-09-11 reconciliation; not re-measured in this GitHub-only pass:
- `device_catalog`: 1,869 total rows; 1,752 active.
- Active rows missing brand/model/source URL: 0 / 0 / 0.
- Active rows missing image reference: 6.
- Active rows with no storage options: 59.
- Duplicate canonical brand+model+model-number groups: 0.
- Duplicate model-number groups: 16; evidence review is required because legitimate regional/hardware variants must not be collapsed automatically.
- `catalog_sync_state`: revision 254; latest then-observed change was an UPDATE to `device_catalog` at 2026-09-10 16:10:18 UTC.

### Inventory integrity

Last verified read-only invariants from the 2026-09-11 reconciliation; re-check before any production-data action:
- orphan inventory -> catalogue references: 0.
- negative acquired/expected-sale prices: 0.
- retired timestamps with incompatible lifecycle status: 0.

### Guardian

Last verified from the 2026-09-11 reconciliation:
- 14 incidents were not yet in resolved/closed/verified state: 2 high, 11 medium, 1 low.
- 5 unresolved incidents required approval and did not yet have approval evidence.
- Guardian repair/decision authority remains human-approval protected; no autonomous repair/approval action is permitted.

### Backup / recovery

Last verified from the 2026-09-11 reconciliation:
- Global `morley-google-drive-backup-daily` cron was active on `0 19 * * *` and its latest observed run succeeded.
- `morley-recovery-health-hourly` was succeeding while correctly reporting a separate stale user-encrypted backup finding.
- The per-user backup flow requires both the Morley user JWT and a live Google access token; no server-side token substitution, key mutation or authorization bypass is permitted.
- Backup freshness must be re-verified through the authorised app flow before claiming recovery health is green.

### Supabase security drift

Last advisor findings carried forward pending fresh verification:
- leaked-password protection disabled warning.
- authenticated-callable `SECURITY DEFINER` function warnings.
- RLS-enabled tables with no policies, including `nova_ai_runs`, requiring intent/consumer review rather than automatic modification.
- No autonomous RLS/auth/function-privilege changes are permitted.

## Active workstreams and impact/risk ranking

| Rank | Lane | Impact | Risk | Current state | Next safe action |
|---|---|---:|---:|---|---|
| 1 | Admin auth / Android parity | 10 | 8 | PR #1369 open; pre-auth navigation containment and WebView freshness repair validated on its head | Keep approval-gated; rebase/reconcile from current main and request explicit approval before merge/deploy |
| 2 | Production login verification | 10 | 5 | PR #1374 merged on current main | Verify live browser/mobile challenge behavior and no new login freeze/black-block regression; collect evidence only unless another defect is demonstrated |
| 3 | Backup/recovery readiness | 10 | 6 | Global backup was healthy; user-encrypted backup was stale at last verified audit | Re-check freshness through existing authorised app flow; no token/key bypass or destructive restore |
| 4 | Nova multi-model quality | 9 | 5 | PR #1355 merged; routing/voice/Vision/telemetry now part of main | Run evaluation drift/cost/reliability checks and compare provider fallback/latency evidence; keep Nova advisory |
| 5 | Catalogue/data quality | 9 | 4 | Last audit found 6 image gaps, 59 storage gaps and 16 model-number collision groups | Manufacturer-first evidence review; never guess or destructively collapse regional variants |
| 6 | Guardian incident hygiene | 9 | 8 | Last audit found 14 unresolved incidents; 5 approval-gated | Read-only correlation/triage only; Guardian code-changing repairs and decisions remain human gated |
| 7 | Release/integration stabilisation | 8 | 3 | Android 2.15.92 published; main has moved beyond its release commit via PR #1374 | Verify OTA/update metadata remains monotonic and that post-release web/auth changes do not imply a new Android release unless native/source identity actually changed |
| 8 | Valuation/Test & Buy/inventory lifecycle | 8 | 5 | Core invariants previously clean; Device Lens staff storage verification is now live in 2.15.92 | Continue contract/regression audit and preserve pricing-authority boundaries |
| 9 | Technical debt / branch hygiene | 5 | 2 | One open PR found; no destructive cleanup justified | Remove only clearly obsolete/superseded work with unambiguous evidence |

## Dependency / protected-boundary map

- Web, Android and Admin share catalogue, pricing and session semantics.
- Device Lens pricing depends on complete identity/photo/storage evidence and existing guarded pricing authority; staff verification supplies evidence but does not grant pricing authority.
- Nova depends on catalogue/search/market contracts and external AI providers; output remains advisory.
- Supabase schema/RLS/auth, secrets, privileged functions/roles, production-destructive operations, signing credentials, GitHub workflow security, protected pricing policy and Guardian approval policy require explicit human approval.
- User encrypted Drive backup requires valid user Google authorization; autonomous server-side token substitution/bypass is forbidden.
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
10. Backup health is not considered green without freshness, integrity and restore-readiness evidence.
11. Gumtree work remains excluded unless Beau explicitly re-enables it.
12. Nova provider changes must not expose prompts, secrets or sensitive production data and must remain within approved provider/security boundaries.

## Failure-pattern / change-correlation record

- 2026-09-12 — Continuity drift: the ledger still described Android 2.15.89, Device Lens integration as unfinished and Nova #1355 as open after those states had changed. Corrective action: reconcile the ledger against live GitHub main/PR/release state before choosing new work.
- 2026-09-12 — Morley Buys mobile auth regression: large inert challenge iframe before credentials. Repair merged in #1374: defer challenge start until required fields are populated, refresh on credential changes, preserve mandatory Turnstile token.
- 2026-09-11 — Device Lens storage dead-end: incomplete Vision storage evidence required staff confirmation but exposed no confirmation mechanism. Repair merged/released in #1370 / 2.15.92 with validated staff-confirmed storage and explicit no-guess behavior.
- 2026-09-11 — Nova scope reconciliation: #1355 removed experimental Gumtree-expanding market-search/pricing-confidence changes before merge while retaining Nova multi-model/voice/Vision/telemetry work.
- 2026-09-11 — Admin WebView/login reliability: earlier Turnstile initialization before credential submission caused retry/freeze behavior; later work continues to protect auth containment and delivery freshness.
- 2026-09-11 — Backup triage: global daily backup scheduler was healthy while the user-encrypted Drive backup was independently stale; do not misdiagnose that as a cron outage or bypass user authorization.
- 2026-09-11 — Bot-authored OTA PR had Actions in `action_required`; proven safe recovery is a fresh current-main owner PR for exact generated metadata with normal protections, never a bypass.
- 2026-09-11 — Release identity merge race: proven safe recovery is to branch from actual post-merge main, advance exactly one release identity, validate, then publish matching signed-artifact metadata.

## Current blockers / approvals

- PR #1369 is approval-gated because it changes an authentication/security boundary. Do not autonomously approve, merge or deploy it.
- Supabase advisor security changes remain approval-gated.
- Guardian repairs/decisions remain approval-gated.
- Per-user encrypted Drive backup freshness still needs fresh authorised-flow evidence before recovery readiness can be claimed green.
- Any destructive catalogue reconciliation remains approval-gated when evidence is ambiguous.

## Next autonomous checkpoint

1. Reconcile/rebase PR #1369 against current main without merging; verify all auth/Admin tests remain green and prepare concise approval evidence.
2. Verify current production/mobile login behavior after #1374 using non-destructive/synthetic evidence where supported.
3. Re-check user-encrypted backup freshness/recovery evidence through the authorised flow.
4. Run Nova post-merge quality/cost/fallback drift checks after #1355.
5. Review catalogue image/storage/model-number gaps manufacturer-first, preserving legitimate variants and never guessing.
6. Correlate unresolved Guardian incidents against recent merges/deployments without applying protected repairs.
