# Morley Ecosystem Autopilot State

Last reconciled: 2026-09-12 21:00 AWST
Source of truth: live GitHub, Supabase and connected-service evidence. Gumtree remains excluded unless Beau explicitly re-enables it.

## Current main and release state

- Current `main`: `921010ea57f43bdb37d66ab3ed66deb2ef03fdbd`.
- Latest material runtime/Admin change: PR #1693, **Fix Admin temporary password malformed JWT error**, merged as `921010ea57f43bdb37d66ab3ed66deb2ef03fdbd`.
- #1693 replaces target-user UUID calls to `supabase.auth.admin.signOut()` with a service-role-only session-revocation helper, preserves caller JWT verification, and adds regression/audit coverage. The PR records that the production migration and `admin-user-control` v31 were already applied before merge; source-of-truth now matches that production change.
- No ordinary runtime PR is open. Protected workflow-security PR #1696 is open as draft and approval-gated.
- Morley Buys 2.15.100 / versionCode 144 remains the advertised OTA release. GitHub Release `v2.15.100` targets runtime source `d44beab010af2be7b4135dc2bb6f3d63e0b2296d`; APK `B-and-L-Morley-2.15.100.apk` is 44,452,238 bytes with SHA-256 `488cf42be87f54201266f9ea5e109911365677524e1f043beabcbf32cd16f175`.
- Main remains protected. Automation must not push directly to main or bypass repository/release protections.

## Production-first triage

### GitHub / CI / workflow reliability

- Latest sampled Morley Ecosystem Autopilot run on current main completed successfully after #1693 merged.
- Issue #1685 tracks repeated `Guarded Auto Review & Merge` failures. Fresh job-log evidence shows the routine-review path succeeds, then `gh pr merge --auto --squash` fails with `GraphQL: Resource not accessible by integration (mergePullRequest)` under the workflow's intentionally least-privilege token (`contents: read`, `pull-requests: write`).
- Protected draft PR #1696 is the narrow proposed correction. It keeps the existing least-privilege token and critical/guarded classification, but makes routine auto-merge arming best-effort so an integration-permission limitation does not turn a successful routine review into a false workflow failure/incident.
- #1696 followed RED -> GREEN regression sequencing: the new least-privilege merge regression failed against the current workflow before the fix, then passed after the one-line fail-safe change. Repository Security Audit is green on exact head `4834b269013a48db43df6c2e77152c908f263016`; B&L Morley Quality Gate and Morley Email Contract are also green. Morley Ultimate Parity Gate was still running at this reconciliation checkpoint and must be green before approval/merge.
- #1696 is workflow-security work and therefore remains explicitly approval-gated even if all checks are green.

### Auth / Supabase security

- Fresh Supabase security-advisor evidence at `2026-09-12 13:03:42+00` still reports **Leaked Password Protection Disabled** for production Auth. Issue #1686 remains the approval-gated remediation lane; automation has not changed Auth configuration.
- The advisor also reports 13 authenticated-callable `SECURITY DEFINER` RPCs. Prior read-only definition inspection found protected Admin/inventory/Guardian mutation RPCs enforce internal enabled-profile Admin/Manager authorization checks; `guardian_report_diagnostic` intentionally accepts authenticated callers while stripping sensitive metadata and conferring no repair authority.
- Twelve RLS-enabled/no-policy service tables remain fail-closed to ordinary Data API callers; no RLS/schema/role changes were made.
- #1693 changes session-revocation implementation but does not authorize broader roles or bypass caller verification.

### Guardian

- Guardian incidents: 28 total / 0 unresolved.
- Guardian code-changing repair remains human-approval gated.

### Nova / AI model comparison

- `nova-orchestrator` provider abstraction and guarded multi-model routing remain in place.
- `nova_ai_runs`: 0 production runs. No quality/latency/reliability/cost routing change may be justified from nonexistent telemetry.
- Next safe measurement lane remains non-sensitive benchmark/evaluation fixtures before any routing-default changes.

### Catalogue / data quality

Fresh read-only invariant sweep:

- Active catalogue records: 1,777.
- Active records missing model number: 235.
- Audit queue: 783 pending / 70 blocked.
- The pending queue increased by 75 since the prior 708-pending checkpoint. The increase is explained by 72 `scheduled_recheck` entries and 3 `missing_model_number` entries created at `2026-09-12 12:17+00`.
- Duplicate pending device IDs: 0. The queue increase is therefore an explained recheck expansion rather than duplicate pending work.
- Existing duplicate canonical/model-number signals remain evidence-only and do not authorize destructive merge/delete actions.
- Manufacturer-first verification remains required. Missing facts stay unverified rather than guessed.
- Fairphone queue #926 remains evidence-backed for observed identifier `FP3`, but applying a shared identifier across commercial variants is still approval-gated production-data reconciliation.

### Backup / recovery

- Global Drive backup recovery remains fail-closed from the previously verified OAuth-refresh failure: `Google OAuth refresh failed: Token has been expired or revoked.` No credential mutation was attempted by automation.
- A separate current recovery-health finding reports the encrypted Drive-backup path as stale: status `open`, occurrence count 84, last seen `2026-09-12 12:17:00+00`, with recorded `last_backup_at` `2026-09-07T13:47:28.023974+00:00` and 36-hour threshold.
- User-scoped encrypted backup evidence and the global scheduled Drive-backup path are distinct and must not be used as proof for one another.
- Recovery is not green until the applicable path has fresh uploaded-content integrity/read-back evidence. Scheduler success alone is never backup success.

### Release / OTA

- Latest GitHub release remains `v2.15.100`; release asset identity is unchanged from the approved OTA promotion.
- Preserve version monotonicity, signing/checksum identity and immutable published-artifact semantics for the next release.

## Active workstreams and priority scoring

Scoring: impact/confidence 1-5 higher is better; risk/effort/dependency risk 1-5 higher means more caution/cost. Priority never overrides protected boundaries.

| Workstream | Impact | Risk | Effort | Confidence | Dependency risk | Priority | Next safe action |
| --- | ---: | ---: | ---: | ---: | ---: | --- | --- |
| Global backup recovery | 5 | 5 | 2 | 5 | 5 | Highest blocker | Keep fail-closed; after user-authorized credential replacement, prove fresh upload + digest read-back. |
| Auth leaked-password protection (#1686) | 5 | 5 | 1 | 5 | 5 | High protected | Await explicit approval; then change only leaked-password protection and rerun advisor + cross-surface auth regression. |
| Guarded Auto Review reliability (#1685/#1696) | 4 | 5 | 1 | 5 | 4 | High protected | Finish parity validation; request explicit approval before merging workflow change. |
| Catalogue verification | 5 | 2 | 5 | 5 | 3 | High safe | Continue manufacturer-first verification across 783 pending / 235 missing-model-number records; no guessing/destructive reconciliation. |
| Nova evaluation telemetry | 4 | 3 | 3 | 5 | 4 | High measurement | Run non-sensitive evaluation fixtures and collect routed evidence before tuning providers/cost defaults. |
| Admin temp-password/session stability | 5 | 4 | 2 | 5 | 5 | Monitor | Keep #1693 regression/audit coverage authoritative and watch for recurrence without broadening session authority. |
| Guardian | 4 | 4 | 1 | 5 | 4 | Monitor | Watch for new evidence-backed incidents; keep repair approval boundary. |
| Release integrity | 5 | 4 | 1 | 5 | 5 | Monitor | Keep 2.15.100 identity immutable; next runtime release must advance version exactly once. |
| Continuity ledger | 4 | 1 | 1 | 5 | 1 | Continuous | Reconcile material runtime SHA, live findings, blockers and next safe actions each run. |

## Lightweight dependency map

- **Morley Buys Android** -> auth/session -> catalogue + Supabase Realtime -> valuation/Test & Buy -> camera/Device Lens/barcode/NFC -> signed APK + OTA.
- **Morley website** -> shared catalogue/pricing contracts -> auth/session -> valuation/Nova-facing contracts -> static/deployment integrity.
- **Morley Admin** -> browser/native auth separation -> privileged role checks -> user/session controls -> support/governance -> invites -> catalogue/Admin contracts -> Admin release/OTA.
- **Nova** -> verified catalogue/evidence -> provider abstraction/routing -> evaluation/telemetry -> protected-action boundaries.
- **Guardian** -> runtime diagnostics + repository context + Nova analysis -> human approval for code-changing repair.
- **Supabase** -> Auth + schema/RLS/RPC authorization -> catalogue revision/Realtime -> audit queues -> backup/recovery metadata.
- **GitHub automation** -> PR risk classification -> least-privilege review -> repository checks/protections -> permitted merge actor; workflow token limitations must not be bypassed by permission expansion without approval.
- **Release infrastructure** -> monotonic version -> CI/security/parity -> signed artifact/checksum -> immutable release -> protected OTA metadata.
- **Global backup** -> scheduler -> Edge Function -> Google OAuth refresh -> Drive upload -> SHA-256 read-back -> audit/retention. Scheduler execution alone is not health.

## Data-integrity invariants

- Preserve legitimate regional/hardware/retail variants; carrier is not brand.
- Never invent model numbers, release dates, storage/RAM/SIM/chipset/specification facts.
- Duplicate identifiers/groups are triage signals, not permission to merge/delete production records.
- Pending audit work must not silently duplicate the same device; current duplicate pending-device count is 0.
- Catalogue counts must not collapse/spike without explained source/import/recheck evidence.
- App/web/Admin/Nova must not silently diverge from shared catalogue authority.
- Inventory lifecycle/status/stock identifiers must remain logically valid.
- Pricing/valuation logic must not bypass protected commercial approval.
- Auth/session changes must preserve caller verification and role authorization; target identifiers must never be treated as access-token JWTs.
- Download invitations remain single-use, expiring, revocable and role-bounded.
- OTA versionCode/versionName/tag/APK URL/SHA-256/notes must identify the same signed bytes.
- Published release identities must never be reused for different APK bytes.
- Backup health requires fresh integrity/read-back evidence, not scheduler/job success alone.

## Failure-pattern knowledge

### Routine auto-review succeeds but merge arming fails

- **Symptom:** `Guarded Auto Review & Merge` classifies a PR as routine and successfully submits the routine approval, then fails at `gh pr merge --auto --squash`.
- **Verified cause:** GitHub reports `Resource not accessible by integration (mergePullRequest)` under the intentionally least-privilege workflow token.
- **Safe correction:** #1696 keeps `contents: read` + `pull-requests: write` and makes merge arming best-effort; do not grant `contents: write` solely to silence the failure.
- **Boundary:** workflow-security changes remain explicit-approval gated.

### Admin password reset malformed JWT

- **Symptom:** password update succeeds and the control path then reports `token contains an invalid number of segments`.
- **Verified cause:** target user UUID was passed to `supabase.auth.admin.signOut()`, which expects an access-token JWT.
- **Fix:** #1693 uses service-role-only target-session revocation and retains caller JWT validation; source now records production migration/function v31 state.
- **Regression:** Admin integration/audit assertions prevent a target UUID from being passed to JWT sign-out again.

### Scheduler success but Drive backup failure

- **Symptom:** scheduler/trigger reports success while downstream backup can still fail.
- **Known cause:** global OAuth refresh credential is expired/revoked.
- **Safe response:** keep recovery red until authorized credential replacement plus fresh Drive upload/digest read-back.

### Privileged `pull_request_target` shell interpolation

- **Root cause:** PR-controlled filename output was previously embedded directly into privileged shell source.
- **Fix:** #1678 / `c0f7f168...` transports it as inert environment data, quotes shell consumption and uses a body file, with dedicated security regression coverage.

### Device Lens two-photo timeout

- **Root cause:** oversized encoded images plus short client timeout/reasoning/output budget allowed socket timeout first.
- **Fix:** #1669 / 2.15.100 uses bounded payload/provider/client windows and structured retryable timeout handling.

## Protected boundaries / approval requirements

- Auth/authorization/RLS, secrets/credentials, destructive schema/data, privileged roles, protected pricing policy, Guardian repair authority, GitHub workflow/repository security, release signing/checksum and OTA/deployment promotion remain explicit-approval gated.
- #1686 leaked-password protection is Auth configuration and remains approval-gated.
- #1696 changes GitHub workflow behavior and remains approval-gated even though it does not expand permissions.
- Google OAuth refresh credential replacement remains a user-authorized credential action.
- Production restore/overwrite remains approval-gated.
- Same-owner actions are never independent approval.

## Definition-of-done checkpoint

Work is not Done until applicable implementation, regression tests, security/permission boundaries, contracts/dependencies, app/web/Admin parity, release/deployment evidence, accessibility/performance/degraded-mode considerations, rollback/recovery implications and this continuity state are reconciled.

## Next safe actions

1. Finish #1696 parity validation; if green, keep it draft and request explicit approval for the protected workflow change.
2. Keep #1686 Auth hardening approval-gated and do not change production Auth without approval.
3. Keep global backup recovery fail-closed until user-authorized credential replacement and verified new upload/read-back.
4. Continue manufacturer-first catalogue verification across the expanded but non-duplicated audit queue.
5. Establish Nova evaluation telemetry with non-sensitive fixtures before changing routing/cost defaults.
6. Monitor #1693 Admin password/session behavior for recurrence and preserve caller/session/role boundaries.
7. Preserve Morley 2.15.100 release identity until a separately validated monotonic release candidate exists.
