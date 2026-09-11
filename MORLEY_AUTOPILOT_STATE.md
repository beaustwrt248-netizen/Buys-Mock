# Morley Ecosystem Autopilot State

Last reconciled: 2026-09-12 01:46 AWST
Main SHA observed during reconciliation: `107d706069b583a8b85fb628b6fac748d9adb0d1`

This is the non-sensitive continuity ledger for Morley ecosystem maintenance. Gumtree is intentionally excluded.

## Release / production snapshot

- Morley Buys Android source on main is now `2.15.94` / versionCode `138` via merged PR #1393, which changes compact mobile navigation so Catalogue replaces Stock/More in the bottom bar and Stock remains available from the hamburger menu.
- **2.15.94 is source identity only at this checkpoint.** Main build/parity are still running; no 2.15.94 GitHub release or OTA manifest has been verified.
- Latest published/OTA-live release remains `2.15.93` / versionCode `137`.
- GitHub Release `v2.15.93` asset `B-and-L-Morley-2.15.93.apk` has verified SHA-256 `e5e12ab3b895c8b29b84b516e18f6f67a6cbac9daa8d45a827052bcae4540eb6`.
- Main `ota/latest.json` still advertises 2.15.93/137 with that exact release URL/checksum.
- Duplicate mobile-navigation PR #1394 was closed as superseded by #1393.

## Protected-boundary record

- Beau explicitly approved #1376 and #1386 before this workflow merged them.
- #1376 merged as `eb1a8385fe157c3de065a26ec4b0a99d698a1780`.
- #1386 merged as `7c27b9dfe2c721f73c2b9b4f865f5d9bd047c6d9`.
- #1390 OTA promotion and #1391 Admin authentication-entry repair later merged concurrently while this workflow was deliberately holding them for protected approval. This workflow did not perform those merges.
- #1392, which materially widens Guardian candidate scope and routes diagnosis/candidate generation through Nova, also merged concurrently while this workflow was holding it for protected approval. This workflow did not perform that merge. The merged design still keeps actual repository writes behind incident approval and does not itself merge/deploy/apply candidate repairs, but its approval provenance must remain visible.
- One historical Guardian approval-required incident also has `applied_at` without `approved_at`. Continue monitoring approval provenance.
- Concurrent protected merges do not create standing authority for later protected changes.

## Production-first health

### Authentication / Admin

- #1376 fail-closed pre-auth Admin navigation containment is on main.
- #1391 WebView focus recovery is on main.
- PR #1396 is now the stronger Android Admin candidate: native Compose email/password entry + dedicated Turnstile WebView, existing AdminApi sign-in/role verification, then secure session handoff into the existing authenticated Admin web workspace. Browser Admin remains unchanged.
- #1396 is **approval-gated** because it replaces the Android authentication-entry architecture. Its first least-privilege failure was a stale test that still required HTML WebView credential focus. The branch has since updated that regression to validate native credential entry, mandatory Turnstile, trusted-origin session handoff and non-exported workspace routing; new exact-head CI is running.
- Physical Samsung verification remains the decisive end-to-end signal for the original installed-app login symptom.

### Guardian

- Last production query returned zero incidents outside resolved/closed/verified/ignored states.
- Recent `user-sync-state` and `user-security-center` HTTP-500 families were traced to missing service-role object privileges; required minimum grants/sequence usage were restored and read back.
- `user_sync_compare_and_set` service-role EXECUTE remains present; RLS remains enabled on the user sync/security tables with policies present.
- #1386 records the approved privilege repair.
- #1392 is now on main: Guardian diagnosis/candidate generation can use Nova intelligence and candidate scope can include `supabase/functions/**` plus at most one new timestamped migration when evidence indicates database work. Protected execution/merge/deployment approval remains a required invariant.

### Catalogue / sync

- `device_catalog`: 1,876 total / 1,759 active.
- Active missing image references: 0.
- Active missing storage options: 56.
- Latest verified catalogue sync revision: 263.
- All active mobile-phone storage gaps found in the last pass were resolved from evidence-backed sources.
- Remaining gaps must be classified as truly missing versus not-applicable before updates. Never invent storage merely to improve a score.
- Preserve legitimate regional/hardware variants; never auto-collapse model-number collisions.

### Inventory

- Last verified invariants: orphan inventory-to-catalogue references 0; negative acquired/expected-sale prices 0; incompatible retired lifecycle timestamps 0.
- No destructive inventory repair was performed.

### Backup / recovery

- Daily Google Drive backup scheduler is active and recent observed scheduler runs succeeded.
- Hourly recovery-health scheduler is active and recent observed runs succeeded.
- Separate user-encrypted backup freshness warning remains open: `stale_backup`, occurrence count 65, last seen `2026-09-11 17:17:00 UTC`.
- Scheduler success is not proof of per-user backup freshness or restore-readiness. Refresh only through the authorised Google flow; never substitute tokens or perform destructive restore tests.

### Nova

- `nova_ai_runs` remains at 0 production rows, so no real production latency/cost/fallback trend can be claimed.
- Multi-model routing/telemetry contracts remain on main.
- #1384 regression-protects the existing measured-cost behavior: ensemble candidate usage is measured before deciding whether to make an additional fusion call. It is not a hard pre-spend cap on initial ensemble calls.

## Active workstreams — impact / risk

| Rank | Lane | Impact | Risk | State | Next safe action |
|---|---|---:|---:|---|---|
| 1 | Admin native Android auth | 10 | 9 | PR #1396 open; first stale regression replaced; exact-head CI running | Finish read-only security/CI review; explicit Beau approval required before merge |
| 2 | Morley 2.15.94 release readiness | 10 | 8 | Source 2.15.94/138 on main; build/parity running; release/OTA still 2.15.93 | Verify main build/signature evidence; do not claim or promote release without protected approval/evidence |
| 3 | Physical login verification | 10 | 5 | #1391 on main; #1396 proposes stronger native solution | Verify on physical Samsung after any approved native-auth release |
| 4 | Backup/recovery readiness | 10 | 6 | Scheduler healthy; per-user encrypted backup stale | Refresh only via authorised user flow; gather isolated non-destructive restore evidence |
| 5 | Catalogue/data quality | 9 | 4 | 1,876 total / 1,759 active; 0 image gaps; 56 storage gaps; revision 263 | Manufacturer-first verification; distinguish N/A from missing |
| 6 | Nova quality/cost | 9 | 5 | Guardrails merged; production telemetry empty | Continue contract/evaluation coverage; wait for real approved traffic before trend claims |
| 7 | Guardian incident hygiene | 9 | 7 | Zero currently unresolved; expanded Nova-assisted candidate pipeline now on main | Read-only recurrence watch; preserve approval provenance and protected execution boundaries |
| 8 | Supabase security drift | 9 | 8 | Protected advisor findings remain | Build consumer/intent map; no RLS/Auth/privilege changes without approval |
| 9 | Valuation/Test & Buy/inventory | 8 | 5 | Core invariants clean | Continue regression/contract audit; preserve protected pricing authority |
| 10 | Technical debt/capacity | 5 | 3 | Narrow cleanup candidates remain | Prefer reversible, evidence-backed cleanup; no broad rewrite or ambiguous deletion |

## Dependency / protected-boundary map

- Web, Android and Admin share catalogue, pricing and session/auth semantics.
- Device Lens pricing requires complete identity/photo/storage evidence and guarded pricing authority; staff verification adds evidence only.
- Nova remains advisory; Guardian owns incident evidence, risk policy and approval state even when Nova assists diagnosis/candidate generation.
- Supabase schema/RLS/auth, privileged roles/functions, secrets, production-destructive operations, protected pricing policy, signing/release credentials, GitHub workflow security and Guardian approval policy require explicit human approval.
- User Drive backup freshness depends on valid user Google authorization.
- OTA metadata must remain monotonic and exactly match the signed release artifact/checksum.

## Critical invariants

1. OTA `versionCode` is monotonic and metadata matches the exact signed artifact/checksum.
2. Material Android source changes mint the next release identity before promotion.
3. A source/version merge or GitHub Release alone is not proof of OTA-live status; main manifest read-back is required.
4. Catalogue identity never silently collapses legitimate regional/hardware variants.
5. Device facts remain evidence-backed; unknown means unverified, never guessed.
6. Pricing/valuation remains blocked when required evidence is unresolved.
7. Inventory lifecycle transitions remain valid and traceable.
8. App/web/Admin/Nova contracts must not silently diverge.
9. Auth/RLS/Guardian/security approval boundaries must not be weakened autonomously.
10. Approval applies only to the specific protected change explicitly approved.
11. Ambiguous production data is never auto-deleted/merged/destructively rewritten.
12. Backup health is not green without freshness, integrity and restore-readiness evidence.
13. Gumtree remains excluded unless Beau explicitly re-enables it.

## Change / failure correlation

- 2026-09-12 — #1393 merged compact mobile navigation and minted source identity 2.15.94/138; published release/OTA remain 2.15.93/137 pending verified build/promotion.
- 2026-09-12 — #1392 Guardian/Nova protected-candidate pipeline merged concurrently while held for approval; this workflow did not merge it.
- 2026-09-12 — 2.15.93 release chain verified end-to-end: signed artifact verified, GitHub Release published, #1390 OTA handoff merged concurrently, and main manifest read back with exact URL/checksum.
- 2026-09-12 — #1391 Admin mobile input/Turnstile repair merged concurrently after corrected UI checklist rerun passed; this workflow did not merge it.
- 2026-09-12 — #1376 and #1386 were explicitly approved by Beau and merged by this workflow.
- 2026-09-12 — Guardian high-500 root cause was missing service-role privileges; current unresolved incident count is zero after repair.
- 2026-09-12 — Guardian approval-provenance discrepancy remains for one historical incident with `applied_at` but no `approved_at`.
- 2026-09-12 — Catalogue scorecard: 1,876 total / 1,759 active, revision 263, 0 image-reference gaps, 56 storage gaps.
- 2026-09-12 — Nova telemetry remains empty; do not invent production cost/latency trends.

## Current blockers / approvals

- **PR #1396 requires explicit Beau approval before merge** because it replaces Morley Admin Android authentication-entry architecture with native credentials/Turnstile/session handoff.
- Any 2.15.94 signing/release/OTA promotion remains protected and needs exact artifact/checksum evidence plus explicit approval unless an external automation acts concurrently, in which case verify and record rather than assume.
- Any new Supabase RLS/Auth/service-role/`SECURITY DEFINER`/schema privilege change remains approval-gated.
- Any new Guardian repair/decision execution remains approval-gated.
- Per-user encrypted Drive backup freshness still needs the authorised Google flow.
- Destructive/ambiguous catalogue reconciliation remains approval-gated.

## Next autonomous checkpoint

1. Finish #1396 exact-head CI/security review; do not merge without explicit approval.
2. Verify 2.15.94 main build/parity/signature outcome; keep source/release/OTA states distinct.
3. Merge this docs-only ledger refresh once exact-head checks are green and it remains conflict-free.
4. Monitor Guardian for recurrence and approval-provenance drift.
5. Continue manufacturer-first catalogue cleanup only where values are truly applicable and evidence-backed.
6. Continue Nova evaluation coverage while production telemetry remains empty.
7. Re-check user Drive backup freshness/recovery evidence through the authorised flow.
