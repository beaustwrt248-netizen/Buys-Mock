# Morley Ecosystem Autopilot State

Last reconciled: 2026-09-12 07:10 AWST
Source of truth for this entry: live GitHub, Supabase and connected Google Drive evidence.

## Current main

- Main SHA: `5f272ec89861d3767fc0f5b98e29e27915fbfb9b`
- Latest merged change: `Reconcile Morley release collision and backup state (#1447)`.
- Main is protected; automation must not push directly to it or bypass repository protections.

## Production-first triage

- Admin authentication/session recovery work from #1417 remains on main.
- GitHub Release `v2.15.95` already exists and targets commit `439447a218b141b330d100c822b4a61352fa1221`; its published APK asset has SHA-256 `b95748540a00be1cc19825fc8149dca3c3b5dffff4073008d13aea6f36c8e527`.
- A later successful Morley APK build ran at commit `99b278de89aaa2c82828962a39b377bea1e9c0e2` while the app still declared release identity 2.15.95/versionCode 139. The subsequent Auto Publish Morley OTA run failed specifically at the immutable-release verification step because the newly built APK did not match the APK already published under `v2.15.95`. This is a confirmed release-identity reuse collision, not a generic build failure.
- The release guard behaved safely: it refused to overwrite or silently reuse an existing version tag for different APK bytes. Do not republish/replace the existing 2.15.95 release asset.
- PR #1432 proposes advancing to 2.15.96/versionCode 140 but is based on old main `439447a...` and is currently not mergeable. It must be reconciled/recreated from current main before any protected release approval.
- PR #1446 fixes the separate auto-review/auto-merge governance defect by restricting automatic merge eligibility to routine changes. It changes GitHub workflow/security policy and remains approval-gated.
- Morley AI assessment engine remains draft in PR #1437 because it contains protected Supabase migration/workflow changes.
- Morley Admin release `admin-v0.1.37` exists with signed asset `Morley-Admin-0.1.37.apk`, SHA-256 `8fe7f83d23d88cc0d477e7f02dd58e42bd6b0127dfda49c835bebd4f0497991f`, and source SHA `b0c6a52382f492d838fe6ea17e9d76972347db94` recorded in the release evidence. Stale/conflicted OTA metadata PR #1416 was closed and fresh current-main replacement PR #1448 is draft and release-gated.
- Google Drive `Morley Backups` still contains no server backup newer than `morley-backup-2026-09-10T19-00-07-760Z.json` (352,981 bytes). The production pg_cron job remains active at `0 19 * * *` UTC. Its 2026-09-11 scheduled invocation returned HTTP 401 `Invalid backup secret`; a fresh manual replay using the current vault-backed scheduler secret passed scheduler authorization but failed in phase `google-auth` with `Google OAuth refresh failed: Token has been expired or revoked.` This proves current backup generation/upload is blocked by the stored Google OAuth refresh credential, not by cron inactivity or the Drive folder disappearing.
- The separate user-encrypted backup record remains the 2026-09-07 AES-256-GCM backup; it was re-verified on 2026-09-11 but no newer user-encrypted backup was created. Verification of the old backup is not equivalent to fresh backup coverage.
- No production restore, credential mutation or destructive backup action has been attempted.
- No Gumtree work is in scope.

## Active workstreams and scores

Scoring scale: impact and confidence 1-5 (higher is better); risk, effort and dependency risk 1-5 (higher means more caution/cost). Priority is a qualitative synthesis, not an automatic authorization to cross protected boundaries.

| Workstream | Evidence / identifier | Impact | Risk | Effort | Confidence | Dependency risk | Priority | Current safe action |
| --- | --- | ---: | ---: | ---: | ---: | ---: | --- | --- |
| Backup freshness / recovery readiness | pg_cron job 3 / `google-drive-backup` / Google Drive Morley Backups | 5 | 5 | 2 | 5 | 5 | Highest, blocked | Stored Google OAuth refresh token is expired/revoked. Re-authorize through the existing approved Google OAuth flow; do not substitute, expose or mutate credentials autonomously. After re-authorization, rerun one backup and verify uploaded digest/readback before calling recovery readiness green. |
| Morley OTA release-identity collision | v2.15.95 / issue #1443 / PR #1432 | 5 | 5 | 2 | 5 | 5 | Highest, protected | Preserve immutable 2.15.95; prepare an exactly-next release identity from current main but do not publish without explicit release approval. |
| Auto-review/merge governance boundary | PR #1446 | 5 | 5 | 1 | 5 | 5 | Highest, protected | Keep validated fix unmerged until explicit approval because it changes workflow/repository-security behavior. |
| Admin 0.1.37 OTA metadata | release `admin-v0.1.37` / PR #1448 | 5 | 4 | 1 | 5 | 4 | High, protected | Keep #1448 draft while checks run; merge/publication remains explicitly approval-gated. |
| Morley AI assessment engine | PR #1437 | 5 | 5 | 4 | 4 | 5 | High, protected | Keep draft; validate core/contracts; no migration/workflow merge without approval. |
| Android catalogue push realtime | PR #1422 | 5 | 3 | 3 | 5 | 4 | High, currently blocked | Preserve work; reconcile onto current main only after release identity sequencing is clear. |
| Durable continuity / triage ledger | #1445 / this file | 4 | 1 | 1 | 5 | 1 | Immediate safe maintenance | Keep reconciled every run; never treat stale entries or the deprecated root ledger as authoritative over live state. |

## Protected blockers / approval requirements

- Release publication, signing/checksum identity, version identity, release asset replacement and deployment promotion are approval-gated.
- Auth/authorization/RLS, secrets, privileged roles, destructive production data work, Guardian repair authority, GitHub workflow/repository security and protected pricing policy are approval-gated.
- Google OAuth credential re-authorization/rotation is a credential boundary and must not be performed autonomously. The current server-backup refresh is blocked until that credential is re-authorized through the existing approved flow.
- Production restore/overwrite and destructive backup operations are approval-gated.
- PR #1446 specifically changes automatic review/merge workflow security and must not be merged without explicit approval.
- PR #1437 specifically includes Supabase migration/workflow changes and must remain draft/unmerged until explicitly approved.
- PR #1448 changes protected Admin OTA release metadata and must remain draft/unmerged until explicitly approved.
- Guardian code-changing repair PRs remain human-approval gated.
- Same-owner actions are not independent approval.

## Lightweight dependency map

- **Morley Buys Android** -> authentication/session, catalogue API/data, Supabase Realtime, valuation/Test & Buy, scanner/NFC, OTA metadata and signed APK identity.
- **Morley website** -> shared catalogue/pricing contracts, auth/session behavior, valuation flows, Nova-facing data contracts and release/static asset integrity.
- **Morley Admin** -> auth/session handoff, privileged role boundaries, support/governance workflows, catalogue/admin contracts and Admin OTA metadata.
- **Nova AI** -> catalogue/data authority, model/provider routing, source verification, support/admin tools, image/camera analysis and protected action boundaries.
- **Guardian** -> runtime diagnostics, Nova diagnostic context, repository repair proposals and human approval boundary for code-changing repair.
- **Supabase** -> schemas, RLS, auth, catalogue state/revisions, realtime publication, pg_cron/pg_net backup triggering and storage/data relationships.
- **Release infrastructure** -> Android/Admin version monotonicity, immutable release tags/assets, OTA manifests, signed artifact identity, checksums, release assets and CI gates.
- **Backup/recovery** -> pg_cron scheduler -> vault-backed scheduler authentication -> `google-drive-backup` Edge Function -> Google OAuth refresh -> Drive upload -> digest readback -> retention -> audit log. A successful cron row only proves request enqueue; the HTTP response and Drive/audit evidence determine actual backup success.

## Data-integrity invariants

The automation must detect and report before destructive repair when any of these are violated:

- One canonical device identity must not silently collapse legitimate regional/hardware variants.
- Canonical model identifiers must not conflict across records without evidence-backed variant handling.
- Required catalogue fields must not become malformed or silently guessed.
- Catalogue counts must not collapse/spike unexpectedly without an explained import/change.
- App/web/Admin/Nova catalogue views must not silently diverge from the shared authority.
- Inventory lifecycle transitions and stock identifiers must remain internally consistent.
- Pricing/valuation relationships must not become impossible or cross protected approval policy.
- OTA/release version, versionCode, tag, checksum, artifact URL and release notes must describe the same signed artifact.
- Once a release tag/version is published, a different APK must never be silently substituted under the same identity.
- A pg_cron `succeeded` row for `net.http_post` must never be treated as backup success without the HTTP response plus Drive/audit evidence.
- Backup health is not green unless freshness, uploaded-content integrity and restore-readiness evidence are all current.
- Production records/images must not be auto-deleted, merged or destructively rewritten when ambiguity remains.

## Failure-pattern knowledge

### Same release identity, different APK

- **Symptom:** Auto Publish Morley OTA fails at `Verify artifact and publish immutable GitHub Release` after a successful APK build.
- **Verified cause:** an existing release tag/version already refers to one APK, while a later source commit was rebuilt without advancing the Android release identity and produced different APK bytes.
- **Safe behavior:** refuse overwrite/reuse of the immutable release; do not weaken the checksum/identity guard.
- **Recovery pattern:** establish the exact current main and published release identity, then prepare an exactly-next version from current main through the protected release path. Never blindly republish the old version.

### Backup cron reports success but no Drive file appears

- **Symptom:** `cron.job_run_details` reports `succeeded`/`1 row`, but the expected daily `morley-backup-*.json` file is absent and no `google_drive_backup_created` audit entry is written.
- **Verified cause pattern:** pg_cron successfully enqueues `net.http_post`; this does not prove the Edge Function completed. Inspect `net._http_response` for the request. The 2026-09-11 run returned 401 `Invalid backup secret`; a fresh replay with the current vault-backed scheduler secret reached the function but failed at `google-auth` because the Google OAuth refresh token is expired/revoked.
- **Safe behavior:** do not bypass OAuth, substitute server-side tokens, expose credentials or claim the backup succeeded based only on cron status.
- **Recovery pattern:** re-authorize the existing Google OAuth connection through the approved credential flow, rerun one backup, verify the uploaded document digest by readback, confirm a new Drive file and audit record, then reassess restore readiness.

## Definition-of-done checkpoint

A work item is not Done until applicable implementation, tests, security/permission boundaries, dependency/contracts, parity, release/deployment evidence, regression coverage, performance/accessibility/degraded-mode considerations, rollback/recovery implications and this ledger are all reconciled.

## Next safe actions

1. Keep the backup pipeline blocked rather than weakening authentication. The next required protected action is re-authorizing the Google OAuth refresh credential through the existing approved flow; after that, rerun and verify one backup end-to-end.
2. Keep #1448 draft and validate its repository/release gates; do not merge/publish Admin OTA metadata without explicit approval.
3. Preserve the immutable 2.15.95 release and do not overwrite its APK or checksum; prepare exactly-next Android release work only from current main.
4. Keep #1446 unmerged pending explicit approval of the workflow-security change.
5. Preserve #1422 while its release version is blocked; avoid creating another conflicting release identity.
6. Keep #1437 draft while continuing non-protected assessment-core/evaluation work only.
7. Expand synthetic-production, data-quality and performance baselines only from measured evidence; never invent telemetry.
