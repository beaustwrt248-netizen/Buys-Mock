# Morley Ecosystem Autopilot State

Last reconciled: 2026-09-13 02:00 AWST
Source of truth: live GitHub/release evidence plus read-only connected Supabase and Google Drive evidence. Gumtree remains excluded unless Beau explicitly re-enables it.

## Current repository and release state

- `main`: `dc597dbc49ac739dd052f6751537817f64973a6f`.
- Morley Buys production OTA is **2.15.102 / versionCode 146**. Protected OTA PR #1767 merged as `1dda6cdb62f39a4fedf9fc3b0de8a83e5ee0b8a0` after the signed release artifact was published.
- `ota/latest.json` points to `v2.15.102/B-and-L-Morley-2.15.102.apk` with SHA-256 `69e1cd3cac7856584491445df07d0ffd711abcd7b74247ca10978103731648bf` and notes `Align Android auth with Morley blue theme (#1760)`.
- GitHub release `v2.15.102` exists for target `fb4f3712a625f76f5019cc5ec2a6219fccfa143f` and contains `B-and-L-Morley-2.15.102.apk` with the same SHA-256 digest.
- Protected workflow fixes #1742 and #1755 are merged. #1742 narrows documentation/test-only service-role false positives while preserving real trust/destructive stops; #1755 verifies final issue state after issue-close transport errors.
- Nova Next bootstrap PR #1772 merged as `b160ae668aecb8f4850589d43c94fdad2e7bf219`. It adds the isolated `nova-next/` successor surface, parity/security contracts and isolated PWA identity without changing current production Nova routing, production authentication wiring, Supabase schema/RLS, OTA/signing, pricing authority or Guardian authority.
- Nova Next degraded-mode PR #1776 merged as `dc597dbc49ac739dd052f6751537817f64973a6f`. It adds a 15-second default API timeout, AbortSignal propagation and explicit `REQUEST_TIMEOUT` evidence so stalled transports cannot leave indefinite waits.
- Open PR #1764, `nova/hybrid-knowledge-foundation`, exact head `45dffc7b996340e5dad9f2e15130443fb25eff84`, is a high-risk Nova/Supabase knowledge-foundation change and remains explicit-approval gated.

## Production-first triage

### CI / automation reliability

- Nova Next PR #1776 exact-head Repository Security Audit, B&L Morley Quality Gate, Morley Email Contract and Morley Ultimate Parity Gate all completed successfully before merge; Nova PR Guard was skipped as non-applicable.
- Nova Next PR #1772 exact-head parity and applicable contract checks completed successfully before merge; the PR was clean/mergeable and isolated from current production application paths.
- #1764 exact-head checks are green for Repository Security Audit, B&L Morley Quality Gate, Morley Ultimate Parity Gate, Full Feature Contract Audit, Admin Control Integration Audit, Morley Restore Point Capture and Morley Email Contract.
- Nova PR Guard intentionally fails closed on #1764 because the diff contains production Supabase migration/function trust-boundary work and real service-role use.
- Guarded Auto Review likewise intentionally stops the protected Nova change. Incident #1766 was reconciled as an expected protected stop and closed `not_planned`; it was not evidence of a Morley production outage.
- Do not weaken the classifier merely to make protected Nova/schema work green.

### Auth / Supabase security

- Connected production project `ghdhairijqjqivqriigi` is `ACTIVE_HEALTHY`.
- Fresh Supabase security-advisor evidence at 2026-09-13 00:21 AWST still reports **Leaked Password Protection Disabled**. Issue #1686 remains protected and approval-gated; no Auth configuration was changed.
- The same security advisor reports 13 authenticated-callable `SECURITY DEFINER` RPCs and 12 RLS-enabled/no-policy tables. Previous read-only review recorded internal role checks/service-only intent for these findings; no RLS/schema/grant change is authorized by this ledger.
- Fresh performance-advisor evidence reports 14 unindexed foreign keys, 8 RLS init-plan warnings and 54 currently-unused indexes. Issue #1770 tracks the performance findings. No schema/RLS/index mutation was made, and unused-index findings do not authorize removal.

### Guardian / Nova

- Guardian incidents remain classified with no unresolved production repair incident in the latest reconciled state. Guardian code-changing repairs remain human-approval gated.
- `nova_ai_runs` remains without a production routing/latency/cost dataset sufficient to justify provider/model-default changes.
- Nova Next is now present on `main` as an isolated successor bootstrap. It is not promoted over current Nova and its live production auth/backend adapters remain intentionally disconnected pending reviewed follow-up work.
- Nova Next API calls now have bounded failure behavior from #1776: stalled safe API transports are aborted at the configured timeout and surface explicit timeout evidence rather than waiting indefinitely.
- #1764 adds hybrid semantic/lexical knowledge retrieval, chunking/ingestion and OpenRouter embeddings. Because it changes production schema, Edge Function behavior and external-AI data flow, explicit approval/security review is required before merge or production application.

### Catalogue / live sync / data integrity

- Active catalogue: 1,777 records.
- Active records missing model number: 235.
- Active records missing image: 0.
- Nova catalogue audit queue: 783 pending / 70 blocked.
- `catalog_sync_state`: revision 272, last reconciled change `2026-09-12 15:17:04+00`, source `device_catalog`, operation `UPDATE`.
- Issue #1771 tracks 15 active shared brand/model-number groups spanning 34 rows. Shared identifiers remain verification targets rather than automatic duplicate findings.
- Missing facts remain unverified rather than guessed. Duplicate/conflicting identifiers remain triage signals only and never authorize destructive merge/delete.

### Backup / recovery

- Legacy shared `Morley Backups` Drive folder visibly contains periodic JSON snapshots through `2026-09-10T19:00:07Z`; absence of newer files in that legacy folder is not by itself proof that the encrypted per-user backup lane failed.
- Production `user_drive_backups` contains a fresh encrypted backup row created `2026-09-12 13:30:07+00`, status `ready`, AES-256-GCM, with a matching `backup_created` event at `2026-09-12 13:30:08+00` carrying `integrity_verified: true` and `client_state_included: true`.
- `recovery_health_findings` currently has 0 open rows; latest recovery signal was `2026-09-12 14:17:00+00`.
- A historical `backup_verified` event exists, but global restore readiness still requires an isolated non-destructive restore/read-back drill before claiming full recovery readiness. Production overwrite/destructive restore and credential replacement remain approval-gated.

### Synthetic / external health

- Repository CI, releases, OTA metadata, connected Supabase and connected Drive metadata are directly observable this reconciliation.
- Independent public-site fetch could not be established from the available external fetch path in the prior production triage, so no unsupported public-web uptime claim is carried forward. Lack of external evidence is not treated as an outage.

## Impact / risk queue

Scoring: impact/confidence 1-5 higher is better; risk/effort/dependency risk 1-5 higher means more caution/cost. Priority never authorizes protected work.

| Workstream | Impact | Risk | Effort | Confidence | Dependency risk | Next safe action |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| #1764 Nova hybrid knowledge foundation | 5 | 5 | 4 | 5 | 5 | Preserve exact-head green evidence; await explicit approval before merge/migration/function deployment. |
| 2.15.102 post-promotion verification | 5 | 3 | 2 | 5 | 4 | Keep OTA/release/source checksum identity under regression monitoring; investigate only evidence-backed post-release defects. |
| Backup/restore readiness | 5 | 4 | 2 | 5 | 5 | Preserve fresh encrypted-backup evidence; perform only isolated non-destructive restore/read-back validation when supported. |
| Auth leaked-password protection #1686 | 5 | 5 | 1 | 5 | 5 | Await explicit approval; then change only the supported Auth setting and rerun advisor/auth regressions. |
| Catalogue verification / data quality #1771 | 5 | 2 | 5 | 5 | 3 | Continue manufacturer-first read-only verification across shared identifiers, 783 pending audits and 235 missing-model-number rows. |
| Supabase performance #1770 | 4 | 5 | 3 | 5 | 5 | Continue read-only query/index analysis; prepare evidence but do not change schema/RLS/indexes without explicit approval. |
| Nova Next successor bootstrap follow-up | 4 | 2 | 3 | 5 | 3 | #1776 closed the indefinite-wait gap; next add narrow reviewed read-only/service adapters and parity/evidence checks while keeping production auth/promotion protected. |
| Nova evaluation telemetry | 4 | 3 | 3 | 5 | 4 | Build non-sensitive benchmark evidence before provider/routing/cost changes. |
| Login/mobile visual/accessibility regression | 4 | 2 | 3 | 5 | 3 | Continue regression coverage for Android auth/mobile login presentation and add Nova Next preview visual checks when a safe preview path exists. |
| Valuation 3.0 / Test & Buy / inventory | 5 | 3 | 4 | 4 | 4 | Rotate contract/e2e/degraded-mode coverage and repair only evidence-backed defects. |

## Dependency / contract map

- **Morley Buys Android** -> auth/session -> Device Lens/photos -> scan history -> diagnostics -> pricing/valuation -> Repair-or-Buy -> staff confirmation -> stock preparation -> signed APK/OTA.
- **Website** -> auth/session -> shared catalogue/pricing -> search/valuation/Nova -> deployment integrity.
- **Admin** -> auth/role boundaries -> support/governance -> catalogue/inventory controls -> Admin release.
- **Nova** -> verified evidence/catalogue -> knowledge store/retrieval -> provider routing -> evaluations/telemetry -> protected action boundaries.
- **Nova Next** -> isolated UI/runtime -> narrow auth/API adapters -> current capability contracts -> evidence/failure handling -> separate development identity -> explicit future promotion gate.
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
- Nova Next development identity and `/nova-next/` scope must remain isolated until an explicit production promotion event satisfies parity, security, auth, release, rollback and human-approval gates.

## Failure-pattern knowledge

- **Safe service-role reference false positive:** fixed by #1742 for documentation/test-only references; real runtime/migration service-role use remains critical.
- **Protected PR creates failure incidents:** #1764 demonstrates expected fail-closed Nova Guard/Guarded Review behavior. Incident #1766 was closed as `not_planned` after reconciliation confirmed the guard was functioning as designed.
- **Recovery incident close succeeds but transport errors afterward:** #1755 verifies final issue state before tolerating the transport error.
- **Unbounded safe API transport:** #1776 ensures Nova Next requests do not wait forever when a transport never resolves; adapters must preserve timeout/abort behavior and explicit `REQUEST_TIMEOUT` evidence.
- **Release-state race:** a protected OTA promotion can merge during ledger reconciliation; always re-read `main`, release and `ota/latest.json` immediately before recording production identity.
- **Release candidate ahead of OTA:** source/OTA divergence is release-readiness state only until protected promotion completes.
- **Legacy Drive folder lag is not definitive encrypted-backup failure:** reconcile Drive-visible artifacts with backup metadata/events and require integrity/read-back evidence.

## Protected boundaries

Auth/authorization/RLS, secrets/credentials, destructive schema/data, privileged roles, protected pricing policy, Guardian repair authority, GitHub workflow/repository security, release signing/checksum and OTA/deployment promotion require explicit approval. Same-owner actions are never treated as independent approval.

## Next safe actions

1. Keep #1764 unmerged/unapplied until explicit approval of this high-risk Nova/Supabase exact head; do not weaken intentional guard failures.
2. Continue Nova Next on fresh isolated branches with narrow read-only/service adapters, capability parity tests and visual/accessibility validation; preserve #1776 timeout/abort behavior and keep production auth, package/signing and promotion separately approval-gated.
3. Continue read-only catalogue verification for #1771 and Nova non-sensitive evaluation lanes while protected work waits.
4. Continue evidence gathering for #1770; do not change production schema/RLS/indexes without explicit approval.
5. Keep #1686, destructive restore, credential replacement and Guardian code-changing repair approval-gated.
6. Rotate login/accessibility, valuation/Test & Buy/inventory, live-sync, release and degraded-mode regressions; fix only evidence-backed defects.
