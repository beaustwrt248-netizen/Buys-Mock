# Morley Autopilot Work-State Ledger

Last reconciled: 2026-09-15 01:18 AWST
Canonical repository baseline: protected `main`.
Observed `main` head: `6ccd68b4a03c1bcacaa9af4ee24ad60ec06d21bb`.

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

- Morley Buys **2.15.106 / versionCode 150** remains the current published OTA baseline; `ota/latest.json` still points to the signed `v2.15.106` APK and checksum.
- The approved Scan Device redesign from #2165 remains in checked-in Kotlin source. Build-time scanner source rewriting is not an acceptable implementation path.
- PR **#2197** merged to protected `main` as `6ccd68b4a03c1bcacaa9af4ee24ad60ec06d21bb` after exact-head Android validation, APK build, UI, OTA-policy, quality, security, feature, recovery and parity checks passed. Main source identity is now **2.15.107 / versionCode 151**.
- **2.15.107 is not yet a published OTA release.** Do not call it released until exact-main signed artifact identity/checksum, OTA metadata/feed and post-publication health are verified through the protected release flow.
- PR **#2204** is a competing scanner fix that introduced a one-shot branch-writing workflow/source materializer. Treat it as superseded; do not merge the workflow/source-rewriter approach.

### Admin web / mobile web

- Authenticated Admin web/mobile continues to use dedicated parity/read-only owners instead of broad legacy browser authority.
- Full catalogue pagination repair #2134 and mobile workspace observer coalescing #2162 are merged and previously verified through Pages/post-deploy smoke/quality/parity checks.
- Admin web remains published from this repository through GitHub Pages at `buyshub.me`, with Cloudflare in front. Vercel is not a Morley deployment dependency.
- Continue responsive parity, mobile clipping/freeze, stale-state, accessibility and role-boundary audits. Do not bypass Cloudflare or weaken Admin/Manager/Staff separation.

### Backup / recovery

- Google Drive OAuth repair and protected manual full-system recovery verification are complete; issue #2051 is closed.
- The last verified full-system backup audit event remains **2026-09-14 05:59:39 UTC**, exporting 13 tables and passing upload/re-download SHA-256 verification (`ed5295d8be1ac429d12fdbfd528d3b1f1ceba61e095850cdea62a3358a567841`, 366,840 bytes).
- Production `morley-google-drive-backup-daily` remains scheduled for `0 19 * * *`.
- The restored daily schedule has not yet reached its next **19:00 UTC** execution window. Require a new scheduler-triggered audit row plus recovery/readback verification before calling unattended scheduling fully proven.
- Production restore/overwrite remains protected and requires explicit approval.

### Nova knowledge / Nova Next

- `nova-knowledge-maintenance` continues healthy: the latest six observed production runs through **2026-09-14 17:10 UTC** all completed with `embedded_ready=4`, `embedded_error=0`, `ingested_error=0` and no error summary.
- Current production Nova remains authoritative while Nova Next is rebuilt and parity/evaluation evidence accumulates.
- Draft PR **#2208** is the active Nova Next completion/OTA lane. Its TDD completion contract exposed missing PWA and Android launcher identity. The branch now contains standard/maskable PWA orb icons, Android adaptive launcher resources, `REQUEST_INSTALL_PACKAGES`, and isolated Nova OTA build configuration. Exact-head isolated validation/parity is rerunning.
- Because #2208 includes release/OTA surface changes, keep protected publication/signing boundaries intact and do not promote until exact-head parity/evaluation/release checks pass and protected approval is obtained.
- Draft PR **#2207** is documentation-only canonical Nova Next status alignment.
- Continue benchmark-based knowledge expansion, citation/source quality, unsupported-claim tracking, provider/fallback quality, latency/cost controls and tool-routing evaluation; corpus growth alone is not proof of answer quality.

### Catalogue audit / data integrity

- The stale-run reconciliation repair is production-proven. Run `cc3cb62f-c42f-46e1-ae81-ea8adb937bea` is `completed`, finished `2026-09-14 15:10:58 UTC`, with terminal notes `verified=13; blocked=62; discrepancy=0; failed=0; pending=0; in_progress=0`.
- Genuine nonterminal run `25b34a8c-3a85-48db-bc57-1bf386a72f08` correctly remains `running`, with `verified=0; blocked=47; discrepancy=0; failed=0; pending=28; in_progress=0` at reconciliation.
- Issue **#2142 is closed as completed** after this production proof.
- Current queue snapshot: **961 verified / 109 blocked / 1,043 pending**. Current audit-run snapshot: **13 completed / 15 queued / 1 running**.
- Catalogue enqueue remains intentionally paused; do not restore it until bounded drain capacity and broader production behavior are proven safe.
- Preserve legitimate Australian/regional/hardware variants. Never guess model identifiers/specifications or auto-delete/merge ambiguous production records.

### Guardian

- Current production incident snapshot: **28 resolved / 0 unresolved**.
- Continue generated-runtime/source-discovery diagnostics, including mapping `about:srcdoc` failures to actual generating sources and preventing stale runtime fallbacks.
- Guardian code-changing repair PRs remain human-approval gated.

### Security / performance

- Protected RPC least-privilege hardening #2154 remains production-verified.
- Latest Supabase security advisor still reports the protected leaked-password/Auth decision plus SECURITY DEFINER/RLS findings requiring review. Do not change Auth/RLS/EXECUTE policy automatically; issue #2033 remains the approval lane.
- Issue #2040 remains open for evidence-backed performance/index classification; do not speculatively drop ambiguous indexes.

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
| #2197 | Morley Buys scanner safe-area / blocked-verification UX | **Merged to main**, source 2.15.107/151 | Keep OTA publication separate/protected. Verify exact-main signed artifact/checksum/feed before any release-complete claim. |
| #2204 | Duplicate scanner fix | Draft / superseded workflow-source-rewriter approach | Close as superseded once no missing behavior remains; never merge the source-rewriter path. |
| #2208 | Nova Next completion + isolated OTA/PWA updates | Draft / active / protected release surface | Let exact-head isolated validation/parity finish; fix only evidence-backed failures. Preserve production Nova and do not publish protected OTA/signing changes autonomously. |
| #2207 | Nova Next canonical status documentation | Draft / docs | Reconcile against live #2208/current-main state; merge only if accurate and checks pass. |
| #1947 | Staged GitLab migration parity | Draft / paused | Keep GitHub `main` canonical. Do not consume more GitLab CI minutes or cut over until explicitly resumed. |
| #2033 | Supabase security advisor | Open / protected | Keep verified RPC hardening; leaked-password/Auth and remaining protected RLS/EXECUTE findings require explicit approval. |
| #2040 | Database performance advisor | Open | Continue evidence-backed query/index mapping; no speculative removals. |
| #431 | Full feature validation | Open | Continue current-main regression auditing; keep physical camera/touch/orientation checks MANUAL-DEVICE unless actually observed. |

## Completed material transitions

- Morley Buys 2.15.106 / 150 released and published through OTA after the deterministic Scan Device redesign.
- Scanner UI now lives directly in checked-in Kotlin source rather than a build-time rewriter.
- Scanner safe-area and blocked-verification UX #2197 merged to main as source version 2.15.107 / 151; OTA publication remains separate and unverified.
- Adaptive Android navigation and catalogue-first intent remain merged.
- Admin web native-authority parity, full-catalogue pagination and mobile workspace observer freeze mitigation remain merged/verified.
- Full-system Google Drive OAuth repair and protected manual backup/recovery verification completed; daily scheduler restored, unattended next-run proof still pending.
- Nova maintenance scheduler/transient-source handling remains healthy.
- Catalogue audit consumer and independent stale-run reconciliation are deployed; #2142 is production-proven and closed.
- Protected Supabase RPC least-privilege hardening remains live and verified.
- Nova Next replacement work is active while production Nova remains preserved.

## High-priority unfinished lanes

1. Keep 2.15.107 OTA publication protected and verify exact-main signing/checksum/feed evidence before release completion.
2. Close superseded scanner PR #2204 after confirming #2197 contains all intended safe behavior.
3. Verify the first unattended full-system backup after the restored 19:00 UTC scheduler execution.
4. Continue bounded catalogue-audit drain verification before restoring any enqueue schedule.
5. Continue Nova Next replacement (#2208) with parity/evaluation gates while preserving production Nova.
6. Continue Admin web/mobile parity, mobile-browser usability, freeze/accessibility and role-boundary auditing.
7. Continue Morley Buys login/temp-password reliability, blue visual consistency and app/web parity with regression coverage.
8. Continue manufacturer-first Australian catalogue enrichment without coupling descriptive data to valuation feeds.
9. Continue read-only security/performance classification; keep the GitLab migration paused until intentionally resumed.

## Protected blockers requiring Beau action

- **Publishing Morley Buys 2.15.107 / 151 through OTA/signing remains a protected release action. Source is merged, but publication is not complete.**
- Auth configuration changes from #2033, including leaked-password protection or future Auth/RLS/EXECUTE changes outside already-approved scope, require explicit approval.
- Any protected release/OTA/signing publication step for #2208 requires exact-main artifact/signing/checksum/OTA evidence and explicit approval where repository governance requires it.
- Production restore/overwrite, OAuth/secret rotation, protected schema/security mutation, signing credential, repository visibility, billing, Guardian code-changing repair or equivalent high-risk action requires explicit approval.

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