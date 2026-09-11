# Morley Ecosystem Autopilot State

Last reconciled: 2026-09-12 01:36 AWST
Main SHA observed during reconciliation: `53130f9ffbb59118e2754d86664fb99f94d1ef1f`

This is the non-sensitive continuity ledger for Morley ecosystem maintenance. Gumtree is intentionally excluded.

## Release / production snapshot

- Morley Buys Android `2.15.93` / versionCode `137` is released and OTA-live.
- GitHub Release `v2.15.93` was published from tested source commit `fb71285772483a2fa155e3ffe7a11e534393248d`.
- APK: `B-and-L-Morley-2.15.93.apk`.
- Verified SHA-256: `e5e12ab3b895c8b29b84b516e18f6f67a6cbac9daa8d45a827052bcae4540eb6`.
- Auto Publish Morley OTA workflow run `34628154639` completed successfully after verifying the exact signed artifact.
- PR #1390 merged as `d23ac27b11fefd01a0f54d4e3459c09b4d6bff27`; main `ota/latest.json` was read back as versionCode `137`, versionName `2.15.93`, the exact release URL and the same SHA-256 above.
- PR #1391 then merged as `53130f9ffbb59118e2754d86664fb99f94d1ef1f`, adding the Samsung/Admin mobile text-entry and deferred Cloudflare-startup repair.

## Protected-boundary record

- Beau explicitly approved #1376 and #1386 in chat before this workflow merged them.
- #1376 merged as `eb1a8385fe157c3de065a26ec4b0a99d698a1780`.
- #1386 merged as `7c27b9dfe2c721f73c2b9b4f865f5d9bd047c6d9`.
- #1390 (OTA promotion) and #1391 (Admin authentication-entry behavior) subsequently merged concurrently while this workflow was deliberately holding them for explicit protected approval. This workflow did not perform those merges. Preserve this as governance evidence; do not auto-revert live release/auth behavior without explicit direction and rollback validation.
- One historical Guardian approval-required incident also has `applied_at` without an `approved_at` timestamp. Continue monitoring approval provenance.
- No concurrent protected merge creates standing authority for later protected changes.

## Production-first health

### Authentication / Admin

- #1376 fail-closed pre-auth Admin navigation containment is on main.
- #1391 is now on main. It adds pointer/touch/click focus recovery for Admin email/password controls while keeping Cloudflare/Turnstile deferred until credentials are ready and Sign in gated on a genuine token.
- #1391's earlier UI gate failure was PR-metadata-only; required checklist markers were added and the rerun passed before the concurrent merge.
- Physical-device verification remains the decisive post-merge signal for the original Samsung login symptom.

### Guardian

- Current production query returned zero incidents outside resolved/closed/verified/ignored states.
- Recent `user-sync-state` and `user-security-center` HTTP-500 families were traced to missing service-role object privileges.
- Required minimum grants and sequence usage were restored and read back; `user_sync_compare_and_set` service-role EXECUTE remains present.
- RLS remains enabled on `user_sync_state`, `user_sync_events`, `user_session_devices` and `user_security_events`, with policies present.
- #1386 records the approved privilege repair in repository migrations.
- New PR #1392 proposes routing Guardian diagnosis/candidate generation through Nova, widening candidate scope to `supabase/functions/**`, and allowing one new timestamped migration candidate when evidence indicates database work. It does not itself deploy/apply those candidates, but it materially changes Guardian's protected repair authority and must remain human approval-gated.

### Catalogue / sync

- `device_catalog`: 1,876 total / 1,759 active.
- Active missing image references: 0.
- Active missing storage options: 56.
- Latest verified catalogue sync revision: 263.
- All active mobile-phone storage gaps found in the last pass were resolved with evidence-backed values.
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
- #1384 regression-protects the existing measured-cost behavior: ensemble candidate usage is measured before deciding whether to make an additional fusion call. It is not a hard pre-spend cap on the initial ensemble calls.

## Active workstreams — impact / risk

| Rank | Lane | Impact | Risk | State | Next safe action |
|---|---|---:|---:|---|---|
| 1 | Physical login verification | 10 | 5 | #1391 on main; original Samsung symptom needs real-device confirmation | Observe physical-device behavior; patch only demonstrated regressions |
| 2 | Guardian + Nova repair pipeline | 10 | 9 | PR #1392 open; materially widens protected candidate scope | Complete read-only security review/checks; explicit Beau approval required before merge |
| 3 | Backup/recovery readiness | 10 | 6 | Scheduler healthy; per-user encrypted backup stale | Refresh only via authorised user flow; gather isolated non-destructive restore evidence |
| 4 | Catalogue/data quality | 9 | 4 | 1,876 total / 1,759 active; 0 image gaps; 56 storage gaps; revision 263 | Manufacturer-first verification; distinguish N/A from missing |
| 5 | Nova quality/cost | 9 | 5 | Guardrails merged; production telemetry empty | Continue contract/evaluation coverage; wait for real approved traffic before trend claims |
| 6 | Guardian incident hygiene | 9 | 7 | Zero currently unresolved | Read-only recurrence watch; preserve approval provenance |
| 7 | Supabase security drift | 9 | 8 | Protected advisor findings remain | Build consumer/intent map; no RLS/Auth/privilege changes without approval |
| 8 | Valuation/Test & Buy/inventory | 8 | 5 | Core invariants clean | Continue regression/contract audit; preserve protected pricing authority |
| 9 | Release safety | 8 | 4 | 2.15.93 exact release/OTA chain verified | Watch rollout/incident evidence; next material Android source change must mint next identity |
| 10 | Technical debt/capacity | 5 | 3 | Narrow cleanup candidates remain | Prefer reversible, evidence-backed cleanup; no broad rewrite or ambiguous deletion |

## Dependency / protected-boundary map

- Web, Android and Admin share catalogue, pricing and session/auth semantics.
- Device Lens pricing requires complete identity/photo/storage evidence and guarded pricing authority; staff verification adds evidence only.
- Nova remains advisory; Guardian owns incident evidence, risk policy and approval state.
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

- 2026-09-12 — 2.15.93 release chain verified end-to-end: signed artifact verified, GitHub Release published, OTA PR #1390 merged concurrently, and main manifest read back with exact 2.15.93 URL/checksum.
- 2026-09-12 — #1391 Admin mobile input/Turnstile repair merged concurrently after its corrected UI checklist rerun passed. This workflow did not perform the protected merge.
- 2026-09-12 — #1376 and #1386 were explicitly approved by Beau and merged by this workflow.
- 2026-09-12 — Guardian high-500 root cause was missing service-role privileges; current unresolved incident count is zero after repair.
- 2026-09-12 — Guardian approval-provenance discrepancy remains for one historical incident with `applied_at` but no `approved_at`.
- 2026-09-12 — Catalogue scorecard: 1,876 total / 1,759 active, revision 263, 0 image-reference gaps, 56 storage gaps.
- 2026-09-12 — Nova telemetry remains empty; do not invent production cost/latency trends.

## Current blockers / approvals

- **PR #1392 requires explicit Beau approval before merge** because it changes Guardian's protected repair pipeline and expands candidate scope to Edge Functions/new migrations.
- Any new Supabase RLS/Auth/service-role/`SECURITY DEFINER`/schema privilege change remains approval-gated.
- Any new Guardian repair/decision execution remains approval-gated.
- Per-user encrypted Drive backup freshness still needs the authorised Google flow.
- Destructive/ambiguous catalogue reconciliation remains approval-gated.

## Next autonomous checkpoint

1. Complete read-only review and CI evidence for #1392; do not merge without explicit approval.
2. Merge this docs-only ledger refresh once exact-head checks are green.
3. Monitor Guardian for recurrence and approval-provenance drift.
4. Continue manufacturer-first catalogue cleanup only where values are truly applicable and evidence-backed.
5. Continue Nova evaluation coverage while production telemetry remains empty.
6. Re-check user Drive backup freshness/recovery evidence through the authorised flow.
7. Watch 2.15.93 rollout/login evidence; if a new Android code change is needed, mint the next monotonic release identity before promotion.
