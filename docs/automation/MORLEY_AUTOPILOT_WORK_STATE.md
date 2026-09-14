# Morley Autopilot Work-State Ledger

Last reconciled: 2026-09-15 07:46 AWST
Canonical repository baseline: protected `main`.
Observed `main` head: `8054e6a12f6de0ae9f98e69cd9ac6ac85f7cac00`.

This is the durable, non-sensitive state ledger for the consolidated Morley ecosystem automation. Reconcile it against live repository, CI, release and connected production services before each automated pass.

## Operating rules

- Resume unfinished work before creating duplicate branches or pull requests.
- Gumtree is excluded unless Beau explicitly re-enables it.
- Prefer small coherent branches from current `main`; never push directly to `main` or bypass protections.
- Continue independent safe lanes while CI, review, credentials or external services block another lane.
- Protected Auth/RLS/authorization, secrets/credentials, destructive production data, privileged roles/permissions, pricing approval policy, workflow/repository security, signing/release credentials, Guardian approval policy, repository visibility, billing and equivalent high-risk changes require explicit Beau approval.
- Guardian code-changing repair PRs remain human-approval gated.
- Do not call work complete until applicable tests/contracts, parity/security checks, deployment/release evidence and rollback implications are verified.
- Release/source identity, OTA publication and deployed production state are separate facts and must be verified separately.

## Current production safety state

### Release / OTA

- Morley Buys **2.15.107 / versionCode 151** is the current published OTA baseline. `ota/latest.json` on protected `main` points to `v2.15.107/B-and-L-Morley-2.15.107.apk` with SHA-256 `ac5be347b2ac4ff88acc6dfe81c076c07e74b4ab8d9161605c78172679ed7199`.
- The immutable GitHub release asset is 44,517,826 bytes and carries the same SHA-256 digest as the OTA feed.
- PR **#2197** merged the scanner safe-area and blocked-verification source repair as source identity **2.15.107 / versionCode 151** at `6ccd68b4a03c1bcacaa9af4ee24ad60ec06d21bb` after Android/regression/lint/signing/parity/security validation.
- User-authored protected promotion PR **#2251** updated only `ota/latest.json` and merged as `4baded9fde34568e3cddd043104c7386d0f00d27` after the required exact-head checks passed.
- Bot-created OTA PR **#2233** was closed unmerged as superseded by #2251; do not revive or merge it.
- The approved Scan Device redesign remains directly in checked-in Kotlin source. Build-time scanner source rewriting is not an acceptable implementation path.
- Superseded scanner PR **#2204** is closed without merge; its one-shot branch-writing workflow/source-materializer path must not be revived.

### Admin web / mobile web

- Authenticated Admin web/mobile continues to use dedicated parity/read-only owners instead of broad legacy browser authority.
- Full catalogue pagination repair #2134 and mobile workspace observer coalescing #2162 are merged and previously verified through Pages/post-deploy smoke/quality/parity checks.
- Admin web remains published from this repository through GitHub Pages at `buyshub.me`, with Cloudflare in front. Vercel is not a Morley deployment dependency.
- Continue responsive parity, mobile clipping/freeze, stale-state, accessibility and role-boundary audits. Do not bypass Cloudflare or weaken Admin/Manager/Staff separation.

### Backup / recovery

- Google Drive OAuth repair and protected manual full-system recovery verification are complete; issue #2051 is closed.
- The restored unattended full-system schedule is production-proven. Cron job `morley-google-drive-backup-daily` ran successfully at **2026-09-14 19:00:00 UTC** and created a scheduler-triggered audit event at **19:00:07 UTC**.
- That unattended backup exported **13 tables**, uploaded `morley-backup-2026-09-14T19-00-02-423Z.json`, and passed upload/re-download SHA-256 recovery verification with digest `f6abe1a0649f30fabcd6e5b48128644210ac3a9f5793f39a2ce4504a5487271b` and **367,668 bytes**. Retention scanned 14 backup files and trashed none.
- Production `morley-google-drive-backup-daily` remains active on `0 19 * * *`.
- Recovery monitoring still reports a separate stale per-user encrypted Drive backup lane. That lane is user-session driven and requires an authenticated Morley bearer token plus a valid Google access token and explicit `action=backup`; it must not be made unattended by retaining or refreshing production user credentials implicitly.
- Do not confuse the per-user freshness finding with the now-proven full-system scheduler. Production restore/overwrite remains protected and requires explicit approval.

### Nova knowledge / Nova Next

- Current production Nova remains authoritative unless a separate protected route/promotion action deliberately changes it.
- PR **#2208** is now **merged**. Its final verified source head was `db4e6c763e4cbd47c36363fda7f7cdc84b779f52`; merge commit `34507af358eb8ae1fc8bc5d33d846f190435efd5` landed the isolated Nova Next UI/PWA/Android/OTA implementation on `main`.
- #2208 exact-head verification passed Nova Next Isolated Validation, Nova Next APK Build, route/password accessibility, Quality Gate, Ultimate Parity, Repository Security, Recovery Backup, Path Stability, catalogue/pricing/email contracts and Restore Point Capture.
- Nova Next OTA path handling is hardened: unsafe version names are rejected, APK cache targets are canonicalized and constrained to the approved update directory, and production Nova Next OTA publication is manual-only through `workflow_dispatch` on `main`.
- The validated Nova Next APK produced during PR review was a **debug validation artifact only**. Merge of #2208 did not itself publish Nova Next OTA or replace the current production Nova route.
- Canonical Nova Next completion/readiness documentation has since been reconciled on `main`, including commit `8054e6a12f6de0ae9f98e69cd9ac6ac85f7cac00`.
- Continue benchmark-based knowledge expansion, citation/source quality, unsupported-claim tracking, provider/fallback quality, latency/cost controls and tool-routing evaluation; corpus growth alone is not proof of answer quality.

### Catalogue audit / data integrity

- The stale-run reconciliation repair is production-proven. Run `cc3cb62f-c42f-46e1-ae81-ea8adb937bea` is `completed`, finished `2026-09-14 15:10:58 UTC`, with terminal notes `verified=13; blocked=62; discrepancy=0; failed=0; pending=0; in_progress=0`.
- Latest verified bounded-drain snapshot remains **961 verified / 162 blocked / 990 pending**, with **13 completed / 14 queued / 2 running** audit runs at the last production read. Enqueue remains paused.
- Do not auto-finalize genuinely nonterminal runs while their queue rows remain pending.
- Issue **#2142 remains closed as completed** after production proof of the stale-run repair.
- Catalogue enqueue remains intentionally paused; do not restore it until bounded drain capacity and broader production behavior are proven safe.
- Preserve legitimate Australian/regional/hardware variants. Never guess model identifiers/specifications or auto-delete/merge ambiguous production records.

### Guardian

- Latest verified production incident snapshot remains **28 resolved / 0 unresolved**.
- Continue generated-runtime/source-discovery diagnostics, including mapping `about:srcdoc` failures to actual generating sources and preventing stale runtime fallbacks.
- Guardian code-changing repair PRs remain human-approval gated.

### Security / performance

- Protected RPC least-privilege hardening #2154 remains production-verified.
- The known leaked-password/Auth decision plus remaining SECURITY DEFINER/RLS findings require reviewed, function-by-function decisions. Do not change Auth/RLS/EXECUTE policy automatically; issue #2033 remains the protected approval lane.
- Issue #2040 remains open for evidence-backed performance/index classification. The latest verified performance-advisor pass reported **70 unused-index INFO findings**.
- Representative zero-scan Guardian/support/inventory/sales indexes already classified as tiny and foreign-key aligned remain **preserve unless workload evidence proves otherwise**. No speculative index drop is authorised.
- One approved redundant Nova revision index has already been removed and production-verified; the remaining advisor findings require equivalent workload/query evidence before any further protected DDL.

### Active production cron jobs

- `buys-privacy-retention-daily` — `17 3 * * *`
- `morley-google-drive-backup-daily` — `0 19 * * *`
- `morley-recovery-health-hourly` — `17 * * * *`
- `nova-catalog-audit-worker-hourly` — `37 * * * *`
- `nova-knowledge-maintenance-every-5-minutes` — `*/5 * * * *`

Catalogue enqueue remains intentionally paused.

## Active repository work

| PR / issue | Lane | State | Next safe action |
| --- | --- | --- | --- |
| #2208 | Nova Next completion + isolated OTA/PWA updates | **Merged / exact-head green** | Keep production Nova authoritative unless a separately approved route/signing/OTA promotion is deliberately performed. |
| #2251 | Morley 2.15.107 OTA promotion | **Merged / published** | Treat 2.15.107 / 151 as current OTA baseline; monitor normal release health and do not duplicate the superseded #2233 path. |
| #2233 | Bot-created Morley 2.15.107 OTA metadata | **Closed unmerged / superseded** | Do not revive; #2251 is the authoritative protected promotion. |
| #1947 | Staged GitLab migration parity | Draft / paused | Keep GitHub `main` canonical. Do not consume more GitLab CI minutes or cut over until explicitly resumed. |
| #2033 | Supabase security advisor | Open / protected | Keep verified RPC hardening; leaked-password/Auth and remaining protected RLS/EXECUTE findings require explicit approval. |
| #2040 | Database performance advisor | Open | Continue evidence-backed query/index mapping; no speculative removals. |
| #431 | Full feature validation | Open | Continue current-main regression auditing; keep physical camera/touch/orientation checks MANUAL-DEVICE unless actually observed. |

## Completed material transitions

- Morley Buys **2.15.107 / 151** is released and published through OTA with immutable release-asset/checksum parity verified.
- Scanner UI lives directly in checked-in Kotlin source rather than a build-time rewriter.
- Scanner safe-area and blocked-verification UX #2197 is merged and published via #2251.
- Superseded scanner PR #2204 and bot OTA PR #2233 are closed unmerged.
- Adaptive Android navigation and catalogue-first intent remain merged.
- Admin web native-authority parity, full-catalogue pagination and mobile workspace observer freeze mitigation remain merged/verified.
- Full-system Google Drive OAuth repair and protected manual backup/recovery verification completed; the restored daily scheduler is production-proven by a scheduler-triggered, recovery-verified 19:00 UTC backup.
- Nova maintenance scheduler/transient-source handling remains healthy.
- Catalogue audit consumer and independent stale-run reconciliation are deployed; #2142 is production-proven and closed.
- Protected Supabase RPC least-privilege hardening remains live and verified.
- Nova Next #2208 is merged after exact-head validation; production Nova remains authoritative until any separate route/signing/OTA promotion is explicitly approved and verified.

## High-priority unfinished lanes

1. Continue post-release health verification for Morley 2.15.107 / 151 without creating duplicate release branches.
2. Keep Nova Next production route replacement, production signing and OTA publication separately protected; merge of #2208 is not itself a production cutover.
3. Continue bounded catalogue-audit drain verification before restoring any enqueue schedule.
4. Continue Nova knowledge/evaluation work and validate answer quality, source quality, latency/cost and tool routing with evidence.
5. Continue Admin web/mobile parity, mobile-browser usability, freeze/accessibility and role-boundary auditing.
6. Continue Morley Buys login/temp-password reliability, blue visual consistency and app/web parity with regression coverage.
7. Continue manufacturer-first Australian catalogue enrichment without coupling descriptive data to valuation feeds.
8. Treat the separate stale per-user encrypted Drive backup as a user-authenticated lane; investigate safe scheduling UX/design without storing or reusing production Google tokens automatically.
9. Continue read-only security/performance classification; keep the GitLab migration paused until intentionally resumed.

## Protected blockers requiring Beau action

- Any Nova Next production route cutover, production signing or OTA publication remains separately protected even though #2208 is merged.
- Auth configuration changes from #2033, including leaked-password protection or future Auth/RLS/EXECUTE changes outside already-approved scope, require explicit approval.
- Production restore/overwrite, OAuth/secret rotation, protected schema/security mutation, signing credential changes, repository visibility, billing, Guardian code-changing repair or equivalent high-risk action requires explicit approval.
- Any attempt to make per-user encrypted Drive backups unattended by retaining/refreshing user Google credentials is a credential/security design change and must not be performed implicitly.

## Reconciliation checklist

1. Confirm live protected `main` SHA.
2. Read current open PRs/issues and reuse existing work.
3. Check exact-head CI state for active high-priority PRs.
4. Check catalogue queue/run counts and ensure runaway enqueueing has not resumed.
5. Check Nova maintenance cadence plus ready/pending/error evidence and Nova Next evaluation state.
6. Check full-system backup scheduler, latest audit event and recovery verification independently from per-user Drive findings.
7. Check serious Guardian/recovery/security findings.
8. Resume the highest-impact already-authorised safe lane that is not blocked.
9. Update this ledger after material state transitions.

## Exclusion

**Gumtree is excluded from all automated work.** Do not inspect, reconcile, clean up, delete, or resume Gumtree work unless Beau explicitly re-enables it.
