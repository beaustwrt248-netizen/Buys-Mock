# Morley Autopilot Work-State Ledger

Last reconciled: 2026-09-14 (Australia/Perth)
Canonical repository baseline: the current protected `main` branch.
Last observed `main` head before this ledger reconciliation: `a5a36c1c21c440648aca1c965cbc6540b18a9150`.

This file is the durable, non-sensitive state ledger for the consolidated Morley ecosystem automation. It records work that is active, blocked, completed, paused for safety, or awaiting protected approval. It must be reconciled against the live repository and connected production services before each automated work pass.

The exact `main` SHA is recorded as an observation rather than as a permanent baseline because merging a ledger-only update advances `main` itself. The protected `main` ref, not an old ledger SHA, remains canonical.

## Operating rules

- Resume unfinished work before creating duplicate branches or pull requests.
- Keep Gumtree work excluded unless Beau explicitly re-enables it.
- Prefer fresh branches from current `main` for new coherent changes.
- Continue independent safe lanes when CI, review, credentials, or external services block another lane.
- Never push directly to `main` or bypass repository protections.
- Never auto-merge protected changes involving authentication/authorization/RLS, secrets/credentials, destructive production data, privileged roles/permissions, protected pricing approval policy, GitHub workflow/repository security, signing/release credentials, Guardian approval policy, repository visibility, billing, or equivalent high-risk boundaries.
- Guardian code-changing repair PRs remain human-approval gated.
- Do not mark a task done until applicable tests, contracts, parity/security checks, deployment/release verification, and rollback implications are satisfied.

## Current production safety state

### Catalogue audit pipeline

- Queue snapshot: **961 verified / 70 blocked / 1,082 pending**.
- Run snapshot: **12 completed / 16 queued / 1 running** at the last verified run-state audit.
- The enqueue-only catalogue cron is intentionally paused because no live database/Edge consumer currently drains the queue. Do not re-enable enqueueing until a tracked consumer is implemented and verified.
- Preserve unresolved model-number gaps as unresolved/blocked rather than guessing identifiers or specifications.
- Data-integrity audit: **1,777 active devices**. Duplicate/shared-model-number groups remain classification work, not automatic merge/delete authority.
- Shared model-number governance entered `main` at `a5a36c1c21c440648aca1c965cbc6540b18a9150`; manufacturer-verified shared identifiers remain explicitly classified while unresolved groups stay non-destructive.

### Backup / recovery

- Last successful full-system Google Drive backup audit: **2026-09-10 19:00 UTC**.
- Full-system backup freshness remains **stale** against the 36-hour readiness threshold.
- `recovery-readiness` production Edge Function v5 exposes independent fail-closed full-system backup freshness (`healthy`, `stale`, `missing`, or `unknown`) from the audit trail.
- The scheduled full-system Google Drive backup cron remains intentionally paused after the server-side OAuth refresh token began returning `expired or revoked` at the `google-auth` phase.
- Android/manual user backup uses a separate fresh access-token path and must not be treated as proof that the server scheduler credential is healthy.
- Production credential replacement and OAuth publishing-state changes require explicit approval.
- Hourly recovery-health monitoring remains active.

### Nova knowledge

- Knowledge snapshot: **4,715 active chunks / 4,708 active sources**.
- Embedding snapshot: **156 ready / 4,559 pending / 0 errors**.
- Current embedding coverage is approximately **3.31%**. Keep expansion evidence-driven and benchmarked; do not treat raw corpus growth as proof of answer-quality improvement.

### Guardian

- Current production incident snapshot: **28 resolved / 0 unresolved**.
- Guardian code-changing repairs remain human-approval gated even when diagnostics are clean.

### Active cron jobs

- `buys-privacy-retention-daily`
- `morley-recovery-health-hourly`

The catalogue enqueue and full-system Google Drive backup jobs are intentionally absent while their root causes remain unresolved.

### Operational history note

- Supabase migration history contains `20260913210621 noop` with the sole statement `select 1;`. It introduced no schema or data change and should not be treated as a functional migration.

## Active repository work

| PR | Lane | State | Safety / next action |
| --- | --- | --- | --- |
| #2029 | Nova Next route/password accessibility | Open, mergeable; exact-head CI green | Includes a new GitHub Actions workflow, so merge remains a protected repository-security change despite the low-risk product code. Require explicit Beau approval before merge. |
| #1947 | Staged GitLab migration parity | Draft | Keep GitHub as canonical baseline until same-SHA GitLab parity, protections, rollback and cutover checks are proven. |

## Completed material transitions

- #1973 merged: introduced this durable non-sensitive work-state ledger on `main` after required `audit` and security checks passed.
- #1974 merged: reconciled the ledger so protected `main`, rather than a self-referential old SHA, is canonical.
- #1975 merged and deployed: full-system backup freshness is now visible through `recovery-readiness` without changing authorization, credentials, restore behavior, schema, or cron state.
- #1976 merged: reconciled recovery-readiness and catalogue-integrity evidence into this durable ledger.
- #1954 merged: battery-health and verified-storage context entered Morley AI/Nova Camera assessment pricing while authoritative base-buy, human-approval, max-buy and minimum-margin boundaries remained unchanged.
- #1958 merged: Morley Admin web/mobile web parity rebuild entered `main`; production deployment verification remains a separate evidence requirement when connected deployment visibility is available.
- #1949 merged: Morley AI assessment runtime loader repair entered `main` after its protected workflow change received approval.
- #2012 merged: historical Drive backup reachability can now be checked without decrypting backup payloads.
- #2013 merged as `207442bd40fb33b4489fdff19e8358561ce059f3`: Nova manual and maintenance ingestion now share one persistence engine while preserving separate auth/privacy boundaries and existing maintenance limits.
- #2020 merged as `fff228d58bc37140ca72a43eb55283402697113a`: central-pricing migration supersession is now explicitly guarded so stale migration authority cannot silently reappear.
- Shared model-number catalogue governance merged as `a5a36c1c21c440648aca1c965cbc6540b18a9150`; classification remains non-destructive for unresolved or legitimate shared identifiers.
- #2017 merged as `7a43762aa56ee6b40de33e878fcd33782ab0aa3b`: OTA metadata now publishes Morley **2.15.103 / versionCode 147** from current `main`. The published APK asset exists and GitHub reports matching SHA-256 `2f3ff1f3f0cd4f9a8ba4d35847a9a84dcc53f75d4a0be1fee5cd68069af8ecba`.
- Stale-base OTA PR #2011 was closed unmerged after #2017 safely superseded it.

## High-priority unfinished lanes

1. **Production-first reliability:** login/temp-password freezes, Admin access, catalogue/sync integrity, Guardian incidents, release failures, Nova availability, backup health, serious security/privacy regressions.
2. **Catalogue processor recovery:** implement a tracked consumer for `nova_catalog_audit_queue` with safe claiming, retries, evidence recording, unresolved blocking, and run finalisation before re-enabling enqueue cron.
3. **Backup recovery:** permanently repair the scheduled server OAuth path; freshness visibility and historical reachability checks are improved, but the credential-dependent backup itself remains paused and stale.
4. **Catalogue integrity:** continue classifying shared model-number groups as legitimate configurations versus true canonical collisions before any merge/deactivation proposal.
5. **Nova next-generation rebuild and knowledge expansion:** continue only through parity/evaluation gates while current production Nova remains intact; #2029 is implementation-ready but protected because it adds a workflow.
6. **Morley Admin parity:** verify post-merge deployment/runtime state where deployment visibility is available and continue non-protected parity defects from evidence.
7. **Morley AI / valuation:** continue adjacent non-protected valuation tests, evidence quality, and regression monitoring without weakening authoritative pricing/approval boundaries.
8. **GitLab staged migration:** continue parity/protection validation without changing production baseline or creating competing `main` histories.

## Protected blockers requiring Beau action

- Merge approval for PRs that cross protected boundaries, including authentication/authorization, protected pricing approval policy, or GitHub workflow/repository-security changes. Current example: #2029.
- Server Google OAuth credential/publishing-state correction for scheduled full-system Drive backups.
- Any production restore/overwrite, protected RLS/schema/security mutation, signing credential, repository visibility, billing, or equivalent high-risk action.

## Reconciliation checklist for each run

1. Confirm live `main` SHA and compare it to the last observed head in this ledger; treat live protected `main` as canonical.
2. Read open PRs and reuse existing branches instead of duplicating work.
3. Check CI/check-run state for active high-priority PRs.
4. Check production catalogue queue/run counts and ensure runaway enqueueing has not resumed.
5. Check backup audit freshness and confirm known-failing backup cron remains fail-closed until repaired.
6. Check serious open recovery/Guardian/security findings.
7. Resume the highest-impact already-authorised safe lane that is not blocked.
8. Update this ledger when a material state transition occurs.

## Exclusion

**Gumtree is excluded from all automated work.** Do not inspect, reconcile, clean up, delete, or resume Gumtree work unless Beau explicitly re-enables it.
