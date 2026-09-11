# Morley Ecosystem Autopilot State

Last reconciled: 2026-09-12 05:57 AWST
Source of truth for this entry: live GitHub and connected Google Drive evidence.

## Current main

- Main SHA: `79f80fba73e7972b8e6d3caa62b2963f162e467d`
- Latest merged change: `Add durable Morley autopilot continuity ledger (#1445)`
- Main is protected; automation must not push directly to it or bypass repository protections.

## Production-first triage

- Admin authentication/session recovery work from #1417 remains on main.
- GitHub Release `v2.15.95` already exists and targets commit `439447a218b141b330d100c822b4a61352fa1221`; its published APK asset has SHA-256 `b95748540a00be1cc19825fc8149dca3c3b5dffff4073008d13aea6f36c8e527`.
- A later successful Morley APK build ran at commit `99b278de89aaa2c82828962a39b377bea1e9c0e2` while the app still declared release identity 2.15.95/versionCode 139. The subsequent Auto Publish Morley OTA run failed specifically at the immutable-release verification step because the newly built APK did not match the APK already published under `v2.15.95`. This is a confirmed release-identity reuse collision, not a generic build failure.
- The guard behaved safely: it refused to overwrite or silently reuse an existing version tag for different APK bytes. Do not republish/replace the existing 2.15.95 release asset.
- PR #1432 proposes advancing to 2.15.96/versionCode 140 but is based on old main `439447a...` and is currently not mergeable. It must be reconciled/recreated from current main before any protected release approval.
- PR #1444 remains release-gated and must not be used to overwrite or redefine the immutable 2.15.95 artifact.
- PR #1446 fixes the separate auto-review/auto-merge governance defect by restricting automatic merge eligibility to routine changes. It changes GitHub workflow/security policy and therefore remains approval-gated.
- Morley AI assessment engine remains draft in PR #1437 because it contains protected Supabase migration/workflow changes.
- Google Drive `Morley Backups` still contains no backup newer than `morley-backup-2026-09-10T19-00-07-760Z.json` (352,981 bytes). At this reconciliation point the latest observed backup is about 27 hours old, so backup freshness remains degraded. No production restore, credential change or destructive action has been attempted.
- No Gumtree work is in scope.

## Active workstreams and scores

Scoring scale: impact and confidence 1-5 (higher is better); risk, effort and dependency risk 1-5 (higher means more caution/cost). Priority is a qualitative synthesis, not an automatic authorization to cross protected boundaries.

| Workstream | Evidence / identifier | Impact | Risk | Effort | Confidence | Dependency risk | Priority | Current safe action |
| --- | --- | ---: | ---: | ---: | ---: | ---: | --- | --- |
| Morley OTA release-identity collision | v2.15.95 / issue #1443 / PRs #1432, #1444 | 5 | 5 | 2 | 5 | 5 | Highest, protected | Preserve immutable 2.15.95; reconcile an exactly-next release from current main only after explicit release approval. |
| Backup freshness / recovery readiness | Google Drive Morley Backups | 5 | 4 | 2 | 5 | 4 | Highest diagnostic | Diagnose missing latest backup non-destructively; do not perform production restore/credential changes without approval. |
| Auto-review/merge governance boundary | PR #1446 | 5 | 5 | 1 | 5 | 5 | Highest, protected | Keep validated fix unmerged until explicit approval because it changes workflow/repository-security behavior. |
| Morley AI assessment engine | PR #1437 | 5 | 5 | 4 | 4 | 5 | High, protected | Keep draft; validate core/contracts; no migration/workflow merge without approval. |
| Android catalogue push realtime | PR #1422 | 5 | 3 | 3 | 5 | 4 | High, currently blocked | Preserve work; reconcile onto current main only after release identity sequencing is clear. |
| Admin 0.1.37 OTA metadata | PR #1416 | 4 | 4 | 1 | 4 | 4 | High, release-gated | Reconcile with current Admin release identity before any publication action. |
| Durable continuity / triage ledger | #1445 / this file | 4 | 1 | 1 | 5 | 1 | Immediate safe maintenance | Keep reconciled every run; never treat stale entries as authoritative over live state. |

## Protected blockers / approval requirements

- Release publication, signing/checksum identity, version identity, release asset replacement and deployment promotion are approval-gated.
- Auth/authorization/RLS, secrets, privileged roles, destructive production data work, Guardian repair authority, GitHub workflow/repository security and protected pricing policy are approval-gated.
- Production restore/overwrite and backup credential changes are approval-gated.
- PR #1446 specifically changes automatic review/merge workflow security and must not be merged without explicit approval.
- PR #1437 specifically includes Supabase migration/workflow changes and must remain draft/unmerged until explicitly approved.
- Guardian code-changing repair PRs remain human-approval gated.
- Same-owner actions are not independent approval.

## Lightweight dependency map

- **Morley Buys Android** -> authentication/session, catalogue API/data, Supabase Realtime, valuation/Test & Buy, scanner/NFC, OTA metadata and signed APK identity.
- **Morley website** -> shared catalogue/pricing contracts, auth/session behavior, valuation flows, Nova-facing data contracts and release/static asset integrity.
- **Morley Admin** -> auth/session handoff, privileged role boundaries, support/governance workflows, catalogue/admin contracts and Admin OTA metadata.
- **Nova AI** -> catalogue/data authority, model/provider routing, source verification, support/admin tools, image/camera analysis and protected action boundaries.
- **Guardian** -> runtime diagnostics, Nova diagnostic context, repository repair proposals and human approval boundary for code-changing repair.
- **Supabase** -> schemas, RLS, auth, catalogue state/revisions, realtime publication and storage/data relationships.
- **Release infrastructure** -> Android/Admin version monotonicity, immutable release tags/assets, OTA manifests, signed artifact identity, checksums, release assets and CI gates.
- **Backup/recovery** -> production data coverage, Google Drive backup delivery, retention/integrity evidence and isolated restore readiness.

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
- Backup freshness must not silently regress; missing expected backup windows require diagnosis before restore claims are made.
- Production records/images must not be auto-deleted, merged or destructively rewritten when ambiguity remains.

## Failure-pattern knowledge

### Same release identity, different APK

- **Symptom:** Auto Publish Morley OTA fails at `Verify artifact and publish immutable GitHub Release` after a successful APK build.
- **Verified cause:** an existing release tag/version already refers to one APK, while a later source commit was rebuilt without advancing the Android release identity and produced different APK bytes.
- **Safe behavior:** refuse overwrite/reuse of the immutable release; do not weaken the checksum/identity guard.
- **Recovery pattern:** establish the exact current main and published release identity, then prepare an exactly-next version from current main through the protected release path. Never blindly republish the old version.

## Definition-of-done checkpoint

A work item is not Done until applicable implementation, tests, security/permission boundaries, dependency/contracts, parity, release/deployment evidence, regression coverage, performance/accessibility/degraded-mode considerations, rollback/recovery implications and this ledger are all reconciled.

## Next safe actions

1. Preserve the immutable 2.15.95 release and do not overwrite its APK or checksum.
2. Keep #1432/#1444 held while constructing/reconciling the exactly-next release identity from current main; release publication remains explicitly approval-gated.
3. Keep #1446 unmerged pending explicit approval of the workflow-security change.
4. Diagnose the missing latest Google Drive backup using read-only evidence and determine whether generation, upload or scheduling failed.
5. Preserve #1422 while its release version is blocked; avoid creating another conflicting release identity.
6. Keep #1437 draft while continuing non-protected assessment-core/evaluation work only.
7. Expand this ledger with synthetic-production, data-quality and performance baselines only from measured evidence; never invent telemetry.
