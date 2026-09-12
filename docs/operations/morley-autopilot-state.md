# Morley Ecosystem Autopilot State

Last reconciled: 2026-09-12 16:58 AWST
Source of truth: live GitHub, Supabase and connected Google Drive evidence. No Gumtree work is in scope.

## Current main and release state

- Main SHA: `d44beab010af2be7b4135dc2bb6f3d63e0b2296d`.
- Latest merged runtime change: PR #1669, `Fix Morley Device Lens analysis timeout`.
- Morley Buys 2.15.99 / versionCode 143 Realtime catalogue sync is merged and published. GitHub Release `v2.15.99` targets `fe6954c6defaabe5558938e33a59f0277572963c`; APK SHA-256 is `21cc345e1929639ad207ab79d4cb9d10fb964b13eb3f410b9e56c66bbcf8bb94`. `ota/latest.json` on main still points to this exact 2.15.99 artifact.
- Morley Buys 2.15.100 / versionCode 144 is built and published from exact main `d44beab010af2be7b4135dc2bb6f3d63e0b2296d`; APK `B-and-L-Morley-2.15.100.apk` SHA-256 is `488cf42be87f54201266f9ea5e109911365677524e1f043beabcbf32cd16f175`.
- Protected OTA promotion for 2.15.100 is prepared as draft PR #1671. It changes only `ota/latest.json` and remains approval-gated; do not merge without explicit Beau approval even when checks are green.
- PR #1669 fixes the two-photo Device Lens timeout by reducing inspection payload weight, bounding provider/client timeouts and using latency-sensitive reasoning/output settings while preserving JWT/profile authorization and visual-damage evidence gates.
- Admin desktop stability repairs #1644 and #1647 are merged. #1647 makes the final desktop geometry authority load last and replaces repeated polling/broad mutation watching with a bounded readiness path.
- Main remains protected. Automation must not push directly to main or bypass repository/release protections.

## Production-first triage

### Release / OTA

- 2.15.99 is the currently advertised OTA release because `ota/latest.json` remains at versionCode 143.
- 2.15.100 release bytes exist and match exact main, but OTA promotion is not complete until protected PR #1671 is explicitly approved, merged and post-merge feed evidence is verified.
- PR #1671 exact head `2b09e6872372fc4b40cca533050b5b5caf7c041d` has already passed Repository Security Audit, B&L Morley Quality Gate, Admin OTA Release Safety, Admin Control Integration, Full Feature Contract Audit, Private Distribution Readiness, Morley Email Contract and Morley Ultimate Parity Gate. It remains draft solely because release promotion is protected.

### Google Drive backup / recovery

- Global scheduler job 3 ran successfully at the database-scheduler layer at `2026-09-11 19:00:00+00`, but scheduler success is not backup success.
- Latest verified successful global Drive backup audit is still `2026-09-10 19:00:12+00`, file ID `1PKNUAny6x8REcuojMqEXdMwMee8d4RVq`, with read-back recovery verification `verified=true`, 352981 bytes and SHA-256 `92fd4a50f60f2ff9c90a288a8ba11a4a2d2f7114abdaaffe6f3ba38feb9c4604`.
- The most recent direct backup-function response observed at `2026-09-12 07:23:12+00` returned HTTP 500 in phase `google-auth`: `Google OAuth refresh failed: Token has been expired or revoked.` This proves the currently stored global refresh credential is still not usable by the backup function.
- User-scoped encrypted Drive backup has separate evidence: a user backup was created on 2026-09-07 and `backup_verified` was recorded at `2026-09-11 18:16:47+00`. Do not confuse this with the global scheduler backup.
- Global recovery therefore remains fail-closed. No production restore, credential mutation or destructive backup action has been attempted by automation.

### Guardian

- Current Guardian incident inventory contains only resolved incidents: 14 low-risk, 12 medium-risk and 2 high-risk. No queued/proposed/open incident currently requires autonomous repair preparation.
- Guardian code-changing repairs remain human-approval gated.

### Nova / AI model comparison

- `nova-orchestrator` is active with GPT/Gemini/Claude provider abstraction, automatic single-vs-ensemble routing, retry/circuit breaking, fusion, degraded fallback and a per-request cost guardrail.
- `nova-ai-metrics` is active, but `nova_ai_runs` currently contains 0 runs, so there is not yet enough production telemetry to claim model-quality, latency, reliability or cost trends. Do not invent baselines; gather evidence before tuning routing.

### Catalogue / data quality

- `device_catalog` currently has 1777 active records.
- Required core identity/source coverage observed: missing brand 0, missing model name 0, missing source URL 0, missing image 0, missing release year/date 0.
- 235 active records have no model number. These remain verification backlog; never fill them by guessing.
- 15 brand+model-number groups contain more than one active row. Several are legitimate retail/regional/configuration variants, while some may be conflicting identities. Do not auto-merge/delete; verify manufacturer/regional evidence first.
- Audit queue: 711 pending, 67 blocked, 960 verified. Continue manufacturer-first verification and record uncertainty rather than fabricating facts.

## Active workstreams and priority scoring

Scoring scale: impact/confidence 1-5 higher is better; risk/effort/dependency risk 1-5 higher means more caution/cost. Priority does not authorize protected changes.

| Workstream | Evidence | Impact | Risk | Effort | Confidence | Dependency risk | Priority | Next safe action |
| --- | --- | ---: | ---: | ---: | ---: | ---: | --- | --- |
| Global backup recovery | scheduler job 3 / `google-drive-backup` | 5 | 5 | 2 | 5 | 5 | Highest blocker | Keep fail-closed. Beau must provide a genuinely valid fresh Google refresh credential through the approved OAuth flow; then immediately run and verify one new global upload/read-back audit. |
| 2.15.100 OTA promotion | release `v2.15.100`, draft #1671 | 5 | 5 | 1 | 5 | 5 | Approval-gated | Preserve draft and green exact-head evidence. Merge only after explicit Beau approval; then verify OTA manifest/feed points to exact release SHA-256. |
| Catalogue verification | 711 pending / 67 blocked | 5 | 2 | 5 | 4 | 3 | High safe lane | Process evidence-backed Australian/manufacturer records, prioritising missing model numbers and suspicious identifier collisions; never guess or destructively reconcile ambiguity. |
| Nova evaluation telemetry | 0 recorded orchestrator runs | 4 | 3 | 3 | 5 | 4 | High measurement lane | Build/use non-sensitive benchmark fixtures and collect enough routed model results to compare accuracy, latency, failure rate and cost before changing routing defaults. |
| Admin stability/parity | #1647 merged | 4 | 3 | 2 | 4 | 4 | Monitor | Continue synthetic/static evidence for desktop/mobile browser and native Admin boundaries; do not weaken auth/CAPTCHA/role gates. |
| Guardian | all current incidents resolved | 4 | 4 | 1 | 5 | 4 | Monitor | Watch for new evidence-backed incidents; keep code-changing repairs approval-gated. |
| Durable continuity ledger | this file | 4 | 1 | 1 | 5 | 1 | Continuous | Reconcile from live evidence every run so stale releases, branches or backup assumptions never drive work. |

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
- **Supabase** -> schema/RLS/auth -> catalogue revision/Realtimes -> assessment/audit -> backup trigger and recovery metadata.
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

- **Symptom:** pg_cron reports `succeeded`/`1 row`, while no new `google_drive_backup_created` audit appears.
- **Current evidence:** direct function response reports `Google OAuth refresh failed: Token has been expired or revoked.`
- **Safe response:** keep recovery red/fail-closed; refresh through the approved user OAuth credential flow, then prove a new Drive upload and digest read-back. Never treat scheduler status as backup success.

### Device Lens two-photo raw timeout

- **Root cause:** inspection path combined oversized encoded images, a 45-second Android read timeout, medium reasoning, 3200-token response budget and no provider-side deadline, allowing the client socket to expire first.
- **Fix:** PR #1669 / 2.15.100 reduces payload, applies bounded 60s provider and 75s client windows, lowers reasoning/output budget, preserves high-detail images and returns structured retryable timeout behavior.
- **Regression:** keep `DeviceInspectionTimeoutPolicyTest` and existing auth/vision gates authoritative.

### Same release identity, different APK

- Preserve immutable release identity and advance version exactly once. Never weaken checksum/version guards.

### Bot-authored protected OTA PR cannot start CI

- Recreate only the exact verified metadata from current main on an authorized owner branch; do not bypass checks or alter artifact identity.

## Definition-of-done checkpoint

A work item is not Done until applicable implementation, tests, security/permission boundaries, dependency/contracts, parity, release/deployment evidence, regression coverage, performance/accessibility/degraded-mode considerations, rollback/recovery implications and this ledger are reconciled.

## Next safe actions

1. Keep global backup recovery fail-closed and surface the invalid/revoked Google refresh credential as the current required human action.
2. Hold protected 2.15.100 OTA PR #1671 in draft until explicit release approval; preserve its already-green exact-head evidence.
3. Continue evidence-backed catalogue verification, especially 235 missing model-number records and 15 identifier-collision groups, without destructive reconciliation.
4. Establish Nova benchmark/telemetry evidence before changing provider-routing/cost defaults; current production telemetry is empty.
5. Continue Admin/browser/native synthetic/regression monitoring and Guardian watch without weakening protected boundaries.
