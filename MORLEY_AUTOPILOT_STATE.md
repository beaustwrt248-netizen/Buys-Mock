# Morley Ecosystem Autopilot State

Last reconciled: 2026-09-12 01:34 AWST
Main SHA observed during reconciliation: `08a6feeab009517b1b11945732edb6cfe9001cd8`

This is the non-sensitive continuity ledger for Morley ecosystem maintenance. Gumtree is intentionally excluded.

## Production / release snapshot

- PR #1387 merged as `fb71285772483a2fa155e3ffe7a11e534393248d`, advancing Android source identity to `2.15.93` / versionCode `137` after the native Turnstile repairs.
- GitHub Release `v2.15.93` is now published from that tested source. The APK `B-and-L-Morley-2.15.93.apk` has SHA-256 `e5e12ab3b895c8b29b84b516e18f6f67a6cbac9daa8d45a827052bcae4540eb6`.
- The automated publisher verified the exact signed APK artifact before publishing the release.
- `ota/latest.json` on main still advertises `2.15.92` / versionCode `136` with SHA-256 `df5ee0c4e7068a0c5ed37a831e2a3582e46696d018a975d6910a1bb3c6cd36f2` at this reconciliation point.
- PR #1390 is the protected automated OTA metadata handoff for 2.15.93. It remains open; do not call 2.15.93 OTA-live until that PR is approved/merged and the main manifest is read back with the exact release URL/checksum.
- Nova production intelligence from #1355 and measured-cost regression coverage from #1384 are on main.

## Protected-boundary reconciliation

- Beau explicitly approved **#1376 and #1386** in chat on 2026-09-12 before their merges were executed in this workflow.
- Admin auth-containment PR #1376 then merged as `eb1a8385fe157c3de065a26ec4b0a99d698a1780`.
- Guardian/service-role privilege PR #1386 then merged as `7c27b9dfe2c721f73c2b9b4f865f5d9bd047c6d9`.
- These approvals are specific to #1376 and #1386 only. They do not authorize future auth/RLS/privilege/Guardian/release/signing changes.
- A separate Guardian audit discrepancy remains worth monitoring: one historical approval-required incident was marked applied/resolved without an `approved_at` timestamp. Do not treat that record as precedent for bypassing approval.

## Live production-first health triage

### Authentication / UI

- #1376 is on main, providing fail-closed pre-auth privileged Admin navigation containment.
- Current main also includes later Admin/Cloudflare presentation fixes.
- PR #1391 addresses a separate physical Samsung WebView regression where Admin login renders but taps do not reliably focus email/password fields, preventing intentionally deferred Cloudflare startup.
- #1391 preserves mandatory Turnstile and keeps Sign in disabled until a genuine token exists. Because it changes authentication-entry behavior, merge remains Beau-approval-gated.
- #1391's UI gate failure was traced to missing required PR-template checklist markers, not a runtime/test failure. The PR body was corrected and the failed checklist job rerun.

### Catalogue / sync — freshly verified

- `device_catalog`: 1,876 total / 1,759 active.
- Active rows with blank/null `image_reference_url`: 0.
- Active rows with no storage options: 56.
- All active mobile-phone storage gaps identified in the latest pass were resolved from evidence-backed sources.
- `catalog_sync_state` is revision 263.
- Remaining storage gaps must be classified as truly missing versus not-applicable before any update; never invent capacities merely to improve the score.
- Catalogue identity must preserve legitimate regional/hardware variants; never collapse model-number collisions automatically.

### Inventory integrity

- Last verified invariants: orphan inventory-to-catalogue references 0; negative acquired/expected-sale prices 0; incompatible retired lifecycle timestamps 0.
- No destructive inventory repair was performed.

### Guardian / sync-security incident state — freshly verified

- Current query returned **zero incidents** outside resolved/closed/verified/ignored states.
- The recent high `user-security-center` and `user-sync-state` HTTP-500 families were traced to missing `service_role` table/identity-sequence privileges.
- Minimum required production grants were restored and independently read back; `user_sync_compare_and_set` service-role EXECUTE remains present.
- RLS remains enabled on `user_sync_state`, `user_sync_events`, `user_session_devices` and `user_security_events`, with policies present.
- Approved PR #1386 is merged so repository migrations now record the production privilege repair.
- Guardian repairs/decisions remain human approval-gated despite the current zero-unresolved snapshot.

### Backup / recovery

- `morley-google-drive-backup-daily` remains active; recent observed scheduler runs succeeded.
- `morley-recovery-health-hourly` remains active; recent observed hourly runs succeeded.
- A separate `stale_backup` warning for the user-encrypted Google Drive backup remains open, occurrence count 65, last seen `2026-09-11 17:17:00 UTC`.
- Scheduler success does not prove per-user backup freshness or restore-readiness. Refresh must use the authorised Google flow; no token substitution or destructive restore.

### Nova quality / cost evidence

- `nova_ai_runs` still contains 0 production rows, so no production latency/cost/fallback trend can be claimed.
- Nova multi-model routing and telemetry contracts remain on main.
- PR #1384 merged regression coverage preserving the actual measured-cost behavior: ensemble candidate usage is measured before deciding whether to make an additional fusion call. This is not a hard pre-spend cap on initial ensemble calls.
- Continue evidence-backed evaluations until real approved traffic creates telemetry.

### Supabase security / performance drift — evidence only

- Existing advisor findings remain evidence-only pending consumer/intent review.
- No further auth/RLS/function-privilege/index change is authorised autonomously.

## Active workstreams and impact/risk ranking

| Rank | Lane | Impact | Risk | Current state | Next safe action |
|---|---|---:|---:|---|---|
| 1 | Morley 2.15.93 OTA promotion | 10 | 8 | Signed GitHub release published; OTA main manifest still 2.15.92; PR #1390 open | Verify required checks/approval; merge only with explicit Beau approval; then read back main manifest/checksum |
| 2 | Admin mobile login text entry | 10 | 8 | PR #1391 open; broad security/integration checks green; UI metadata gate corrected/rerun | Finish CI evidence; hold runtime merge for Beau approval |
| 3 | Production login verification | 10 | 5 | Latest native/web fixes on main; Admin focus repair not merged | After approved merge/publication, verify physical-device behavior and patch only demonstrated regressions |
| 4 | Backup/recovery readiness | 10 | 6 | Global jobs healthy; per-user backup stale | Refresh only through authorised user flow; gather isolated non-destructive restore evidence when safely available |
| 5 | Nova multi-model quality | 9 | 5 | Routing/cost guardrails merged; production telemetry empty | Continue evidence-backed evaluations/contract checks; compare real telemetry only when samples exist |
| 6 | Catalogue/data quality | 9 | 4 | 1,876 total / 1,759 active; 0 image-reference gaps; 56 storage gaps; revision 263 | Manufacturer-first verification; distinguish not-applicable storage from true missing data |
| 7 | Guardian incident hygiene | 9 | 7 | No currently unresolved incidents; approved privilege repair formalized | Monitor recurrence and approval-ledger integrity; any new repair remains human gated |
| 8 | Supabase security drift | 9 | 8 | Protected advisor findings remain | Build consumer/intent map; no additional RLS/Auth/privilege changes without approval |
| 9 | Valuation/Test & Buy/inventory lifecycle | 8 | 5 | Core invariants clean; Device Lens storage verification released previously | Continue regression/contract audit; preserve protected pricing authority |
| 10 | Technical debt / capacity | 5 | 3 | Large historical branch set and known advisor cleanup candidates | Prefer narrow evidence-backed cleanup; no ambiguous deletion or broad rewrite |

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
3. A source/version merge or GitHub Release alone is not proof that OTA metadata is live.
4. Catalogue identity never silently collapses legitimate regional/hardware variants.
5. Device facts/specifications remain evidence-backed; unknown means unverified, never guessed.
6. Pricing/valuation remains blocked when required evidence is unresolved.
7. Inventory lifecycle transitions remain valid and traceable.
8. App/web/Admin/Nova contracts must not silently diverge.
9. Auth/RLS/Guardian/security approval boundaries must not be weakened or bypassed autonomously.
10. Approval is specific to the explicitly approved protected change and does not create standing authority for later changes.
11. Ambiguous production data is never auto-deleted/merged/destructively rewritten.
12. Backup health is not green without freshness, integrity and restore-readiness evidence.
13. Gumtree remains excluded unless Beau explicitly re-enables it.

## Failure-pattern / change-correlation record

- 2026-09-12 — Protected changes #1376 and #1386 were explicitly approved by Beau in chat and then merged; future protected changes still require separate approval.
- 2026-09-12 — 2.15.93 signed release: exact tested artifact was verified and GitHub Release v2.15.93 published; protected OTA manifest handoff remains separate in #1390.
- 2026-09-12 — Guardian high-500 root cause: service-role privileges were missing for user sync/security backing objects; production grants were restored, read back, and migration #1386 merged after approval. Current unresolved Guardian count is zero.
- 2026-09-12 — Guardian audit discrepancy: one historical approval-required incident was marked applied/resolved without an approval timestamp. Continue monitoring this boundary.
- 2026-09-12 — Admin Samsung login focus regression: #1391 isolates unreliable mobile text-entry/focus while retaining deferred Turnstile and token gating; UI-gate failure was metadata-only and its PR checklist was corrected.
- 2026-09-12 — Catalogue scorecard advanced to 1,876 total / 1,759 active, revision 263, 0 image-reference gaps and 56 storage gaps.
- 2026-09-12 — Nova telemetry remains empty; measured-cost guardrail semantics are regression-protected without inventing cost/latency claims.

## Current blockers / approvals

- **PR #1390 requires Beau's explicit approval** before OTA metadata promotion can be merged. The signed 2.15.93 GitHub release exists, but main OTA still points to 2.15.92 until that handoff is safely completed.
- **PR #1391 requires Beau's explicit approval** before merge because it changes Admin authentication-entry behavior. Complete CI evidence first.
- Any further Supabase RLS/Auth/`SECURITY DEFINER`/role privilege changes are approval-gated.
- Guardian repairs/decisions remain approval-gated.
- Per-user encrypted Drive backup freshness still needs the authorised Google flow.
- Destructive/ambiguous catalogue reconciliation remains approval-gated.

## Next autonomous checkpoint

1. Complete #1391 checklist/parity CI after the metadata-only PR-body correction; do not merge without approval.
2. Inspect #1390 action-required checks and preserve protected OTA approval; after any approved merge, read back `ota/latest.json` and verify exact v2.15.93 URL/checksum.
3. Merge this docs-only ledger refresh once its exact-head checks are green.
4. Continue catalogue manufacturer-first cleanup only where missing values are truly applicable and evidence-backed.
5. Monitor Guardian for recurrence and the approval-ledger discrepancy without changing protected policy.
6. Continue Nova evaluation coverage while production telemetry remains empty.
7. Re-check user Drive backup freshness/recovery evidence through the authorised flow.
