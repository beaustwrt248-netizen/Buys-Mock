# Morley Ecosystem Autopilot State

Last reconciled: 2026-09-12 14:36 AWST
Source of truth for this entry: live GitHub and connected Google Drive evidence, with previously verified protected-service findings retained only where no newer recovery evidence exists.

## Current main

- Main SHA: `fe4691f328b94b079d587cf7d93f4e29b3d2742b`.
- Latest merged change: `Publish Morley 2.15.98 OTA metadata` (approved release sequence, PR #1603).
- Morley Buys 2.15.98 / versionCode 142 source was merged by PR #1592 as `9a7c3ae59e7f5b77b06fe2aaf77abe6310215a9e`, restoring Morley AI Device Lens scanner routing.
- Protected auto-review/auto-merge governance repair #1576 is merged with explicit approval; only routine work is eligible for automation, while guarded/protected changes require manual review.
- Main remains protected; automation must not push directly to it or bypass repository protections.

## Production-first triage

- **Morley Buys 2.15.98 release is complete.** Build run `34678061085` passed release-readiness, regression tests, Android lint, release APK build, signer verification, login-video packaging verification and artifact upload.
- GitHub Release `v2.15.98` is published against exact tested source `9a7c3ae59e7f5b77b06fe2aaf77abe6310215a9e` with APK `B-and-L-Morley-2.15.98.apk` and SHA-256 `c2f7d86723f53f3148c6a2435ad788f1fb82adba0d7527287c40b4f2bc1aaced`.
- `ota/latest.json` on main now declares 2.15.98 / versionCode 142 and matches the same release URL and SHA-256. Owner-authored replacement PR #1603 was used because bot-authored #1602 was blocked before CI jobs started; #1603 passed Repository Security, Quality, parity, Admin OTA Safety, feature-contract, Admin integration, private-distribution and email checks before merge.
- Admin browser authentication recovery PR #1593 is merged. It restored the same-origin Turnstile transport for browser Admin sign-in while preserving the separate native Android boundary.
- Autopilot incident #1604 was traced to an intermediate #1590 browser-isolation test that expected the temporary `workspace-loader.js?v=1` design. The later merged #1593 deliberately replaced that contract; current main no longer contains the stale assertion. #1604 is closed as superseded evidence, not as approval of #1590.
- Diagnostic PR #1598 remains draft because it changes a GitHub workflow. Its live Admin browser probe may collect evidence, but it must not merge without explicit workflow-security approval.
- Morley AI assessment engine PR #1437 is merged as `42d90c20cc67820bd4ff18e844a3e6f60eb9cbb1`; its approved production assessment migration was applied and its protected commercial/action boundaries remain staff-confirmed.
- Latest observed published Admin release is `admin-v0.1.41`, with signed APK `Morley-Admin-0.1.41.apk` and release digest `295953c3afc1bc412926eb8802b7c062fecce3fcd0256e6c98f89174bcbc884c`.
- Google Drive folder `Morley Backups` is reachable, but current Drive search returns no backup-file listing from that folder. There is therefore no fresh evidence that backup freshness recovered. The previously verified blocker remains the expired/revoked Google OAuth refresh credential until a successful re-authorization plus new Drive upload/digest readback proves otherwise.
- No production restore, credential mutation or destructive backup action has been attempted.
- No Gumtree work is in scope.

## Active workstreams and scores

Scoring scale: impact and confidence 1-5 (higher is better); risk, effort and dependency risk 1-5 (higher means more caution/cost). Priority is a qualitative synthesis, not authorization to cross protected boundaries.

| Workstream | Evidence / identifier | Impact | Risk | Effort | Confidence | Dependency risk | Priority | Current safe action |
| --- | --- | ---: | ---: | ---: | ---: | --- | --- | --- |
| Backup freshness / recovery readiness | Google Drive `Morley Backups` / `google-drive-backup` | 5 | 5 | 2 | 4 | 5 | Highest, blocked | Keep fail-closed. Re-authorize the existing Google OAuth connection through the approved credential flow, then run one backup and verify upload, digest readback and audit evidence. |
| Android catalogue push realtime | PR #1422 | 5 | 4 | 3 | 5 | 4 | Highest safe feature lane, design/approval gated | Do not merge stale #1422. Design a fresh-current-main implementation using authenticated Realtime plus bounded recovery reconciliation, preserving auth/RLS and separating any GitHub workflow-contract change behind its protected approval boundary. |
| Admin browser/native auth stability | #1593 / #1598 / Admin 0.1.41 | 5 | 4 | 2 | 4 | 5 | High, monitor | Continue live/synthetic and regression evidence without weakening Turnstile/auth/session boundaries; keep diagnostic workflow changes draft unless approved. |
| Nova / Morley assessment quality | #1437 / assessment core | 5 | 4 | 3 | 4 | 4 | High | Continue evaluation, source-quality, latency/cost and protected-command drift checks; consequential actions remain human-confirmed. |
| Morley 2.15.98 release | #1592 / #1603 / `v2.15.98` | 5 | 5 | 2 | 5 | 5 | Completed milestone | Preserve immutable release identity and monitor post-release health; do not advance Android identity until a subsequent source-changing release candidate is actually ready. |
| Durable continuity / triage ledger | this file | 4 | 1 | 1 | 5 | 1 | Immediate maintenance | Reconcile every run from live state; never allow stale release/backup/governance entries to drive action. |

## Protected blockers / approval requirements

- Release publication, signing/checksum identity, version identity, release asset replacement and deployment promotion remain approval-gated unless an already-approved repository release pipeline is completing an explicitly approved release sequence.
- Auth/authorization/RLS, secrets, privileged roles, destructive production data work, Guardian repair authority, GitHub workflow/repository security and protected pricing policy remain approval-gated.
- Google OAuth credential re-authorization/rotation is a credential boundary and must not be performed autonomously.
- Production restore/overwrite and destructive backup operations remain approval-gated.
- Guardian code-changing repair PRs remain human-approval gated.
- Same-owner actions are not independent approval.

## Lightweight dependency map

- **Morley Buys Android** -> authentication/session, catalogue API/data, Supabase Realtime, valuation/Test & Buy, Device Lens/code scanning/NFC, OTA metadata and signed APK identity.
- **Morley website** -> shared catalogue/pricing contracts, auth/session behavior, valuation flows, Nova-facing data contracts and release/static asset integrity.
- **Morley Admin** -> browser/native auth boundaries, privileged roles, support/governance workflows, catalogue/admin contracts and Admin OTA metadata.
- **Nova AI / Morley assessment** -> catalogue authority, provider/model routing, verified evidence, support/admin tools, image/camera analysis, Condition Score and protected action boundaries.
- **Guardian** -> runtime diagnostics, Nova context, repository repair proposals and human approval for code-changing repairs.
- **Supabase** -> schemas, RLS, auth, catalogue state/revisions, Realtime publication, assessment persistence/audit and backup triggering.
- **Release infrastructure** -> Android/Admin version monotonicity, immutable tags/assets, OTA manifests, signed artifact identity, checksums and CI gates.
- **Backup/recovery** -> scheduler -> `google-drive-backup` -> Google OAuth refresh -> Drive upload -> digest readback -> retention/audit. Scheduler success alone is not backup success.

## Data-integrity invariants

- Canonical devices must preserve legitimate regional/hardware variants and avoid conflicting model identifiers.
- Required catalogue facts must be evidence-backed; missing facts remain unverified rather than guessed.
- Catalogue counts must not collapse/spike unexpectedly without an explained source/import change.
- App/web/Admin/Nova catalogue views must not silently diverge from shared authority.
- Inventory lifecycle transitions and stock identifiers must remain internally consistent.
- Pricing/valuation relationships must not bypass protected commercial approval policy.
- OTA/release version, versionCode, tag, checksum, artifact URL and notes must describe the same signed artifact.
- A published release identity must never be silently reused for different APK bytes.
- Backup health is not green unless freshness, uploaded-content integrity and restore-readiness evidence are current.
- Ambiguous production records/images must not be auto-deleted, merged or destructively rewritten.

## Failure-pattern knowledge

### Same release identity, different APK

- **Symptom:** OTA publication rejects an APK after a successful build because an existing version/tag has different bytes.
- **Safe behavior:** preserve the immutable release identity and advance exactly once through the protected release path; never weaken checksum/identity guards.

### Bot-authored protected OTA PR cannot start CI

- **Symptom:** generated OTA PR is mergeable but all required workflows finish `action_required` with zero jobs.
- **Observed 2.15.98 case:** #1602 was created by `github-actions[bot]`; required jobs did not start. The exact same metadata recreated as owner-authored #1603 ran the full gate suite normally.
- **Safe behavior:** do not bypass checks. Recreate only the exact already-verified metadata from current main under an authorized branch/PR, run the complete required gates, close the blocked duplicate, and merge only after all required checks are green and release approval exists.

### Backup scheduler appears healthy but no Drive backup exists

- **Symptom:** scheduler invocation succeeds or the folder exists, but no new backup/upload/audit evidence appears.
- **Verified historical cause:** the backup function reached Google OAuth and the stored refresh token was expired/revoked.
- **Safe behavior:** do not bypass OAuth or substitute/expose credentials. Re-authorize through the approved flow, then prove a new upload and digest readback before restoring green status.

### Stale Android release branch

- **Symptom:** a valid feature PR remains open across multiple release identities and can no longer satisfy exact-next OTA/version policy.
- **Current example:** #1422 was created from old main `062e64b...`; its Realtime implementation is absent from current main and its embedded Android identity is obsolete.
- **Safe behavior:** do not force/rebase a stale release identity into main. Preserve the feature intent and recreate/reconcile from fresh main with current contracts and an exactly-next identity only if/when Android source changes still require one.

### Stale intermediate auth-contract incident

- **Symptom:** automation opens a security incident for a failing intermediate PR revision after a later production-recovery design has superseded that exact contract.
- **Observed case:** #1604 came from #1590 commit `c054760c...`, whose single failed assertion required `workspace-loader.js?v=1`; merged #1593 replaced that browser flow and current main no longer contains the assertion.
- **Safe behavior:** trace the failure to the exact head SHA and current main before changing auth code. Close only the stale incident evidence; do not infer approval for the superseded design branch.

## Definition-of-done checkpoint

A work item is not Done until applicable implementation, tests, security/permission boundaries, dependency/contracts, parity, release/deployment evidence, regression coverage, performance/accessibility/degraded-mode considerations, rollback/recovery implications and this ledger are reconciled.

## Next safe actions

1. Keep backup recovery fail-closed until Google OAuth is re-authorized and one fresh backup is verified end-to-end.
2. Present the fresh-current-main Android catalogue Realtime design for approval before implementing; do not reuse #1422's stale release identity or silently bundle workflow-security changes.
3. Continue Admin auth synthetic/regression monitoring across browser and native boundaries; inspect #1598 diagnostic evidence when its run completes while keeping the workflow PR draft.
4. Continue Nova assessment/provider quality, cost, source and safety-drift evaluation without granting protected action authority.
5. Continue current-main data-integrity, catalogue-quality, performance, accessibility and degraded-mode rotation using measured evidence only.
6. Monitor Morley 2.15.98 post-release health and preserve its immutable APK/checksum identity.
