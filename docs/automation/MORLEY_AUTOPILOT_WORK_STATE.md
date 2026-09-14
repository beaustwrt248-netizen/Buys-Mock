# Morley Autopilot Work-State Ledger

Last reconciled: 2026-09-14 15:15 AWST
Canonical repository baseline: the current protected `main` branch.
Last observed `main` head before this ledger reconciliation: `6418bcaaa1ce40dcc8bf840eac510b0ffb901065`.

This is the durable, non-sensitive state ledger for the consolidated Morley ecosystem automation. Reconcile it against live repository and connected production services before each automated work pass. The observed SHA is informational only because a ledger-only merge advances `main`; protected `main` remains canonical.

## Operating rules

- Resume unfinished work before creating duplicate branches or pull requests.
- Gumtree is excluded unless Beau explicitly re-enables it.
- Prefer small coherent branches from current `main` and preserve repository protections.
- Continue independent safe lanes while CI, review, credentials, or external services block another lane.
- Do not autonomously merge/apply protected changes involving Auth/RLS/authorization, secrets/credentials, destructive production data, privileged roles/permissions, protected pricing approval policy, GitHub workflow/repository security, signing/release credentials, Guardian approval policy, repository visibility, billing, or equivalent high-risk boundaries.
- Guardian code-changing repair PRs remain human-approval gated.
- Do not mark work complete until applicable tests/contracts, parity/security checks, deployment/release evidence, and rollback implications are verified.

## Current production safety state

### Release / OTA

- Morley Buys **2.15.105** is published and is the current OTA identity: `versionCode 149`, release tag `v2.15.105`.
- Release asset `B-and-L-Morley-2.15.105.apk` is present and the GitHub release digest matches OTA metadata SHA-256 `609722093702223cfd1261734dfce7abce77dad1db30c4d403c938b7b9ede590`.
- Release target is source commit `392a76d859e80332b0ad8fe8bf2ea69591589a80` from PR #2055; OTA metadata publication subsequently advanced `main` through PR #2067.
- PR #2055 is complete: adaptive Android navigation now follows the approved primary destination contract `Home / Catalogue / Scan / Trade`, with More in the hamburger path and Stock secondary.

### Backup / recovery

- Google Drive OAuth refresh-token rotation is proven healthy without exposing credential material.
- A protected Vault-backed full-system invocation of `google-drive-backup` succeeded at **2026-09-14 05:59:39 UTC** and created `morley-backup-2026-09-14T05-59-36-046Z.json`.
- The backup exported **13 tables**, re-downloaded the uploaded object, and verified SHA-256 `ed5295d8be1ac429d12fdbfd528d3b1f1ceba61e095850cdea62a3358a567841` over **366,840 bytes**; retention scanned 13 backups and trashed 0.
- `admin_audit_log` independently records `google_drive_backup_created` with `recovery_test.verified=true` for the same digest.
- Protected PR **#2064** was explicitly approved, squash-merged as `6418bcaaa1ce40dcc8bf840eac510b0ffb901065`, and its repository-owned scheduler migration was applied successfully to production.
- Production now contains exactly one active `morley-google-drive-backup-daily` job (jobid 19) at `0 19 * * *`, using the existing Vault-backed `morley_backup_scheduler_secret`, `google-drive-backup`, and a 120-second HTTP timeout.
- Issue **#2051** remains open only until the restored scheduler produces its first genuine automatic 19:00 UTC full-system backup with upload/re-download digest verification. Do not substitute the earlier manual invocation for that automatic-run proof.
- The open `stale_backup` recovery finding belongs to a **separate per-user encrypted Drive-backup subsystem** (`user_drive_backups` / `user-google-drive-backup`), not the full-system scheduler. Dedicated issue **#2070** tracks that distinction and its freshness semantics.
- The per-user browser client keeps its Google access token in memory only and performs change/daily automatic backups only while the active browser session remains Google-connected. Do not weaken token handling or persist raw Google access tokens to silence the 36-hour warning.

### Nova knowledge

- The recurring knowledge-maintenance outage was repaired and issue **#2049 is closed as completed**.
- Production has exactly one active `nova-knowledge-maintenance-every-5-minutes` cron job at `*/5 * * * *`, Vault-backed and bounded to four embeddings plus four ingestion documents per invocation.
- PR #2050 records the canonical scheduler reconciliation and regression contract in repository source.
- Continue measuring ready/pending/error movement and evaluation quality; raw corpus growth is not proof of better answers.

### Catalogue / data integrity

- Last durable queue snapshot: **961 verified / 70 blocked / 1,082 pending**; last durable live device snapshot: **1,776 active devices**.
- The enqueue-only catalogue cron remains paused because no verified live consumer drains the queue. Do not re-enable until a tracked consumer with safe claim/retry/finalisation behavior is implemented and verified.
- Preserve unresolved model-number gaps and shared-model-number groups as unresolved/classification work; never guess identifiers/specifications or auto-merge ambiguous production records.

### Guardian

- Last durable incident snapshot: **28 resolved / 0 unresolved**.
- Continue source-discovery/runtime diagnostics, but Guardian code-changing repair PRs remain human-approval gated.

### Security / performance

- Issue **#2033** tracks Supabase security-advisor least-privilege review. Current known advisor classes include leaked-password protection disabled, authenticated-executable `SECURITY DEFINER` functions, and RLS-enabled tables with no policies. Do not change protected Auth/RLS/EXECUTE/SECURITY DEFINER behavior without explicit approval.
- Issue **#2040** tracks database performance-advisor findings. Preserve integrity-bearing indexes and require dependency/rollback evidence before DDL cleanup.

### Active production cron jobs

- `buys-privacy-retention-daily`
- `morley-google-drive-backup-daily`
- `morley-recovery-health-hourly`
- `nova-knowledge-maintenance-every-5-minutes`

Catalogue enqueue remains intentionally paused until a verified consumer exists.

## Active repository work

| PR / issue | Lane | State | Safety / next action |
| --- | --- | --- | --- |
| #2051 | Full-system Google Drive backup recovery | Scheduler restored / awaiting automatic proof | Observe the first true automatic 19:00 UTC full-system run; require HTTP/upload success plus re-download SHA-256 verification before closing. |
| #2070 | Per-user encrypted Drive backup freshness | Open / evidence established | Keep distinct from full-system health. Decide active-session-only versus unattended per-user backup semantics before any OAuth/Auth/RLS design change. |
| #2033 | Supabase security advisor | Open | Continue read-only least-privilege evidence; protected Auth/RLS/EXECUTE/SECURITY DEFINER mutations require explicit approval. |
| #2040 | Database performance advisor | Open | Classify unused/redundant indexes conservatively; require dependency and rollback evidence before DDL cleanup. |
| #1947 | Staged GitLab migration parity | Draft | GitHub `main` remains canonical until same-SHA parity, protections, rollback and final cutover checks are proven. |

## Completed material transitions

- Durable non-sensitive autopilot ledger and reconciliation workflow established.
- Nova recurring maintenance scheduler restored and issue #2049 closed after canonical automatic execution evidence.
- Google Drive OAuth refresh token rotated successfully and full-system manual backup passed built-in download/SHA-256 recovery verification.
- Full-system Google Drive daily scheduler restored from repository-owned source through approved PR #2064 and verified active in production.
- PR #2055 merged and Morley **2.15.105 / versionCode 149** was built, released and published through OTA with matching APK digest.
- Morley Vision requires explicit staff-confirmed condition before authoritative pricing/stock handoff; AI condition remains advisory.
- Shared model-number catalogue governance remains non-destructive and manufacturer-evidence driven.

## High-priority unfinished lanes

1. Production-first reliability: login/temp-password freezes, Admin access, catalogue/sync integrity, Guardian/runtime errors, release failures, Nova availability, backup health, and serious security/privacy regressions.
2. Backup recurrence evidence: verify the restored full-system scheduler's first automatic 19:00 UTC run, while tracking per-user encrypted backup freshness separately in #2070.
3. Catalogue processor recovery: implement a tracked consumer for the audit queue with safe claiming, retries, evidence recording, unresolved blocking, and run finalisation before re-enabling enqueue cron.
4. Nova Next rebuild and knowledge expansion: preserve production Nova until parity/evaluation gates prove replacement readiness.
5. Admin web/mobile parity and Morley Buys login/UI reliability: continue regression-tested, non-protected fixes from current `main`.
6. Catalogue/device intelligence: continue verified Australian manufacturer-first enrichment without coupling descriptive data to live valuation feeds.
7. Security/performance hardening: continue read-only classification and prepare narrow proposals without crossing protected Auth/RLS/schema/security boundaries.
8. GitLab staged migration: continue parity/protection validation without changing the production baseline or creating competing `main` histories.

## Protected blockers requiring Beau action

- Security configuration changes arising from #2033, including leaked-password protection or EXECUTE/RLS/`SECURITY DEFINER` changes, require explicit approval.
- Any durable unattended per-user Google Drive credential design arising from #2070 is a protected OAuth/Auth boundary and requires explicit approval before implementation/deployment.
- Any production restore/overwrite, protected RLS/schema/security mutation, signing credential, repository visibility, billing, Guardian code-changing repair, or equivalent high-risk action requires explicit approval.

## Reconciliation checklist for each run

1. Confirm live `main` SHA and compare it with this observation; protected live `main` is canonical.
2. Read open PRs/issues and reuse existing branches instead of duplicating work.
3. Check exact-head CI state for active high-priority PRs.
4. Check catalogue queue/run counts and ensure runaway enqueueing has not resumed.
5. Check Nova maintenance cadence and ready/pending/error evidence.
6. Check full-system backup audit freshness and scheduler presence independently from per-user encrypted Drive-backup findings.
7. Check serious open recovery/Guardian/security findings.
8. Resume the highest-impact already-authorised safe lane that is not blocked.
9. Update this ledger whenever a material state transition occurs.

## Exclusion

**Gumtree is excluded from all automated work.** Do not inspect, reconcile, clean up, delete, or resume Gumtree work unless Beau explicitly re-enables it.
