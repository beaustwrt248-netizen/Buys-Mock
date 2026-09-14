# Morley Autopilot Work-State Ledger

Last reconciled: 2026-09-14 (Australia/Perth)
Canonical repository baseline: the current protected `main` branch.
Last observed `main` head before this ledger reconciliation: `d1c30743ae299273ac574421e0c1ca096990780a`.

This file is the durable, non-sensitive state ledger for the consolidated Morley ecosystem automation. It records work that is active, blocked, completed, paused for safety, or awaiting protected approval. Reconcile it against live repository and connected production services before each automated work pass.

The exact `main` SHA is an observation rather than a permanent baseline because merging a ledger update advances `main` itself. Protected `main`, not an old ledger SHA, remains canonical.

## Operating rules

- Resume unfinished work before creating duplicate branches or pull requests.
- Keep Gumtree work excluded unless Beau explicitly re-enables it.
- Prefer fresh branches from current `main` for coherent changes.
- Continue independent safe lanes when CI, review, credentials, or external services block another lane.
- Never push directly to `main` or bypass repository protections.
- Never auto-merge protected changes involving authentication/authorization/RLS, secrets/credentials, destructive production data, privileged roles/permissions, protected pricing approval policy, GitHub workflow/repository security, signing/release credentials, Guardian approval policy, repository visibility, billing, or equivalent high-risk boundaries.
- Guardian code-changing repair PRs remain human-approval gated.
- Do not mark a task done until applicable tests, contracts, parity/security checks, deployment/release verification, and rollback implications are satisfied.

## Current production safety state

### Catalogue audit pipeline

- Queue snapshot: **961 verified / 70 blocked / 1,082 pending**.
- Live data-integrity snapshot: **1,776 active devices**.
- The one-device reduction from the previous 1,777 snapshot remains traceable to Apple row `1197` (`A3461`) being deactivated while canonical Apple row `1202` remains active with the complete storage set. No production data mutation was performed by this reconciliation run.
- The enqueue-only catalogue cron remains paused because there is no verified live consumer draining the queue. Do not re-enable enqueueing until a tracked consumer is implemented and verified.
- Preserve unresolved model-number gaps and shared-model-number groups as unresolved/classification work rather than guessing identifiers or auto-merging records.

### Backup / recovery

- Last successful full-system Google Drive backup audit remains **2026-09-10 19:00 UTC**.
- Full-system backup freshness remains **stale** against the 36-hour readiness threshold.
- Protected manual invocation on 2026-09-14 reached the backup function successfully but failed in `google-auth` with the bounded Google response **`Token has been expired or revoked.`**
- Root cause is the configured `GOOGLE_DRIVE_OAUTH_REFRESH_TOKEN`; scheduler-secret authentication and backup export/recovery-verification code reached the expected path.
- Issue **#2051** tracks the approved credential rotation and required end-to-end recovery verification.
- The server backup scheduler remains fail-closed while the refresh token is invalid. Do not mask the stale finding or claim backup health before a new upload passes the built-in download/SHA-256 verification.
- Hourly recovery-health monitoring remains active.
- The connected Supabase tooling does not expose project Edge Function secret mutation, and Google OAuth refresh-token issuance requires interactive Google authorization. No credential material is stored in this ledger.

### Nova knowledge

- Baseline corpus remains approximately **4,715 active chunks / 4,708 active sources**; ingestion may add or update bounded records during maintenance.
- At the latest verified snapshot after scheduler restoration: **172 ready / 4,544 pending / 0 errors**.
- The recurring maintenance failure was an absent/stopped trigger, not a broken embedding engine.
- The approved emergency production scheduler was restored and automatically processed successful 4x4 batches at 03:50 and 03:55 UTC with zero embedding errors.
- PR **#2050** merged as `d1c30743ae299273ac574421e0c1ca096990780a`, recording the emergency migration and adding a canonical idempotent scheduler reconciliation + regression contract.
- Production now has exactly one canonical active job named `nova-knowledge-maintenance-every-5-minutes`, scheduled `*/5 * * * *`, Vault-backed by `morley_backup_scheduler_secret`, bounded to 4 embeddings and 4 ingestion documents per invocation.
- Keep issue **#2049** open until an automatic run under the canonical job identity is verified; then close it as completed.

### Guardian

- Current production incident snapshot remains **28 resolved / 0 unresolved**.
- Guardian code-changing repairs remain human-approval gated even when diagnostics are clean.

### Security advisor

- Production Supabase advisor evidence is tracked in issue **#2033**.
- Current findings include leaked-password protection disabled, authenticated-executable `SECURITY DEFINER` functions requiring least-privilege review, and RLS-enabled/no-policy tables that were read-only audited rather than blindly changed.
- Read-only sampling found no demonstrated active authorization bypass in the reviewed privileged functions.
- Do not change Auth configuration, EXECUTE grants, `SECURITY DEFINER`/`SECURITY INVOKER`, RLS policy, schema exposure, or privileged-function behavior without explicit protected-boundary approval.

### Database performance

- Issue **#2040** tracks performance-advisor findings.
- `app_invites_active_email_idx` is a unique partial index on `lower(email)` for unused invites; it protects integrity and must not be dropped as a mere unused-index cleanup.
- `idx_app_invites_email_unused` is a non-unique index over the same expression/predicate and remains a candidate for dependency-backed redundancy cleanup only after rollback/performance evidence. No production index was dropped by this reconciliation run.

### Active cron jobs

- `buys-privacy-retention-daily`
- `morley-recovery-health-hourly`
- `nova-knowledge-maintenance-every-5-minutes`

The catalogue enqueue and full-system Google Drive backup jobs remain absent/paused while their root causes are unresolved.

### Operational history note

- Supabase migration history contains `20260913210621 noop` with the sole statement `select 1;`. It introduced no schema or data change and must not be treated as a functional migration.
- Supabase migration history also records the approved emergency Nova scheduler restore as `20260914034903 restore_nova_knowledge_maintenance_scheduler`; PR #2050 adds the matching repository migration plus the canonical follow-up reconciliation migration.

## Active repository work

| PR / issue | Lane | State | Safety / next action |
| --- | --- | --- | --- |
| #2051 | Full-system Google Drive backup recovery | Blocked on interactive Google OAuth + secret rotation | Beau approved the credential repair. Obtain a new offline refresh token through Google authorization, rotate it through an approved secret-management path, then require successful upload + digest recovery verification before closing. |
| #2049 | Nova recurring knowledge maintenance | Production repaired; repository reconciliation merged | Verify the next automatic canonical scheduler run and close once queue progress/error-free execution is confirmed. |
| #2033 | Supabase security-advisor review | Open | Continue read-only least-privilege evidence; protected Auth/RLS/EXECUTE/SECURITY DEFINER mutations require explicit approval. |
| #2040 | Database performance advisor review | Open | Classify unused/redundant indexes conservatively; preserve integrity-bearing indexes and require dependency/rollback evidence before DDL cleanup. |
| #1947 | Staged GitLab migration parity | Draft | Keep GitHub as canonical baseline until same-SHA GitLab parity, protections, rollback and cutover checks are proven. |

## Completed material transitions

- #1973/#1974 introduced and stabilised this durable non-sensitive work-state ledger.
- #1975/#1976 added fail-closed recovery-readiness visibility and reconciled backup/catalogue state.
- #1954 merged battery-health and verified-storage valuation context without weakening authoritative pricing approval boundaries.
- #1958 merged Morley Admin web/mobile parity rebuild; deployment/runtime verification remains evidence-driven.
- #1949 merged Morley AI assessment runtime loader repair after its protected workflow change received approval.
- #2012 merged historical Drive-backup reachability checks without decrypting backup payloads.
- #2013 merged Nova shared ingestion while preserving separate auth/privacy boundaries and bounded maintenance limits.
- #2020 merged central-pricing migration supersession protection.
- Shared model-number catalogue governance merged non-destructively; unresolved/shared identifiers remain classification work.
- #2029 merged after explicit approval: Nova Next route/password accessibility improvements and their protected workflow coverage entered `main`.
- #2032 merged: Morley Vision now requires an explicit staff-confirmed condition before pricing/stock handoff while preserving AI condition as advisory evidence.
- Morley **2.15.104** OTA metadata was published on `main`; release asset/checksum/feed identity was verified during the release lane.
- #2050 merged as `d1c30743ae299273ac574421e0c1ca096990780a`: approved Nova knowledge-maintenance scheduler recovery is now represented in repository source with a canonical five-minute Vault-backed 4x4 reconciliation contract.

## High-priority unfinished lanes

1. **Production-first reliability:** login/temp-password freezes, Admin access, catalogue/sync integrity, Guardian incidents, release failures, Nova availability, backup health, and serious security/privacy regressions.
2. **Backup recovery:** rotate the revoked Google Drive refresh token through an approved secret path, execute a full backup, verify download/digest recovery evidence, and clear the stale finding only from real evidence.
3. **Catalogue processor recovery:** implement a tracked consumer for `nova_catalog_audit_queue` with safe claiming, retries, evidence recording, unresolved blocking, and run finalisation before re-enabling enqueue cron.
4. **Security hardening:** continue #2033 least-privilege analysis without changing protected auth/RLS/security boundaries absent explicit approval.
5. **Catalogue integrity:** continue classifying shared model-number groups before any merge/deactivation proposal.
6. **Nova next-generation rebuild and knowledge expansion:** continue through parity/evaluation gates while current production Nova remains intact; monitor maintenance throughput and quality rather than treating corpus size as quality proof.
7. **Morley Admin parity:** verify deployment/runtime state where visibility exists and continue evidence-backed non-protected parity defects.
8. **Morley AI / valuation:** continue non-protected valuation tests/evidence quality/regression monitoring without weakening authoritative pricing/approval boundaries.
9. **GitLab staged migration:** continue parity/protection validation without changing the production baseline or creating competing `main` histories.

## Protected blockers requiring Beau action

- Interactive Google authorization is required to issue a replacement offline refresh token for the full-system Drive backup. The token must be rotated through an approved secret-management path and must never be pasted into repository source/issues/logs.
- Security configuration changes arising from #2033, including leaked-password protection or EXECUTE/RLS/`SECURITY DEFINER` changes, require explicit approval.
- Any production restore/overwrite, additional protected RLS/schema/security mutation, signing credential, repository visibility, billing, or equivalent high-risk action requires explicit approval.

## Reconciliation checklist for each run

1. Confirm live `main` SHA and compare it with the last observed head; protected live `main` is canonical.
2. Read open PRs/issues and reuse existing branches instead of duplicating work.
3. Check exact-head CI state for active high-priority PRs.
4. Check catalogue queue/run counts and ensure runaway enqueueing has not resumed.
5. Check Nova maintenance cadence, ready/pending/error counts, and bounded failure evidence.
6. Check backup audit freshness and keep the known-failing backup path fail-closed until credentials are repaired and recovery verification passes.
7. Check serious open recovery/Guardian/security findings.
8. Resume the highest-impact already-authorised safe lane that is not blocked.
9. Update this ledger whenever a material state transition occurs.

## Exclusion

**Gumtree is excluded from all automated work.** Do not inspect, reconcile, clean up, delete, or resume Gumtree work unless Beau explicitly re-enables it.
