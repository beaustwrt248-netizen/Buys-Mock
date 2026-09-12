# Morley Ecosystem Autopilot State

Last reconciled: 2026-09-12 18:57 AWST
Source of truth: live GitHub, Supabase and connected service evidence. No Gumtree work is in scope.

## Current main and release state

- Last material release-state main SHA at reconciliation: `78d5411e7a14e03da7fb78425c6525852d1deb24`. Ledger-only documentation commits may follow it and do not change runtime/release state.
- Protected OTA PR #1674 was merged on 2026-09-12 at 10:47:28 UTC after external approval/promotion. It changed only `ota/latest.json`.
- Morley Buys 2.15.100 / versionCode 144 is now the advertised OTA release. `ota/latest.json` points to `v2.15.100/B-and-L-Morley-2.15.100.apk` with SHA-256 `488cf42be87f54201266f9ea5e109911365677524e1f043beabcbf32cd16f175` and release note `Fix Morley Device Lens analysis timeout (#1669)`.
- GitHub Release `v2.15.100` targets runtime source `d44beab010af2be7b4135dc2bb6f3d63e0b2296d`; its APK asset is 44,452,238 bytes and carries the same SHA-256 as the OTA manifest.
- The post-promotion `Morley Ecosystem Autopilot` workflow completed successfully on the OTA metadata commit. No runtime/release PR is currently open.
- PR #1669 remains the latest runtime release change: it reduces Device Lens inspection payload weight, applies bounded provider/client timeouts and preserves JWT/profile authorization plus high-detail damage evidence.
- Admin desktop stability repairs #1644 and #1647 remain merged. Android catalogue Realtime sync #1640 remains merged and superseded the one-second polling loop.
- Main remains protected. Automation must not push directly to main or bypass repository/release protections.

## Production-first triage

### Release / OTA

- 2.15.100 is fully promoted in source metadata: versionCode 144, versionName 2.15.100, release URL and SHA-256 agree with the published GitHub release asset.
- Release identity is immutable: published 2.15.100 bytes must not be replaced or reused for different content.
- Any future release must advance version identity and repeat exact-source build, security/parity/quality, signer/checksum, OTA monotonicity and post-promotion verification.

### Google Drive backup / recovery

- Global scheduler job 3 last recorded a database-scheduler success at `2026-09-11 19:00:00+00`; scheduler success is not backup success.
- Latest verified successful global Drive backup audit remains `2026-09-10 19:00:12+00`, with successful upload/read-back integrity evidence recorded by the prior reconciliation.
- The most recent direct backup-function response observed at `2026-09-12 07:23:12+00` returned HTTP 500 in phase `google-auth`: `Google OAuth refresh failed: Token has been expired or revoked.`
- No newer successful global upload/read-back evidence exists. Recovery therefore remains red/fail-closed until a valid user-authorized refresh credential is supplied and a fresh upload plus digest read-back is verified.
- User-scoped encrypted Drive backup is separate from the global scheduler path and must not be treated as evidence that the global backup is healthy.
- No production restore, credential mutation or destructive backup action has been attempted by automation.

### Guardian

- Guardian inventory currently contains 28 incidents and 0 unresolved/open incidents.
- Guardian code-changing repairs remain human-approval gated.

### Nova / AI model comparison

- `nova-orchestrator` remains active with GPT/Gemini/Claude provider abstraction, automatic single-vs-ensemble routing, retry/circuit breaking, fusion, degraded fallback and per-request cost guardrails.
- `nova_ai_runs` currently contains 0 runs. No production quality/latency/reliability/cost baseline can be claimed yet; routing defaults must not be tuned from invented evidence.

### Catalogue / data quality

- `device_catalog` currently has 1777 active records.
- Missing active model number: 235. Missing active image: 0.
- Audit queue currently has 709 pending and 69 blocked records after manufacturer-first review of two missing-model-number items.
- Queue #912 (realme P4 Lite 5G) and #923 (HUAWEI MateBook Pro S) were moved from pending to blocked because their official manufacturer product/specification pages confirm the devices but do not publish a safe model-number code. Evidence findings were recorded; no model number was guessed and no catalogue row was destructively changed.
- `catalog_sync_state` revision is 269, last changed at `2026-09-12 09:02:27+00`.
- Missing facts remain verification backlog; never fill them by guessing. Potential duplicate identifier groups remain triage signals, not permission for destructive merge/delete.

## Active workstreams and priority scoring

Scoring scale: impact/confidence 1-5 higher is better; risk/effort/dependency risk 1-5 higher means more caution/cost. Priority does not authorize protected changes.

| Workstream | Evidence | Impact | Risk | Effort | Confidence | Dependency risk | Priority | Next safe action |
| --- | --- | ---: | ---: | ---: | ---: | ---: | --- | --- |
| Global backup recovery | scheduler job 3 / `google-drive-backup` | 5 | 5 | 2 | 5 | 5 | Highest blocker | Keep fail-closed. A valid fresh Google refresh credential must be supplied through the approved user OAuth flow; then immediately verify one new upload and digest read-back. |
| Catalogue verification | 709 pending / 235 missing model numbers | 5 | 2 | 5 | 4 | 3 | High safe lane | Process evidence-backed Australian/manufacturer records; never guess or destructively reconcile ambiguity. |
| Nova evaluation telemetry | 0 recorded orchestrator runs | 4 | 3 | 3 | 5 | 4 | High measurement lane | Build/use non-sensitive benchmark fixtures and collect enough routed results to compare accuracy, latency, failure rate and cost before changing routing defaults. |
| Admin stability/parity | #1647 merged | 4 | 3 | 2 | 4 | 4 | Monitor | Continue synthetic/static evidence for desktop/mobile browser and native Admin boundaries; do not weaken auth/CAPTCHA/role gates. |
| Guardian | 28 total / 0 open | 4 | 4 | 1 | 5 | 4 | Monitor | Watch for new evidence-backed incidents; keep code-changing repairs approval-gated. |
| Release integrity | 2.15.100 live | 5 | 4 | 1 | 5 | 5 | Monitor | Preserve immutable release identity, exact checksum and monotonic next-version rules. |
| Durable continuity ledger | this file | 4 | 1 | 1 | 5 | 1 | Continuous | Reconcile from live evidence every run so stale release, branch or backup assumptions never drive work. |

## Protected boundaries / approval requirements

- Release/OTA publication, signing/checksum identity, release asset replacement and deployment promotion remain approval-gated.
- Auth/authorization/RLS, secrets/credentials, privileged roles, destructive production data/schema actions, Guardian repair authority, repository/workflow security and protected pricing policy remain approval-gated.
- Google OAuth credential re-authorization/rotation is a credential boundary and cannot be completed autonomously.
- Production restore/overwrite and destructive backup operations remain approval-gated.
- Same-owner actions are not independent approval.

## Lightweight dependency map

- **Morley Buys Android** -> auth/session -> catalogue API/data + Supabase Realtime -> valuation/Test & Buy -> Device Lens/code scan/NFC -> signed release + OTA identity.
- **Morley website** -> shared catalogue/pricing contracts -> auth/session -> valuation/Nova-facing contracts -> static/deployment integrity.
- **Morley Admin** -> browser/native auth separation -> privileged roles -> support/governance -> catalogue/admin contracts -> Admin release/OTA.
- **Nova** -> verified catalogue/evidence -> external model provider routing -> operational telemetry -> protected action boundaries.
- **Guardian** -> runtime diagnostics + repository context + Nova engineering analysis -> human approval for code-changing repair.
- **Supabase** -> schema/RLS/auth -> catalogue revision/Realtime -> assessment/audit -> backup trigger and recovery metadata.
- **Release infrastructure** -> exact-next version identity -> CI/security/parity -> signed APK/checksum -> immutable release -> protected OTA manifest.
- **Global backup** -> pg_cron -> pg_net -> `google-drive-backup` -> OAuth refresh -> Drive upload -> SHA-256 read-back -> retention/audit. Scheduler success alone is not health.

## Data-integrity invariants

- Preserve legitimate regional/hardware/retail variants; carrier is not brand.
- Missing catalogue facts remain unverified rather than guessed.
- Duplicate model-number groups are triage signals, not permission to merge records.
- Catalogue counts must not collapse/spike without explained source/import evidence.
- App/web/Admin/Nova views must not silently diverge from shared catalogue authority.
- Inventory state transitions and stock identifiers must remain internally consistent.
- Pricing/valuation logic must not bypass protected commercial approvals.
- OTA versionCode/versionName/tag/APK URL/SHA-256/notes must describe the same signed artifact.
- Published release identities must never be reused for different APK bytes.
- Backup health is green only with fresh uploaded-content integrity/read-back evidence, not merely a successful scheduler row.

## Failure-pattern knowledge

### Scheduler succeeded but Drive backup failed

- **Symptom:** pg_cron reports `succeeded`/`1 row`, while the Edge Function can still fail later.
- **Current evidence:** latest observed function response reports `Google OAuth refresh failed: Token has been expired or revoked.`
- **Safe response:** keep recovery red/fail-closed; refresh through the approved user OAuth credential flow, then prove a new Drive upload and digest read-back. Never treat scheduler status as backup success.

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

1. Keep global backup recovery fail-closed and surface the invalid/revoked Google refresh credential as the current required human action.
2. Continue evidence-backed catalogue verification, especially 235 missing-model-number records and the pending audit queue, without destructive reconciliation.
3. Establish Nova benchmark/telemetry evidence before changing provider-routing/cost defaults; current production telemetry is empty.
4. Continue Admin/browser/native synthetic/regression monitoring and Guardian watch without weakening protected boundaries.
5. Preserve 2.15.100 release/OTA identity and require a new monotonic version for all future runtime releases.
