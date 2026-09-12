# Morley Ecosystem Autopilot State

Last reconciled: 2026-09-12 23:00 AWST
Source of truth: live GitHub/release evidence and connected-service evidence available to the automation. Gumtree remains excluded unless Beau explicitly re-enables it.

## Current repository and release state

- `main`: `24a6724d0d00b21bfc69a0406f847dd0febb4e2b` — PR #1701 merged successfully with Backup & Restore Center, durable AI Scan History, Repair-or-Buy and purpose-specific Universal Buy Search work.
- Android source candidate on main is 2.15.101 / versionCode 145, but production OTA remains deliberately pinned to published 2.15.100 / versionCode 144 until a separately validated release/promotion action is approved.
- Production OTA metadata: 2.15.100 / 144, APK SHA-256 `488cf42be87f54201266f9ea5e109911365677524e1f043beabcbf32cd16f175`.
- Protected draft PR #1740: fail-safe recovery-incident closure for Morley Ecosystem Autopilot. Required checks observed green; workflow-security change remains human-approval gated.
- Protected draft PR #1742: Guarded Auto Review classifier repair for false critical stops on negative/sanitizer-only privileged-key references. RED contract reproduced the defect; implementation is in exact-head verification. Workflow-security change remains human-approval gated.
- Open incident #1741 tracks the Guarded Auto Review false-red discovered after #1701. It is an automation reliability issue, not evidence of a production runtime outage.

## Production-first triage

### CI / automation reliability

- #1701 exact-head pre-merge security, quality, parity, Android regression/lint/APK, restore-capture and OTA-policy checks were green before the approved merge.
- Post-merge Morley Ecosystem Autopilot completed successfully on main.
- Guarded Auto Review run 34700870872 falsely classified #1701 as critical because safe test/sanitizer text contained a privileged key name. Root cause is verified; #1742 narrows text evaluation without expanding workflow permissions or weakening real destructive/trust-boundary stops.
- #1740 addresses a separate false-red where GitHub successfully closed a recovery incident before the CLI returned a GraphQL error. Its fix verifies final issue state and otherwise remains fail-closed.

### Auth / Supabase security

- Issue #1686 remains open for Supabase leaked-password protection. Auth configuration is protected and has not been changed automatically.
- Existing Auth/RLS/role boundaries remain authoritative. New protected schema/RLS/security changes require explicit approval.

### Guardian / Nova

- Guardian code-changing repairs remain human-approval gated.
- Nova provider/model routing changes require non-sensitive evaluation evidence for accuracy, reliability, source quality, latency and cost before defaults change.
- No provider secret changes or new sensitive-data sharing are authorized.

### Catalogue / data integrity

- Continue manufacturer-first verification, prioritising Australian official evidence.
- Never guess model numbers, dates, RAM/storage/SIM/chipset/specifications.
- Duplicate/conflicting device identifiers, orphaned media, lifecycle contradictions and sync divergence are detection signals only; ambiguous production records are not auto-merged/deleted/re-written.

### Backup / recovery

- Restore-point capture is now present and passing repository checks for applicable changes.
- Production overwrite/destructive restore remains approval-gated.
- Global Google Drive backup remains fail-closed until an authorized working OAuth refresh credential can prove a fresh upload and digest read-back; scheduler activity alone is not recovery evidence.

## Impact / risk queue

Scoring: impact/confidence 1-5 higher is better; risk/effort/dependency risk 1-5 higher means more caution/cost. Priority never authorizes protected work.

| Workstream | Impact | Risk | Effort | Confidence | Dependency risk | Next safe action |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| #1742 Guarded Auto Review false-positive repair | 5 | 5 | 2 | 5 | 5 | Finish exact-head checks; keep approval-gated because workflow-security behavior changes. |
| #1740 recovery-incident closure fail-safe | 4 | 5 | 2 | 5 | 4 | Preserve green evidence and request approval; do not merge autonomously. |
| Release/integration stabilization for 2.15.101 candidate | 5 | 5 | 3 | 5 | 5 | Keep OTA 2.15.100 unchanged; complete release-readiness evidence before any protected promotion. |
| Global backup recovery | 5 | 5 | 2 | 5 | 5 | Keep fail-closed; after authorized credential replacement, prove fresh upload + digest read-back. |
| Auth leaked-password protection #1686 | 5 | 5 | 1 | 5 | 5 | Await explicit approval, then rerun advisor and auth regressions. |
| Catalogue verification / data-quality scorecard | 5 | 2 | 5 | 5 | 3 | Continue read-only manufacturer-first verification and invariant detection. |
| Nova multi-model evaluation / routing | 4 | 3 | 3 | 4 | 4 | Gather non-sensitive benchmark, fallback, latency and cost evidence before changing defaults. |
| Login/mobile visual/accessibility regression | 4 | 2 | 3 | 4 | 3 | Continue representative mobile login/layout/accessibility checks without weakening auth. |
| Valuation 3.0 / Test & Buy / inventory lifecycle regression | 5 | 3 | 4 | 4 | 4 | Rotate contract/e2e coverage and repair only evidence-backed defects. |

## Dependency / contract map

- **Morley Buys Android** -> auth/session -> Device Lens/photos -> assessment evidence -> diagnostics -> pricing -> valuation/Repair-or-Buy -> explicit staff confirmation -> stock preparation -> signed APK/OTA.
- **Website** -> catalogue/pricing contracts -> auth/session -> search/valuation/Nova -> deployment integrity.
- **Admin** -> auth/role boundaries -> assessment/support/governance -> catalogue controls -> Admin release.
- **Nova** -> verified catalogue/evidence -> model/provider routing -> evaluation/telemetry -> protected action boundaries.
- **Guardian** -> diagnostics + repository/runtime evidence -> guarded repair proposal -> human approval for code-changing repair.
- **Supabase** -> Auth + RLS/RPC authorization -> assessment/history/restore metadata -> catalogue/Realtime -> backup metadata.
- **Release** -> monotonic source identity -> quality/security/parity -> signed artifact/checksum -> immutable release -> protected OTA metadata.
- **Backup** -> scheduler -> backend function -> Google OAuth -> Drive upload -> digest read-back -> retention/audit.

## Critical invariants

- Never invent device facts; unresolved identity/storage remains unresolved.
- Hardware tests are observed results or `not_tested`; never infer a pass.
- AI commercial output is advisory; staff explicitly confirms final grade, buy price, repair disposition and stock publication.
- Raw IMEI/serial/secret material must not leak into ordinary history, metadata, logs or AI audit payloads.
- App/web/Admin/Nova share catalogue and assessment contracts without silent divergence.
- Duplicate catalogue identifiers/groups are triage signals only.
- Inventory lifecycle and stock identifiers remain logically valid.
- Auth/session behavior preserves caller verification and role checks.
- OTA versionCode/versionName/tag/APK URL/SHA-256 describe the same signed bytes; published releases remain immutable.
- Backup health requires fresh integrity/read-back evidence, not scheduler success.
- Workflow classifiers may ignore verified negative/test/sanitizer references but must continue to stop real credential, destructive-data and trust-boundary changes.

## Failure-pattern knowledge

- **Guarded Auto Review false critical on safe security text:** global diff scanning matched negative assertions and sanitizer deny-lists. Proven direction: inspect added lines per file, distinguish explicit safe references, retain real critical patterns and protected path gating. Regression coverage is in #1742.
- **Recovery incident close succeeds but CLI errors afterward:** verify final issue state; tolerate only when the incident is demonstrably closed. Regression/fix is in #1740.
- **Routine review succeeds, merge arming fails:** integration may not be able to invoke merge; arming remains best-effort without permission expansion.
- **Privileged workflow shell interpolation:** transport PR-controlled values through inert environment values; retain regression coverage.
- **Device Lens two-photo timeout:** bounded image/provider/client windows plus retryable timeout handling shipped in 2.15.100.
- **Release parity failure on feature branch:** treat stale/equal live identity as a release-readiness signal; do not bump or publish merely to silence CI.
- **Backup scheduler can appear healthy while Drive write fails:** require verified upload + digest read-back.

## Protected boundaries

Auth/authorization/RLS, secrets/credentials, destructive schema/data, privileged roles, protected pricing policy, Guardian repair authority, GitHub workflow/repository security, release signing/checksum and OTA/deployment promotion require explicit approval. Same-owner actions are never treated as independent approval.

## Next safe actions

1. Finish #1742 exact-head security/quality/parity verification and leave it approval-gated.
2. Preserve #1740 green evidence and leave it approval-gated.
3. Keep production OTA on 2.15.100 while preparing complete 2.15.101 release-readiness evidence; do not promote automatically.
4. Continue read-only catalogue/data-quality and Nova evaluation lanes while protected workflow PRs wait.
5. Keep #1686 Auth hardening, Google OAuth credential replacement, destructive restore and any Guardian code-changing repair approval-gated.
6. Rotate login/accessibility, valuation/Test & Buy/inventory, live-sync and degraded-mode regressions and fix only evidence-backed defects.
