# Morley Ecosystem Autopilot State

Last reconciled: 2026-09-12 AWST
Main SHA at live-health baseline: `ab7d762cbb158ecfad0d28011e3e42fb4165a60a`

This is the non-sensitive continuity ledger for Morley ecosystem maintenance. Gumtree is intentionally excluded.

## Production / release snapshot

- Morley Buys Android release `2.15.92` is published from merge commit `b89e77a033f46b53c2f6429f0f0bde10c3bf04ee` with asset `B-and-L-Morley-2.15.92.apk` and SHA-256 `df5ee0c4e7068a0c5ed37a831e2a3582e46696d018a975d6910a1bb3c6cd36f2`.
- `ota/latest.json` advertises the same `2.15.92` / versionCode `136`, APK URL and SHA-256. No OTA identity/checksum contradiction was found.
- PR #1370 is merged and closes the Device Lens storage-verification dead-end with validated staff-confirmed storage and explicit no-guess guidance while preserving the fail-closed pricing evidence gate.
- PR #1374 is merged and repairs the Morley Buys mobile security-challenge lifecycle. Turnstile is deferred until required credentials are ready, refreshes when credentials change, and remains mandatory before sign-in.
- Nova production-intelligence PR #1355 is merged. Multi-model routing, bounded retry/circuit-breaking, cost metadata, native mode selection, speech input, Vision damage regions and operational telemetry are on main; Gumtree expansion was removed before merge.
- PR #1375 reconciled this ledger and is merged as `ab7d762cbb158ecfad0d28011e3e42fb4165a60a` after Repository Security Audit, B&L Morley Quality Gate, Morley Email Contract and Morley Ultimate Parity Gate passed.
- PR #1369 remains open but stale/diverged. Its Android WebView freshness work is already represented on current main (`LOAD_NO_CACHE`, cache clearing and versioned fresh home URL), while the separate pre-auth mobile More-menu containment defect still needs a fresh protected candidate. Authentication/security-boundary changes remain explicit-approval gated.

## Live production-first health triage

### Authentication / UI

- Current main includes PR #1374 mobile challenge-flow repair.
- Current Admin source still constructs the enhanced mobile `#adminMoreMenu` outside `#appView`, so the structural pre-auth navigation-containment defect reported by #1369 remains relevant even though that PR is stale.
- Current Admin Android already forces a fresh privileged hosted shell and does not rely on the stale WebView behavior from the old PR base.
- No auth/RLS/role/secret bypass is authorised by this ledger.

### Catalogue / sync — freshly verified

Read-only production audit on 2026-09-12:
- `device_catalog`: 1,873 total rows; 1,756 active.
- Active rows missing brand/model/source URL: 0.
- Active rows missing image reference: 6.
- Active rows with no storage options: 59.
- Duplicate active model-number groups: 16; these require evidence review because legitimate regional/hardware variants must not be collapsed automatically.
- Canonical duplicate-group count was not reasserted in this pass because the combined read-only query was not accepted by the tool; do not treat the older count as freshly verified.
- `catalog_sync_state`: revision 258. Latest observed event is an `INSERT` on `device_catalog` at `2026-09-11 16:31:46 UTC`.
- Compared with the prior verified baseline, total and active catalogue counts each increased by four while the image/storage gap counts and model-number collision-group count stayed unchanged. This is correlation evidence only, not proof that the four inserted rows caused every count change.

### Inventory integrity — freshly verified

Read-only invariants pass:
- orphan inventory -> catalogue references: 0.
- negative acquired/expected-sale prices: 0.
- retired timestamps with incompatible lifecycle status: 0.

### Guardian — freshly verified

- 14 incidents are not yet in resolved/closed/verified state: 2 high, 11 medium, 1 low.
- 5 unresolved incidents require approval and have no `approved_at` evidence.
- Guardian repair/decision authority remains human-approval protected; no autonomous repair/approval action was taken.

### Backup / recovery — freshly verified

- Latest recorded user-encrypted Google Drive backup remains `ready`, format v1, AES-256-GCM, 701 bytes, created `2026-09-07 13:47:28 UTC`.
- `restored_at` is still null. No restore-readiness success can be claimed from this record.
- The backup remains stale relative to the current date and still requires the existing authorised user Google Drive flow for a safe refresh/verification.
- No server-side token substitution, key mutation, credential change, destructive restore or authorization bypass is permitted.

### Nova quality / cost evidence — freshly verified

- `nova_ai_runs` exists with provider/mode/model, latency, token, cost, success/degraded and failure metadata fields.
- It currently contains zero rows: no first/last run, no measured production latency, no production fallback/degraded sample and no recorded spend.
- Therefore provider quality/cost/safety drift cannot yet be judged from production telemetry. Continue static/contract/evaluation coverage and treat telemetry-derived conclusions as unavailable until real approved traffic is recorded.

### Supabase security drift — freshly verified, evidence only

Security advisors currently report:
- 12 `RLS enabled, no policy` informational findings, including `nova_ai_runs`, Nova catalogue/knowledge tables, buy-price tables and backup-key tables. Some may intentionally be service-only; consumer/privilege intent must be mapped before any change.
- 13 warnings for `SECURITY DEFINER` functions executable by authenticated users, including Admin invite/inventory RPCs and Guardian decision/control/report RPCs.
- leaked-password protection disabled warning.

These findings touch protected auth/RLS/privilege boundaries. No autonomous schema, policy, function privilege or Auth configuration change is authorised.

### Supabase performance drift — freshly verified, evidence only

- 45 indexes are currently reported as unused. No indexes were removed because low-volume/recent workloads can make usage evidence misleading.
- `public.nova_ai_runs` has one duplicate-index warning for `nova_ai_runs_user_created_idx` and `nova_ai_runs_user_id_idx`.
- Index removal is a production schema change and is not authorised autonomously; additionally, `nova_ai_runs` currently has zero rows, so there is no urgent measured production cost from the duplication.

## Active workstreams and impact/risk ranking

| Rank | Lane | Impact | Risk | Current state | Next safe action |
|---|---|---:|---:|---|---|
| 1 | Admin auth / Android parity | 10 | 8 | Old #1369 is stale; Android freshness already landed elsewhere; pre-auth body-mounted More menu remains structurally exposed | Prepare a narrow fresh current-main web-containment candidate and regression evidence; do not merge/deploy without explicit approval |
| 2 | Backup/recovery readiness | 10 | 6 | Latest user-encrypted backup is still the 2026-09-07 ready record; no restore recorded | Re-check/refresh only through existing authorised user flow; no token/key bypass or destructive restore |
| 3 | Production login verification | 10 | 5 | PR #1374 merged on current main | Verify live browser/mobile challenge behavior with read-only/synthetic evidence where supported; only patch demonstrated regressions |
| 4 | Nova multi-model quality | 9 | 5 | #1355 merged but `nova_ai_runs` has zero telemetry rows | Strengthen static/evaluation coverage and verify routing/fallback/cost guardrails without inventing production metrics |
| 5 | Catalogue/data quality | 9 | 4 | 1,873 total / 1,756 active; 6 image gaps, 59 storage gaps, 16 model-number collision groups | Manufacturer-first evidence review; never guess or destructively collapse regional variants |
| 6 | Guardian incident hygiene | 9 | 8 | 14 unresolved; 5 approval-gated | Read-only correlation/triage only; Guardian code-changing repairs and decisions remain human gated |
| 7 | Supabase security drift | 9 | 8 | 12 no-policy INFO, 13 authenticated SECURITY DEFINER WARN, leaked-password warning | Build consumer/intent evidence map; any RLS/auth/privilege change requires explicit approval |
| 8 | Release/integration stabilisation | 8 | 3 | Android 2.15.92 metadata/release checksum match | Continue release identity/parity/contract checks after material native changes; never publish without exact signed artifact evidence |
| 9 | Valuation/Test & Buy/inventory lifecycle | 8 | 5 | Core inventory invariants freshly clean; Device Lens storage verification is released | Continue contract/regression audit and preserve pricing-authority boundaries |
| 10 | Technical debt / capacity | 5 | 3 | 45 unused-index notices + one duplicate Nova index; large historical branch set remains ambiguous | Prefer evidence-backed simplification; no index/branch deletion without clear safety and relevance evidence |

## Dependency / protected-boundary map

- Web, Android and Admin share catalogue, pricing and session semantics.
- Device Lens pricing depends on complete identity/photo/storage evidence and existing guarded pricing authority; staff verification supplies evidence but does not grant pricing authority.
- Nova depends on catalogue/search/market contracts and approved external AI providers; output remains advisory.
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
13. Advisor warnings are evidence, not permission for automatic security/schema changes.

## Failure-pattern / change-correlation record

- 2026-09-12 — Continuity drift: stale ledger state understated Android/Nova progress. Corrective action: reconcile ledger against live GitHub and service evidence before task selection.
- 2026-09-12 — Morley Buys mobile auth regression: large inert challenge iframe before credentials. Repair merged in #1374; challenge remains mandatory but starts only when credentials are ready.
- 2026-09-12 — Admin auth candidate drift: #1369 diverged from main while newer Admin Android freshness fixes landed independently. Treat its web containment concept separately from its superseded Android/release parts; never stale-merge.
- 2026-09-12 — Catalogue baseline moved from 1,869/1,752 to 1,873/1,756 with sync revision 258 and latest observed `device_catalog` INSERT. Quality-gap counts remained stable; no destructive reconciliation was attempted.
- 2026-09-12 — Nova operational telemetry has zero rows after the multi-model merge. Do not invent latency/cost/reliability conclusions; rely on tests/evaluations until real approved traffic produces evidence.
- 2026-09-11 — Device Lens storage dead-end fixed/released in #1370 / 2.15.92 with staff-confirmed storage and explicit no-guess behavior.
- 2026-09-11 — Nova #1355 removed experimental Gumtree-expanding market-search/pricing-confidence changes before merge.
- 2026-09-11 — Backup triage proved global scheduler health and stale user-encrypted backup are separate conditions; never bypass user authorization to refresh it.
- 2026-09-11 — Release identity races are handled by branching from actual post-merge main, advancing exactly one identity, validating, and publishing exact signed-artifact metadata.

## Current blockers / approvals

- A fresh Admin pre-auth navigation-containment repair will require explicit approval before merge/deployment because it changes an authentication/security boundary.
- Supabase RLS/Auth/`SECURITY DEFINER` privilege changes are approval-gated.
- Guardian repairs/decisions remain approval-gated.
- Per-user encrypted Drive backup freshness still needs fresh authorised-flow evidence before recovery readiness can be claimed green.
- Any destructive catalogue reconciliation remains approval-gated when evidence is ambiguous.
- Nova telemetry currently lacks production samples; this is an evidence limitation, not permission to fabricate benchmark conclusions.

## Next autonomous checkpoint

1. Prepare and validate a fresh current-main Admin pre-auth navigation-containment candidate, keeping it unmerged pending explicit approval.
2. Audit Nova provider/routing static contracts and evaluation coverage while telemetry is empty; add only evidence-backed non-protected regression coverage.
3. Verify current production/mobile login behavior after #1374 using non-destructive/synthetic evidence where supported.
4. Continue manufacturer-first catalogue evidence review for the six image gaps, 59 storage gaps and 16 model-number collision groups.
5. Correlate unresolved Guardian incidents against recent merges/deployments without applying protected repairs.
6. Re-check authorised user-backup freshness when the approved Google Drive flow produces new evidence.
