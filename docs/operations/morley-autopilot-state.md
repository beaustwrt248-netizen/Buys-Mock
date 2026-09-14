# Morley Ecosystem Autopilot State

Last reconciled: 2026-09-14 11:19 AWST
Source of truth: live GitHub/release evidence plus read-only connected Supabase evidence. Gumtree remains excluded unless Beau explicitly re-enables it.

## Current repository and release state

- `main`: `150e3113a7f61f743f7c21e233e0baba74d86427`.
- PR #2029 (`fix: announce Nova Next routes and password visibility`) was explicitly approved by Beau and squash-merged into `main` at `150e3113a7f61f743f7c21e233e0baba74d86427` after its exact-head repository security, parity, quality, Nova Next isolated validation and accessibility checks passed.
- Morley Buys production OTA metadata is **2.15.103 / versionCode 147** and points to `v2.15.103/B-and-L-Morley-2.15.103.apk` with SHA-256 `2f3ff1f3f0cd4f9a8ba4d35847a9a84dcc53f75d4a0be1fee5cd68069af8ecba`.
- Open PR #2032 (`Require staff condition confirmation and generic Vision guidance`) is the active Morley Vision safety/review lane. It advances Android source identity to **2.15.104 / versionCode 148**, keeps AI condition advisory, requires explicit staff condition confirmation, preserves original AI evidence and removes phone-specific default guidance for unknown categories. Do not merge until its exact-head required checks are complete and green.
- Draft PR #1947 remains the staged GitLab migration validation lane. GitHub remains the canonical production baseline until explicit migration cutover conditions are satisfied.

## Production-first triage

### Auth / Supabase security

- Connected production project `ghdhairijqjqivqriigi` remains reachable.
- Supabase security advisor at `2026-09-14T03:18:24Z` reports **Leaked Password Protection Disabled**. This remains a protected Auth configuration change and requires explicit approval before modification.
- The same advisor reports **13 authenticated-callable SECURITY DEFINER functions** and **17 RLS-enabled/no-policy tables**. These are tracked as security-hardening findings; no RLS, grants, function security mode, schema or production-data mutation is authorized by this ledger.
- Previous read-only review found the no-policy tables are service-role restricted and sampled privileged RPCs retain explicit application role checks. Keep validating before proposing any protected change.

### Guardian / Nova

- Guardian unresolved incidents: **0**.
- Nova knowledge: **4,715 active chunks / 4,708 active sources / 156 embeddings ready / 4,559 pending / 0 embedding errors**.
- Nova Next remains isolated from production promotion. PR #2029 improved route focus announcement and password-toggle accessibility state without changing production auth/RLS/provider routing or promotion boundaries.
- No provider/model-default change is justified without benchmark evidence and non-sensitive production telemetry.

### Catalogue / live sync / data integrity

- Active catalogue: **1,776 records**.
- Nova catalogue audit queue: **961 verified / 70 blocked / 1,082 pending**.
- Shared/reused model identifiers remain triage signals only. Never auto-merge or delete ambiguous device records; verify manufacturer-first and preserve legitimate regional/configuration variants.
- Carrier remains metadata, never the device brand. Tablets remain a separate category from mobiles.

### Backup / recovery

- Latest ready encrypted per-user Drive backup row: `2026-09-12 13:30:07+00`.
- Recovery health now has **1 open warning**: `stale_backup` / `Encrypted Google Drive backup is stale`, last observed `2026-09-14 03:17:00+00`.
- The broken credential-dependent full-system backup path remains paused. Do not replace OAuth credentials, perform production restore/overwrite or weaken recovery checks without explicit approval.
- Full recovery readiness still requires isolated non-destructive read-back/restore evidence; scheduler/table state alone is insufficient.

### CI / release / deployment

- `main` remains protected and all work must go through branches/PRs; never push directly to `main`.
- PR #2032 is currently waiting on exact-head GitHub Actions checks; continue independent safe lanes while CI is running.
- OTA/release completion requires monotonic version identity, exact artifact/checksum parity, installability and post-deploy evidence. A source version bump alone is not a release.

## Active impact / risk queue

| Workstream | Impact | Risk | Confidence | Next safe action |
| --- | ---: | ---: | ---: | --- |
| PR #2032 Morley Vision staff condition confirmation | 5 | 3 | 5 | Wait for exact-head checks; merge only if required checks are green and no protected boundary is triggered; release remains separate. |
| Backup freshness / recovery | 5 | 5 | 5 | Keep warning visible; gather non-destructive evidence only. Credential repair remains approval-gated. |
| Auth leaked-password protection | 5 | 5 | 5 | Prepare evidence; do not change Auth config without explicit approval. |
| SECURITY DEFINER / no-policy Supabase findings | 5 | 5 | 5 | Continue read-only call/grant/policy review; no RLS/grant/schema mutation without approval. |
| Catalogue verification | 5 | 2 | 5 | Continue manufacturer-first verification of pending/blocked rows and shared model identifiers; never guess facts. |
| Nova Next parity and knowledge quality | 4 | 2 | 5 | Continue isolated accessibility, degraded-mode, retrieval/evaluation and parity work without production promotion. |
| Admin web/mobile parity | 4 | 2 | 4 | Continue evidence-backed responsive/parity fixes that preserve Admin/Manager/Staff least privilege. |
| Login/session reliability | 5 | 3 | 5 | Keep regression coverage for success/failure/slow network/expired session/temp-password and fix only reproduced defects. |
| Valuation 3.0 / Test & Buy / inventory | 5 | 3 | 4 | Expand contract/e2e/degraded-mode coverage while preserving staff approval and pricing authority boundaries. |
| GitLab staged migration #1947 | 3 | 5 | 4 | Keep draft/unmerged until branch/tag/ref parity, protection, rollback and explicit cutover conditions are satisfied. |

## Dependency / contract map

- **Morley Buys Android** -> auth/session -> Device Lens/photos -> scan history -> diagnostics -> pricing/valuation -> Repair-or-Buy -> staff confirmation -> stock preparation -> signed APK/OTA.
- **Website** -> auth/session -> shared catalogue/pricing -> search/valuation/Nova -> deployment integrity.
- **Admin** -> auth/role boundaries -> support/governance -> catalogue/inventory controls -> Admin release.
- **Nova** -> verified evidence/catalogue -> knowledge store/retrieval -> provider routing -> evaluations/telemetry -> protected action boundaries.
- **Nova Next** -> isolated UI/runtime -> narrow auth/API adapters -> current capability contracts -> evidence/failure handling -> explicit future promotion gate.
- **Guardian** -> diagnostic evidence -> guarded repair proposal -> human approval for code-changing repair.
- **Supabase** -> Auth/RLS/RPC authorization -> catalogue/Realtime -> Nova knowledge -> assessment/history/backup metadata.
- **Release** -> monotonic source identity -> security/quality/parity -> signed artifact/checksum -> immutable release -> protected OTA metadata.
- **Backup** -> scheduler/client -> Google OAuth -> Drive upload -> digest/integrity evidence -> retention/audit -> isolated restore-readiness drill.

## Critical invariants

- Never invent device facts; unresolved identity/storage remains unresolved.
- Hardware tests are observed results or `not_tested`; never infer a pass.
- AI commercial output is advisory; staff explicitly confirms protected commercial decisions.
- Raw IMEI/serial/secret material must not leak into ordinary history, metadata, logs or external-AI payloads.
- App/web/Admin/Nova share catalogue and assessment contracts without silent divergence.
- Duplicate catalogue identifiers/groups are triage signals only.
- Inventory lifecycle and stock identifiers remain logically valid.
- Auth/session behavior preserves caller verification and role checks.
- Service-role credentials remain server-side only; new external providers may not receive sensitive production data without approval/security review.
- OTA versionCode/versionName/tag/APK URL/SHA-256 must describe the same signed bytes; published releases remain immutable.
- Backup health requires fresh integrity evidence and restore-readiness evidence; scheduler/table status alone is insufficient.
- Protected CI failures must not be silenced by weakening trust/destructive classifiers.
- Nova Next remains isolated until an explicit production promotion event satisfies parity, security, auth, release, rollback and human-approval gates.
- Gumtree work remains excluded.

## Protected boundaries

Auth/authorization/RLS, secrets/credentials, destructive schema/data, privileged roles, protected pricing approval policy, Guardian repair authority, GitHub workflow/repository security, release signing/checksum and OTA/deployment promotion require explicit approval. Same-owner actions are never treated as independent approval.

## Next safe actions

1. Recheck PR #2032 exact-head CI; if all required checks are green and no protected boundary applies, merge through repository protections. Do not call 2.15.104 released until signed artifact, checksum, OTA and install/post-release evidence exist.
2. Continue read-only security review of the 13 SECURITY DEFINER RPCs and 17 no-policy RLS tables; prepare a protected hardening plan rather than mutating production.
3. Keep the stale-backup warning active and collect non-destructive backup/recovery evidence; credential replacement remains approval-gated.
4. Continue catalogue verification and Nova evaluation/knowledge lanes while CI or protected work waits.
5. Keep #1947 draft/unmerged until GitLab parity/protection/rollback and explicit cutover conditions are satisfied.
