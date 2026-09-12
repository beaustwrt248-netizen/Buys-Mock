# Morley Ecosystem Autopilot State

Last reconciled: 2026-09-12 20:06 AWST
Source of truth: live GitHub, Supabase and connected service evidence. No Gumtree work is in scope.

## Current main and release state

- Last material runtime/Admin main SHA at reconciliation: `6bb1080475e6036b0874d338d89ce0fc76f5d56b`. Ledger-only documentation commits may follow it and do not change runtime/release state.
- Security hardening PR #1678 was explicitly approved and merged as `c0f7f16824dc72771da85fe9f603ccee9c4b5843`. It removes direct interpolation of PR-controlled critical filenames into privileged `pull_request_target` shell source, preserves `contents: read` + `pull-requests: write`, and adds a regression check. Security issue #1643 is closed as completed.
- Admin invitation delivery PR #1681 is the latest material runtime/Admin change. It adds audited direct email delivery for secure one-use app download invitations while retaining existing Admin/Manager distribution boundaries and Copy/Share recovery.
- No runtime pull request is currently open.
- Morley Buys 2.15.100 / versionCode 144 remains the advertised OTA release. `ota/latest.json` points to `v2.15.100/B-and-L-Morley-2.15.100.apk` with SHA-256 `488cf42be87f54201266f9ea5e109911365677524e1f043beabcbf32cd16f175` and release note `Fix Morley Device Lens analysis timeout (#1669)`.
- GitHub Release `v2.15.100` targets runtime source `d44beab010af2be7b4135dc2bb6f3d63e0b2296d`; its APK asset is 44,452,238 bytes and carries the same SHA-256 as the OTA manifest.
- Main remains protected. Automation must not push directly to main or bypass repository/release protections.

## Production-first triage

### Release / OTA

- 2.15.100 release identity remains internally consistent: versionCode 144, versionName 2.15.100, release URL and SHA-256 agree with the published GitHub release asset.
- Published release identity is immutable; future runtime releases must advance version identity and repeat exact-source build, security/parity/quality, signer/checksum, OTA monotonicity and post-promotion verification.

### GitHub / CI / security

- The latest sampled Morley Ecosystem Autopilot workflow on the latest material main commit completed successfully.
- Issue #1676 (`Guarded Auto Review & Merge` autopilot failure) is closed as completed.
- Issue #1643 is closed after the validated #1678 workflow-security fix.
- #1678 validation evidence on its exact head included green Repository Security Audit, B&L Morley Quality Gate, Morley Ultimate Parity Gate and Morley Email Contract. The dedicated regression first failed against the vulnerable form, then passed after hardening.
- Fresh Supabase security-advisor evidence reports production Auth leaked-password protection is disabled. Issue #1686 records the finding and the approval-gated remediation plan; no Auth configuration was changed autonomously.
- The same advisor run reports authenticated execution grants on several `SECURITY DEFINER` RPCs. Read-only definition inspection found explicit enabled-profile Admin/Manager authorization checks on the protected Admin/inventory/Guardian mutation RPCs. `guardian_report_diagnostic` intentionally accepts authenticated callers, strips sensitive metadata keys and does not confer repair authority. No authorization broadening was performed.
- RLS-enabled/no-policy advisor findings on service-only tables remain informational: with RLS enabled and no public policy they are fail-closed to ordinary Data API callers. No RLS changes were made.

### Google Drive backup / recovery

- Global scheduler job 3 last recorded a database-scheduler success at `2026-09-11 19:00:00+00`; scheduler success is not backup success.
- Latest verified successful global Drive backup audit remains `2026-09-10 19:00:12+00`, with successful upload/read-back integrity evidence recorded by prior reconciliation.
- The most recent direct backup-function response observed at `2026-09-12 07:23:12+00` returned HTTP 500 in phase `google-auth`: `Google OAuth refresh failed: Token has been expired or revoked.`
- Current read-only production state still contains one open recovery-health finding. Recovery remains red/fail-closed until a valid user-authorized refresh credential is supplied and a fresh upload plus digest read-back is verified.
- User-scoped encrypted Drive backup is separate from the global scheduler path and must not be treated as evidence that the global backup is healthy.
- No production restore, credential mutation or destructive backup action has been attempted by automation.

### Guardian

- Guardian inventory currently contains 28 incidents and 0 unresolved/open incidents.
- Guardian code-changing repairs remain human-approval gated.

### Nova / AI model comparison

- `nova-orchestrator` remains active with GPT/Gemini/Claude provider abstraction, automatic single-vs-ensemble routing, retry/circuit breaking, fusion, degraded fallback and per-request cost guardrails.
- `nova_ai_runs` currently contains 0 runs. No production quality/latency/reliability/cost baseline can be claimed yet; routing defaults must not be tuned from invented evidence.

### Catalogue / data quality

Read-only invariant sweep at reconciliation:

- Active catalogue records: 1777.
- Missing active model number: 235.
- Missing active image: 0.
- Audit queue: 708 pending / 70 blocked.
- `catalog_sync_state`: revision 269, last changed `2026-09-12 09:02:27+00`, source `device_catalog`, operation `UPDATE`.
- Duplicate active model-number groups: 16. Duplicate brand/model/year groups: 12. These remain triage signals only; they do not authorize merge/delete.
- Orphan active buy-price rows: 0.
- Negative active buy prices: 0.
- Download invite rows: 2 total; 0 currently active by expiry/redeemed/revoked criteria.
- Queue #926 / Fairphone 3+ now has a tier-1 manufacturer finding from Fairphone B.V.'s 26-Aug-2020 EU Declaration of Conformity. The document lists product descriptions `Fairphone 3, Fairphone 3+` and type designations `Fairphone 3, FP3`, supporting observed model identifier `FP3` with 0.99 confidence. Because applying the identifier would create a legitimate shared identifier across commercial variants and changes production catalogue data, the finding remains open and approval-gated; the catalogue row was not mutated.
- Missing facts remain verification backlog; never fill them by guessing.

## Active workstreams and priority scoring

Scoring scale: impact/confidence 1-5 higher is better; risk/effort/dependency risk 1-5 higher means more caution/cost. Priority does not authorize protected changes.

| Workstream | Evidence | Impact | Risk | Effort | Confidence | Dependency risk | Priority | Next safe action |
| --- | --- | ---: | ---: | ---: | ---: | ---: | --- | --- |
| Global backup recovery | one open recovery finding; invalid Google refresh token | 5 | 5 | 2 | 5 | 5 | Highest blocker | Keep fail-closed. After user-authorized credential replacement, verify a new upload and digest read-back before declaring recovery healthy. |
| Auth security hardening | Supabase advisor: leaked-password protection disabled; #1686 | 5 | 5 | 1 | 5 | 5 | High protected lane | Await explicit approval before changing Auth configuration; after approval, enable only leaked-password protection and run cross-surface auth regression plus advisor recheck. |
| Catalogue verification | 708 pending / 235 missing model numbers | 5 | 2 | 5 | 4 | 3 | High safe lane | Continue manufacturer-first verification; keep ambiguous/shared identifiers approval-gated and never guess. |
| Nova evaluation telemetry | 0 recorded orchestrator runs | 4 | 3 | 3 | 5 | 4 | High measurement lane | Build/use non-sensitive benchmark fixtures and collect routed results before changing routing defaults. |
| Admin invitation delivery | #1681 merged; 2 invite rows / 0 currently active | 4 | 3 | 2 | 4 | 4 | Monitor | Validate invite lifecycle/email contract and ensure expired/redeemed/revoked semantics stay fail-closed. |
| Admin stability/parity | latest material main includes #1681 | 4 | 3 | 2 | 4 | 4 | Monitor | Continue synthetic/static evidence for browser/native Admin boundaries; do not weaken auth/CAPTCHA/role gates. |
| Guardian | 28 total / 0 open | 4 | 4 | 1 | 5 | 4 | Monitor | Watch for new evidence-backed incidents; keep code-changing repairs approval-gated. |
| Release integrity | 2.15.100 live | 5 | 4 | 1 | 5 | 5 | Monitor | Preserve immutable release identity, exact checksum and monotonic next-version rules. |
| Durable continuity ledger | this file | 4 | 1 | 1 | 5 | 1 | Continuous | Reconcile from live evidence every run using material-state SHAs so the ledger does not invalidate itself when documentation merges. |

## Protected boundaries / approval requirements

- Release/OTA publication, signing/checksum identity, release asset replacement and deployment promotion remain approval-gated.
- Auth/authorization/RLS, secrets/credentials, privileged roles, destructive production data/schema actions, Guardian repair authority, repository/workflow security and protected pricing policy remain approval-gated.
- Enabling leaked-password protection is an Auth security configuration change and is tracked in #1686 for explicit approval.
- Google OAuth credential re-authorization/rotation is a credential boundary and cannot be completed autonomously.
- Production restore/overwrite and destructive backup operations remain approval-gated.
- Same-owner actions are not independent approval.

## Lightweight dependency map

- **Morley Buys Android** -> auth/session -> catalogue API/data + Supabase Realtime -> valuation/Test & Buy -> Device Lens/code scan/NFC -> signed release + OTA identity.
- **Morley website** -> shared catalogue/pricing contracts -> auth/session -> valuation/Nova-facing contracts -> static/deployment integrity.
- **Morley Admin** -> browser/native auth separation -> privileged roles -> support/governance -> invitation distribution -> catalogue/admin contracts -> Admin release/OTA.
- **Nova** -> verified catalogue/evidence -> external model provider routing -> operational telemetry -> protected action boundaries.
- **Guardian** -> runtime diagnostics + repository context + Nova engineering analysis -> human approval for code-changing repair.
- **Supabase** -> schema/RLS/auth -> catalogue revision/Realtime -> assessment/audit -> backup trigger and recovery metadata.
- **Release infrastructure** -> exact-next version identity -> CI/security/parity -> signed APK/checksum -> immutable release -> protected OTA manifest.
- **Global backup** -> pg_cron -> pg_net -> `google-drive-backup` -> OAuth refresh -> Drive upload -> SHA-256 read-back -> retention/audit. Scheduler success alone is not health.

## Data-integrity invariants

- Preserve legitimate regional/hardware/retail variants; carrier is not brand.
- Missing catalogue facts remain unverified rather than guessed.
- Duplicate model-number or brand/model/year groups are triage signals, not permission to merge records.
- Catalogue counts must not collapse/spike without explained source/import evidence.
- App/web/Admin/Nova views must not silently diverge from shared catalogue authority.
- Inventory state transitions and stock identifiers must remain internally consistent.
- Pricing/valuation logic must not bypass protected commercial approvals.
- Active buy prices must reference existing catalogue rows and must never be negative.
- Download invites must remain single-use/revocable/expiring and must not broaden Admin/Manager distribution authority.
- OTA versionCode/versionName/tag/APK URL/SHA-256/notes must describe the same signed artifact.
- Published release identities must never be reused for different APK bytes.
- Backup health is green only with fresh uploaded-content integrity/read-back evidence, not merely a successful scheduler row.

## Failure-pattern knowledge

### Scheduler succeeded but Drive backup failed

- **Symptom:** pg_cron reports `succeeded`/`1 row`, while the Edge Function can still fail later.
- **Current evidence:** latest observed function response reports `Google OAuth refresh failed: Token has been expired or revoked.`
- **Safe response:** keep recovery red/fail-closed; refresh through the approved user OAuth credential flow, then prove a new Drive upload and digest read-back. Never treat scheduler status as backup success.

### Privileged pull_request_target shell interpolation

- **Symptom:** PR-controlled filename data was expanded directly into a `run:` shell body in a workflow with `pull-requests: write`.
- **Root cause:** untrusted multiline step output was embedded in shell source instead of transported as inert data.
- **Fix:** #1678 / `c0f7f168...` transports the value through `env`, references a quoted shell variable and posts via body file while keeping least-privilege permissions and no PR checkout.
- **Regression:** dedicated workflow-security regression is part of Repository Security Audit and must remain authoritative.

### Commit-recursive operational ledger

- **Symptom:** writing the current main SHA into the same ledger that is then merged makes that SHA stale immediately.
- **Safe pattern:** record the last material runtime/Admin/release SHA and explicitly allow ledger-only documentation commits to follow it.

### Device Lens two-photo raw timeout

- **Root cause:** inspection path combined oversized encoded images, a short Android read timeout, medium reasoning, large response budget and no provider-side deadline, allowing the client socket to expire first.
- **Fix:** PR #1669 / 2.15.100 reduces payload, applies bounded provider/client windows, lowers reasoning/output budget, preserves high-detail images and returns structured retryable timeout behavior.
- **Regression:** keep `DeviceInspectionTimeoutPolicyTest` and existing auth/vision gates authoritative.

### Same release identity, different APK

- Preserve immutable release identity and advance version exactly once. Never weaken checksum/version guards.

### Protected release promotion

- Prepare exact verified metadata on a protected PR, require all applicable release gates plus explicit approval, then verify main/feed/checksum after merge. Never infer approval from the same repository-owner identity.

## Definition-of-done checkpoint

A work item is not Done until applicable implementation, tests, security/permission boundaries, dependency/contracts, parity, release/deployment evidence, regression coverage, performance/accessibility/degraded-mode considerations, rollback/recovery implications and this ledger are reconciled.

## Next safe actions

1. Keep global backup recovery fail-closed and surface the invalid/revoked Google refresh credential only when human action is required.
2. Keep #1686 approval-gated; do not alter Auth configuration until explicit approval is supplied.
3. Continue evidence-backed catalogue verification, especially 235 missing-model-number records and the pending audit queue, without destructive reconciliation; Fairphone #926 is now evidence-backed but remains approval-gated for application.
4. Establish Nova benchmark/telemetry evidence before changing provider-routing/cost defaults; current production telemetry is empty.
5. Continue Admin invitation lifecycle/browser/native regression monitoring and Guardian watch without weakening protected boundaries.
6. Preserve 2.15.100 release/OTA identity and require a new monotonic version for all future runtime releases.
