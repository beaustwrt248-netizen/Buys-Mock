# Morley Autopilot Work-State Ledger

Last reconciled: 2026-09-14 17:04 AWST
Canonical repository baseline: protected `main`.
Observed `main` head: `a5e49957fa35a39e0d613107449e7fd65a6ba200`.

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
- PR #2055 is complete: adaptive Android navigation follows `Home / Catalogue / Scan / Trade`, with More in the hamburger path and Stock secondary.

### Web catalogue presentation

- PR **#2081** merged as `0360709b356eb9b9498291f24d74e7c4654258fd` after exact-head security, quality, parity, UI consistency and checklist gates were green.
- Tablets and Smartwatches now have dedicated catalogue sections and group legitimate connectivity/storage variants without deleting or rewriting source catalogue rows or pricing data.
- Visible Storage/Connectivity selections resolve to existing authoritative source rows; impossible synthetic combinations are not created.

### Backup / recovery

- Google Drive OAuth refresh-token rotation and manual full-system recovery verification remain healthy.
- The last verified full-system backup audit event is **2026-09-14 05:59:39 UTC**, exporting 13 tables and passing upload/re-download SHA-256 verification (`ed5295d8be1ac429d12fdbfd528d3b1f1ceba61e095850cdea62a3358a567841`, 366,840 bytes).
- PR **#2064** is merged and its approved migration is applied in production.
- Production `cron.job` contains exactly one active `morley-google-drive-backup-daily` at `0 19 * * *`, using the existing Vault-backed scheduler secret.
- The restored cron has not yet reached its first post-repair 19:00 UTC execution window; keep issue #2051 open until a new scheduled audit event proves the automatic path end-to-end with recovery verification.
- The open `stale_backup` finding belongs to the separate per-user encrypted Drive backup subsystem and is tracked by issue **#2070**; do not conflate it with full-system backup health.

### Nova knowledge

- Production has one active `nova-knowledge-maintenance-every-5-minutes` cron at `*/5 * * * *`.
- Current chunk embedding snapshot: **312 ready / 4,404 pending / 0 error rows observed**.
- Continue measuring evaluation quality, unsupported claims, citation/source quality, latency/provider failures and benchmark drift; raw corpus growth alone is not proof of better answers.

### Catalogue / data integrity

- Current audit queue snapshot: **961 verified / 70 blocked / 1,082 pending**.
- Audit-run snapshot: **12 completed / 16 queued / 1 running**. Recent scheduled runs remain unfinished with 75 scanned and 0 verified.
- Current active device count remains **1,776**.
- Read-only production inspection confirms the only database function referencing `nova_catalog_audit_queue` is the enqueue function, and the active Edge Function inventory has no dedicated catalogue-audit queue consumer.
- Issue **#2093** tracks the missing safe claim/process/finalise worker. Catalogue enqueue remains paused; do not re-enable it until a tested consumer with bounded claiming, retry/evidence recording and run finalisation is verified.
- Preserve unresolved model-number gaps and shared-model groups as unresolved classification work; never guess identifiers/specifications or auto-merge ambiguous production records.

### Guardian

- Current production incident snapshot: **28 resolved / 0 unresolved**.
- Continue source-discovery/runtime diagnostics. Guardian code-changing repair PRs remain human-approval gated.

### Admin web/mobile parity

- PR #2072 merged: Morley Admin web Refresh reloads live data instead of presenting stale refresh behavior.
- PR **#2080** merged as `a5e49957fa35a39e0d613107449e7fd65a6ba200` after the native-authority refactor reached an all-green exact-head gate set and the protected merge was approved.
- Authenticated Admin web/mobile now uses `admin-native-parity-core.js` plus a dedicated read-only catalogue owner rather than executing the broader legacy `app.js`, pricing, release-writer and control-governance owners.
- Catalogue and Staff alerts are read-only; Release is read-only; Safe Controls owns only maintenance/message/Admin OTA state; user access omits legacy delete/force-signout/display-name editing while retaining native-equivalent role, enable/disable, invite and password operations; manual notifications retain audience/user targeting without installation/device targeting.
- Auth/RLS/schema, Guardian authority, pricing formulas, native Android behavior and APK release identity were not changed by #2080.
- Exact-head checks were green before merge, including Repository Security Audit, B&L Morley Quality Gate, Full Feature Contract Audit, Admin OTA Release Safety, Admin Support Governance, Admin Device Governance, Admin Control Integration Audit, Admin Android Least-Privilege Gate, Morley Ultimate Parity Gate, Recovery Backup Contract, Web Release Smoke Checks, Morley Email Contract and UI PR Checklist Gate. Post-merge workflows observed for the merge SHA show no failed runs.

### Security / performance

- Issue **#2033** tracks Supabase security-advisor least-privilege review. Protected Auth/RLS/EXECUTE/SECURITY DEFINER changes require explicit approval.
- Issue **#2040** tracks performance-advisor findings. Preserve integrity-bearing indexes and require dependency/rollback evidence before DDL cleanup.

### Active production cron jobs

- `buys-privacy-retention-daily` — `17 3 * * *`
- `morley-google-drive-backup-daily` — `0 19 * * *`
- `morley-recovery-health-hourly` — `17 * * * *`
- `nova-knowledge-maintenance-every-5-minutes` — `*/5 * * * *`

Catalogue enqueue remains intentionally paused.

## Active repository work

| PR / issue | Lane | State | Next safe action |
| --- | --- | --- | --- |
| #2051 | Full-system Drive backup recovery | Scheduler restored; automatic proof pending | After the first post-repair 19:00 UTC run, require a new scheduler-triggered backup audit row with verified re-download digest before closing. |
| #2093 | Catalogue audit consumer | Root cause confirmed | Implement and test a bounded claim/process/finalise consumer before re-enabling enqueue. |
| #2070 | Per-user encrypted Drive freshness semantics | Open | Keep separate from full-system backup; define active-session vs unattended freshness semantics before implementation. |
| #2033 | Supabase security advisor | Open / protected | Continue read-only least-privilege evidence. |
| #2040 | Database performance advisor | Open | Continue conservative classification; no speculative index removal. |
| #1947 | Staged GitLab migration parity | Draft | GitHub `main` remains canonical until same-SHA parity, protections, rollback and final cutover checks are proven. |

## Completed material transitions

- Durable non-sensitive autopilot ledger and reconciliation workflow established.
- Nova recurring maintenance scheduler restored.
- Full-system Google Drive OAuth repaired; recovery verification passed; daily scheduler restored in production.
- Morley Buys 2.15.105 / 149 released through OTA with matching digest.
- Adaptive Android navigation repair merged.
- Admin web live Refresh parity repair merged.
- Admin web/mobile native-authority parity hardened and merged through #2080 with legacy browser-only authority removed from the authenticated parity loader.
- Tablet and Smartwatch dedicated catalogue grouping merged through #2081 without source-row or pricing mutation.
- Catalogue audit queue non-drainage was traced to the absence of a verified consumer and captured in #2093; runaway enqueue remains prevented.
- Morley Vision keeps AI condition advisory until explicit staff-confirmed condition handoff.
- Shared model-number governance remains non-destructive and evidence-driven.

## High-priority unfinished lanes

1. Production reliability: login/temp-password freezes, Admin access/state, catalogue/sync integrity, Guardian/runtime errors, release/deployment failures, Nova availability/quality, backup health and serious security/privacy regressions.
2. Verify the first automatic full-system backup after scheduler restoration.
3. Implement and verify the #2093 catalogue-audit consumer before any enqueue cron is restored.
4. Continue Admin web/mobile functional parity and mobile-browser usability now that native authority is aligned.
5. Continue Nova Next replacement and knowledge evaluation while preserving production Nova until parity/evaluation gates pass.
6. Continue Morley Buys UI/login reliability and app/web parity with regression coverage.
7. Continue manufacturer-first Australian catalogue enrichment without coupling descriptive data to valuation feeds.
8. Continue read-only security/performance classification and GitLab staged migration validation.

## Protected blockers requiring Beau action

- Security configuration changes arising from #2033, including Auth/RLS/EXECUTE/SECURITY DEFINER changes, require explicit approval.
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
