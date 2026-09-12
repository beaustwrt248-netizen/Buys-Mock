# Morley Ecosystem Autopilot State

Last reconciled: 2026-09-12 21:00 AWST
Source of truth: live GitHub, Supabase and connected-service evidence. Gumtree remains excluded unless Beau explicitly re-enables it.

## Material state

- Last material runtime/Admin main SHA: `921010ea57f43bdb37d66ab3ed66deb2ef03fdbd` (PR #1693). Ledger-only documentation commits may follow this SHA without changing runtime/release state.
- PR #1693 fixes the Admin temporary-password malformed-JWT failure by replacing target-user UUID calls to `supabase.auth.admin.signOut()` with a service-role-only session-revocation helper, while retaining caller JWT verification and regression/audit coverage. The PR records that the production migration and `admin-user-control` v31 were already applied before merge.
- No ordinary runtime PR is open. Protected workflow-security PR #1696 is open as draft and approval-gated.
- Morley Buys 2.15.100 / versionCode 144 remains the advertised OTA release. GitHub Release `v2.15.100` targets `d44beab010af2be7b4135dc2bb6f3d63e0b2296d`; APK `B-and-L-Morley-2.15.100.apk` is 44,452,238 bytes with SHA-256 `488cf42be87f54201266f9ea5e109911365677524e1f043beabcbf32cd16f175`.
- Main remains protected. Never push directly to main or bypass branch/release protections.

## Production-first triage

### GitHub / CI

- Latest sampled Morley Ecosystem Autopilot run on material main completed successfully after #1693 merged.
- Issue #1685 tracks repeated `Guarded Auto Review & Merge` failures. Verified logs show routine classification and approval succeed, then `gh pr merge --auto --squash` fails with `GraphQL: Resource not accessible by integration (mergePullRequest)` under the intentionally least-privilege workflow token (`contents: read`, `pull-requests: write`).
- Draft PR #1696 is the narrow protected correction: retain least privilege and all critical/guarded classifications, but make routine auto-merge arming best-effort so an integration limitation does not create a false workflow failure.
- #1696 used RED -> GREEN coverage: the new least-privilege merge regression failed before the workflow change, then passed after it. Exact head `4834b269013a48db43df6c2e77152c908f263016` is green for Repository Security Audit, B&L Morley Quality Gate, Morley Ultimate Parity Gate and Morley Email Contract; Nova PR Guard correctly skipped as non-Nova.
- #1696 remains explicit-approval gated because it changes GitHub workflow behavior.

### Auth / Supabase security

- Fresh Supabase advisor evidence at `2026-09-12 13:03:42+00` still reports **Leaked Password Protection Disabled**. Issue #1686 remains approval-gated; automation changed no Auth configuration.
- Advisor evidence also reports 13 authenticated-callable `SECURITY DEFINER` RPCs. Prior read-only inspection found protected Admin/inventory/Guardian mutation RPCs enforce enabled-profile Admin/Manager checks; `guardian_report_diagnostic` intentionally accepts authenticated callers while stripping sensitive metadata and conferring no repair authority.
- Twelve RLS-enabled/no-policy service tables remain fail-closed to ordinary Data API callers. No RLS/schema/role changes were made.

### Guardian / Nova

- Guardian: 28 incidents / 0 unresolved. Code-changing Guardian repair stays human-approval gated.
- `nova_ai_runs`: 0 production runs. Do not tune provider routing/cost defaults from nonexistent telemetry; collect non-sensitive benchmark evidence first.

### Catalogue / data integrity

- Active catalogue records: 1,777.
- Active records missing model number: 235.
- Audit queue: 783 pending / 70 blocked.
- Pending count rose by 75 from the previous 708 checkpoint: 72 `scheduled_recheck` plus 3 `missing_model_number` rows created at `2026-09-12 12:17+00`.
- Duplicate pending device IDs: 0, so the increase is explained recheck expansion rather than duplicate pending work.
- Missing facts remain unverified rather than guessed. Duplicate identifiers/groups remain triage signals only and never authorize merge/delete.
- Fairphone queue #926 remains evidence-backed for observed identifier `FP3`, but applying a shared identifier across commercial variants is production-data reconciliation and remains approval-gated.

### Backup / recovery

- Global Drive backup recovery remains fail-closed from the verified OAuth-refresh failure: `Google OAuth refresh failed: Token has been expired or revoked.` No credential mutation was attempted.
- Separate recovery-health evidence reports an encrypted Drive-backup path stale: `open`, occurrence count 84, last seen `2026-09-12 12:17:00+00`, recorded `last_backup_at` `2026-09-07T13:47:28.023974+00:00`, threshold 36 hours.
- User-scoped encrypted backup and the global scheduled backup are distinct. Neither can prove health for the other.
- Backup/recovery is green only after fresh uploaded-content integrity/read-back evidence; scheduler success alone is never backup success.

### Release integrity

- Latest GitHub release remains `v2.15.100`; published APK identity is unchanged from the approved OTA promotion.
- Preserve version monotonicity, signer/checksum identity and immutable published-artifact semantics.

## Impact / risk queue

Scoring: impact/confidence 1-5 higher is better; risk/effort/dependency risk 1-5 higher means more caution/cost. Priority never authorizes protected work.

| Workstream | Impact | Risk | Effort | Confidence | Dependency risk | Next safe action |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| Global backup recovery | 5 | 5 | 2 | 5 | 5 | Keep fail-closed; after user-authorized credential replacement, prove fresh upload + digest read-back. |
| Auth leaked-password protection #1686 | 5 | 5 | 1 | 5 | 5 | Await explicit approval; then change only leaked-password protection and rerun advisor + cross-surface auth regression. |
| Auto-review reliability #1685/#1696 | 4 | 5 | 1 | 5 | 4 | Await explicit approval for the fully green workflow PR. |
| Catalogue verification | 5 | 2 | 5 | 5 | 3 | Continue manufacturer-first verification across 783 pending / 235 missing-model-number rows; no guessing/destructive reconciliation. |
| Nova evaluation telemetry | 4 | 3 | 3 | 5 | 4 | Run non-sensitive evaluation fixtures before changing routing/cost defaults. |
| Admin password/session stability | 5 | 4 | 2 | 5 | 5 | Keep #1693 regression/audit coverage authoritative and watch for recurrence. |
| Guardian | 4 | 4 | 1 | 5 | 4 | Monitor for new evidence-backed incidents; keep repair approval boundary. |
| Release integrity | 5 | 4 | 1 | 5 | 5 | Preserve 2.15.100 identity until a separately validated monotonic release candidate exists. |

## Dependency / contract map

- **Morley Buys Android** -> auth/session -> catalogue + Realtime -> valuation/Test & Buy -> Device Lens/barcode/NFC -> signed APK + OTA.
- **Website** -> shared catalogue/pricing contracts -> auth/session -> valuation/Nova contracts -> deployment integrity.
- **Admin** -> browser/native auth separation -> privileged role checks -> session/user controls -> support/governance -> invites -> catalogue/Admin release.
- **Nova** -> verified catalogue/evidence -> provider routing -> evaluation/telemetry -> protected-action boundaries.
- **Guardian** -> diagnostics + repository context + Nova analysis -> human approval for code-changing repair.
- **Supabase** -> Auth + schema/RLS/RPC authorization -> catalogue revision/Realtime -> audit queues -> backup/recovery metadata.
- **GitHub automation** -> risk classification -> least-privilege review -> checks/protections -> permitted merge actor. Never expand workflow permissions solely to silence integration limits.
- **Release** -> monotonic version -> quality/security/parity -> signed artifact/checksum -> immutable release -> protected OTA metadata.
- **Global backup** -> scheduler -> Edge Function -> Google OAuth refresh -> Drive upload -> SHA-256 read-back -> audit/retention.

## Critical invariants

- Preserve legitimate regional/hardware/retail variants; carrier is not brand.
- Never invent device model numbers, release dates, storage/RAM/SIM/chipset/specifications.
- Duplicate catalogue identifiers/groups are triage signals, not permission to mutate production data.
- Audit work must not silently duplicate the same pending device; current duplicate pending-device count is 0.
- App/web/Admin/Nova must not silently diverge from shared catalogue authority.
- Inventory lifecycle/status/stock identifiers must remain logically valid.
- Pricing/valuation must not bypass protected commercial approval.
- Auth/session changes must preserve caller verification and role checks; a target UUID must never be treated as an access-token JWT.
- Invites remain single-use, expiring, revocable and role-bounded.
- OTA versionCode/versionName/tag/APK URL/SHA-256/notes must describe the same signed bytes; published release identities are immutable.
- Backup health requires fresh integrity/read-back evidence, not scheduler success.

## Failure-pattern knowledge

### Routine review succeeds, merge arming fails
- Verified cause: the least-privilege workflow token cannot invoke `mergePullRequest` in this repository context.
- Safe correction: #1696 makes the arming attempt best-effort while keeping `contents: read` + `pull-requests: write`; do not grant `contents: write` solely for automation convenience.

### Admin password reset malformed JWT
- Verified cause: target UUID was passed to `supabase.auth.admin.signOut()`, which expects an access-token JWT.
- Fix: #1693 uses service-role-only target-session revocation and retains caller JWT validation.

### Scheduler success but Drive backup failure
- Known cause: global OAuth refresh credential is expired/revoked.
- Safe response: remain red until authorized credential replacement plus fresh upload/digest read-back.

### Privileged `pull_request_target` shell interpolation
- Fix: #1678 / `c0f7f168...` transports PR-controlled values as inert env data and posts via body file; dedicated security regression remains authoritative.

### Device Lens two-photo timeout
- Fix: #1669 / 2.15.100 uses bounded image payload/provider/client windows and structured retryable timeout handling.

## Protected boundaries

Auth/authorization/RLS, secrets/credentials, destructive schema/data, privileged roles, protected pricing policy, Guardian repair authority, GitHub workflow/repository security, release signing/checksum and OTA/deployment promotion require explicit approval. #1686 and #1696 remain approval-gated. Google OAuth credential replacement is user-authorized. Production restore/overwrite is approval-gated. Same-owner actions are never independent approval.

## Next safe actions

1. Keep #1696 draft and approval-gated despite all required checks being green.
2. Keep #1686 Auth hardening approval-gated.
3. Keep global backup recovery fail-closed until credential replacement and verified upload/read-back.
4. Continue manufacturer-first catalogue verification across the expanded, non-duplicated queue.
5. Build Nova non-sensitive evaluation telemetry before any provider-routing/cost change.
6. Monitor #1693 Admin password/session behavior for recurrence.
7. Preserve 2.15.100 release identity until a new monotonic candidate is independently validated.
