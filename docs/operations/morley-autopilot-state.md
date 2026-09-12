# Morley Ecosystem Autopilot State

Last reconciled: 2026-09-12 14:25 AWST
Source of truth for this entry: live GitHub and connected Google Drive evidence, with previously verified protected-service findings retained only where no newer recovery evidence exists.

## Current main

- Main SHA: `ccd9442ae91a98337379e9b13d9e6a48300e8a15`.
- Latest merged change: `Enforce protected manual-review boundaries` (approved PR #1576).
- Parent release/source change: PR #1592 merged as `9a7c3ae59e7f5b77b06fe2aaf77abe6310215a9e`, restoring Morley AI Device Lens scanner routing and advancing Android source identity to 2.15.98 / versionCode 142.
- Main remains protected; automation must not push directly to it or bypass repository protections.

## Production-first triage

- Admin browser authentication recovery PR #1593 is merged on the current main ancestry. It restored the same-origin Turnstile transport for browser Admin sign-in while preserving the separate native Android boundary.
- Morley Buys source on main now declares 2.15.98 / versionCode 142 through merged PR #1592. The push-triggered `Build B&L Morley APK` run `34678061085` is still active at this reconciliation point; release-readiness/version/login-video setup and regression tests have passed, with Android lint/build/sign/release steps not yet all complete. Do not claim 2.15.98 published until the release and OTA evidence exists.
- Published Morley Buys release/OTA remains 2.15.97 / versionCode 141. `ota/latest.json` still points at `v2.15.97`, APK `B-and-L-Morley-2.15.97.apk`, SHA-256 `353c47208ccafac6d1c97b16008f1f3f8fda6772d947eb9b6473a6920844a401`.
- Protected auto-review/auto-merge governance repair #1576 is now merged with explicit approval. Automation eligibility is restricted to routine work; guarded workflow/security/release-sensitive changes require manual approval and destructive/credential/trust-breaking changes remain hard-stopped.
- Morley AI assessment engine PR #1437 is merged as `42d90c20cc67820bd4ff18e844a3e6f60eb9cbb1`; its approved production assessment migration was applied and its protected commercial/action boundaries remain staff-confirmed.
- Latest observed published Admin release is `admin-v0.1.41`, with signed APK `Morley-Admin-0.1.41.apk` and release digest `295953c3afc1bc412926eb8802b7c062fecce3fcd0256e6c98f89174bcbc884c`.
- Google Drive folder `Morley Backups` is reachable, but current Drive search returns no backup-file listing from that folder. There is therefore no fresh evidence in this run that backup freshness recovered. The previously verified blocker remains the expired/revoked Google OAuth refresh credential until a successful re-authorization plus new Drive upload/digest readback proves otherwise.
- No production restore, credential mutation or destructive backup action has been attempted.
- No Gumtree work is in scope.

## Active workstreams and scores

Scoring scale: impact and confidence 1-5 (higher is better); risk, effort and dependency risk 1-5 (higher means more caution/cost). Priority is a qualitative synthesis, not authorization to cross protected boundaries.

| Workstream | Evidence / identifier | Impact | Risk | Effort | Confidence | Dependency risk | Priority | Current safe action |
| --- | --- | ---: | ---: | ---: | ---: | --- | --- | --- |
| Morley 2.15.98 build/release sequencing | #1592 / run `34678061085` / `ota/latest.json` | 5 | 5 | 2 | 5 | 5 | Highest, active | Let the existing protected pipeline finish; verify signed artifact, release tag/digest and OTA metadata before any success claim or next Android version. |
| Backup freshness / recovery readiness | Google Drive `Morley Backups` / `google-drive-backup` | 5 | 5 | 2 | 4 | 5 | Highest, blocked | Keep fail-closed. Re-authorize the existing Google OAuth connection through the approved credential flow, then run one backup and verify upload, digest readback and audit evidence. |
| Android catalogue push realtime | PR #1422 | 5 | 3 | 3 | 5 | 4 | High, release-blocked | Preserve the feature work but do not merge the stale release branch. Reconcile onto fresh main only after 2.15.98 publication/OTA identity is settled and retest all consumers/contracts. |
| Admin browser/native auth stability | #1593 / Admin 0.1.41 | 5 | 4 | 2 | 4 | 5 | High, monitor | Continue synthetic/live evidence and regression checks without weakening Turnstile/auth/session boundaries. |
| Nova / Morley assessment quality | #1437 / assessment core | 5 | 4 | 3 | 4 | 4 | High | Continue evaluation, source-quality, latency/cost and protected-command drift checks; consequential actions remain human-confirmed. |
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

### Backup scheduler appears healthy but no Drive backup exists

- **Symptom:** scheduler invocation succeeds or the folder exists, but no new backup/upload/audit evidence appears.
- **Verified historical cause:** the backup function reached Google OAuth and the stored refresh token was expired/revoked.
- **Safe behavior:** do not bypass OAuth or substitute/expose credentials. Re-authorize through the approved flow, then prove a new upload and digest readback before restoring green status.

### Stale Android release branch

- **Symptom:** a valid feature PR remains open across multiple release identities and can no longer satisfy exact-next OTA/version policy.
- **Current example:** #1422 was created from old main `062e64b...` and its Realtime implementation is absent from current main.
- **Safe behavior:** do not force/rebase a stale release identity into main. Preserve the feature intent, wait for the active release sequence to settle, then recreate/reconcile from fresh main with current contracts and an exactly-next identity if Android source changes still require one.

## Definition-of-done checkpoint

A work item is not Done until applicable implementation, tests, security/permission boundaries, dependency/contracts, parity, release/deployment evidence, regression coverage, performance/accessibility/degraded-mode considerations, rollback/recovery implications and this ledger are reconciled.

## Next safe actions

1. Follow run `34678061085` to completion. If successful, verify the 2.15.98 signed APK, release tag/target/digest and subsequent OTA metadata before treating the release as complete.
2. Keep backup recovery fail-closed until Google OAuth is re-authorized and one fresh backup is verified end-to-end.
3. After 2.15.98 sequencing is settled, reconcile #1422 onto fresh main rather than merging the stale branch; rerun catalogue live-sync, Android, security, quality, parity and release-version contracts.
4. Continue Admin auth synthetic/regression monitoring across browser and native boundaries.
5. Continue Nova assessment/provider quality, cost, source and safety-drift evaluation without granting protected action authority.
6. Continue current-main data-integrity, catalogue-quality, performance, accessibility and degraded-mode rotation using measured evidence only.
