# Morley Ecosystem Autopilot State

Last reconciled: 2026-09-13 00:04 AWST
Source of truth: live GitHub/release evidence plus read-only connected Supabase evidence. Gumtree remains excluded unless Beau explicitly re-enables it.

## Current repository and release state

- `main`: `fb4f3712a625f76f5019cc5ec2a6219fccfa143f` — PR #1760 merged Android auth presentation onto shared Morley visual tokens and advanced the source candidate to 2.15.102 / versionCode 146.
- Production OTA is 2.15.101 / versionCode 145. `ota/latest.json` points to `v2.15.101/B-and-L-Morley-2.15.101.apk` with SHA-256 `2a79f4842dbfb82fd734d4669731016636fc2276d491e0a3a1096d7c3fa0a3c4`.
- Source and production identities intentionally differ: 2.15.102 is only a candidate on `main`; no 2.15.102 OTA promotion is claimed or authorized by this ledger update.
- Protected workflow fixes #1742 and #1755 are merged. #1742 narrows service-role false-positive handling while preserving real trust/destructive stops; #1755 verifies incident state after issue-close transport errors.
- Open PR #1764, `nova/hybrid-knowledge-foundation`, exact head `042b82a801c74ec32e9b85b49621a4735d135913`, is a high-risk Nova/Supabase knowledge-foundation change and remains explicit-approval gated.

## Production-first triage

### CI / automation reliability

- #1764 exact-head checks observed green for Repository Security Audit, B&L Morley Quality Gate, Morley Ultimate Parity Gate, Full Feature Contract Audit, Admin Control Integration Audit, Morley Restore Point Capture and Morley Email Contract.
- Nova PR Guard intentionally fails closed on #1764 because the change includes production Supabase migration/function trust-boundary work and real service-role use.
- Guarded Auto Review likewise intentionally stops the protected Nova change. Incidents #1765 and #1766 therefore represent an approval-gated protected stop, not evidence of a Morley production outage.
- Guarded Auto Review logs also match `TRUNCATE` text and a test-source service-role reference, but actual service-role use in `supabase/functions/nova-knowledge/index.ts` and the production migration independently justifies the critical stop. Do not weaken the classifier merely to make this PR green.

### Auth / Supabase security

- Connected production project `ghdhairijqjqivqriigi` is `ACTIVE_HEALTHY`.
- Fresh security advisor evidence still reports **Leaked Password Protection Disabled**. Issue #1686 remains protected and approval-gated; no Auth configuration was changed.
- Security advisor also reports 13 authenticated-callable `SECURITY DEFINER` RPCs and 12 RLS-enabled/no-policy tables. Existing reviewed authorization/fail-closed boundaries remain authoritative; no RLS/schema/grant change was made by this reconciliation.

### Guardian / Nova

- Guardian incidents: 28 total / 0 unresolved by current state classification. Guardian code-changing repairs remain human-approval gated.
- `nova_ai_runs`: 0. There is still no production routing/latency/cost dataset that justifies changing Nova provider/model defaults.
- #1764 adds hybrid semantic/lexical knowledge retrieval, chunking/ingestion and OpenRouter embeddings. Because it changes production schema, Edge Function behavior and external-AI data flow, approval/security review is required before production application.

### Catalogue / live sync / data integrity

- Active catalogue: 1,777 records.
- Active records missing model number: 235.
- Active records missing image: 0.
- Nova catalogue audit queue: 783 pending / 70 blocked.
- `catalog_sync_state`: revision 272, last change `2026-09-12 15:17:04+00`, source `device_catalog`, operation `UPDATE`.
- Missing facts remain unverified rather than guessed. Duplicate/conflicting identifiers remain triage signals only and never authorize destructive merge/delete.

### Backup / recovery

- Current `recovery_health_findings` contains no open finding rows.
- That does **not** establish global Google Drive backup health. The last verified global lane previously failed OAuth refresh; recovery remains unverified until a fresh Drive upload plus digest/read-back integrity check succeeds with authorized credentials.
- Production overwrite/destructive restore and credential replacement remain approval-gated.

### Synthetic / external health

- Repository CI and connected Supabase are directly observable this run. Public-site fetch tooling did not return an independently usable production-page response, so no new external web uptime claim is recorded.
- Synthetic failures remain triage signals; lack of external evidence must not be converted into a production-outage claim.

## Impact / risk queue

Scoring: impact/confidence 1-5 higher is better; risk/effort/dependency risk 1-5 higher means more caution/cost. Priority never authorizes protected work.

| Workstream | Impact | Risk | Effort | Confidence | Dependency risk | Next safe action |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| #1764 Nova hybrid knowledge foundation | 5 | 5 | 4 | 5 | 5 | Preserve exact-head green evidence; await explicit approval before any production migration/function deployment or merge. |
| 2.15.102 release readiness | 5 | 5 | 3 | 5 | 5 | Keep OTA on 2.15.101 until signed artifact/checksum, release gates, rollback and post-promotion evidence are complete. |
| Global Drive backup verification | 5 | 5 | 2 | 5 | 5 | Keep unverified/fail-closed until authorized fresh upload + digest read-back succeeds. |
| Auth leaked-password protection #1686 | 5 | 5 | 1 | 5 | 5 | Await explicit approval; then change only the supported Auth setting and rerun advisor/auth regressions. |
| Catalogue verification / data quality | 5 | 2 | 5 | 5 | 3 | Continue manufacturer-first read-only verification across 783 pending / 235 missing-model-number rows. |
| Nova evaluation telemetry | 4 | 3 | 3 | 5 | 4 | Build non-sensitive benchmark evidence before provider/routing/cost changes. |
| Login/mobile visual/accessibility regression | 4 | 2 | 3 | 5 | 3 | Continue regression coverage for the newly aligned Android auth presentation and mobile login work. |
| Valuation 3.0 / Test & Buy / inventory | 5 | 3 | 4 | 4 | 4 | Rotate contract/e2e/degraded-mode coverage and repair only evidence-backed defects. |

## Dependency / contract map

- **Morley Buys Android** -> auth/session -> Device Lens/photos -> scan history -> diagnostics -> pricing/valuation -> Repair-or-Buy -> staff confirmation -> stock preparation -> signed APK/OTA.
- **Website** -> auth/session -> shared catalogue/pricing -> search/valuation/Nova -> deployment integrity.
- **Admin** -> auth/role boundaries -> support/governance -> catalogue/inventory controls -> Admin release.
- **Nova** -> verified evidence/catalogue -> knowledge store/retrieval -> provider routing -> evaluations/telemetry -> protected action boundaries.
- **Guardian** -> diagnostic evidence -> guarded repair proposal -> human approval for code-changing repair.
- **Supabase** -> Auth/RLS/RPC authorization -> catalogue/Realtime -> Nova knowledge -> assessment/history/backup metadata.
- **Release** -> monotonic source identity -> security/quality/parity -> signed artifact/checksum -> immutable release -> protected OTA metadata.
- **Backup** -> scheduler/function -> Google OAuth -> Drive upload -> digest read-back -> retention/audit.

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
- Backup health requires fresh integrity/read-back evidence, not scheduler or table status alone.
- Protected CI failures must not be silenced by weakening trust/destructive classifiers.

## Failure-pattern knowledge

- **Safe service-role reference false positive:** fixed by #1742 for documentation/test-only references; real runtime/migration service-role use remains critical.
- **Protected PR creates failure incidents:** #1764 demonstrates expected fail-closed Nova Guard/Guarded Review behavior; classify it as a protected stop, not a production outage.
- **Recovery incident close succeeds but transport errors afterward:** #1755 verifies final issue state before tolerating the transport error.
- **Routine merge arming unavailable to integration:** keep least privilege; do not expand workflow authority just to clear a routine merge action.
- **Release candidate ahead of OTA:** treat source/OTA divergence as release-readiness state, not an excuse to auto-promote.
- **Backup scheduler can appear healthy while Drive write fails:** require verified upload + digest read-back.

## Protected boundaries

Auth/authorization/RLS, secrets/credentials, destructive schema/data, privileged roles, protected pricing policy, Guardian repair authority, GitHub workflow/repository security, release signing/checksum and OTA/deployment promotion require explicit approval. Same-owner actions are never treated as independent approval.

## Next safe actions

1. Keep #1764 unmerged/unapplied until explicit approval of this high-risk Nova/Supabase head; do not weaken the two intentional guard failures.
2. Preserve OTA 2.15.101 while validating the 2.15.102 candidate independently before any protected release promotion.
3. Continue read-only catalogue verification and Nova non-sensitive evaluation lanes while protected work waits.
4. Keep #1686, Google OAuth credential replacement, destructive restore and Guardian code-changing repair approval-gated.
5. Rotate login/accessibility, valuation/Test & Buy/inventory, live-sync, release and degraded-mode regressions; fix only evidence-backed defects.
