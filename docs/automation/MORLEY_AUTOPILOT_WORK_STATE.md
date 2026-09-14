# Morley Autopilot Work-State Ledger

Last reconciled: 2026-09-14 23:55 AWST
Canonical repository baseline: protected `main`.
Observed `main` head: `af0449e0967f9781cedcfd150957ed86f0197f9f`.

This is the durable, non-sensitive state ledger for the consolidated Morley ecosystem automation. Reconcile it against live repository and connected production services before each automated pass.

## Operating rules

- Resume unfinished work before creating duplicate branches or pull requests.
- Gumtree is excluded unless Beau explicitly re-enables it.
- Prefer small coherent branches from current `main`; never push directly to `main` or bypass protections.
- Continue independent safe lanes while CI, review, credentials or external services block another lane.
- Protected Auth/RLS/authorization, secrets/credentials, destructive production data, privileged roles/permissions, pricing approval policy, workflow/repository security, signing/release credentials, Guardian approval policy, repository visibility, billing and equivalent high-risk changes require explicit Beau approval.
- Guardian code-changing repair PRs remain human-approval gated.
- Do not call work complete until applicable tests/contracts, parity/security checks, deployment/release evidence and rollback implications are verified.

## Current production safety state

### Release / OTA

- Morley Buys **2.15.105 / versionCode 149** remains the current published OTA identity.
- Release asset digest and OTA metadata were previously verified equal at `609722093702223cfd1261734dfce7abce77dad1db30c4d403c938b7b9ede590`.
- Adaptive Android navigation follows `Home / Catalogue / Scan / Trade`, with More in the hamburger path and Stock secondary.
- Draft PR **#2165** is the approved Scan Device presentation refresh. It remains intentionally unmerged while the UI is moved into checked-in `DeviceLensActivity.kt`; build-time source rewriting is not acceptable. The dedicated source rewriter has been deleted and a deterministic-source guard now blocks its return.
- #2165 currently carries the exactly-next source identity **2.15.106 / versionCode 150** because the repository OTA policy requires a fresh exactly-next identity for Android source changes. This is not evidence of OTA publication or release completion.

### Admin web / mobile web

- Native-authority parity remains merged: authenticated Admin web/mobile uses the dedicated parity core and read-only catalogue/release owners instead of broad legacy browser authority.
- PR **#2134** fixes incomplete Admin catalogue loading by paging all active catalogue rows in deterministic order instead of relying on a single Supabase response page.
- #2134 included cache/deployment contract updates and regression coverage. Its merge commit `e53bbb542143e1c520ad8c38fdfb5a0b05957a8c` was published by GitHub Pages run **#626**; the deploy step and post-deploy smoke tests both passed.
- PR **#2162** merged as `af0449e0967f9781cedcfd150957ed86f0197f9f` and coalesces the mobile Admin workspace `MutationObserver` refresh path to a single animation-frame update, with regression coverage and cache-busting.
- Current-main GitHub Pages deployment, post-deploy smoke tests, B&L Morley Quality Gate and Morley Ultimate Parity Gate all passed for #2162.
- Admin web is published from this repository through GitHub Pages at the `buyshub.me` custom domain, with Cloudflare in front. Vercel is not a Morley deployment dependency.
- Cloudflare challenge behavior must not be bypassed. Require separate live deployment evidence before claiming future Admin web changes are active.

### Backup / recovery

- Google Drive OAuth refresh-token rotation and protected manual full-system recovery verification are complete; issue **#2051** is closed.
- The last verified full-system backup audit event remains **2026-09-14 05:59:39 UTC**, exporting 13 tables and passing upload/re-download SHA-256 verification (`ed5295d8be1ac429d12fdbfd528d3b1f1ceba61e095850cdea62a3358a567841`, 366,840 bytes).
- Production `cron.job` contains exactly one active `morley-google-drive-backup-daily` at `0 19 * * *`, using the existing Vault-backed scheduler secret.
- At this reconciliation time the restored daily cron had not yet reached its first post-repair **19:00 UTC** execution window. Require a new scheduler-triggered audit row plus recovery verification before treating unattended backup scheduling as fully proven.
- PR **#2137** merged as `1f40bef4e3d53bfe0352397c37623a183e32be7e`; its exact head passed the applicable Quality, Security, Full Feature, Recovery, Path Stability, Pricing, Catalogue and Ultimate Parity gates.
- `recovery-readiness` production version **7** separates connection-dependent per-user encrypted Drive freshness from global full-system recovery health.
- Issue **#2070 is closed as completed**. No durable unattended per-user OAuth-token storage was introduced.

### Nova knowledge maintenance

- PR **#2125** merged as `e63c6932c59d2e1bd327187fcf2c467297d16683` and `nova-knowledge-maintenance` production version **9** remains active.
- The latest eight observed runs from **15:15 through 15:50 UTC** all completed successfully with `embedded_ready=4`, `embedded_error=0`, `ingested_skipped=4`, `ingested_error=0` and no error summary.
- Current embedding snapshot: **636 ready / 4,080 pending / 0 observed error rows**.
- Issue **#2103** remains closed after post-deploy verification.
- Continue evaluation quality, unsupported-claim, citation/source, latency/provider-failure and benchmark-drift measurement; corpus growth alone is not proof of better answers.

### Catalogue audit / data integrity

- Issue **#2093** is complete: a bounded catalogue-audit claim/process/finalise consumer exists.
- PR **#2147** merged as `3a390f97d44f530e6b4d2d7050785764d053bb7b`; production `nova-catalog-audit` is version **4** and performs independent stale-run reconciliation.
- Issue **#2142** is closed after production verification.
- Current queue snapshot: **961 verified / 99 blocked / 1,053 pending**. Current audit-run snapshot: **13 completed / 15 queued / 1 running**; the running batch had **38 pending / 37 blocked** when inspected.
- `nova-catalog-audit-worker-hourly` remains active at `37 * * * *`; catalogue enqueue remains intentionally absent.
- Do not restore automatic enqueueing until drain capacity and broader production behavior are proven safe.
- Preserve unresolved model-number gaps and legitimate regional/hardware variants; never guess identifiers/specifications or auto-merge ambiguous production records.

### Guardian

- Current production incident snapshot: **28 resolved / 0 unresolved**.
- Continue generated-runtime/source-discovery diagnostics. Guardian code-changing repair PRs remain human-approval gated.

### Security / performance

- PR **#2154** merged as `25e3c6ede7f537f120d790a8405ce3a969c43add` after Beau explicitly approved the protected RPC hardening.
- Production verification shows all 13 reviewed SECURITY DEFINER RPCs deny anonymous execution while preserving intended authenticated execution, and `guardian_report_diagnostic` requires an enabled profile.
- The 17 RLS-enabled/no-policy internal tables still grant no anon/authenticated DML access. The advisor continues to report those INFO findings because the tables are intentionally server/internal.
- Issue **#2033** remains open only for the outstanding leaked-password-protection/Auth configuration decision and continuing least-privilege review; do not change paid plan/Auth settings without explicit approval.
- Fresh performance advisor state is **70 unused-index INFO findings** after the one proven redundant Nova revision index cleanup.
- Issue **#2040** remains open. Current read-only structural review found no additional exact duplicate public index definition that justifies a speculative DROP.

### CI / incident reconciliation

- Intermediate-commit cancellation issues generated while active branches move are not evidence of final-head regressions. Reconcile each against the latest exact PR head before changing production or closing a task.
- The active Scan Device PR #2165 is draft and must remain protected until its final checked-in source implementation and exact-head gates are complete.

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
| #2165 | Morley Buys Scan Device redesign | Draft / protected by build-release policy | Commit approved presentation directly to `DeviceLensActivity.kt`; no build-time source rewrite; preserve 2.15.106/150 only if current main/latest remain 2.15.105/149; rerun exact-head gates before any merge/release. |
| #1947 | Staged GitLab migration parity | Draft / paused | Keep GitHub `main` canonical. Do not consume more GitLab CI minutes or cut over until explicitly resumed. |
| #2033 | Supabase security advisor | Open / protected | Keep verified RPC hardening; leaked-password protection remains an Auth/plan decision requiring explicit approval. |
| #2040 | Database performance advisor | Open | Continue evidence-backed query/index mapping; 70 INFO findings remain and no speculative removals are authorised. |
| #431 | Full feature validation | Open | Continue current-main regression auditing; keep physical camera/touch/orientation checks MANUAL-DEVICE unless actually observed. |

## Completed material transitions

- Durable non-sensitive autopilot ledger and reconciliation workflow established.
- Morley Buys 2.15.105 / 149 released through OTA with matching digest.
- Adaptive Android navigation and catalogue-first intent merged.
- Admin web native-authority parity hardened and broad legacy browser authority removed from the authenticated parity loader.
- Admin full-catalogue pagination repair #2134 merged and GitHub Pages deployment/post-deploy smoke verified.
- Admin mobile workspace observer freeze mitigation #2162 merged and current-main Pages/quality/parity verification passed.
- Tablet and Smartwatch dedicated catalogue grouping merged without source-row or pricing mutation.
- Full-system Google Drive OAuth repaired; protected manual backup/recovery verification passed; daily scheduler restored.
- Recovery-readiness separates per-user encrypted Drive freshness from global backup health; issue #2070 is closed.
- Nova maintenance scheduler restored and transient source-boundary failure handling repaired/deployed; issue #2103 closed.
- Catalogue audit consumer and stale-run reconciliation implemented/deployed; #2142 closed after production verification.
- Protected Supabase RPC least-privilege hardening #2154 merged/applied with live ACL/profile-gate verification.
- One proven redundant Nova revision index removed while the required unique revision integrity index remains.
- Morley Vision keeps AI condition advisory until explicit staff-confirmed condition handoff.

## High-priority unfinished lanes

1. Finish #2165 Scan Device presentation as deterministic checked-in source; keep the dashboard unchanged and preserve inspection/staff/pricing/inventory authority.
2. Verify the first unattended full-system backup after the restored 19:00 UTC scheduler execution.
3. Continue bounded catalogue-audit drain verification before restoring any enqueue schedule.
4. Continue Admin web/mobile parity, mobile-browser usability and freeze/accessibility auditing after #2162.
5. Continue Nova Next replacement and knowledge evaluation while preserving production Nova until parity/evaluation gates pass.
6. Continue Morley Buys login/temp-password reliability, blue visual consistency and app/web parity with regression coverage.
7. Continue manufacturer-first Australian catalogue enrichment without coupling descriptive data to valuation feeds.
8. Continue read-only security/performance classification; keep the GitLab migration paused until its external blockers are intentionally resumed.

## Protected blockers requiring Beau action

- Auth configuration changes arising from #2033, including leaked-password protection or future Auth/RLS/EXECUTE changes outside the already-approved #2154 scope, require explicit approval.
- Any protected release/OTA/signing publication step for #2165 requires the normal exact-main artifact/signing/checksum/OTA evidence and explicit approval where repository governance requires it.
- Production restore/overwrite, OAuth/secret rotation, protected schema/security mutation, signing credential, repository visibility, billing, Guardian code-changing repair or equivalent high-risk action requires explicit approval.

## Reconciliation checklist

1. Confirm live protected `main` SHA.
2. Read current open PRs/issues and reuse existing work.
3. Check exact-head CI state for active high-priority PRs.
4. Check catalogue queue/run counts and ensure runaway enqueueing has not resumed.
5. Check Nova maintenance cadence plus ready/pending/error evidence.
6. Check full-system backup scheduler, latest audit event and recovery verification independently from per-user Drive findings.
7. Check serious Guardian/recovery/security findings.
8. Resume the highest-impact already-authorised safe lane that is not blocked.
9. Update this ledger after material state transitions.

## Exclusion

**Gumtree is excluded from all automated work.** Do not inspect, reconcile, clean up, delete, or resume Gumtree work unless Beau explicitly re-enables it.
