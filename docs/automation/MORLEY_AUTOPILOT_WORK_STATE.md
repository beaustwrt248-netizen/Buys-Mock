# Morley Autopilot Work-State Ledger

Last reconciled: 2026-09-14 (Australia/Perth)
Canonical repository baseline: `main` at `7b7c6316addfc5e45a1561b704d5148c017a1fbd`

This file is the durable, non-sensitive state ledger for the consolidated Morley ecosystem automation. It records work that is active, blocked, completed, paused for safety, or awaiting protected approval. It must be reconciled against the live repository and connected production services before each automated work pass.

## Operating rules

- Resume unfinished work before creating duplicate branches or pull requests.
- Keep Gumtree work excluded unless Beau explicitly re-enables it.
- Prefer fresh branches from current `main` for new coherent changes.
- Continue independent safe lanes when CI, review, credentials, or external services block another lane.
- Never push directly to `main` or bypass repository protections.
- Never auto-merge protected changes involving authentication/authorization/RLS, secrets/credentials, destructive production data, privileged roles/permissions, protected pricing policy, GitHub workflow/repository security, signing/release credentials, Guardian approval policy, repository visibility, billing, or equivalent high-risk boundaries.
- Guardian code-changing repair PRs remain human-approval gated.
- Do not mark a task done until applicable tests, contracts, parity/security checks, deployment/release verification, and rollback implications are satisfied.

## Current production safety state

### Catalogue audit pipeline

- Queue snapshot: **961 verified / 70 blocked / 1,082 pending**.
- Run snapshot: **12 completed / 16 queued / 1 running**.
- The enqueue-only catalogue cron is intentionally paused because no live database/Edge consumer currently drains the queue. Do not re-enable enqueueing until a tracked consumer is implemented and verified.
- Preserve unresolved model-number gaps as unresolved/blocked rather than guessing identifiers or specifications.

### Backup / recovery

- Last successful full-system Google Drive backup audit: **2026-09-10 19:00 UTC**.
- The scheduled full-system Google Drive backup cron is intentionally paused after the server-side OAuth refresh token began returning `expired or revoked` at the `google-auth` phase.
- Android/manual user backup uses a separate fresh access-token path and must not be treated as proof that the server scheduler credential is healthy.
- Production credential replacement and OAuth publishing-state changes require explicit approval.
- Hourly recovery-health monitoring remains active.

### Active cron jobs

- `buys-privacy-retention-daily`
- `morley-recovery-health-hourly`

The catalogue enqueue and full-system Google Drive backup jobs are intentionally absent while their root causes remain unresolved.

## Active repository work

| PR | Lane | State | Safety / next action |
| --- | --- | --- | --- |
| #1958 | Morley Admin web/mobile web parity rebuild | Open, mergeable | Authentication/bootstrap and role-aware Admin surfaces are changed. Treat merge as protected; require Beau approval after all required checks are green. |
| #1954 | Morley AI battery/storage assessment pricing | Draft, mergeable | Continue CI and valuation-contract verification. Preserve protected authoritative-price/max-buy/min-margin boundaries. |
| #1950 | Repository path stability guard | Draft, mergeable | GitHub workflow/security boundary. Keep draft and require explicit approval before merge. |
| #1949 | Morley AI assessment runtime loader | Open, mergeable | Includes GitHub Actions workflow change; explicit approval required before merge. |
| #1947 | Staged GitLab migration parity | Draft, mergeable | Keep GitHub as canonical baseline until same-SHA GitLab parity, protections, rollback and cutover checks are proven. |

## High-priority unfinished lanes

1. **Production-first reliability:** login/temp-password freezes, Admin access, catalogue/sync integrity, Guardian incidents, release failures, Nova availability, backup health, serious security/privacy regressions.
2. **Catalogue processor recovery:** implement a tracked consumer for `nova_catalog_audit_queue` with safe claiming, retries, evidence recording, unresolved blocking, and run finalisation before re-enabling enqueue cron.
3. **Backup recovery:** permanently repair the scheduled server OAuth path and extend recovery monitoring so missing full-system backup audits are detected independently from user backup health.
4. **Morley Admin parity:** finish #1958 validation and hold protected auth/role merge for approval.
5. **Morley AI / valuation:** finish #1954 verification without weakening protected pricing controls.
6. **Nova next-generation rebuild and knowledge expansion:** continue only through parity/evaluation gates while current production Nova remains intact.
7. **GitLab staged migration:** continue parity/protection validation without changing production baseline or creating competing `main` histories.

## Protected blockers requiring Beau action

- Merge approval for PRs that cross protected boundaries, including authentication/authorization or GitHub workflow/repository-security changes.
- Server Google OAuth credential/publishing-state correction for scheduled full-system Drive backups.
- Any production restore/overwrite, protected RLS/schema/security mutation, signing credential, repository visibility, billing, or equivalent high-risk action.

## Reconciliation checklist for each run

1. Confirm live `main` SHA and compare it to this ledger.
2. Read open PRs and reuse existing branches instead of duplicating work.
3. Check CI/check-run state for active high-priority PRs.
4. Check production catalogue queue/run counts and ensure runaway enqueueing has not resumed.
5. Check backup audit freshness and confirm known-failing backup cron remains fail-closed until repaired.
6. Check serious open recovery/Guardian/security findings.
7. Resume the highest-impact already-authorised safe lane that is not blocked.
8. Update this ledger when a material state transition occurs.

## Exclusion

**Gumtree is excluded from all automated work.** Do not inspect, reconcile, clean up, delete, or resume Gumtree work unless Beau explicitly re-enables it.
