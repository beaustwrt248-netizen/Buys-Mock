# Morley Ecosystem Autopilot State

Last reconciled: 2026-09-12 01:31 AWST
Main SHA observed during reconciliation: `08a6feeab009517b1b11945732edb6cfe9001cd8`

This is the non-sensitive continuity ledger for Morley ecosystem maintenance. Gumtree is intentionally excluded.

## Production / release snapshot

- Published Morley Buys Android OTA remains `2.15.92` / versionCode `136`. Its GitHub release asset and `ota/latest.json` still match SHA-256 `df5ee0c4e7068a0c5ed37a831e2a3582e46696d018a975d6910a1bb3c6cd36f2`.
- PR #1387 merged as `fb71285772483a2fa155e3ffe7a11e534393248d`, advancing Android source identity to `2.15.93` / versionCode `137` after the native Turnstile repairs. The merge did **not** publish OTA metadata or claim a release.
- Exact-head #1387 checks passed before merge: Repository Security Audit, Full Feature Contract Audit, B&L Morley Quality Gate, Morley Ultimate Parity Gate, Morley OTA Version Policy, Android PR Read-Only Validation, Build B&L Morley APK and associated integration/safety contracts.
- The post-merge main build for 2.15.93 is running. Official release/OTA promotion remains pending exact signed-artifact/signature/checksum evidence and the protected release/signing approval boundary.
- PR #1374 repaired browser/mobile challenge lifecycle. PR #1377 and the later default-on native Turnstile bootstrap removed AndroidBridge timing dependency while preserving Admin deferred `about:blank` startup and mandatory CAPTCHA.
- The mobile-first full-screen stock-image login carousel workstream is already merged via PR #1297. It rotates cached catalogue imagery with broken-image/video fallback, reduced-motion behavior, responsive safe-area handling and accessible form semantics.
- Nova production intelligence from #1355 is on main. Multi-model routing, bounded retry/circuit breaking, cost metadata, native mode selection, speech input and Vision damage regions are present. Gumtree-expanding experimental work was removed before merge.

## Protected-boundary reconciliation

Two protected changes reached `main` during concurrent work even though the repository evidence immediately beforehand explicitly said they required Beau approval:

1. **Admin auth containment PR #1376** merged as `eb1a8385fe157c3de065a26ec4b0a99d698a1780`. It hardens pre-auth privileged navigation containment. The PR has no submitted GitHub review, and its only recorded approval-evidence comment explicitly said it remained unmerged pending Beau's approval. Do not treat the repository-owner identity that performed the merge as independent approval.
2. **Guardian/service-role privilege PR #1386** merged as `7c27b9dfe2c721f73c2b9b4f865f5d9bd047c6d9`. Its comment explicitly said the database-role privilege repair and repository merge were approval-gated and must not auto-merge. No submitted GitHub review is recorded. The equivalent production grants had already been applied before repository merge.

Do **not** automatically revert either protected change: #1376 is security-hardening and #1386 restored access needed to resolve live HTTP-500 failures; reverting is itself a protected/security action and could reintroduce production failure. Beau must review/ratify or direct remediation. Until then this is a governance exception, not evidence that future protected changes may auto-merge.

## Live production-first health triage

### Authentication / UI

- Current native/web Turnstile source includes default-on native challenge bootstrap, duplicate-success-message removal and a later Admin login security-state presentation repair.
- Published Android OTA is still 2.15.92, so the latest native candidate is not considered deployed until 2.15.93 is signed, published and OTA metadata is verified.
- Admin Android uses `LOAD_NO_CACHE`, clears legacy cache and loads a versioned fresh Admin URL. #1376's fail-closed pre-auth navigation containment is now on main, subject to the governance exception above.

### Catalogue / sync — freshly verified

- `device_catalog`: 1,873 total / 1,756 active.
- Active rows missing brand/model/source URL: 0.
- Active rows with blank/null `image_reference_url`: 0, improved from 6 earlier in this run. Populated reference URLs do **not** prove every value is a directly renderable image asset; clients must continue to preload/skip broken or non-image references safely.
- Active rows with no storage options: 58, improved from 59 earlier in this run.
- Duplicate active model-number groups: 16 at the last verified collision audit; preserve legitimate regional/hardware variants and never collapse automatically.
- `catalog_sync_state` advanced to revision 260. Latest observed event was an UPDATE at `2026-09-11 17:11:49 UTC`.
- Motorola `razr 70 plus` / XT2659-3 remains intentionally without a storage option because accessible first-party material did not provide dependable capacity evidence. Unknown remains preferable to guessing.

### Inventory integrity — freshly verified

- Orphan inventory -> catalogue references: 0.
- Negative acquired/expected-sale prices: 0.
- Retired timestamps with incompatible lifecycle status: 0.

### Guardian / sync-security incident state — freshly verified

- The previously high `user-security-center` and `user-sync-state` HTTP-500 incidents now read `resolved`; no Guardian incident currently remains outside resolved/closed/verified states.
- Current read-back confirms RLS remains enabled on `user_sync_state`, `user_sync_events`, `user_session_devices` and `user_security_events`.
- Current minimum service-role table grants needed by the deployed functions are present: sync-state SELECT; sync-events INSERT; session-devices SELECT/INSERT/UPDATE; security-events SELECT/INSERT. Required event-sequence USAGE and `user_sync_compare_and_set` service-role EXECUTE are also present.
- This is functional evidence for the repair, not approval evidence for the protected privilege mutation. No further Guardian repair/decision action is authorised without Beau's explicit direction.

### Backup / recovery — freshly verified

- `morley-google-drive-backup-daily` is active on `0 19 * * *`; latest observed daily run (2026-09-10 19:00 UTC) and prior two runs succeeded.
- `morley-recovery-health-hourly` is active and recent hourly runs are succeeding.
- Latest user-encrypted Drive backup remains `ready`, AES-256-GCM, format v1, 701 bytes, created `2026-09-07 13:47:28 UTC`; `restored_at` is null.
- Global scheduler health and stale per-user backup freshness are separate conditions. Refresh requires the existing authorised user Google flow; no token substitution, key mutation or destructive restore is permitted.

### Nova quality / cost evidence

- `nova_ai_runs` currently contains zero production rows, so no production cost/latency/fallback trend can be claimed yet.
- Static source confirms admin-only Auto/GPT/Gemini/Claude/Consensus orchestration, bounded retry/circuit breakers and default `NOVA_MAX_REQUEST_COST_USD` of $0.25.
- Current main strengthened measured-cost guardrail regression coverage. Continue tests/evaluations until approved real traffic produces telemetry; do not invent benchmark results.

### Supabase security / performance drift — evidence only

- Latest advisor baseline: 12 RLS-enabled/no-policy INFO findings, 13 authenticated-callable `SECURITY DEFINER` WARN findings, and leaked-password protection disabled warning.
- Performance baseline: 45 unused-index notices and one duplicate-index warning on `nova_ai_runs`.
- No further auth/RLS/function-privilege/index change is authorised autonomously. Existing protected #1386 change is recorded above as a governance exception requiring Beau review.

## Active workstreams and impact/risk ranking

| Rank | Lane | Impact | Risk | Current state | Next safe action |
|---|---|---:|---:|---|---|
| 1 | Governance / protected-change review | 10 | 10 | #1376 and #1386 reached main without independent approval evidence | Notify Beau; preserve evidence; do not auto-revert or normalize the exception |
| 2 | Morley Android release stabilisation | 10 | 4 | 2.15.93/137 identity merged; post-merge signed build evidence pending; OTA still 2.15.92 | Verify main build signature/artifact/checksum; prepare release approval evidence; do not publish OTA autonomously |
| 3 | Production login verification | 10 | 5 | Latest native/web challenge fixes are on main; published APK remains older | Verify signed 2.15.93 candidate and physical-device behavior after approved publication |
| 4 | Backup/recovery readiness | 10 | 6 | Global jobs healthy; per-user backup stale; no restore evidence | Refresh only through authorised user flow; isolated restore-readiness evidence when safely available |
| 5 | Nova multi-model quality | 9 | 5 | Routing/cost guardrails merged; production telemetry empty | Expand evidence-backed evaluations/contract checks; compare real telemetry only when samples exist |
| 6 | Catalogue/data quality | 9 | 4 | 0 blank image-reference fields, 58 storage gaps, 16 model-number collision groups | Manufacturer-first verification; separately audit directly renderable image coverage; never guess |
| 7 | Guardian incident hygiene | 9 | 8 | All previously tracked incidents currently resolved/closed/verified; protected repair provenance needs review | Read-only recurrence watch; any new repair/decision stays approval-gated |
| 8 | Supabase security drift | 9 | 8 | Advisor warnings remain | Build consumer/intent map; no additional protected RLS/Auth/privilege changes without approval |
| 9 | Valuation/Test & Buy/inventory lifecycle | 8 | 5 | Core invariants clean; Device Lens storage verification released in 2.15.92 | Continue regression/contract audit; preserve protected pricing authority |
| 10 | Technical debt / capacity | 5 | 3 | Unused/duplicate index evidence and large historical branch set | Prefer narrow evidence-backed cleanup; no ambiguous branch/schema/data deletion |

## Dependency / protected-boundary map

- Web, Android and Admin share catalogue, pricing and session/auth semantics.
- Device Lens pricing requires complete identity/photo/storage evidence and guarded pricing authority; staff verification adds evidence only.
- Nova depends on catalogue/search/model-provider contracts and remains advisory.
- Supabase schema/RLS/auth, privileged functions/roles, secrets, production-destructive operations, protected pricing policy, signing/release credentials, GitHub workflow security and Guardian approval policy require explicit human approval.
- Same repository-owner identity is never independent approval for protected work.
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
9. Auth/RLS/Guardian/security approval boundaries must not be weakened or bypassed autonomously.
10. A successful protected change does not retroactively create approval authority.
11. Ambiguous production data is never auto-deleted/merged/destructively rewritten.
12. Backup health is not green without freshness, integrity and restore-readiness evidence.
13. Gumtree remains excluded unless Beau explicitly re-enables it.

## Failure-pattern / change-correlation record

- 2026-09-12 — **Protected-boundary governance exception:** #1376 and #1386 merged despite explicit repository evidence that Beau approval was still required; #1386 production grants had also been applied before repository merge. Safe response is evidence preservation, no autonomous rollback, and explicit Beau review/ratification or remediation direction.
- 2026-09-12 — Release identity race: repeated 2.15.93 candidates became stale while auth/Nova work advanced main. PR #1387 is the successful identity merge after exact-head validation.
- 2026-09-12 — Native Turnstile bootstrap: current main uses default-on bootstrap for real challenge navigation while preserving deferred Admin about:blank behavior.
- 2026-09-12 — Catalogue scorecard improved: blank/null image-reference fields 6 -> 0 and active missing-storage rows 59 -> 58; reference population is not equivalent to verified direct-image loadability.
- 2026-09-12 — Guardian high HTTP-500 patterns were traced to missing service-role DML/sequence privileges; current read-back shows the minimum grants and RLS still present and the incidents now resolved. The protected-change approval process was not satisfied by that functional success.
- 2026-09-12 — Nova telemetry remains empty; do not invent cost/latency trends. Static measured-cost guardrail coverage was strengthened on main.
- 2026-09-11 — Device Lens storage dead-end fixed/released in #1370 / 2.15.92 with staff-confirmed storage and explicit no-guess behavior.
- 2026-09-11 — Global backup scheduler health and stale user-authorised Drive backup are distinct; never bypass Google authorization to refresh it.

## Current blockers / approvals

- **Beau review is required for protected changes #1376 and #1386 that have already reached main/production without independent approval evidence.** Do not autonomously revert or treat them as precedent.
- Official Morley Buys 2.15.93 release/OTA publication remains unapproved until the main signed build produces exact artifact/signature/checksum evidence; release/signing promotion is protected.
- Any further Supabase RLS/Auth/`SECURITY DEFINER`/role privilege changes are approval-gated.
- Guardian repairs/decisions remain approval-gated despite the current zero-unresolved snapshot.
- Per-user encrypted Drive backup freshness still needs the authorised Google flow.
- Destructive/ambiguous catalogue reconciliation remains approval-gated.

## Next autonomous checkpoint

1. Verify the post-merge 2.15.93 main build through regression, lint, release build, signer verification, packaged login-video identity and uploaded artifact/checksum; prepare, but do not autonomously publish, official OTA/release.
2. Preserve #1376/#1386 approval evidence and await Beau's review; do not roll back protected live behavior without explicit direction and safety validation.
3. Watch Guardian read-only for recurrence of the resolved sync/security 500 fingerprints.
4. Audit renderable catalogue image coverage separately from populated reference URLs; continue manufacturer-first storage/model verification.
5. Continue Nova evaluation and cost/fallback regression coverage while production telemetry remains empty.
6. Re-check user Drive backup freshness/recovery evidence through the authorised flow.
