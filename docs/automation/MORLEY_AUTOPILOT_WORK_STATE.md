# Morley Autopilot Work-State Ledger

Last reconciled: 2026-09-14 23:10 AWST
Canonical repository baseline: protected `main`.
Observed `main` head: `3a390f97d44f530e6b4d2d7050785764d053bb7b`.

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

### Admin web / mobile web

- Native-authority parity remains merged: authenticated Admin web/mobile uses the dedicated parity core and read-only catalogue/release owners instead of broad legacy browser authority.
- PR **#2134** fixes incomplete Admin catalogue loading by paging all active catalogue rows in deterministic order instead of relying on a single Supabase response page.
- #2134 included cache/deployment contract updates and regression coverage. Treat source merge as proven; do not claim a production web deployment without separate deployment evidence.
- Connected Vercel scope currently returns no teams/projects, so production Admin web deployment cannot yet be independently verified through that connector.
- Continue mobile-browser layout/freezing/accessibility and native-app parity auditing from current main.

### Backup / recovery

- Google Drive OAuth refresh-token rotation and protected manual full-system recovery verification are complete; issue **#2051** is closed.
- The last verified full-system backup audit event remains **2026-09-14 05:59:39 UTC**, exporting 13 tables and passing upload/re-download SHA-256 verification (`ed5295d8be1ac429d12fdbfd528d3b1f1ceba61e095850cdea62a3358a567841`, 366,840 bytes).
- Production `cron.job` contains exactly one active `morley-google-drive-backup-daily` at `0 19 * * *`, using the existing Vault-backed scheduler secret.
- The restored daily cron has not yet reached its first post-repair 19:00 UTC execution window. Require a new scheduler-triggered audit row plus recovery verification before treating unattended backup scheduling as fully proven.
- PR **#2137** merged as `1f40bef4e3d53bfe0352397c37623a183e32be7e`; its exact head passed Quality, Security, Full Feature, Recovery Backup, Path Stability, Pricing Migration, Catalogue Classification and Ultimate Parity gates.
- The `recovery-readiness` Edge Function is active as production version **7** and separates `stale_backup` findings into connection-dependent per-user encrypted Drive freshness while keeping global full-system recovery findings distinct.
- The approved migration removed only redundant non-unique `idx_app_invites_email_unused`; required UNIQUE `app_invites_active_email_idx` remains present.
- Issue **#2070** remains open until active-session freshness semantics are fully reconciled end-to-end; no OAuth token persistence or unattended per-user credential model was introduced.

### Nova knowledge maintenance

- PR **#2125** merged as `e63c6932c59d2e1bd327187fcf2c467297d16683` and the `nova-knowledge-maintenance` Edge Function is active as production version **9**.
- The repair isolates clearly transient gateway/502/503/504/upstream source-query failures, records privacy-safe source-boundary diagnostics, preserves fail-closed behavior for auth/permission/schema/unknown errors, and continues independent embedding work instead of aborting the whole cycle.
- Twelve consecutive post-repair runs through 14:20 UTC were healthy; issue **#2103** is closed.
- Latest observed embedding snapshot before this reconciliation: **568 ready / 4,148 pending / 0 error**.
- Continue evaluation quality, unsupported-claim, citation/source, latency/provider-failure and benchmark-drift measurement; raw corpus growth alone is not proof of better answers.

### Catalogue audit / data integrity

- Issue **#2093** is complete: a bounded catalogue-audit claim/process/finalise consumer exists.
- PR **#2147** merged as `3a390f97d44f530e6b4d2d7050785764d053bb7b` after exact-head Security, Quality, Full Feature, Catalogue, Recovery, Path Stability and Ultimate Parity checks passed.
- Production `nova-catalog-audit` Edge Function is ACTIVE as version **3** with `verify_jwt=false` preserved for the existing maintenance-secret scheduler contract.
- Version 3 adds deterministic run-state calculation plus a bounded reconciliation sweep of up to 8 oldest `running` audit runs independently of the current claimed batch. It does not alter catalogue facts, pricing, enqueue cadence, Auth/RLS, Guardian authority or worker claim semantics.
- Current queue snapshot before post-deploy scheduled proof: **961 verified / 90 blocked / 1 in_progress / 1,061 pending**.
- Known stale run `cc3cb62f-c42f-46e1-ae81-ea8adb937bea` has **62 blocked + 13 verified** terminal queue rows but was still marked `running` before v3 deployment. Run `25b34a8c-3a85-48db-bc57-1bf386a72f08` remains legitimately non-terminal with pending work.
- Issue **#2142** remains open until a post-deploy worker invocation proves the stale terminal run is finalized while the genuinely non-terminal run remains running.
- `nova-catalog-audit-worker-hourly` remains active at `37 * * * *`; automatic enqueue remains intentionally absent.
- Preserve unresolved model-number gaps and legitimate regional/hardware variants; never guess identifiers/specifications or auto-merge ambiguous production records.

### Guardian

- Current production incident snapshot: **28 resolved / 0 unresolved**.
- Continue generated-runtime/source-discovery diagnostics. Guardian code-changing repair PRs remain human-approval gated.

### Security / performance

- Issue **#2033** tracks Supabase security-advisor least-privilege review. Protected Auth/RLS/EXECUTE/SECURITY DEFINER changes require explicit approval.
- Issue **#2040** tracks performance-advisor findings.
- PR **#2137** removed one proven redundant invite lookup index while preserving its UNIQUE integrity index.
- PR **#2143** subsequently merged an evidence-backed cleanup of the redundant Nova revision sort index while preserving the UNIQUE Nova revision identity index. No broader speculative index cleanup is authorised.

### CI incident reconciliation

- Autopilot cancellation issues **#2138** (Ultimate Parity) and **#2140** (B&L Quality) came from intermediate #2137 commits while the branch was moving.
- Final #2137 head passed both workflows plus remaining gates, so both incidents are closed without bypassing checks.

### Active production cron jobs

- `buys-privacy-retention-daily` — `17 3 * * *`
- `morley-google-drive-backup-daily` — `0 19 * * *`
- `morley-recovery-health-hourly` — `17 * * * *`
- `nova-knowledge-maintenance-every-5-minutes` — `*/5 * * * *`
- `nova-catalog-audit-worker-hourly` — `37 * * * *`

Catalogue enqueue remains intentionally paused.

## Active repository work

| PR / issue | Lane | State | Next safe action |
| --- | --- | --- | --- |
| #2142 | Catalogue audit stale run reconciliation | Merged/deployed; production proof pending | Observe the next post-deploy v3 worker invocation; verify stale terminal run finalizes and non-terminal run remains running, then close if clean. |
| #1947 | Staged GitLab migration parity | Draft | Keep GitHub `main` canonical; continue same-SHA parity/protection/rollback validation without cutover. |
| #2070 | Per-user encrypted Drive freshness semantics | Open / partially remediated | Verify consumers use separated global/user findings and decide whether activity-aware suppression is needed; unattended OAuth storage remains protected. |
| #2033 | Supabase security advisor | Open / protected | Continue read-only least-privilege inventory and prepare narrow proposals only; no Auth/RLS/EXECUTE mutation without Beau approval. |
| #2040 | Database performance advisor | Open | Continue evidence-backed query/index mapping after approved redundant-index cleanups; no speculative removals. |
| #431 | Full feature validation | Open | Continue current-main regression auditing; keep physical camera/touch/orientation checks marked MANUAL-DEVICE unless actually observed. |

## Completed material transitions

- Durable non-sensitive autopilot ledger and reconciliation workflow established.
- Morley Buys 2.15.105 / 149 released through OTA with matching digest.
- Adaptive Android navigation and catalogue-first intent merged.
- Admin web native-authority parity hardened and broad legacy browser authority removed from authenticated parity loader.
- Admin full-catalogue pagination repair merged through #2134 with regression coverage.
- Tablet and Smartwatch dedicated catalogue grouping merged without source-row or pricing mutation.
- Full-system Google Drive OAuth repaired; protected manual backup/recovery verification passed; daily scheduler restored.
- Recovery-readiness separates per-user encrypted Drive freshness from global backup health.
- Nova recurring maintenance scheduler restored and transient source-boundary handling repaired/deployed through #2125.
- Catalogue audit queue consumer implemented/deployed through #2093; stale-run reconciliation implemented/deployed through #2147; automatic enqueue remains paused pending broader production verification.
- Two evidence-backed redundant indexes have been removed while their corresponding UNIQUE integrity indexes remain preserved.
- Morley Vision keeps AI condition advisory until explicit staff-confirmed condition handoff.
- Shared model-number governance remains non-destructive and evidence-driven.

## High-priority unfinished lanes

1. Production-first reliability: login/temp-password freezes, Admin access/state, catalogue/sync integrity, Guardian/runtime errors, release/deployment failures, Nova availability/quality, backup health and serious security/privacy regressions.
2. Observe and verify the first post-deploy v3 catalogue worker reconciliation for issue #2142.
3. Verify the first automatic full-system backup after the restored 19:00 UTC scheduler execution.
4. Verify recovery-readiness consumers display per-user stale backup separately from global full-system recovery state.
5. Continue Admin web/mobile functional parity, mobile-browser usability and deployment verification from #2134.
6. Continue Nova Next replacement and knowledge evaluation while preserving production Nova until parity/evaluation gates pass.
7. Continue Morley Buys UI/login reliability and app/web parity with regression coverage.
8. Continue manufacturer-first Australian catalogue enrichment without coupling descriptive data to valuation feeds.
9. Continue read-only security/performance classification and staged GitLab migration validation.

## Protected blockers requiring Beau action

- Security configuration changes arising from #2033, including Auth/RLS/EXECUTE/SECURITY DEFINER changes, require explicit approval.
- Durable unattended per-user Google Drive OAuth/credential storage under #2070 requires explicit protected approval before implementation.
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
